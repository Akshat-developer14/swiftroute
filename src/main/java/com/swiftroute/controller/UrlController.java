package com.swiftroute.controller;

import com.swiftroute.dto.request.CreateUrlRequest;
import com.swiftroute.dto.response.UrlResponse;
import com.swiftroute.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UrlController {

    @PostMapping("/urls")
    public ResponseEntity<String> createUrl(
//            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal User user
            ) {
        return ResponseEntity.ok("Url created");
    }
}
