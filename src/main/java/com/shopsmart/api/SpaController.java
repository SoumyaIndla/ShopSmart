package com.shopsmart.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * The React app is served from the Spring Boot static resources in production.
 * These routes forward to index.html so the SPA can handle /admin itself.
 */
@Controller
public class SpaController {

    @GetMapping({"/admin", "/admin/"})
    public String admin() {
        return "forward:/index.html";
    }
}
