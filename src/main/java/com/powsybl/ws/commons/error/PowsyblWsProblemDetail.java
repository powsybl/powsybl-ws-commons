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
        @JsonProperty(value = "type", required = false) URI type,
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

    public Optional<ServerName> server() {
        return Optional.ofNullable(server);
    }

    public Optional<BusinessErrorCode> businessErrorCode() {
        return Optional.ofNullable(businessErrorCode);
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
        if (status != null) {
            HttpStatus resolved = HttpStatus.resolve(status);
            setDetail(resolved != null ? resolved.getReasonPhrase() : String.valueOf(status));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static final class Builder {
        private final PowsyblWsProblemDetail target;
        private final List<ChainEntry> chainEntries;

        private ServerName hopFrom;
        private HttpMethodValue hopMethod;
        private ErrorPath hopPath;
        private Integer hopStatus;
        private Instant hopTimestamp;

        private Builder(HttpStatusCode status) {
            this.target = new PowsyblWsProblemDetail(Objects.requireNonNull(status, "status"));
            this.chainEntries = new ArrayList<>();
        }

        private Builder(PowsyblWsProblemDetail template) {
            Integer status = template.getStatus();
            HttpStatusCode statusCode = status != null
                ? HttpStatusCode.valueOf(status)
                : HttpStatus.INTERNAL_SERVER_ERROR;
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

        public Builder type(URI type) {
            target.setType(type);
            return this;
        }

        public Builder title(String title) {
            target.setTitle(title);
            return this;
        }

        public Builder detail(String detail) {
            target.setDetail(detail);
            return this;
        }

        public Builder status(HttpStatusCode status) {
            target.applyStatus(status);
            return this;
        }

        public Builder instance(URI instance) {
            target.setInstance(instance);
            return this;
        }

        public Builder server(ServerName server) {
            target.server = server;
            return this;
        }

        public Builder server(String server) {
            target.server = ServerName.of(server);
            return this;
        }

        public Builder businessErrorCode(BusinessErrorCode businessErrorCode) {
            target.businessErrorCode = businessErrorCode;
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

        public Builder path(ErrorPath path) {
            target.path = path;
            return this;
        }

        public Builder path(String path) {
            target.path = ErrorPath.of(path);
            return this;
        }

        public Builder traceId(TraceId traceId) {
            target.traceId = traceId;
            return this;
        }

        public Builder traceId(String traceId) {
            target.traceId = TraceId.of(traceId);
            return this;
        }

        public Builder hop(ServerName fromServer, HttpMethodValue method, ErrorPath path, Integer status, Instant timestamp) {
            this.hopFrom = fromServer;
            this.hopMethod = method;
            this.hopPath = path;
            this.hopStatus = status;
            this.hopTimestamp = timestamp;
            return this;
        }

        public Builder hop(String fromServer, String method, String path, Integer status, Instant timestamp) {
            this.hopFrom = ServerName.of(fromServer);
            this.hopMethod = HttpMethodValue.of(method);
            this.hopPath = ErrorPath.of(path);
            this.hopStatus = status;
            this.hopTimestamp = timestamp;
            return this;
        }

        public PowsyblWsProblemDetail build() {
            if (target.timestamp == null) {
                target.timestamp = Instant.now();
            }
            target.ensureDetail();
            List<ChainEntry> updatedChain = new ArrayList<>(chainEntries);
            ChainEntry hopEntry = createHopEntry(updatedChain);
            if (hopEntry != null) {
                updatedChain.add(0, hopEntry);
            }
            target.chain = List.copyOf(updatedChain);
            return target;
        }

        private ChainEntry createHopEntry(List<ChainEntry> existing) {
            if (hopFrom == null) {
                return null;
            }
            ServerName hopTo = determineHopTarget(existing);
            if (hopTo == null) {
                return null;
            }
            Instant hopInstant = hopTimestamp != null ? hopTimestamp : target.timestamp;
            Integer hopStatusValue = hopStatus != null ? hopStatus : target.getStatus();
            return new ChainEntry(hopFrom, hopTo, hopMethod, hopPath, hopStatusValue, hopInstant);
        }

        private ServerName determineHopTarget(List<ChainEntry> existing) {
            if (!existing.isEmpty()) {
                ChainEntry first = existing.get(0);
                if (first.fromServer() != null) {
                    return first.fromServer();
                }
                if (first.toServer() != null) {
                    return first.toServer();
                }
            }
            return target.server;
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
