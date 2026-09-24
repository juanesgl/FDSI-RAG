package co.edu.eci.asistente.asistente_soc.infrastructure.out.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ManualIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ManualIngestionRunner.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Value("${rag.manuals-location}")
    private String manualsLocation;

    public ManualIngestionRunner(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            Integer existentes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
            if (existentes != null && existentes > 0) {
                log.info("El vector store ya tiene {} fragmentos ingeridos, se omite la ingesta de manuales.", existentes);
                return;
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar el vector store ({}). Se intenta la ingesta de manuales.", e.getMessage());
        }

        try {
            ingestarManuales();
        } catch (Exception e) {
            log.error("Fallo la ingesta de manuales al vector store. El RAG respondera sin contexto.", e);
        }
    }

    private void ingestarManuales() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(manualsLocation);

        if (resources.length == 0) {
            log.warn("No se encontraron manuales en {}. El RAG respondera sin contexto hasta que se agreguen.", manualsLocation);
            return;
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> todosLosFragmentos = new ArrayList<>();

        for (Resource resource : resources) {
            TextReader reader = new TextReader(resource);
            reader.getCustomMetadata().put("source", resource.getFilename());
            List<Document> documentos = reader.get();
            todosLosFragmentos.addAll(splitter.apply(documentos));
        }

        log.info("Ingestando {} fragmentos de {} manuales al vector store...", todosLosFragmentos.size(), resources.length);
        vectorStore.add(todosLosFragmentos);
        log.info("Ingesta de manuales completada.");
    }
}
