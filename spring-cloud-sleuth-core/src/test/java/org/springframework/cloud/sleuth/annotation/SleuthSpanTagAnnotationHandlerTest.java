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

package org.springframework.cloud.sleuth.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(classes = SleuthSpanTagAnnotationHandlerTest.TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SleuthSpanTagAnnotationHandlerTest {

	@Autowired
	private SleuthSpanTagAnnotationHandler spanUtil;
	
	@Autowired
	private SleuthTagValueResolver tagValueResolver;
	
	@Test
	public void shouldUseCustomTagValueResolver() throws NoSuchMethodException, SecurityException {
		Method method = AnnotationMockClass.class.getMethod("getAnnotationForTagValueResolver", String.class);
		Annotation annotation = method.getParameterAnnotations()[0][0];
		if (annotation instanceof SleuthSpanTag) {
			String resolvedValue = spanUtil.resolveTagValue((SleuthSpanTag) annotation, "test");
			assertThat(resolvedValue).isEqualTo("Value from myCustomTagValueResolver");
			Mockito.verify(tagValueResolver).resolveTagValue("test");
		} else {
			fail("Annotation was not SleuthSpanTag");
		}
	}
	
	@Test
	public void shouldUseTagValueExpression() throws NoSuchMethodException, SecurityException {
		Method method = AnnotationMockClass.class.getMethod("getAnnotationForTagValueExpression", String.class);
		Annotation annotation = method.getParameterAnnotations()[0][0];
		if (annotation instanceof SleuthSpanTag) {
			String resolvedValue = spanUtil.resolveTagValue((SleuthSpanTag) annotation, "test");
			
			assertThat(resolvedValue).isEqualTo("4 characters");
		} else {
			fail("Annotation was not SleuthSpanTag");
		}
	}
	
	@Test
	public void shouldReturnArgumentToString() throws NoSuchMethodException, SecurityException {
		Method method = AnnotationMockClass.class.getMethod("getAnnotationForArgumentToString", String.class);
		Annotation annotation = method.getParameterAnnotations()[0][0];
		if (annotation instanceof SleuthSpanTag) {
			String resolvedValue = spanUtil.resolveTagValue((SleuthSpanTag) annotation, "test");
			assertThat(resolvedValue).isEqualTo("test");
		} else {
			fail("Annotation was not SleuthSpanTag");
		}
	}
	
	protected class AnnotationMockClass {
		
		public void getAnnotationForTagValueResolver(@SleuthSpanTag(value = "test", tagValueResolverBeanName = "myCustomTagValueResolver") String test) {
		}
		
		public void getAnnotationForTagValueExpression(@SleuthSpanTag(value = "test", tagValueExpression = "length() + ' characters'") String test) {
		}
		
		public void getAnnotationForArgumentToString(@SleuthSpanTag(value = "test") String test) {
		}
	}
	
	@Configuration
	@Import({ TraceAutoConfiguration.class, CreateSleuthTestConfiguration.class, SleuthAnnotationAutoConfiguration.class })
	protected static class TestConfiguration {
		
	}
	
	@Configuration
	protected static class CreateSleuthTestConfiguration {
		
		@Bean(name = "myCustomTagValueResolver")
		public SleuthTagValueResolver tagValueResolver() {
			return Mockito.spy(new SleuthTagValueResolver() {
				
				@Override
				public String resolveTagValue(Object parameter) {
					return "Value from myCustomTagValueResolver";
				}
			});
		}

		@Bean
		public SleuthSpanCreator sleuthSpanCreator() {
			return Mockito.mock(SleuthSpanCreator.class);
		}
		
	}
}
