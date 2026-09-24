package co.edu.eci.asistente.asistente_soc;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * H2 (usado en test/resources/application.yaml) no soporta pgvector, y no queremos que el
 * pipeline de CI dependa de una API key real. Por eso se excluyen los autoconfigure de
 * OpenAI/PgVector en el perfil de test y se mockean aqui los beans que el modulo RAG
 * (AsistenteIaAdapter, ManualIngestionRunner) necesita para poder inyectarse.
 */
@SpringBootTest
@Import(AsistenteSocApplicationTests.MockConfig.class)
class AsistenteSocApplicationTests {

	@TestConfiguration
	static class MockConfig {

		@Bean
		ChatClient.Builder chatClientBuilder() {
			ChatClient.Builder builder = mock(ChatClient.Builder.class);
			when(builder.defaultSystem(anyString())).thenReturn(builder);
			when(builder.build()).thenReturn(mock(ChatClient.class));
			return builder;
		}

		@Bean
		VectorStore vectorStore() {
			return mock(VectorStore.class);
		}
	}

	@Test
	void contextLoads() {
	}

}