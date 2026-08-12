package com.capstone.travelbusan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TravelbusanApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelbusanApplication.class, args);
	}

}
