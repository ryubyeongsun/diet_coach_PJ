package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<String> health() {
        return ApiResponse.ok("dietcoach-backend is up 🚀", "UP");
    }
}
