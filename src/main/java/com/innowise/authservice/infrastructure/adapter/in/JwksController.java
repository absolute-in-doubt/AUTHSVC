package com.innowise.authservice.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;

import java.util.Map;

@RestController
@RequestMapping("/.well-known")
public class JwksController {

    private final JWKSet jwkSet;

    public JwksController(JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }

    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwkSet.toJSONObject();
    }
}