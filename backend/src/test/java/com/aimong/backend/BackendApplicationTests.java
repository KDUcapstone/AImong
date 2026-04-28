package com.aimong.backend;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.flyway.enabled=false")
class BackendApplicationTests {

	@MockitoBean
	FirebaseApp firebaseApp;

	@MockitoBean
	FirebaseAuth firebaseAuth;

	@Test
	void contextLoads() {
	}

}
