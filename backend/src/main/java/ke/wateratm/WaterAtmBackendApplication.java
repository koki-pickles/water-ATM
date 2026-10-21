package ke.wateratm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WaterAtmBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WaterAtmBackendApplication.class, args);
	}

}
