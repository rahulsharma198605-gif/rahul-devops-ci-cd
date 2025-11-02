package com.example.webapp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppControllerTest {
    @Test
    void hello_returnsExpectedMessage() {
        AppController controller = new AppController();
        assertEquals("Hello CI/CD World from DevOps Training 3!", controller.hello());
    }
}
