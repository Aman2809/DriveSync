package com.project.cloudsync.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @GetMapping
    public Map<String, Object> handleError(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", request.getAttribute("javax.servlet.error.status_code"));
        response.put("message", "An error occurred during authentication. Please try again.");
        return response;
    }
}
