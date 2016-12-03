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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.assertions.SleuthAssertions;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.sleuth.util.ArrayListSpanAccumulator;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import io.opentracing.Span;
import io.opentracing.Tracer;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
	classes = OpenTracingTests.Config.class)
public class OpenTracingTests {

	@Autowired Tracer tracer;
	@Autowired ArrayListSpanAccumulator accumulator;

	@Test
	public void should_prepare_a_trace_for_exporting_to_zipkin() {
		long startMicros = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
		Span span = this.tracer.buildSpan("foo").withTag("foo", "bar")
				.withStartTimestamp(startMicros).start();
		span.log("some_event").setBaggageItem("baggage", "value")
				.setOperationName("new_name");

		span.close();

		SleuthAssertions.then(this.accumulator.getSpans()).hasSize(1);
		org.springframework.cloud.sleuth.Span storedSpan = this.accumulator.getSpans().get(0);
		SleuthAssertions.then(storedSpan)
				.hasATag("foo", "bar")
				.hasBaggageItem("baggage", "value")
				.hasNameEqualTo("new_name");
		SleuthAssertions.then(storedSpan.getBegin()).isEqualTo(TimeUnit.MICROSECONDS.toMillis(startMicros));
	}

	@DefaultTestAutoConfiguration
	static class Config {
		@Bean AlwaysSampler alwaysSampler() {
			return new AlwaysSampler();
		}

		@Bean ArrayListSpanAccumulator accumulator() {
			return new ArrayListSpanAccumulator();
		}
	}
}