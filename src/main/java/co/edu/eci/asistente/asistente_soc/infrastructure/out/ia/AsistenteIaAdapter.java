package co.edu.eci.asistente.asistente_soc.infrastructure.out.ia;

import co.edu.eci.asistente.asistente_soc.domain.model.Incidente;
import co.edu.eci.asistente.asistente_soc.domain.ports.out.AsistenteIaPort;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            Responde EXCLUSIVAMENTE con un JSON valido, sin texto adicional, con esta forma:
            {
              "tipo": "PHISHING|ACCESO_NO_AUTORIZADO|MALWARE|FUGA_DE_INFORMACION|DENEGACION_DE_SERVICIO|INFORMACION_INSUFICIENTE|OTRO",
              "severidad": "BAJA|MEDIA|ALTA|CRITICA",
              "justificacion": "string",
              "accionesRecomendadas": ["string"],
              "informacionInsuficiente": true|false,
              "preguntaSeguimiento": "string o null"
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
    private final ObjectMapper objectMapper;

    @Value("${rag.top-k:4}")
    private int topK;

    public AsistenteIaAdapter(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.vectorStore = vectorStore;
        this.objectMapper = new ObjectMapper();
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

        String rawJson;
        try {
            rawJson = chatClient.prompt().user(userPrompt).call().content();
        } catch (Exception e) {
            log.error("Fallo la llamada al LLM para el incidente {}", incidente.getId(), e);
            incidente.marcarParaAprobacion(
                    "OTRO", "MEDIA",
                    "No se pudo contactar al modelo de lenguaje: " + e.getMessage()
                            + ". Requiere clasificacion manual."
            );
            return;
        }

        aplicarRespuestaDelModelo(incidente, rawJson);
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

    private void aplicarRespuestaDelModelo(Incidente incidente, String rawJson) {
        try {
            RespuestaLlm respuesta = objectMapper.readValue(rawJson, RespuestaLlm.class);

            if (Boolean.TRUE.equals(respuesta.informacionInsuficiente())) {
                incidente.marcarParaAprobacion(
                        "INFORMACION_INSUFICIENTE",
                        "BAJA",
                        "Se requiere mas informacion: " + respuesta.preguntaSeguimiento()
                );
                return;
            }

            String accionPropuesta = construirTextoAccion(respuesta);
            incidente.marcarParaAprobacion(respuesta.tipo(), respuesta.severidad(), accionPropuesta);
        } catch (Exception e) {
            log.error("Respuesta del LLM no tuvo el formato JSON esperado para el incidente {}: {}",
                    incidente.getId(), rawJson, e);
            // No inventamos una clasificacion no confiable: se deja explicito para revision humana.
            incidente.marcarParaAprobacion(
                    "OTRO", "MEDIA",
                    "La respuesta del modelo no se pudo interpretar de forma confiable. Revisar manualmente."
            );
        }
    }

    private String construirTextoAccion(RespuestaLlm respuesta) {
        StringBuilder sb = new StringBuilder();
        sb.append(respuesta.justificacion()).append("\n\nAcciones recomendadas:\n");
        List<String> acciones = respuesta.accionesRecomendadas();
        if (acciones == null || acciones.isEmpty()) {
            sb.append("- (el modelo no propuso acciones concretas, revisar manualmente)");
        } else {
            for (String accion : acciones) {
                sb.append("- ").append(accion).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String nullSafe(String value) {
        return (value == null || value.isBlank()) ? "no especificado" : value;
    }

    private record RespuestaLlm(
            String tipo,
            String severidad,
            String justificacion,
            List<String> accionesRecomendadas,
            Boolean informacionInsuficiente,
            String preguntaSeguimiento
    ) {
    }
}
