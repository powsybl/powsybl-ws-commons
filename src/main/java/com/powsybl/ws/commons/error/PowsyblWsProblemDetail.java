/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * Shared {@link ProblemDetail} subclass that carries typed metadata used across Powsybl services.
 */
public final class PowsyblWsProblemDetail extends ProblemDetail {

    private ServerName server;
    private BusinessErrorCode businessErrorCode;
    private Instant timestamp;
    private ErrorPath path;
    private TraceId traceId;
    private List<ChainEntry> chain;

    @JsonCreator
    public PowsyblWsProblemDetail(
        @JsonProperty(value = "type") URI type,
        @JsonProperty("title") String title,
        @JsonProperty("status") Integer status,
        @JsonProperty("detail") String detail,
        @JsonProperty("instance") URI instance,
        @JsonProperty("server") ServerName server,
        @JsonProperty("businessErrorCode") BusinessErrorCode businessErrorCode,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("path") ErrorPath path,
        @JsonProperty("traceId") TraceId traceId,
        @JsonProperty("chain") List<ChainEntry> chain
    ) {
        super(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR.value());
        HttpStatusCode resolvedStatus = status != null
            ? HttpStatusCode.valueOf(status)
            : HttpStatus.INTERNAL_SERVER_ERROR;
        applyStatus(resolvedStatus);
        setType(type != null ? type : URI.create("about:blank"));
        setTitle(title);
        setDetail(detail);
        setInstance(instance);
        this.server = server;
        this.businessErrorCode = businessErrorCode;
        this.timestamp = timestamp;
        this.path = path;
        this.traceId = traceId;
        this.chain = chain != null ? new ArrayList<>(chain) : new ArrayList<>();
        ensureDetail();
    }

