/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.SpanExtractor;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanExtractor;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.sleuth.util.ArrayListSpanAccumulator;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.cloud.sleuth.util.TextMapUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

import static org.springframework.cloud.sleuth.assertions.SleuthAssertions.then;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
	classes = OpenTracingTests.Config.class)
public class OpenTracingTests {

	@Autowired Tracer tracer;
	@Autowired ArrayListSpanAccumulator accumulator;

	@Before
	public void setup() {
		ExceptionUtils.setFail(true);
	}

	@After
	public void after() {
		this.accumulator.clear();
		then(ExceptionUtils.getLastException()).isNull();
	}

	@Test
	public void should_prepare_a_trace_for_exporting_to_zipkin() {
		long startMicros = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
		Span span = this.tracer.buildSpan("foo").withTag("foo", "bar")
				.withStartTimestamp(startMicros).start();
		span.log("some_event").setBaggageItem("baggage", "value")
				.setOperationName("new_name");

		span.close();

		then(this.accumulator.getSpans()).hasSize(1);
		org.springframework.cloud.sleuth.Span storedSpan = this.accumulator.getSpans().get(0);
		then(storedSpan)
				.hasATag("foo", "bar")
				.hasBaggageItem("baggage", "value")
				.hasNameEqualTo("new_name");
		then(storedSpan.getBegin()).isEqualTo(TimeUnit.MICROSECONDS.toMillis(startMicros));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void should_inject_tracing_context_to_a_carrier() {
		Span span = this.tracer.buildSpan("foo").start();
		TestTextMap testTextMap = new TestTextMap();

		this.tracer.inject(span.context(), Format.Builtin.TEXT_MAP, testTextMap);

		then(testTextMap.iterator()).contains(entry("key", "value"));
		span.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void should_extract_tracing_context_from_a_carrier() {
		TestTextMap testTextMap = new TestTextMap();
		testTextMap.put("key", "value");

		SpanContext span = this.tracer.extract(Format.Builtin.TEXT_MAP, testTextMap);

		then((org.springframework.cloud.sleuth.Span) span).hasNameEqualTo("value");
	}

	private Map.Entry<String, String> entry(String key, String value) {
		return new AbstractMap.SimpleEntry<>(key, value);
	}

	class TestTextMap implements TextMap {

		private final Map<String, String> map = new HashMap<>();

		@Override public Iterator<Map.Entry<String, String>> iterator() {
			return this.map.entrySet().iterator();
		}

		@Override public void put(String key, String value) {
			this.map.put(key, value);
		}
	}

	@DefaultTestAutoConfiguration
	static class Config {
		@Bean AlwaysSampler alwaysSampler() {
			return new AlwaysSampler();
		}

		@Bean ArrayListSpanAccumulator accumulator() {
			return new ArrayListSpanAccumulator();
		}

		@Bean SpanExtractor<TextMap> customSpanExtractor() {
			return new CustomHttpSpanExtractor();
		}
		@Bean SpanInjector<TextMap> customSpanInjector() {
			return new CustomHttpSpanInjector();
		}

	}

	static class CustomHttpSpanExtractor implements HttpSpanExtractor {

		@Override public org.springframework.cloud.sleuth.Span joinTrace(TextMap carrier) {
			Map<String, String> map = TextMapUtil.asMap(carrier);
			return org.springframework.cloud.sleuth.Span.builder().name(map.get("key")).build();
		}
	}

	static class CustomHttpSpanInjector implements HttpSpanInjector {

		@Override
		public void inject(SpanContext spanContext, TextMap carrier) {
			carrier.put("key", "value");
		}
	}
}