package com.givinghands.givinghands;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GivinghandsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GivinghandsApplication.class, args);
	}

}
