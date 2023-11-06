package com.powsybl.ws_common_spring_test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simple Spring boot application doing nothing because need to test if autoconfiguration from
 * another base-package work.
 */
@SuppressWarnings({
    "java:S2187" //this isn't a class containing tests
})
@SpringBootApplication
public class SpringBootApplicationForTest {
}
