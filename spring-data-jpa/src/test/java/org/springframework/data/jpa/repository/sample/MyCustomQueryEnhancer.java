/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.DefaultQueryEnhancer;
import org.springframework.data.jpa.repository.query.QueryEnhancer;

/**
 * Test out a custom {@link QueryEnhancer} that leverages the {@link DefaultQueryEnhancer} under the hood.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 */
public class MyCustomQueryEnhancer extends QueryEnhancer {

	private DefaultQueryEnhancer defaultQueryEnhancer;

	public MyCustomQueryEnhancer(DeclaredQuery query) {

		super(query);
		this.defaultQueryEnhancer = new DefaultQueryEnhancer(query);
	}

	@Override
	public String applySorting(Sort sort, String alias) {
		return defaultQueryEnhancer.applySorting(sort, alias);
	}

	@Override
	public String detectAlias() {
		return defaultQueryEnhancer.detectAlias();
	}

	@Override
	public String createCountQueryFor(String countProjection) {
		// we return this because we use this to test if the correct enhancer is used
		return "SELECT distinct(1) FROM User u";
	}

	@Override
	public String getProjection() {
		return defaultQueryEnhancer.getProjection();
	}

	@Override
	public Set<String> getJoinAliases() {
		return defaultQueryEnhancer.getJoinAliases();
	}

}
