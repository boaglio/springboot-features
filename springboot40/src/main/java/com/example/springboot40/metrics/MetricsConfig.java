package com.example.springboot40.metrics;

import io.micrometer.common.annotation.ValueExpressionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Spring Boot 4.0's metrics-aspects auto-configuration will only build the
 * {@code @MeterTag} handler beans if a {@link ValueExpressionResolver} bean
 * is present - Micrometer ships the interface but not a SpEL implementation,
 * so applications provide their own, as here.
 */
@Configuration
class MetricsConfig {

    @Bean
    ValueExpressionResolver spelValueExpressionResolver() {
        SpelExpressionParser parser = new SpelExpressionParser();
        return (expression, parameterValue) -> parser.parseExpression(expression)
                .getValue(new StandardEvaluationContext(parameterValue), String.class);
    }
}
