package org.comroid.restless.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.RatelimitedEndpoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public interface Ratelimiter extends BiFunction<RatelimitedEndpoint, REST.Request, CompletableFuture<REST.Request>> {
    FluentLogger logger = FluentLogger.forEnclosingClass();
    Ratelimiter INSTANT = new Support.Instant();

    static Ratelimiter ofPool(ScheduledExecutorService executor, RatelimitedEndpoint... endpoints) {
        return new Support.OfPool(executor, endpoints);
    }

    static int calculateOffset(int rps, int size) {
        return ((size / rps) * 1000) + ((size % rps) * (1000 / rps));
    }

    /**
     * Applies the provided {@linkplain REST.Request request} with the given {@linkplain RatelimitedEndpoint endpoint}
     * to this ratelimiter and returns a {@linkplain CompletableFuture CompletionStage} that completes with the given
     * {@linkplain REST.Request request} as soon as this ratelimiter allows its execution.
     *
     * @param restEndpoint The endpoint that is gonna be accessed
     * @param request      The request that is yet to be executed
     * @return A CompletableFuture that completes as soon as the ratelimiter allows execution of the request.
     */
    @Override
    CompletableFuture<REST.Request> apply(RatelimitedEndpoint restEndpoint, REST.Request request);

    final class Support {
        private static final class Instant implements Ratelimiter {
            private Instant() {
            }

            @Override
            public CompletableFuture<REST.Request> apply(RatelimitedEndpoint restEndpoint, REST.Request request) {
                return CompletableFuture.completedFuture(request);
            }
        }

        private static final class OfPool implements Ratelimiter {
            private final Map<RatelimitedEndpoint, Queue<BoxedRequest>> upcoming = new ConcurrentHashMap<>();
            private final ScheduledExecutorService executor;
            private final RatelimitedEndpoint[] pool;
            private final int globalRatelimit;

            private OfPool(ScheduledExecutorService executor, RatelimitedEndpoint[] pool) {
                Span<Integer> globalRatelimits = Stream.of(pool)
                        .map(RatelimitedEndpoint::getGlobalRatelimit)
                        .distinct()
                        .collect(Span.collector());
                if (!globalRatelimits.isSingle())
                    throw new IllegalArgumentException("Global ratelimit is not unique");

                this.executor = executor;
                this.pool = pool;
                this.globalRatelimit = globalRatelimits.requireNonNull();
            }

            @Override
            public synchronized CompletableFuture<REST.Request> apply(RatelimitedEndpoint restEndpoint, REST.Request request) {
                if (Arrays.stream(pool).noneMatch(restEndpoint::equals))
                    throw new IllegalArgumentException("Given endpoint is not part of pool");

                final int rps = restEndpoint.getRatePerSecond();
                final Queue<BoxedRequest> queue = queueOf(restEndpoint);

                if (queue.isEmpty() || (rps == -1 && restEndpoint.getGlobalRatelimit() == -1))
                    return CompletableFuture.completedFuture(request);

                return CompletableFuture.supplyAsync(() -> {
                    final BoxedRequest boxed = new BoxedRequest(request);

                    synchronized (queue) {
                        final int sendInMs = calculateOffset(rps, queue.size())
                                + calculateOffset(globalRatelimit, currentQueueSize());

                        logger.at(Level.FINE).log("Calculated execution offset of %dms for %s", request);

                        queue.add(boxed);
                        executor.schedule(() -> {
                            boxed.complete();

                            if (!queue.remove(boxed))
                                throw new RuntimeException("Could not remove BoxedRequest from Queue!");
                        }, sendInMs, TimeUnit.MILLISECONDS);
                    }

                    return boxed;
                }).thenCompose(boxed -> boxed.future);
            }

            private Queue<BoxedRequest> queueOf(RatelimitedEndpoint endpoint) {
                return upcoming.computeIfAbsent(endpoint, (key) -> new LinkedBlockingQueue<>());
            }

            private int currentQueueSize() {
                return upcoming.values()
                        .stream()
                        .mapToInt(Collection::size)
                        .sum();
            }
        }

        private static final class BoxedRequest {
            private final CompletableFuture<REST.Request> future = new CompletableFuture<>();
            private final REST.Request request;

            private BoxedRequest(REST.Request request) {
                this.request = request;
            }

            private void complete() {
                future.complete(request);
            }
        }
    }
}
