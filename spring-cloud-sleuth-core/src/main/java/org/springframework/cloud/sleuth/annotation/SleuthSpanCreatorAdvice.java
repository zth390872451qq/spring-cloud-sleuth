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
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

@Aspect
class SleuthSpanCreatorAdvice {

	private SleuthSpanCreator spanCreator;
	private Tracer tracer;

	public SleuthSpanCreatorAdvice(SleuthSpanCreator spanCreator, Tracer tracer) {
		this.spanCreator = spanCreator;
		this.tracer = tracer;
	}

	@Around("@annotation(createSleuthSpan)")
	public Object instrumentOnMethodAnnotation(ProceedingJoinPoint pjp, CreateSleuthSpan createSleuthSpan) throws Throwable {
		Span span = null;
		try {
			span = this.spanCreator.createSpan(pjp, createSleuthSpan);
			return pjp.proceed();
		} finally {
			if (span != null) {
				this.tracer.close(span);
			}
		}
	}

}
