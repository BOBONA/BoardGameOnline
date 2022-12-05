package com.bobona.bgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class BgoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BgoApplication.class, args);
	}
}
