package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.example.api")
@ComponentScan({"com.example.api", "com.example.queuing", "com.example.service"})
public class Info7255Application {

	public static void main(String[] args) {
		SpringApplication.run(Info7255Application.class, args);
	}

}
