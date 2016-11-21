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

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

class DefaultSleuthSpanCreator implements SleuthSpanCreator {
	
	private final Tracer tracer;
	private final SleuthSpanTagAnnotationHandler annotationSpanUtil;

	DefaultSleuthSpanCreator(Tracer tracer, SleuthSpanTagAnnotationHandler annotationSpanUtil) {
		this.tracer = tracer;
		this.annotationSpanUtil = annotationSpanUtil;
	}

	@Override
	public Span createSpan(JoinPoint pjp, CreateSleuthSpan createSleuthSpanAnnotation) {
		if (this.tracer.isTracing()) {
			String key = StringUtils.isNotEmpty(createSleuthSpanAnnotation.name()) ?
					createSleuthSpanAnnotation.name() :
					pjp.getSignature().getDeclaringType().getSimpleName() + "/" + pjp.getSignature().getName();
			Span span = this.tracer.createSpan(key);
			annotationSpanUtil.addAnnotatedParameters(pjp);
			return span;
		}
		return null;
	}

}
