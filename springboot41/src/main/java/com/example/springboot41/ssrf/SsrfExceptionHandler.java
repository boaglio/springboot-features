package com.example.springboot41.ssrf;

import org.springframework.boot.http.client.FilteredHostException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps a blocked outbound request to a clean 403 instead of a 500, so
 * callers get a deliberate "not allowed" rather than a stack trace.
 */
@RestControllerAdvice
class SsrfExceptionHandler {

    // FilteredHostException is what InetAddressFilter (see SsrfConfig) throws
    // when a target address is on its blocklist; without this handler it
    // would surface as an unhandled 500.
    @ExceptionHandler(FilteredHostException.class)
    ProblemDetail handleFilteredHost(FilteredHostException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Requested host is not allowed");
    }
}