    private PowsyblWsProblemDetail(HttpStatusCode status) {
        super(status != null ? status.value() : HttpStatus.INTERNAL_SERVER_ERROR.value());
        applyStatus(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        HttpStatus resolved = status instanceof HttpStatus httpStatus
            ? httpStatus
            : HttpStatus.resolve(getStatus());
        if (resolved != null) {
            setTitle(resolved.getReasonPhrase());
        }
        this.chain = new ArrayList<>();
    }

    private void applyStatus(HttpStatusCode status) {
        if (status instanceof HttpStatus httpStatus) {
            setStatus(httpStatus);
        } else if (status != null) {
            setStatus(status.value());
        }
    }

    public static Builder builder(HttpStatus status) {
        return builder((HttpStatusCode) status);
    }

    public static Builder builder(HttpStatusCode status) {
        return new Builder(status);
    }

    public static Builder builderFrom(PowsyblWsProblemDetail template) {
        return new Builder(Objects.requireNonNull(template, "template"));
    }

    @JsonProperty("server")
    public ServerName getServer() {
        return server;
    }

    @JsonProperty("businessErrorCode")
    public BusinessErrorCode getBusinessErrorCode() {
        return businessErrorCode;
    }

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getTimestamp() {
        return timestamp;
    }

    @JsonProperty("path")
    public ErrorPath getPath() {
        return path;
    }

    @JsonProperty("traceId")
    public TraceId getTraceId() {
        return traceId;
    }

    @JsonProperty("chain")
    public List<ChainEntry> getChain() {
        if (chain == null || chain.isEmpty()) {
            return List.of();
        }
        return List.copyOf(chain);
    }

    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    public Optional<ErrorPath> path() {
        return Optional.ofNullable(path);
    }

    public Optional<TraceId> traceId() {
        return Optional.ofNullable(traceId);
    }

    public List<ChainEntry> chainEntries() {
        return getChain();
    }

    private void ensureDetail() {
        if (hasText(getDetail())) {
            return;
        }
        if (hasText(getTitle())) {
            setDetail(getTitle());
            return;
        }
        Integer status = getStatus();
        HttpStatus resolved = HttpStatus.resolve(status);
        setDetail(resolved != null ? resolved.getReasonPhrase() : String.valueOf(status));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static final class Builder {
        private final PowsyblWsProblemDetail target;
        private final List<ChainEntry> chainEntries;

        private ServerName fromServer;
        private HttpMethodValue method;
        private ErrorPath path;
        Integer status;
        private Instant timestamp;

        private Builder(HttpStatusCode status) {
            this.target = new PowsyblWsProblemDetail(Objects.requireNonNull(status, "status"));
            this.chainEntries = new ArrayList<>();
        }

        private Builder(PowsyblWsProblemDetail template) {
            int status = template.getStatus();
            HttpStatusCode statusCode = HttpStatusCode.valueOf(status);
            this.target = new PowsyblWsProblemDetail(statusCode);
            this.target.setType(template.getType());
            this.target.setTitle(template.getTitle());
            this.target.setDetail(template.getDetail());
            this.target.setInstance(template.getInstance());
            this.target.server = template.server;
            this.target.businessErrorCode = template.businessErrorCode;
            this.target.timestamp = template.timestamp;
            this.target.path = template.path;
            this.target.traceId = template.traceId;
            this.chainEntries = template.chain != null
                ? new ArrayList<>(template.chain)
                : new ArrayList<>();
        }

        public Builder title(String title) {
            target.setTitle(title);
            return this;
        }

        public Builder detail(String detail) {
            target.setDetail(detail);
            return this;
        }

        public Builder server(String server) {
            target.server = ServerName.of(server);
            return this;
        }

        public Builder businessErrorCode(String businessErrorCode) {
            target.businessErrorCode = BusinessErrorCode.of(businessErrorCode);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            target.timestamp = timestamp;
            return this;
        }

        public Builder path(String path) {
            target.path = ErrorPath.of(path);
            return this;
        }

        public Builder traceId(String traceId) {
            target.traceId = TraceId.of(traceId);
            return this;
        }

        public Builder appendChain(String fromServer, String method, String path, Integer status, Instant timestamp) {
            this.fromServer = ServerName.of(fromServer);
            this.method = HttpMethodValue.of(method);
            this.path = ErrorPath.of(path);
            this.status = status;
            this.timestamp = timestamp;
            return this;
        }

        public PowsyblWsProblemDetail build() {
            if (target.timestamp == null) {
                target.timestamp = Instant.now();
            }
            target.ensureDetail();
            List<ChainEntry> updatedChain = new ArrayList<>(chainEntries);
            ChainEntry chainEntry = createNewChainEntry(updatedChain);
            if (chainEntry != null) {
                updatedChain.addFirst(chainEntry);
            }
            target.chain = List.copyOf(updatedChain);
            return target;
        }

        private ChainEntry createNewChainEntry(List<ChainEntry> existing) {
            if (fromServer == null) {
                return null;
            }
            ServerName toServer = determineTarget(existing);
            if (toServer == null) {
                return null;
            }
            Instant instant = timestamp != null ? timestamp : target.timestamp;
            Integer statusValue = status != null ? status : target.getStatus();
            return new ChainEntry(fromServer, toServer, method, path, statusValue, instant);
        }

        private ServerName determineTarget(List<ChainEntry> existing) {
            return existing.isEmpty()
                ? target.server
                : Optional.ofNullable(existing.getFirst().fromServer())
                .orElse(target.server);
        }
    }

    public record ServerName(@JsonValue String value) {
        public ServerName {
            Objects.requireNonNull(value, "server name");
        }

        public static ServerName of(String value) {
            return value != null ? new ServerName(value) : null;
        }
    }

    public record BusinessErrorCode(@JsonValue String value) {
        public static BusinessErrorCode of(String value) {
            return value != null ? new BusinessErrorCode(value) : null;
        }
    }

    public record ErrorPath(@JsonValue String value) {
        public static ErrorPath of(String value) {
            return value != null ? new ErrorPath(value) : null;
        }
    }

    public record TraceId(@JsonValue String value) {
        public static TraceId of(String value) {
            return value != null ? new TraceId(value) : null;
        }
    }

    public record HttpMethodValue(@JsonValue String value) {
        public static HttpMethodValue of(String value) {
            return value != null ? new HttpMethodValue(value) : null;
        }
    }

    public record ChainEntry(
        @JsonProperty("from-server") ServerName fromServer,
        @JsonProperty("to-server") ServerName toServer,
        @JsonProperty("method") HttpMethodValue method,
        @JsonProperty("path") ErrorPath path,
        @JsonProperty("status") Integer status,
        @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
    ) {
        @JsonCreator
        public ChainEntry {
            Objects.requireNonNull(fromServer, "from-server");
            Objects.requireNonNull(toServer, "to-server");
            Objects.requireNonNull(timestamp, "timestamp");
        }
    }
}
