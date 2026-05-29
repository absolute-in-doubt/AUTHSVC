package com.innowise.authservice.infrastructure.security.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import org.junit.jupiter.api.Test;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;


import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    {

        RSAKey rsaKey = null;
        try {
            rsaKey = new RSAKeyGenerator(2048)
                    .keyID("key-1")
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

        System.out.println(rsaKey.toJSONString());
    }

    @Test
    void createJwt() {
    }

    @Test
    void decode() {
    }

    @Test
    void isTokenValid() {
    }

    @Test
    void convert() {
    }
}