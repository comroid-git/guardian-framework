package org.comroid.restless.endpoint;

public interface RatelimitedEndpoint {
    /**
     * @return The rate of how often this endpoint can be accessed per second.
     * Equals {@code -1}, if there is no ratelimit on this endpoint.
     */
    int getRatePerSecond();

    /**
     * @return The global ratelimit of all endpoints in this pool.
     * Equals {@code -1}, if there is no global ratelimit.
     */
    int getGlobalRatelimit();
}
