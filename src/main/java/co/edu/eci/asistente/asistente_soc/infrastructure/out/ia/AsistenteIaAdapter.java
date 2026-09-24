package co.edu.eci.asistente.asistente_soc.infrastructure.out.ia;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.AsistenteIaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AsistenteIaAdapter implements AsistenteIaPort {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIaAdapter.class);

    private static final String SYSTEM_PROMPT = """
            Eres un asistente de clasificacion y contencion de incidentes de seguridad de la
            informacion para un SOC. SOLO puedes proponer acciones respaldadas por los
            fragmentos de procedimientos internos que se te entregan como contexto. Si el
            contexto no cubre el caso, dilo en la justificacion en vez de inventar un
            procedimiento.

            Si la descripcion del incidente no trae datos suficientes para clasificar con
            confianza (falta el sistema afectado, el usuario, o el tipo de actividad
            observada), responde con informacionInsuficiente=true y una preguntaSeguimiento
            concreta, dejando tipo="INFORMACION_INSUFICIENTE".

            Las acciones de contencion NO deben ser genericas (p. ej. "bloquear IP"): deben
            ser comandos ejecutables u operaciones especificas aplicables a una arquitectura
            orientada a servicios. Para cada accion indica el comando concreto, el sistema o
            servicio objetivo y el parametro exacto (IP, usuario, host, puerto).

            Responde EXCLUSIVAMENTE con un JSON valido, sin texto adicional, con esta forma:
            {
              "tipo": "PHISHING|ACCESO_NO_AUTORIZADO|MALWARE|FUGA_DE_INFORMACION|DENEGACION_DE_SERVICIO|INFORMACION_INSUFICIENTE|OTRO",
              "severidad": "BAJA|MEDIA|ALTA|CRITICA",
              "justificacion": "string",
              "accionesRecomendadas": [
                {"accion": "BLOQUEAR_IP", "objetivo": "203.0.113.9", "comando": "iptables -A INPUT -s 203.0.113.9 -j DROP"}
              ],
              "comandoEjecucion": "comando principal ejecutable o null",
              "sistemaAfectado": "sistema o servicio objetivo o null",
              "ipBloqueada": "IP a bloquear o null",
              "informacionInsuficiente": false,
              "preguntaSeguimiento": null
            }
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            INCIDENTE REPORTADO
            Descripcion: %s
            Sistema/origen: %s
            Usuario afectado: %s
            IP afectada: %s
            Impacto declarado: %s

            PROCEDIMIENTOS INTERNOS RELEVANTES (recuperados por busqueda semantica)
            %s
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${rag.top-k:4}")
    private int topK;

    public AsistenteIaAdapter(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.vectorStore = vectorStore;
    }

    @Override
    public void analizarYProponerContencion(Incidente incidente) {
        List<Document> fragmentosRelevantes = buscarProcedimientosRelevantes(incidente.getDescripcion());
        String contexto = formatearContexto(fragmentosRelevantes);

        String userPrompt = USER_PROMPT_TEMPLATE.formatted(
                incidente.getDescripcion(),
                nullSafe(incidente.getSistema()),
                nullSafe(incidente.getUsuario()),
                nullSafe(incidente.getIp()),
                nullSafe(incidente.getImpacto()),
                contexto
        );

        RespuestaLlm respuesta;
        try {
            // Structured Outputs: Spring AI genera el JSON schema desde RespuestaLlm y
            // obliga al modelo a devolver exactamente esa forma (sin parseo manual).
            respuesta = chatClient.prompt().user(userPrompt).call().entity(RespuestaLlm.class);
        } catch (Exception e) {
            log.error("Fallo la llamada al LLM para el incidente {}", incidente.getId(), e);
            incidente.marcarParaAprobacion(
                    "OTRO", "MEDIA",
                    "No se pudo contactar al modelo de lenguaje: " + e.getMessage()
                            + ". Requiere clasificacion manual."
            );
            return;
        }

        aplicarRespuestaDelModelo(incidente, respuesta);
    }

    private List<Document> buscarProcedimientosRelevantes(String descripcion) {
        SearchRequest request = SearchRequest.builder()
                .query(descripcion)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(request);
    }

    private String formatearContexto(List<Document> fragmentos) {
        if (fragmentos == null || fragmentos.isEmpty()) {
            return "(No se encontraron procedimientos relevantes en la base de conocimiento)";
        }
        return fragmentos.stream()
                .map(doc -> "- [%s] %s".formatted(
                        doc.getMetadata().getOrDefault("source", "desconocido"),
                        doc.getText()))
                .collect(Collectors.joining("\n"));
    }

    private void aplicarRespuestaDelModelo(Incidente incidente, RespuestaLlm respuesta) {
        if (respuesta == null) {
            incidente.marcarParaAprobacion(
                    "OTRO", "MEDIA",
                    "El modelo no devolvio una respuesta interpretable. Revisar manualmente."
            );
            return;
        }

        if (Boolean.TRUE.equals(respuesta.informacionInsuficiente())) {
            incidente.marcarParaAprobacion(
                    "INFORMACION_INSUFICIENTE",
                    "BAJA",
                    "Se requiere mas informacion: " + respuesta.preguntaSeguimiento()
            );
            return;
        }

        incidente.marcarParaAprobacion(
                respuesta.tipo(),
                respuesta.severidad(),
                construirTextoAccion(respuesta),
                respuesta.comandoEjecucion(),
                respuesta.sistemaAfectado(),
                respuesta.ipBloqueada()
        );
    }

    private String construirTextoAccion(RespuestaLlm respuesta) {
        StringBuilder sb = new StringBuilder();
        sb.append(respuesta.justificacion()).append("\n\nAcciones recomendadas:\n");
        List<RespuestaLlm.AccionContencion> acciones = respuesta.accionesRecomendadas();
        if (acciones == null || acciones.isEmpty()) {
            sb.append("- (el modelo no propuso acciones concretas, revisar manualmente)");
        } else {
            for (RespuestaLlm.AccionContencion accion : acciones) {
                sb.append("- [").append(accion.accion()).append("]");
                if (accion.objetivo() != null && !accion.objetivo().isBlank()) {
                    sb.append(" ").append(accion.objetivo());
                }
                if (accion.comando() != null && !accion.comando().isBlank()) {
                    sb.append(" -> ").append(accion.comando());
                }
                sb.append("\n");
            }
        }
        if (respuesta.comandoEjecucion() != null && !respuesta.comandoEjecucion().isBlank()) {
            sb.append("\nComando principal: ").append(respuesta.comandoEjecucion()).append("\n");
        }
        return sb.toString().trim();
    }

    private String nullSafe(String value) {
        return (value == null || value.isBlank()) ? "no especificado" : value;
    }
}