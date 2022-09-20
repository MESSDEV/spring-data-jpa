/*
 * Copyright 2008-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to explicitly select which {@link QueryEnhancer} to use. If placed on a repository, the given
 * {@link QueryEnhancer} will be used for all queries. If provided on a method, the given {@link QueryEnhancer} will be
 * only used for that method. NOTE: Method-level overrides superceded repository-level overrides.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @since 3.1
 */
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryEnhancerOverride {

	/**
	 * The selected {@link QueryEnhancer} to use.
	 */
	Class<? extends QueryEnhancer> value() default DefaultQueryEnhancer.class;
}
