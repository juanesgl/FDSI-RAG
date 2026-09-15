package co.edu.eci.asistente.asistente_soc;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * H2 (usado en test/resources/application.yaml) no soporta pgvector, y no queremos que el
 * pipeline de CI dependa de una API key real. Por eso se excluyen los autoconfigure de
 * OpenAI/PgVector en el perfil de test y se mockean aqui los beans que el modulo RAG
 * (AsistenteIaAdapter, ManualIngestionRunner) necesita para poder inyectarse.
 */
@SpringBootTest
class AsistenteSocApplicationTests {

	@MockitoBean
	private ChatClient.Builder chatClientBuilder;

	@MockitoBean
	private VectorStore vectorStore;

	@Test
	void contextLoads() {
	}

}
