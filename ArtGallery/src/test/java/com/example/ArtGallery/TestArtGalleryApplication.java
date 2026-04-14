package com.example.ArtGallery;

import org.springframework.boot.SpringApplication;

public class TestArtGalleryApplication {

	public static void main(String[] args) {
		SpringApplication.from(ArtGalleryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
