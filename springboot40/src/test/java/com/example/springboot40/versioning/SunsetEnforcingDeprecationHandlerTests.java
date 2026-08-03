package com.example.springboot40.versioning;

import java.net.URI;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

import com.example.springboot40.web.GreetingController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-tests {@link SunsetEnforcingDeprecationHandler} directly (no MVC
 * dispatch involved), covering both sides of it: still-just-a-warning before
 * the sunset date, hard rejection after, and - the part that's easy to get
 * wrong, see the class Javadoc - leaving other controllers alone entirely.
 * {@link ApiVersioningTests} only ever sees the "after, and it's
 * ProductController" state, since version 1's configured sunset date is
 * already in the past.
 * <p>
 * Requests are represented with {@link SemanticApiVersionParser} directly -
 * the same parser {@code SunsetEnforcingDeprecationHandler} uses internally
 * to key its sunset-date map - so these parsed values are equal to what
 * Spring MVC would actually resolve a request's version to. The handler
 * argument is a real {@link HandlerMethod}, since that's what MVC actually
 * passes and what the bean-type scoping check inspects.
 */
class SunsetEnforcingDeprecationHandlerTests {

    private final SemanticApiVersionParser versionParser = new SemanticApiVersionParser();

    @Test
    void beforeSunsetItOnlySendsWarningHeaders() throws NoSuchMethodException {
        SunsetEnforcingDeprecationHandler handler = new SunsetEnforcingDeprecationHandler();
        handler.configureVersion("1", ZonedDateTime.now().plusDays(30))
                .setDeprecationDate(ZonedDateTime.now())
                .setSunsetLink(URI.create("https://example.com/migration-guide"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handleVersion(this.versionParser.parseVersion("1"), productControllerHandlerMethod(),
                new MockHttpServletRequest(), response);

        assertThat(response.getHeader("Deprecation")).isNotBlank();
        assertThat(response.getHeader("Sunset")).isNotBlank();
    }

    @Test
    void afterSunsetItRejectsWithUnauthorized() throws NoSuchMethodException {
        SunsetEnforcingDeprecationHandler handler = new SunsetEnforcingDeprecationHandler();
        handler.configureVersion("1", ZonedDateTime.now().minusDays(1));

        MockHttpServletResponse response = new MockHttpServletResponse();
        Comparable<?> requestVersion = this.versionParser.parseVersion("1");
        HandlerMethod productControllerHandlerMethod = productControllerHandlerMethod();

        assertThatThrownBy(() -> handler.handleVersion(requestVersion, productControllerHandlerMethod,
                new MockHttpServletRequest(), response))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(401));
    }

    @Test
    void aVersionWithNoConfiguredSunsetIsLeftAlone() throws NoSuchMethodException {
        SunsetEnforcingDeprecationHandler handler = new SunsetEnforcingDeprecationHandler();
        handler.configureVersion("1", ZonedDateTime.now().plusDays(30));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handleVersion(this.versionParser.parseVersion("2"), productControllerHandlerMethod(),
                new MockHttpServletRequest(), response);

        assertThat(response.getHeader("Deprecation")).isNull();
        assertThat(response.getHeader("Sunset")).isNull();
    }

    @Test
    void aControllerThatNeverOptedIntoVersioningIsLeftAloneEvenPastSunset() throws NoSuchMethodException {
        SunsetEnforcingDeprecationHandler handler = new SunsetEnforcingDeprecationHandler();
        handler.configureVersion("1", ZonedDateTime.now().minusDays(1));

        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod greetingControllerHandlerMethod = new HandlerMethod(
                new GreetingController(), GreetingController.class.getMethod("greet", String.class));

        handler.handleVersion(this.versionParser.parseVersion("1"), greetingControllerHandlerMethod,
                new MockHttpServletRequest(), response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Deprecation")).isNull();
    }

    private static HandlerMethod productControllerHandlerMethod() throws NoSuchMethodException {
        return new HandlerMethod(new ProductController(), ProductController.class.getMethod("getProductV1", String.class));
    }
}
