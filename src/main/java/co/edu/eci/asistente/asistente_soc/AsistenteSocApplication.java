package co.edu.eci.asistente.asistente_soc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AsistenteSocApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsistenteSocApplication.class, args);
	}

}
