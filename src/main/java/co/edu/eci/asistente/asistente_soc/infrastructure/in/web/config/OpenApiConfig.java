package co.edu.eci.asistente.asistente_soc.infrastructure.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI asistenteSocOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Asistente SOC - API de Alertas")
                        .description("API para inyectar alertas de seguridad (reales o simuladas) y "
                                + "consultar el resultado del triaje automatico con IA.")
                        .version("v1"));
    }
}
