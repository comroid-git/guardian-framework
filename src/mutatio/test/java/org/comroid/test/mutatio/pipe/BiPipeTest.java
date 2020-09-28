package org.comroid.test.mutatio.pipe;

import org.comroid.mutatio.span.Span;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BiPipeTest {
    private Span<String> refs;

    @Before
    public void setup() {
        this.refs = IntStream.range(0, 30)
                .mapToObj(x -> IntStream.range(0, x)
                        .mapToObj(y -> 'a')
                        .map(String::valueOf)
                        .collect(Collectors.joining()))
                .collect(Span.collector());
    }

    @Test
    public void testSimple() {
        refs.pipe()
                .bi(String::length)
                .forEach((str, len) -> Assert.assertEquals(str.length(), (int) len));
    }

    @Test
    public void testRemapFirst() {
        refs.pipe()
                .bi(String::length)
                .filterSecond(x -> x > 3)
                .mapFirst(str -> str.substring(str.length() + 2))
                .forEach((str, len) -> Assert.assertEquals((int) len, str.length() - 2));
    }

    @Test
    public void testRemapSecond() {
        refs.pipe()
                .bi(String::length)
                .mapSecond(x -> x * 2)
                .forEach((str, len) -> Assert.assertEquals(str.length() * 2, (int) len));
    }
}
