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

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(classes = SleuthSpanCreatorAdviceNegativTest.TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SleuthSpanCreatorAdviceNegativTest {

	@Autowired
	private NotAnnotatedTestBeanI testBean;
	
	@Autowired
	private TestBeanI annotatedTestBean;
	
	@Autowired
	private SleuthSpanCreatorAdviceHolder adviceHolder;
	
	@Autowired
	private Tracer tracer;
	
	@Before
	public void setup() {
		Mockito.reset(tracer);
		Mockito.when(tracer.isTracing()).thenReturn(true);
	}

	@Test
	public void shouldNotCallAdviceForNotAnnotatedBean() {
		testBean.testMethod();
		
		Mockito.verifyZeroInteractions(adviceHolder.advice);
	}

	@Test
	public void shouldCallAdviceForAnnotatedBean() throws Throwable {
		annotatedTestBean.testMethod();
		
		Mockito.verify(tracer).createSpan(
				Mockito.eq("TestBeanI/testMethod"), Mockito.<Span> any());
		Mockito.verify(adviceHolder.advice).instrumentOnMethodAnnotation(
				Mockito.<ProceedingJoinPoint> any(), Mockito.any(CreateSleuthSpan.class));
		
	}
	
	protected interface NotAnnotatedTestBeanI {

		void testMethod();
	}

	protected static class NotAnnotatedTestBean implements NotAnnotatedTestBeanI {

		@Override
		public void testMethod() {
		}

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
	@Import({ TraceAutoConfiguration.class, CreateSleuthTestConfiguration.class })
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class CreateSleuthTestConfiguration {
		
		@Bean
		public SleuthSpanCreatorAdviceHolder adviceHolder(SleuthSpanCreator spanCreator) {
			SleuthSpanCreatorAdvice advice = new SleuthSpanCreatorAdvice(spanCreator, tracer());
			advice = Mockito.spy(advice);
			SleuthSpanCreatorAdviceHolder adviceHolder = new SleuthSpanCreatorAdviceHolder();
			adviceHolder.setAdvice(advice);
			return adviceHolder;
		}
		
		@Bean
		public SleuthSpanTagAnnotationHandler spanUtil(ApplicationContext context) {
			return new SleuthSpanTagAnnotationHandler(context, tracer());
		}

		@Bean
		public SleuthSpanCreateBeanPostProcessor sleuthSpanCreateBeanPostProcessor(SleuthSpanCreatorAdviceHolder adviceHolder) {
			SleuthSpanCreateBeanPostProcessor postProcessor = new SleuthSpanCreateBeanPostProcessor(adviceHolder.advice);
			return postProcessor;
		}

		@Bean
		public NotAnnotatedTestBeanI testBean() {
			return new NotAnnotatedTestBean();
		}

		@Bean
		public SleuthSpanCreator sleuthSpanCreator(SleuthSpanTagAnnotationHandler annotationSpanUtil) {
			return new DefaultSleuthSpanCreator(tracer(), annotationSpanUtil);
		}
		
		@Bean
		public TestBeanI annotatedTestBean() {
			return new TestBean();
		}
		
		@Bean
		public Tracer tracer() {
			return Mockito.mock(Tracer.class);
		}

	}
	
	protected static class SleuthSpanCreatorAdviceHolder {
		
		private SleuthSpanCreatorAdvice advice;

		public SleuthSpanCreatorAdvice getAdvice() {
			return advice;
		}

		public void setAdvice(SleuthSpanCreatorAdvice advice) {
			this.advice = advice;
		}
		
		
	}
}
