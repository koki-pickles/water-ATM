package ke.wateratm;

import org.springframework.boot.SpringApplication;

public class TestWaterAtmBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(WaterAtmBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
