package org.springframework.cloud.sleuth.instrument.web;

import org.springframework.cloud.sleuth.SpanInjector;

import io.opentracing.propagation.TextMap;

/**
 * Contract for injecting tracing headers from a {@link TextMap}
 * via HTTP headers
 *
 * @author Marcin Grzejszczak
 * @since 1.2.0
 */
// TODO: Breaking change - SpanTextMap -> TextMap
public interface HttpSpanInjector extends SpanInjector<TextMap> {
}
