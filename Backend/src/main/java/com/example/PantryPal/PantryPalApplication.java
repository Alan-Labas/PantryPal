package com.example.PantryPal;

import com.example.PantryPal.service.KonzolaService;
import com.example.PantryPal.service.ReceptParserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Scanner;

@SpringBootApplication
public class PantryPalApplication {
    public static void main(String[] args) {
        SpringApplication.run(PantryPalApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "pantrypal.console.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner zagon(KonzolaService konzolaService, ReceptParserService parser) {
        return args -> {
            // Neposredni zagon ostaja za avtomatizacijo in predstavitev naloge 2.
            if (Arrays.asList(args).contains("--parser")) {
                int stevilo = 100;
                for (String argument : args) {
                    if (argument.startsWith("--stevilo=")) {
                        stevilo = Integer.parseInt(argument.substring("--stevilo=".length()));
                    }
                }
                parser.zajemiRecepte(stevilo);
                return;
            }
            konzolaService.zagon(new Scanner(System.in));
        };
    }
}
