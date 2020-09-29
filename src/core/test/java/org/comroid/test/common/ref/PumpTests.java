package org.comroid.test.common.ref;

import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PumpTests {
    private List<String> controlGroup;

    @Before
    public void setup() {
        controlGroup = Collections.unmodifiableList(IntStream.range(0, 50)
                .mapToObj(txt -> UUID.randomUUID())
                .map(UUID::toString)
                .collect(Collectors.toList()));
    }

    @Test
    public void testBasicOperations() throws InterruptedException {
        final Pump<String> remapOp = (Pump<String>) Pump.<String>create()
                .map(String::toLowerCase);
        controlGroup.stream()
                .map(Reference::constant)
                .forEach(remapOp);
        for (int i = 0; i < controlGroup.size(); i++)
            Assert.assertEquals("index " + i, controlGroup.get(i).toLowerCase(), remapOp.get(i));

        final Pump<String> filterOp = (Pump<String>) Pump.<String>create()
                .filter(str -> str.chars()
                        .map(Character::toLowerCase)
                        .allMatch(c -> c != 'a'));
        controlGroup.stream()
                .map(Reference::constant)
                .forEach(filterOp);
        for (int i = 0; i < filterOp.size(); i++)
            filterOp.getReference(i)
                    .wrap()
                    .map(String::toLowerCase)
                    .ifPresent(str -> Assert.assertFalse(str.contains("a")));

        final Pump<String> filterMapOp = (Pump<String>) Pump.<String>create()
                .map(String::toLowerCase)
                .filter(str -> str.chars()
                        .allMatch(c -> c != 'a'));
        controlGroup.stream()
                .map(Reference::constant)
                .forEach(filterMapOp);
        for (int i = 0; i < filterOp.size(); i++)
            filterOp.getReference(i)
                    .wrap()
                    .ifPresent(str -> Assert.assertFalse(str.contains("a")));
    }
}
