package org.comroid.restless.endpoint;

public interface EndpointScope extends RatelimitDefinition {
    String getUrlExtension();

    String[] getRegExpGroups();

    default int getParameterCount() {
        return getRegExpGroups().length;
    }

    @Override
    default int getRatePerSecond() {
        return -1;
    }

    @Override
    default int getGlobalRatelimit() {
        return -1;
    }
}
