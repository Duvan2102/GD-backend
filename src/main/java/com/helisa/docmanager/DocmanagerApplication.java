package com.helisa.docmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocmanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocmanagerApplication.class, args);
	}

}
