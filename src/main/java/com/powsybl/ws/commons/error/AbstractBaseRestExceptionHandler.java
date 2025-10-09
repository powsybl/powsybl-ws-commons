package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Optional;

/**
 * Reusable, typed base for translating exceptions to PowsyblWsProblemDetail.
 *
 * @param <E> domain exception type
 * @param <C> business error code enum
 */
public abstract class AbstractBaseRestExceptionHandler<E extends Exception, C> {

    private final ServerNameProvider serverNameProvider;

    private static final String DEFAULT_REMOTE_ERROR_MESSAGE = "An unexpected error occurred while calling a remote server";

    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected AbstractBaseRestExceptionHandler(ServerNameProvider serverNameProvider) {
        this.serverNameProvider = serverNameProvider;
    }

    private String serverName() {
        return serverNameProvider.serverName();
    }

    protected abstract Optional<PowsyblWsProblemDetail> getRemoteError(AbstractPowsyblWsException ex);

    protected abstract Optional<BusinessErrorCode> getBusinessCode(AbstractPowsyblWsException ex);

    protected abstract HttpStatus mapStatus(BusinessErrorCode code);

    protected abstract AbstractPowsyblWsException wrapRemote(PowsyblWsProblemDetail remoteError);

    @ExceptionHandler(HttpStatusCodeException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleRemoteException(
        HttpStatusCodeException exception, HttpServletRequest request) {

        AbstractPowsyblWsException wrapped = mapRemoteException(exception);
        return handleDomainException(wrapped, request);
    }

    @ExceptionHandler
    protected ResponseEntity<PowsyblWsProblemDetail> handleDomainException(
        AbstractPowsyblWsException exception, HttpServletRequest request) {

        HttpStatus status = resolveStatus(exception);
        return ResponseEntity.status(status)
            .body(buildErrorResponse(request, status, exception));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleAllExceptions(
        Exception exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = firstNonBlank(exception.getMessage(), status.getReasonPhrase());
        return ResponseEntity.status(status)
            .body(buildErrorResponse(request, status, null, message));
    }

    private PowsyblWsProblemDetail buildErrorResponse(HttpServletRequest request, HttpStatus status, AbstractPowsyblWsException exception) {
        BusinessErrorCode currentBusinessCode = exception.getBusinessErrorCode().orElse(null);
        PowsyblWsProblemDetail remoteError = exception.getRemoteError().orElse(null);

        String businessErrorCode = remoteError != null
            ? remoteError.getBusinessErrorCode().value()
            : currentBusinessCode != null ? currentBusinessCode.value() : null;
        String message = firstNonBlank(
            remoteError != null ? remoteError.getDetail() : null,
            exception.getMessage(),
            status.getReasonPhrase()
        );
        Instant now = Instant.now();
        String localPath = request.getRequestURI();
        String method = request.getMethod();

        PowsyblWsProblemDetail.Builder builder;
        if (remoteError != null) {
            String path = remoteError.path()
                .map(PowsyblWsProblemDetail.ErrorPath::value)
                .orElse(localPath);
            builder = PowsyblWsProblemDetail.builderFrom(remoteError)
                .title(status.getReasonPhrase())
                .detail(message)
                .appendChain(serverName(), method, path, status.value(), now);
            if (remoteError.getBusinessErrorCode() == null && businessErrorCode != null) {
                builder.businessErrorCode(businessErrorCode);
            }

        } else {
            builder = PowsyblWsProblemDetail.builder(status)
                .title(status.getReasonPhrase())
                .server(serverName())
                .businessErrorCode(businessErrorCode)
                .detail(message)
                .timestamp(now)
                .path(localPath);
        }
        return builder.build();
    }

    private PowsyblWsProblemDetail buildErrorResponse(HttpServletRequest request, HttpStatus status, String businessErrorCode, String message) {
        Instant timestamp = Instant.now();
        return PowsyblWsProblemDetail.builder(status)
            .title(status.getReasonPhrase())
            .server(serverName())
            .businessErrorCode(businessErrorCode)
            .detail(message)
            .timestamp(timestamp)
            .path(request.getRequestURI())
            .build();
    }

    private HttpStatus resolveStatus(AbstractPowsyblWsException exception) {
        return getRemoteError(exception)
            .map(PowsyblWsProblemDetail::getStatus)
            .map(this::resolveStatus)
            .orElseGet(() -> getBusinessCode(exception)
                .map(this::mapStatus)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private HttpStatus resolveStatus(Integer code) {
        return HttpStatus.valueOf(code);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    protected String codeValue(C code) {
        try {
            return (String) code.getClass().getMethod("value").invoke(code);
        } catch (Exception ignored) {
            return code.toString();
        }
    }

    protected AbstractPowsyblWsException mapRemoteException(HttpStatusCodeException ex) {
        try {
            byte[] body = ex.getResponseBodyAsByteArray();
            PowsyblWsProblemDetail remote = objectMapper.readValue(body, PowsyblWsProblemDetail.class);
            return wrapRemote(remote);
        } catch (Exception ignored) {
            return wrapRemote(fallbackRemoteError());
        }
    }

    private PowsyblWsProblemDetail fallbackRemoteError() {
        Instant now = Instant.now();
        return PowsyblWsProblemDetail.builder(HttpStatus.INTERNAL_SERVER_ERROR)
            .server(serverName())
            .businessErrorCode(codeValue(defaultRemoteErrorCode()))
            .detail(DEFAULT_REMOTE_ERROR_MESSAGE)
            .timestamp(now)
            .path(serverName())
            .build();
    }

    protected abstract C defaultRemoteErrorCode();
}
