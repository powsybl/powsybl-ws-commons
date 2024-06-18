/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.service;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeDeserializer;
import com.powsybl.commons.report.ReportNodeJsonModule;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author Mathieu Deharbe <mathieu.deharbe_externe at rte-france.com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class ReportServiceTest {

    private static final UUID REPORT_UUID = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
    private static final UUID REPORT_ERROR_UUID = UUID.fromString("9928181c-7977-4592-ba19-88027e4254e4");

    private static final String REPORT_JSON = "{\"version\":\"2.0\",\"reportRoot\":{\"messageKey\":\"test\",\"dictionaries\":{\"default\":{\"test\":\"a test\"}}}}";

    private MockWebServer server;

    private ReportService reportService;

    @Before
    public void setUp() throws IOException {
        var objectMapper = Jackson2ObjectMapperBuilder.json().build();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new ReportNodeJsonModule());
        objectMapper.setInjectableValues(new InjectableValues.Std().addValue(ReportNodeDeserializer.DICTIONARY_VALUE_ID, null));

        reportService = new ReportService(objectMapper,
                initMockWebServer(),
                new RestTemplate());
    }

    @After
    public void tearDown() {
        try {
            server.shutdown();
        } catch (Exception e) {
            // Nothing to do
        }
    }

    private String initMockWebServer() throws IOException {
        server = new MockWebServer();
        server.start();

        final Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String requestPath = Objects.requireNonNull(request.getPath());
                if (requestPath.equals(String.format("/v1/reports/%s", REPORT_UUID))) {
                    assertEquals(REPORT_JSON, request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                } else if (requestPath.equals(String.format("/v1/reports/%s?reportTypeFilter=MockReportType&errorOnReportNotFound=false", REPORT_UUID))) {
                    assertEquals("", request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                } else if (requestPath.equals(String.format("/v1/reports/%s", REPORT_ERROR_UUID))) {
                    return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                } else {
                    return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()).setBody("Path not supported: " + request.getPath());
                }
            }
        };

        server.setDispatcher(dispatcher);

        // Ask the server for its URL. You'll need this to make HTTP requests.
        HttpUrl baseHttpUrl = server.url("");
        return baseHttpUrl.toString().substring(0, baseHttpUrl.toString().length() - 1);
    }

    @Test
    public void testSendReport() {
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("test", "a test").build();
        reportService.sendReport(REPORT_UUID, reportNode);
        assertThrows(RestClientException.class, () -> reportService.sendReport(REPORT_ERROR_UUID, reportNode));
    }

    @Test
    public void testDeleteReport() {
        reportService.deleteReport(REPORT_UUID, "MockReportType");
        assertThrows(RestClientException.class, () -> reportService.deleteReport(REPORT_ERROR_UUID, "MockReportType"));
    }
}

