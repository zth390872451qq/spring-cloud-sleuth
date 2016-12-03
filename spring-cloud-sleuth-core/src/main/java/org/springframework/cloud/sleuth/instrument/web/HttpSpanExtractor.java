package org.springframework.cloud.sleuth.instrument.web;

import org.springframework.cloud.sleuth.SpanExtractor;

import io.opentracing.propagation.TextMap;

/**
 * Contract for extracting tracing headers from a {@link TextMap}
 * via HTTP headers
 *
 * @author Marcin Grzejszczak
 * @since 1.2.0
 */
public interface HttpSpanExtractor extends SpanExtractor<TextMap> {
}
