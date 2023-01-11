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
package org.springframework.data.jpa.repository.query;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.util.Lazy;

/**
 * Encapsulates different strategies for the creation of a {@link QueryEnhancer} from a {@link DeclaredQuery}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @since 2.7.0
 */
public final class QueryEnhancerFactory {

	private static final Log LOG = LogFactory.getLog(QueryEnhancerFactory.class);

	private static final boolean JSQLPARSER_IN_CLASSPATH = isJSqlParserInClassPath();

	private QueryEnhancerFactory() {}

	/**
	 * Creates a {@link List} containing all the possible {@link QueryEnhancer} implementations for native queries. The
	 * order of the entries indicates that the first {@link QueryEnhancer} should be used. If this is not possible the
	 * next until the last one is reached which will be always {@link DefaultQueryEnhancer}.
	 *
	 * @param query the query for which the list should be created for
	 * @return list containing all the suitable implementations for the given query
	 */
	private static List<Lazy<QueryEnhancer>> nativeQueryEnhancers(DeclaredQuery query) {

		ArrayList<Lazy<QueryEnhancer>> suitableImplementations = new ArrayList<>();

		if (qualifiesForJSqlParserUsage(query)) {
			suitableImplementations.add(Lazy.of(() -> new JSqlParserQueryEnhancer(query)));
		}

		// DefaultQueryEnhancer has to be the last since this is our fallback
		suitableImplementations.add(Lazy.of(() -> new DefaultQueryEnhancer(query)));
		return suitableImplementations;
	}

	/**
	 * Creates a {@link List} containing all the possible {@link QueryEnhancer} implementations for non-native queries.
	 * The order of the entries indicates that the first {@link QueryEnhancer} should be used. If this is not possible the
	 * next until the last one is reached which will be always {@link DefaultQueryEnhancer}.
	 *
	 * @param query the query for which the list should be created for
	 * @return list containing all the suitable implementations for the given query
	 */
	private static List<Lazy<QueryEnhancer>> nonNativeQueryEnhancers(DeclaredQuery query) {

		ArrayList<Lazy<QueryEnhancer>> suitableImplementations = new ArrayList<>();

		// DefaultQueryEnhancer has to be the last since this is our fallback
		suitableImplementations.add(Lazy.of(() -> new DefaultQueryEnhancer(query)));
		return suitableImplementations;
	}

	/**
	 * Creates a new {@link QueryEnhancer} for the given {@link DeclaredQuery}.
	 *
	 * @param query must not be {@literal null}.
	 * @return an implementation of {@link QueryEnhancer} that suits the query the most
	 */
	public static QueryEnhancer forQuery(DeclaredQuery query) {

		if (query.hasQueryEnhancerOverride()) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Using " + QueryEnhancerOverride.class.getSimpleName()
						+ " for the query [%s]".formatted(query.getQueryString()));
			}
			return getExplicitlyChosenQueryEnhancer(query);
		}

		if (query.getQueryEnhancer() != null) {

			if (LOG.isDebugEnabled()) {
				LOG.debug("Using " + query.getQueryEnhancer().getClass().getSimpleName()
						+ " for the query [%s]".formatted(query.getQueryString()));
			}
			return query.getQueryEnhancer();
		}

		return findBestQueryEnhancer(query);
	}

	/**
	 * Gets the {@link QueryEnhancer} that was selected by using the {@link QueryEnhancerOverride}.
	 *
	 * @param query the query for which we want to extract the {@link QueryEnhancer}
	 * @return an implementation of {@link QueryEnhancer} that was provided as {@link QueryEnhancerOverride}
	 */
	private static QueryEnhancer getExplicitlyChosenQueryEnhancer(DeclaredQuery query) {

		try {
			return Objects.requireNonNull(query.getQueryEnhancerOverride()) //
					.value() //
					.getConstructor(DeclaredQuery.class) //
					.newInstance(query);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new IllegalStateException("Could not create QueryEnhancer of type %s for query [%s]!".formatted(
					Objects.requireNonNull(query.getQueryEnhancerOverride()).value().getName(), query.getQueryString()), e);
		}
	}

	/**
	 * Tries to find the best {@link QueryEnhancer} implementation for the given query.
	 *
	 * @param query the query for which we want to find the implementation
	 * @return the best fit {@link QueryEnhancer}
	 */
	private static QueryEnhancer findBestQueryEnhancer(DeclaredQuery query) {

		List<Lazy<QueryEnhancer>> suitableQueryEnhancers = query.isNativeQuery() //
				? nativeQueryEnhancers(query) //
				: nonNativeQueryEnhancers(query);

		for (Lazy<QueryEnhancer> suitableQueryEnhancer : suitableQueryEnhancers) {

			try {
				return suitableQueryEnhancer.get();
			} catch (Exception e) {
				LOG.debug("Falling back to next QueryEnhancer implementation, due to exception.", e);
			}
		}

		throw new IllegalStateException(
				"No QueryEnhancer found for the query [%s]! This should not happen since the default implementation (DefaultQueryEnhancer) should have been called!"
						.formatted(query.getQueryString()));
	}

	/**
	 * Checks if a given query can be process with the JSqlParser under the condition that the parser is in the classpath.
	 *
	 * @param query the query we want to check
	 * @return <code>true</code> if JSqlParser is in the classpath and the query is classified as a native query otherwise
	 *         <code>false</code>
	 */
	private static boolean qualifiesForJSqlParserUsage(DeclaredQuery query) {
		return JSQLPARSER_IN_CLASSPATH && query.isNativeQuery();
	}

	/**
	 * Checks whether JSqlParser is in classpath or not.
	 *
	 * @return <code>true</code> when in classpath otherwise <code>false</code>
	 */
	private static boolean isJSqlParserInClassPath() {

		try {
			Class.forName("net.sf.jsqlparser.parser.JSqlParser", false, QueryEnhancerFactory.class.getClassLoader());
			LOG.info("JSqlParser is in classpath; If applicable JSqlParser will be used");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
