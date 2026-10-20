package com.example.PantryPal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "pantrypal.console.enabled=false")
class PantryPalApplicationTests {

	@Test
	void contextLoads() {
	}

}
