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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

class SleuthSpanTagAnnotationHandler {

	private final ApplicationContext context;
	private final Tracer tracer;

	SleuthSpanTagAnnotationHandler(ApplicationContext context, Tracer tracer) {
		this.context = context;
		this.tracer = tracer;
	}

	public void addAnnotatedParameters(JoinPoint pjp) {
		try {
			Signature signature = pjp.getStaticPart().getSignature();
			if (signature instanceof MethodSignature) {
				MethodSignature ms = (MethodSignature) signature;
				Method method = ms.getMethod();
				Method mostSpecificMethod = AopUtils.getMostSpecificMethod(method, pjp.getTarget().getClass());
				List<SleuthAnnotatedParameterContainer> annotatedParametersIndices = findAnnotatedParameters(
						mostSpecificMethod, pjp.getArgs());
				if (!method.equals(mostSpecificMethod)) {
					List<SleuthAnnotatedParameterContainer> annotatedParametersIndicesForActualMethod = findAnnotatedParameters(
							method, pjp.getArgs());
					mergeAnnotatedParameterContainers(annotatedParametersIndices, annotatedParametersIndicesForActualMethod);
				}
				addAnnotatedArguments(annotatedParametersIndices);
			}

		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private void mergeAnnotatedParameterContainers(List<SleuthAnnotatedParameterContainer> annotatedParametersIndices,
			List<SleuthAnnotatedParameterContainer> annotatedParametersIndicesForActualMethod) {
		for (SleuthAnnotatedParameterContainer container : annotatedParametersIndicesForActualMethod) {
			final int index = container.getParameterIndex();
			boolean parameterContained = annotatedParametersIndices.stream()
					.anyMatch(new Predicate<SleuthAnnotatedParameterContainer>() {
						@Override
						public boolean test(SleuthAnnotatedParameterContainer t) {
							return t.getParameterIndex() == index;
						}
					});
			if (!parameterContained) {
				annotatedParametersIndices.add(container);
			}
		}
	}

	private void addAnnotatedArguments(List<SleuthAnnotatedParameterContainer> toBeAdded) {
		for (SleuthAnnotatedParameterContainer container : toBeAdded) {
			String tagValue = resolveTagValue(container.getAnnotation(), container.getArgument());
			tracer.addTag(container.getAnnotation().value(), tagValue);
		}
	}

	String resolveTagValue(SleuthSpanTag annotation, Object argument) {
		if (argument == null) {
			return "null";
		}
		if (StringUtils.isNotBlank(annotation.tagValueResolverBeanName())) {
			SleuthTagValueResolver tagValueResolver = context.getBean(annotation.tagValueResolverBeanName(),
					SleuthTagValueResolver.class);
			if (tagValueResolver != null) {
				return tagValueResolver.resolveTagValue(argument);
			}
		} else if (StringUtils.isNotBlank(annotation.tagValueExpression())) {
			try {
				ExpressionParser expressionParser = new SpelExpressionParser();
				Expression expression = expressionParser.parseExpression(annotation.tagValueExpression());
				return expression.getValue(argument, String.class);
			} catch (Exception e) {
			}
		}
		return argument.toString();
	}

	private List<SleuthAnnotatedParameterContainer> findAnnotatedParameters(Method method, Object[] args) {
		Parameter[] parameters = method.getParameters();
		List<SleuthAnnotatedParameterContainer> result = new ArrayList<>();
		int i = 0;
		for (Parameter parameter : parameters) {
			if (parameter.isAnnotationPresent(SleuthSpanTag.class)) {
				SleuthSpanTag annotation = parameter.getAnnotation(SleuthSpanTag.class);
				SleuthAnnotatedParameterContainer container = new SleuthAnnotatedParameterContainer();
				container.setAnnotation(annotation);
				container.setArgument(args[i]);
				container.setParameter(parameter);
				container.setParameterIndex(i);
				result.add(container);
			}
			i++;
		}
		return result;
	}

}
