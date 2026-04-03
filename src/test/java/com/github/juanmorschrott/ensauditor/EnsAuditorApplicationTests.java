package com.github.juanmorschrott.ensauditor;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class EnsAuditorApplicationTests {

	@Test
    void verifyModulithArchitecture() {
        ApplicationModules.of(EnsAuditorApplication.class).verify();
    }
}
