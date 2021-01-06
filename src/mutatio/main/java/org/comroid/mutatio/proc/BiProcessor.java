package org.comroid.mutatio.proc;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.ref.Reference;
import org.comroid.util.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

public interface BiProcessor<A, B> extends Processor<Pair<A, B>> {
    static <A, B> BiProcessor<A, B> from(Reference<? extends A> refA, Reference<? extends B> refB) {
        return new Support.FromRefs<>(refA, refB);
    }

    default BiProcessor<A, B> filter(BiPredicate<? super A, ? super B> predicate) {
        return addStage(StageAdapter.filter(pair -> predicate.test(pair.getFirst(), pair.getSecond())));
    }

    default BiProcessor<A, B> filterFirst(Predicate<? super A> predicate) {
        return addStage(StageAdapter.filter(pair -> predicate.test(pair.getFirst())));
    }

    default BiProcessor<A, B> filterSecond(Predicate<? super B> predicate) {
        return addStage(StageAdapter.filter(pair -> predicate.test(pair.getSecond())));
    }

    default <R> BiProcessor<R, B> mapFirst(Function<? super A, ? extends R> mapper) {
        return new Support.Base<>(this, StageAdapter.map(pair -> new Pair<>(mapper.apply(pair.getFirst()), pair.getSecond())));
    }

    default <R> BiProcessor<A, R> mapSecond(Function<? super B, ? extends R> mapper) {
        return new Support.Base<>(this, StageAdapter.map(pair -> new Pair<>(pair.getFirst(), mapper.apply(pair.getSecond()))));
    }

    default <R> BiProcessor<R, B> flatMapFirst(Function<? super A, ? extends Reference<? extends R>> flatMapper) {
        return new Support.Base<>(this, StageAdapter.map(pair -> new Pair<>(
                flatMapper.apply(pair.getFirst()).get(),
                pair.getSecond()
        )));
    }

    default <R> BiProcessor<A, R> flatMapSecond(Function<? super B, ? extends Reference<? extends R>> flatMapper) {
        return new Support.Base<>(this, StageAdapter.map(pair -> new Pair<>(
                pair.getFirst(),
                flatMapper.apply(pair.getSecond()).get()
        )));
    }

    default BiProcessor<A, B> peek(BiConsumer<? super A, ? super B> action) {
        return filter((a, b) -> {
            action.accept(a, b);
            return true;
        });
    }

    default <R> Reference<R> merge(BiFunction<A, B, R> mergeFunction) {
        return Reference.provided(() -> {
            final Pair<A, B> pair = get();

            if (pair == null) return null;
            return mergeFunction.apply(pair.getFirst(), pair.getSecond());
        });
    }

    @Internal
    <X, Y> BiProcessor<X, Y> addStage(StageAdapter<Pair<A, B>, Pair<X, Y>> adapter);

    default Processor<A> drop() {
        return map(Pair::getFirst);
    }

    final class Support {
        public static class Base<Ai, Bi, Ao, Bo>
                extends Processor.Support.Base<Pair<Ai, Bi>, Pair<Ao, Bo>>
                implements BiProcessor<Ao, Bo> {
            protected final @Nullable Reference<? extends Ai> refA;
            protected final @Nullable Reference<? extends Bi> refB;
            private final StageAdapter<Pair<Ai, Bi>, Pair<Ao, Bo>> adapter;
            private final Reference<Pair<Ao, Bo>> advancedRef;

            protected Base(
                    @NotNull Reference<Pair<Ai, Bi>> dummyParentRef,
                    Reference<? extends Ai> refA,
                    Reference<? extends Bi> refB,
                    @Nullable StageAdapter<Pair<Ai, Bi>, Pair<Ao, Bo>> adapter
            ) {
                super(dummyParentRef);

                this.refA = refA;
                this.refB = refB;
                this.adapter = adapter;
                this.advancedRef = adapter.advance(parent);
            }

            protected Base(
                    Reference<Pair<Ai, Bi>> parent,
                    StageAdapter<Pair<Ai, Bi>, Pair<Ao, Bo>> adapter
            ) {
                super(parent);

                this.refA = null;
                this.refB = null;
                this.adapter = adapter;
                this.advancedRef = adapter.advance(parent);
            }

            @Override
            public <X, Y> BiProcessor<X, Y> addStage(StageAdapter<Pair<Ao, Bo>, Pair<X, Y>> adapter) {
                return new Base<>(this, adapter);
            }

            @Override
            protected Pair<Ao, Bo> doGet() {
                return advancedRef.get();
            }
        }

        public static class FromRefs<A, B> extends Base<A, B, A, B> {
            protected FromRefs(Reference<? extends A> refA, Reference<? extends B> refB) {
                super(new DummyParentRef<>(refA, refB), refA, refB, StageAdapter.filter(any -> true));
            }
        }

        private static final class DummyParentRef<A, B> extends Reference.Support.Default<Pair<A, B>> {
            private final Reference<A> refA;
            private final Reference<B> refB;

            @Override
            public boolean isOutdated() {
                return refA.isOutdated() || refB.isOutdated();
            }

            public DummyParentRef(Reference<? extends A> refA, Reference<? extends B> refB) {
                super(true, new Pair<>(refA.get(), refB.get()));

                this.refA = Polyfill.uncheckedCast(refA);
                this.refB = Polyfill.uncheckedCast(refB);
            }

            @Override
            protected Pair<A, B> doGet() {
                return new Pair<>(refA.get(), refB.get());
            }

            @Override
            protected boolean doSet(Pair<A, B> value) {
                return refA.set(value.getFirst()) & refB.set(value.getSecond());
            }
        }
    }
}
