package com.epam.edp.demo.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pavlo_Yemelianov
 */
@Hidden
@RestController
public class HelloEdpController {

    @GetMapping(value = "/api/hello")
    public String hello() {
        return "Hello, EDP!";
    }
}
