package com.aimong.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(loadCredentials())
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (StringUtils.hasText(firebaseProperties.getServiceAccountPath())) {
            try (InputStream inputStream = Files.newInputStream(Path.of(firebaseProperties.getServiceAccountPath()))) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        return GoogleCredentials.getApplicationDefault();
    }
}
