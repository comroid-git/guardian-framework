package org.comroid.webkit.model;

import org.comroid.restless.REST;

import java.util.Map;

public interface PagePropertiesProvider {
    Map<String, Object> findPageProperties(REST.Header.List headers);
}
