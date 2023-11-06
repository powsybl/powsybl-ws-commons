package com.powsybl.ws_common_spring_test;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {
    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPath() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Hello world!\"}");
    }
}
