package com.facelogin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = FaceLoginApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FaceLoginApplicationTests {

    @Test
    void contextLoads() {
        // Test that the Spring Boot context loads correctly
    }

}
