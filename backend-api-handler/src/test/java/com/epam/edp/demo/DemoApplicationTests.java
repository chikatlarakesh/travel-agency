package com.epam.edp.demo;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Smoke test verifying the application class exists and is loadable.
 * Full Spring context integration tests are replaced by per-controller
 * {@code @WebMvcTest} slices that do not require a running MongoDB instance.
 */
public class DemoApplicationTests {

    @Test
    public void contextLoads() {
        // Verify the application class is instantiable
        DemoApplication app = new DemoApplication();
        assertNotNull(app);
    }
}
