package web.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = { "web.service" })
@SpringBootApplication
public class ServingLayerConfiguration {

	public static void main(String[] args) {
		SpringApplication.run(ServingLayerConfiguration.class, args);
	}
	
}

//popular BD, como apicarlo a nuestra seccion