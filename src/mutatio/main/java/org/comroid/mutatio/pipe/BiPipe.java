package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.Reference;
import org.comroid.util.Pair;

import java.util.function.*;

public final class BiPipe<A, B, X, Y> extends BasicPipe<Pair<A, B>, Pair<X, Y>> {
    <T> BiPipe(Pipe<A> base, Function<A, B> bMapper) {
        super(base.map(a -> new Pair<>(a, bMapper.apply(a))));
    }

    private BiPipe(BiPipe<?, ?, A, B> base, StageAdapter<Pair<A, B>, Pair<X, Y>> adapter) {
        super(base, adapter);
    }

    public BiPipe<X, Y, X, Y> filter(BiPredicate<? super X, ? super Y> predicate) {
        return new BiPipe<>(this, StageAdapter
                .filter(pair -> predicate.test(pair.getFirst(), pair.getSecond())));
    }

    public BiPipe<X, Y, X, Y> filterFirst(Predicate<? super X> predicate) {
        return new BiPipe<>(this, StageAdapter
                .filter(pair -> predicate.test(pair.getFirst())));
    }

    public BiPipe<X, Y, X, Y> filterSecond(Predicate<? super Y> predicate) {
        return new BiPipe<>(this, StageAdapter
                .filter(pair -> predicate.test(pair.getSecond())));
    }

    public <R> BiPipe<X, Y, R, Y> mapFirst(Function<? super X, ? extends R> mapper) {
        return new BiPipe<>(this, StageAdapter
                .map(pair -> new Pair<>(mapper.apply(pair.getFirst()), pair.getSecond())));
    }

    public <R> BiPipe<X, Y, X, R> mapSecond(Function<? super Y, ? extends R> mapper) {
        return new BiPipe<>(this, StageAdapter
                .map(pair -> new Pair<>(pair.getFirst(), mapper.apply(pair.getSecond()))));
    }

    public <R> BiPipe<X, Y, R, Y> flatMapFirst(Function<? super X, ? extends Reference<? extends R>> mapper) {
        return new BiPipe<>(this, StageAdapter
                .map(pair -> new Pair<>(mapper.apply(pair.getFirst()).get(), pair.getSecond())));
    }

    public <R> BiPipe<X, Y, X, R> flatMapSecond(Function<? super Y, ? extends Reference<? extends R>> mapper) {
        return new BiPipe<>(this, StageAdapter
                .map(pair -> new Pair<>(pair.getFirst(), mapper.apply(pair.getSecond()).get())));
    }

    public <R> Pipe<R> merge(BiFunction<X, Y, R> mergeFunction) {
        return map(pair -> mergeFunction.apply(pair.getFirst(), pair.getSecond()));
    }

    public BiPipe<X, Y, X, Y> peek(BiConsumer<? super X, ? super Y> action) {
        return new BiPipe<>(this, StageAdapter
                .peek(pair -> action.accept(pair.getFirst(), pair.getSecond())));
    }

    public void forEach(BiConsumer<? super X, ? super Y> action) {
        forEach(pair -> action.accept(pair.getFirst(), pair.getSecond()));
    }

    public Pipe<X> drop() {
        return map(Pair::getFirst);
    }
}
