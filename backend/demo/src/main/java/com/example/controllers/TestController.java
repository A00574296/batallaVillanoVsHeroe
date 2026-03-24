package com.example.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.services.FirebaseService;

@RestController
public class TestController {

    private final FirebaseService firebaseService;

    public TestController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping("/test-fb")
    public String test() {
        return firebaseService.guardarDato();
    }
}
