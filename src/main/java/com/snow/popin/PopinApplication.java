package com.snow.popin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PopinApplication {

	public static void main(String[] args) {
		SpringApplication.run(PopinApplication.class, args);
	}

}
