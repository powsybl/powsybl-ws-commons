/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * Shared {@link ProblemDetail} subclass that carries typed metadata used across Powsybl services.
 */
@Getter
@JsonIgnoreProperties({"instance", "type"})
@EqualsAndHashCode(callSuper = true)
public final class PowsyblWsProblemDetail extends ProblemDetail {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private String server;
    private String businessErrorCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private String path;
    private String traceId;
    private final List<ChainEntry> chain;

    /**
     * Custom map that must be rendered as a nested JSON object: "businessErrorValues": { ... }
     * We intentionally do NOT use the ProblemDetail internal properties map for JSON,
     * because it is expanded at top level via ProblemDetailJacksonMixin (@JsonAnyGetter),
     * therefore, it's content cannot be deserialized systematically
     */
    private final Map<String, Object> businessErrorValues = new LinkedHashMap<>();

    @JsonCreator
    public PowsyblWsProblemDetail(
        @JsonProperty("title") String title,
        @JsonProperty("status") Integer status,
        @JsonProperty("detail") String detail,
        @JsonProperty("server") String server,
        @JsonProperty("businessErrorCode") String businessErrorCode,
        @JsonProperty("businessErrorValues") Map<String, Object> businessErrorValues,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("path") String path,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("chain") List<ChainEntry> chain
    ) {
        super(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR.value());
        setTitle(title);
        setDetail(detail);
        if (businessErrorValues != null) {
            this.businessErrorValues.putAll(businessErrorValues);
        }
        this.server = server;
        this.businessErrorCode = businessErrorCode;
        this.timestamp = timestamp;
        this.path = path;
        this.traceId = traceId;
        this.chain = chain != null ? new ArrayList<>(chain) : new ArrayList<>();
    }

    public PowsyblWsProblemDetail(ProblemDetail problemDetail) {
        super(problemDetail);
        this.timestamp = Instant.now();
        this.chain = new ArrayList<>();
    }

    private PowsyblWsProblemDetail(@NonNull HttpStatusCode status) {
        super(status.value());
        this.chain = new ArrayList<>();
    }

    private PowsyblWsProblemDetail() {
        super();
        this.chain = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(HttpStatusCode status) {
        return new Builder(status);
    }

    public static Builder builder(ProblemDetail problemDetail) {
        return new Builder(problemDetail);
    }

    public static PowsyblWsProblemDetail fromBytes(byte[] bytes) throws IOException {
        return OBJECT_MAPPER.readValue(bytes, PowsyblWsProblemDetail.class);
    }

    public static PowsyblWsProblemDetail fromException(Exception exception, String serverName) {
        if (exception instanceof AbstractBusinessException businessException) {
            return PowsyblWsProblemDetail.builder()
                .server(serverName)
                .businessErrorCode(businessException.getBusinessErrorCode().value())
                .businessErrorValues(businessException.getBusinessErrorValues())
                .detail(businessException.getMessage())
                .build();
        }
        if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
            PowsyblWsProblemDetail problemDetail;
            try {
                byte[] body = httpStatusCodeException.getResponseBodyAsByteArray();
                problemDetail = fromBytes(body);
            } catch (Exception ignored) {
                problemDetail = PowsyblWsProblemDetail.builder(httpStatusCodeException.getStatusCode())
                    .server(serverName)
                    .detail(exception.getMessage())
                    .build();
            }
            problemDetail.wrap(serverName);
            return problemDetail;
        }
        if (exception instanceof ErrorResponse errorResponse) {
            PowsyblWsProblemDetail problemDetail = PowsyblWsProblemDetail.builder(errorResponse.getBody())
                .server(serverName)
                .build();
            problemDetail.wrap(serverName);
            return problemDetail;
        }
        return PowsyblWsProblemDetail.builder()
            .server(serverName)
            .detail(exception.getMessage())
            .build();
    }

    @NonNull
    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void wrap(String fromServer, String method, String path) {
        String toServer = chain.isEmpty() ? server : chain.getFirst().fromServer();
        var newChainEntry = new ChainEntry(fromServer, toServer, method, path, Instant.now());
        chain.addFirst(newChainEntry);
    }

    public void wrap(String fromServer) {
        String toServer = chain.isEmpty() ? server : chain.getFirst().fromServer();
        var newChainEntry = new ChainEntry(fromServer, toServer, Instant.now());
        chain.addFirst(newChainEntry);
    }

    @Getter
    public static final class Builder {
        private final PowsyblWsProblemDetail target;

        private Builder() {
            this.target = new PowsyblWsProblemDetail();
        }

        private Builder(HttpStatusCode status) {
            this.target = new PowsyblWsProblemDetail(Objects.requireNonNull(status, "status"));
        }

        private Builder(ProblemDetail problemDetail) {
            this.target = new PowsyblWsProblemDetail(problemDetail);
        }

        public Builder detail(String detail) {
            target.setDetail(detail);
            return this;
        }

        public Builder server(String server) {
            target.server = server;
            return this;
        }

        public Builder businessErrorCode(String businessErrorCode) {
            target.businessErrorCode = businessErrorCode;
            return this;
        }

        public Builder businessErrorValues(Map<String, Object> businessErrorValues) {
            target.businessErrorValues.clear();
            if (businessErrorValues != null) {
                target.businessErrorValues.putAll(businessErrorValues);
            }
            return this;
        }

        public Builder path(String path) {
            target.path = path;
            return this;
        }

        public PowsyblWsProblemDetail build() {
            target.timestamp = Instant.now();
            target.traceId = MDC.get("traceId");
            Objects.requireNonNull(target.server);
            Objects.requireNonNull(target.getDetail());
            return target;
        }
    }

    public record ChainEntry(String fromServer, String toServer, String method, String path, Instant timestamp) {
        @JsonCreator
        public ChainEntry(
            @JsonProperty("from-server") String fromServer,
            @JsonProperty("to-server") String toServer,
            @JsonProperty("method") String method,
            @JsonProperty("path") String path,
            @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
        ) {
            Objects.requireNonNull(fromServer, "from-server");
            Objects.requireNonNull(toServer, "to-server");
            Objects.requireNonNull(timestamp, "timestamp");
            this.fromServer = fromServer;
            this.toServer = toServer;
            this.method = method;
            this.path = path;
            this.timestamp = timestamp;
        }

        public ChainEntry(String fromServer, String toServer, Instant timestamp) {
            this(fromServer, toServer, null, null, timestamp);
        }
    }
}
