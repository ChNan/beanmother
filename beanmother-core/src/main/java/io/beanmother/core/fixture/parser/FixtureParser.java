package io.beanmother.core.fixture.parser;

import io.beanmother.core.fixture.FixtureMap;

import java.util.Map;

/**
 * The root interface for parsing fixture string to {@link java.util.Map<String, Object>}
 */
public interface FixtureParser {
    /**
     * Parse fixture string to Map
     * @return fixture map
     */
    Map<String, FixtureMap> parse(String fixture);
}