package com.example.springboot40.versioning;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

/**
 * Wraps {@link StandardApiVersionDeprecationHandler} - still sends the
 * Deprecation/Sunset/Link headers it produces - but also enforces the
 * sunset date, which Spring's built-in handler does not do on its own: the
 * interceptor that calls {@code handleVersion} always lets the request
 * proceed to the controller regardless of what the handler does with the
 * response (confirmed by decompiling
 * {@code AbstractHandlerMapping$ApiVersionDeprecationHandlerInterceptor#preHandle},
 * which unconditionally returns {@code true}). Once "now" is past the
 * registered sunset date for the resolved request version - including when
 * that version was only reached via the configured default, not an explicit
 * header - this throws instead, rejecting the request with 401 before the
 * controller method ever runs.
 * <p>
 * {@code configureApiVersioning} wires up version resolution for the whole
 * app's default {@code RequestMappingHandlerMapping}, not just for
 * controllers that declare a {@code version} attribute - with a default
 * version configured, every request (e.g. {@code GreetingController}'s,
 * which never opted into versioning at all) resolves a version and reaches
 * this handler. So {@code handleVersion} only acts when the target
 * controller is actually {@link ProductController}; every other endpoint in
 * the app is left alone, sunset or not.
 */
class SunsetEnforcingDeprecationHandler implements ApiVersionDeprecationHandler {

    private final StandardApiVersionDeprecationHandler headers = new StandardApiVersionDeprecationHandler();
    private final ApiVersionParser<?> versionParser = new SemanticApiVersionParser();
    private final Map<Comparable<?>, ZonedDateTime> sunsetDates = new HashMap<>();

    /**
     * Registers the sunset date for enforcement and forwards it to the
     * delegate so the "Sunset" header still gets sent while the deadline
     * hasn't passed yet. Chain {@code setDeprecationDate}/{@code setSunsetLink}
     * on the returned spec as usual.
     */
    StandardApiVersionDeprecationHandler.VersionSpec configureVersion(String version, ZonedDateTime sunsetDate) {
        this.sunsetDates.put(this.versionParser.parseVersion(version), sunsetDate);
        return this.headers.configureVersion(version).setSunsetDate(sunsetDate);
    }

    @Override
    public void handleVersion(Comparable<?> requestVersion, Object handler,
            HttpServletRequest request, HttpServletResponse response) {

        if (!(handler instanceof HandlerMethod handlerMethod)
                || handlerMethod.getBeanType() != ProductController.class) {
            return;
        }

        ZonedDateTime sunsetDate = this.sunsetDates.get(requestVersion);
        if (sunsetDate != null && ZonedDateTime.now(sunsetDate.getZone()).isAfter(sunsetDate)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "API version '" + requestVersion + "' was sunset on " + sunsetDate + " and is no longer available");
        }
        this.headers.handleVersion(requestVersion, handler, request, response);
    }
}
