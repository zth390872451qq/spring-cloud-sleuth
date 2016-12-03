package org.springframework.cloud.sleuth.instrument.messaging;

import org.springframework.cloud.sleuth.SpanExtractor;

import io.opentracing.propagation.TextMap;

/**
 * Contract for extracting tracing headers from a {@link TextMap}
 * via message headers
 *
 * @author Marcin Grzejszczak
 * @since 1.2.0
 */
// TODO: Breaking change - SpanTextMap -> TextMap
public interface MessagingSpanTextMapExtractor extends SpanExtractor<TextMap> {
}
