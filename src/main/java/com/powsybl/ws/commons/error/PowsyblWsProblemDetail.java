/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.annotation.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

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

    private String server;
    private String businessErrorCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private String path;
    private String traceId;
    private final List<ChainEntry> chain;

    /**
     * Custom properties map that must be rendered as a nested JSON object: "properties": { ... }
     * We intentionally do NOT use the ProblemDetail internal properties map for JSON,
     * because it is expanded at top level via ProblemDetailJacksonMixin (@JsonAnyGetter), therefore, it's content cannot be deserialized systematically
     */
    private final Map<String, Object> properties = new LinkedHashMap<>();

    @JsonCreator
    public PowsyblWsProblemDetail(
        @JsonProperty("title") String title,
        @JsonProperty("status") Integer status,
        @JsonProperty("detail") String detail,
        @JsonProperty("server") String server,
        @JsonProperty("businessErrorCode") String businessErrorCode,
        @JsonProperty("properties") Map<String, Object> properties,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("path") String path,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("chain") List<ChainEntry> chain
    ) {
        super(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR.value());
        setTitle(title);
        setDetail(detail);
        if (properties != null) {
            this.properties.putAll(properties);
        }
        this.server = server;
        this.businessErrorCode = businessErrorCode;
        this.timestamp = timestamp;
        this.path = path;
        this.traceId = traceId;
        this.chain = chain != null ? new ArrayList<>(chain) : new ArrayList<>();
    }

    private PowsyblWsProblemDetail(@NonNull HttpStatusCode status) {
        super(status.value());
        this.chain = new ArrayList<>();
    }

    public static Builder builder(HttpStatusCode status) {
        return new Builder(status);
    }

    public void wrap(String fromServer, String method, String path) {
        String toServer = chain.isEmpty() ? server : chain.getFirst().fromServer();
        var newChainEntry = new ChainEntry(fromServer, toServer, method, path, Instant.now());
        chain.addFirst(newChainEntry);
    }

    /**
     * Override ProblemDetail.getProperties() to prevent Jackson mixin (@JsonAnyGetter)
     * from expanding the map as top-level JSON properties.
     */
    @Override
    @JsonIgnore
    public Map<String, Object> getProperties() {
        return super.getProperties();
    }

    /**
     * Expose our custom properties map as a nested "properties" JSON object.
     */
    @JsonProperty("properties")
    public Map<String, Object> getJsonProperties() {
        return properties;
    }

    @Getter
    public static final class Builder {
        private final PowsyblWsProblemDetail target;

        private Builder(HttpStatusCode status) {
            this.target = new PowsyblWsProblemDetail(Objects.requireNonNull(status, "status"));
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

        public Builder properties(Map<String, Object> properties) {
            target.properties.clear();
            if (properties != null) {
                target.properties.putAll(properties);
            }
            return this;
        }

        public Builder path(String path) {
            target.path = path;
            return this;
        }

        public PowsyblWsProblemDetail build() {
            target.timestamp = Instant.now();
            Objects.requireNonNull(target.server);
            Objects.requireNonNull(target.getDetail());
            Objects.requireNonNull(target.path);
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
    }
}
