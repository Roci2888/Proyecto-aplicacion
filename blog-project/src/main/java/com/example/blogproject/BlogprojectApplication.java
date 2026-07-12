package com.example.blogproject;

import com.example.blogproject.infrastructure.moderation.SightengineService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class BlogprojectApplication implements CommandLineRunner {

	private final SightengineService sightengineService;

	public static void main(String[] args) {
		SpringApplication.run(BlogprojectApplication.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			// Test con una imagen segura
			String urlSegura = "https://sightengine.com/assets/img/examples/example7.jpg";
			boolean esSegura = sightengineService.moderateAndVerify(urlSegura);
			System.out.println("¿La imagen es segura? " + (esSegura ? "Sí" : "No"));

			// Test con una imagen que podría ser inapropiada (ejemplo)
			// String urlInsegura = "https://ejemplo.com/imagen-inapropiada.jpg";
			// boolean esSegura2 = sightengineService.moderateAndVerify(urlInsegura);
			// System.out.println("¿La imagen es segura? " + (esSegura2 ? "Sí" : "No"));

		} catch (Exception e) {
			System.err.println("Error en el test: " + e.getMessage());
		}
	}
}
