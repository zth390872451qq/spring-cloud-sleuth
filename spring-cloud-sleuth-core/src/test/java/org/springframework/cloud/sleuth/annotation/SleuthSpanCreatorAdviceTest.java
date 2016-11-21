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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(classes = SleuthSpanCreatorAdviceTest.TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SleuthSpanCreatorAdviceTest {
	
	@Autowired
	private TestBeanI testBean;
	
	@Autowired
	private Tracer tracer;
	
	@Before
	public void setup() {
		Mockito.reset(tracer);
		Mockito.when(tracer.isTracing()).thenReturn(true);
	}
	
	@Test
	public void shouldCreateSpanWhenAnnotationOnInterfaceMethod() {
		testBean.testMethod();
		
		Mockito.verify(tracer).createSpan(
				Mockito.eq("TestBeanI/testMethod"), Mockito.<Span> any());
	}
	
	@Test
	public void shouldCreateSpanWhenAnnotationOnClassMethod() {
		testBean.testMethod2();
		
		Mockito.verify(tracer).createSpan(
				Mockito.eq("TestBeanI/testMethod2"), Mockito.<Span> any());
	}
	
	@Test
	public void shouldCreateSpanWithCustomNameWhenAnnotationOnClassMethod() {
		testBean.testMethod3();
		
		Mockito.verify(tracer).createSpan(Mockito.eq("testMethod3"), Mockito.<Span> any());
	}
	
	@Test
	public void shouldCreateSpanWithCustomNameWhenAnnotationOnInterfaceMethod() {
		testBean.testMethod4();
		
		Mockito.verify(tracer).createSpan(Mockito.eq("testMethod4"), Mockito.<Span> any());
	}
	
	@Test
	public void shouldCreateSpanWithTagWhenAnnotationOnInterfaceMethod() {
		testBean.testMethod5("test");
		
		Mockito.verify(tracer).addTag(Mockito.eq("testTag"), Mockito.eq("test"));
		Mockito.verify(tracer).createSpan(Mockito.eq("testMethod5"), Mockito.<Span> any());
	}
	
	@Test
	public void shouldCreateSpanWithTagWhenAnnotationOnClassMethod() {
		testBean.testMethod6("test");
		
		Mockito.verify(tracer).addTag(Mockito.eq("testTag6"), Mockito.eq("test"));
		Mockito.verify(tracer).createSpan(Mockito.eq("testMethod6"), Mockito.<Span> any());
	}
	
	@Test
	public void shouldNotCreateSpanWhenNotAnnotated() {
		testBean.testMethod7();
		
		Mockito.verifyZeroInteractions(tracer);
	}
	
	protected interface TestBeanI {
		
		@CreateSleuthSpan
		void testMethod();
		
		void testMethod2();
		
		void testMethod3();
		
		@CreateSleuthSpan(name = "testMethod4")
		void testMethod4();
		
		@CreateSleuthSpan(name = "testMethod5")
		void testMethod5(@SleuthSpanTag("testTag") String test);
		
		void testMethod6(String test);
		
		void testMethod7();
	}
	
	protected static class TestBean implements TestBeanI {

		@Override
		public void testMethod() {
		}

		@CreateSleuthSpan
		@Override
		public void testMethod2() {
		}

		@CreateSleuthSpan(name = "testMethod3")
		@Override
		public void testMethod3() {
		}

		@Override
		public void testMethod4() {
		}
		
		@Override
		public void testMethod5(String test) {
		}

		@CreateSleuthSpan(name = "testMethod6")
		@Override
		public void testMethod6(@SleuthSpanTag("testTag6") String test) {
			
		}

		@Override
		public void testMethod7() {
		}
	}
	
	@Configuration
	@Import({ TraceAutoConfiguration.class, CreateSleuthTestConfiguration.class, SleuthAnnotationAutoConfiguration.class })
	protected static class TestConfiguration {
		
	}
	
	@Configuration
	protected static class CreateSleuthTestConfiguration {

		@Bean
		public TestBeanI testBean() {
			return new TestBean();
		}
		
		@Bean
		public SleuthSpanCreator sleuthSpanCreator(SleuthSpanTagAnnotationHandler annotationSpanUtil) {
			return new DefaultSleuthSpanCreator(tracer(), annotationSpanUtil);
		}
		
		@Bean
		public Tracer tracer() {
			return Mockito.mock(Tracer.class);
		}
		
	}
}
