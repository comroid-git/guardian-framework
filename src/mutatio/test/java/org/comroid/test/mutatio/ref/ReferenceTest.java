package org.comroid.test.mutatio.ref;

import org.comroid.mutatio.ref.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReferenceTest {
    private String testGroup;
    private AtomicInteger computationCounter;

    private Reference<String> emptyRef;
    private Reference<String> nullRef;
    private Reference<String> valueRef;
    private Reference<Integer> hashRef;

    @Before
    public void setup() {
        testGroup = UUID.randomUUID().toString();
        computationCounter = new AtomicInteger(0);

        emptyRef = Reference.empty();
        nullRef = Reference.constant(null);
        valueRef = Reference.create(testGroup);
        hashRef = valueRef.process().map(s -> {
            System.out.printf("Computation occurred; now %d\n", computationCounter.incrementAndGet());
            return s.hashCode();
        });
    }

    @Test
    public void test() {
        Assert.assertEquals("emptyRef != nullRef", emptyRef, nullRef);
    }

    @Test
    public void testGet() {
        Assert.assertNull("emptyRef", emptyRef.get());
        Assert.assertNull("nullRef", nullRef.get());
        Assert.assertNotNull("valueRef", valueRef.get());
        Assert.assertNotNull("hashRef", hashRef.get());

        hashRef.get();
        Assert.assertEquals("too many computations", 1, computationCounter.get());
    }

    @Test
    public void testWrap() {
        Assert.assertFalse("emptyRef", emptyRef.wrap().isPresent());
        Assert.assertFalse("nullRef", nullRef.wrap().isPresent());
        Assert.assertTrue("valueRef", valueRef.wrap().isPresent());
        Assert.assertTrue("hashRef", hashRef.wrap().isPresent());

        hashRef.get();
        Assert.assertEquals("too many computations", 1, computationCounter.get());
    }

    @Test(expected = NullPointerException.class)
    public void testRequireOnNull() {
        emptyRef.requireNonNull();
    }

    @Test
    public void testRequireOnNonNull() {
        Assert.assertEquals("valueRef", testGroup, valueRef.requireNonNull());
        Assert.assertEquals("hashRef", testGroup.hashCode(), (int) hashRef.requireNonNull());

        hashRef.get();
        Assert.assertEquals("too many computations", 1, computationCounter.get());
    }

    @Test
    public void testRecompute() {
        hashRef.get();
        hashRef.get();

        Assert.assertEquals("test group", testGroup, valueRef.get());
        Assert.assertEquals("test group hash", testGroup.hashCode(), (int) hashRef.requireNonNull("hash"));

        Assert.assertEquals("too many computations", 1, computationCounter.get());

        final String newValue = UUID.randomUUID().toString();

        Assert.assertTrue("Setting valueRef", valueRef.set(newValue));

        hashRef.get();
        hashRef.get();

        Assert.assertEquals("new value", newValue, valueRef.get());
        Assert.assertEquals("new hash", newValue.hashCode(), (int) hashRef.requireNonNull("hash"));

        Assert.assertEquals("too many computations", 2, computationCounter.get());
    }
}
