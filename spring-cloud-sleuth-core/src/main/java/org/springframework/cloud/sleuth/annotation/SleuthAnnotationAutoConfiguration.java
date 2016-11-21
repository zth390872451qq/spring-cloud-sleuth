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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * to enable annotation based span creation.
 *
 * @author Marcin Grzejszczak
 * @since 1.2.0
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(name = "spring.sleuth.annotation.enabled", matchIfMissing = true)
public class SleuthAnnotationAutoConfiguration {
	
	@Autowired
	private Tracer tracer;
	
	@Bean
	SleuthSpanTagAnnotationHandler spanUtil(ApplicationContext context) {
		return new SleuthSpanTagAnnotationHandler(context, this.tracer);
	}

	@Bean
	@ConditionalOnMissingBean(SleuthSpanCreator.class)
	SleuthSpanCreator spanCreator() {
		return new DefaultSleuthSpanCreator(this.tracer, spanUtil(null));
	}

	@Bean
	SleuthSpanCreatorAdvice sleuthSpanCreatorAdvice(SleuthSpanCreator creator) {
		return new SleuthSpanCreatorAdvice(creator, this.tracer);
	}

	@Bean
	SleuthSpanCreateBeanPostProcessor sleuthSpanCreateBeanPostProcessor(SleuthSpanCreator spanCreator) {
		return new SleuthSpanCreateBeanPostProcessor(new SleuthSpanCreatorAdvice(spanCreator, tracer));
	}

}
