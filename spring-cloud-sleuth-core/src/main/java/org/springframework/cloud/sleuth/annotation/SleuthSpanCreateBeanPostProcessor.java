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

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

class SleuthSpanCreateBeanPostProcessor implements BeanPostProcessor {
	
	private SleuthSpanCreatorAdvice advice;

	public SleuthSpanCreateBeanPostProcessor(SleuthSpanCreatorAdvice advice) {
		this.advice = advice;
	}
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws
			BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws
			BeansException {
		boolean atLeastOneMethodAnnotated = false;
		for (Method method : bean.getClass().getMethods()) {
			if (SleuthAnnotationUtils.isMethodAnnotated(method)) {
				atLeastOneMethodAnnotated = true;
				break;
			}
		}
		if (!atLeastOneMethodAnnotated && (AopUtils.isAopProxy(bean) || AopUtils.isCglibProxy(bean) || AopUtils
				.isJdkDynamicProxy(bean))) {
			Class<?> beanTargetClass = AopUtils.getTargetClass(bean);
			for (Method method : beanTargetClass.getMethods()) {
				if (SleuthAnnotationUtils.isMethodAnnotated(method)) {
					atLeastOneMethodAnnotated = true;
					break;
				}
			}
		}
		if (!atLeastOneMethodAnnotated) {
			return bean;
		}
		AspectJProxyFactory factory = new AspectJProxyFactory(bean);
		factory.addAspect(advice);
		return factory.getProxy();
	}

}
