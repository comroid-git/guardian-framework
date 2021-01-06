package org.comroid.test.common.ref;

import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PumpTests {
    private List<String> controlGroup;

    @Before
    public void setup() {
        controlGroup = Collections.unmodifiableList(IntStream.range(0, 50)
                .mapToObj(txt -> UUID.randomUUID())
                .map(UUID::toString)
                .map(String::toUpperCase)
                .collect(Collectors.toList()));
    }

    @Test
    public void testFull() {
        final Pump<String> pump1 = Pump.create();
        final Pump<String> remapOp = (Pump<String>) pump1
                .map(String::toLowerCase);
        // fill with existing
        controlGroup.forEach(getStringConsumer(pump1));

        // check if pump sizes
        Assert.assertEquals("pump size", controlGroup.size(), pump1.size());
        Assert.assertEquals("remapOp size", controlGroup.size(), remapOp.size());

        // check existing
        for (int i = 0; i < controlGroup.size(); i++)
            Assert.assertEquals("index " + i, controlGroup.get(i).toLowerCase(), remapOp.get(i));

        pump1.clear();
        // check if remap op is empty
        Assert.assertEquals("pump size", 0, pump1.size());
        Assert.assertEquals("remapOp size", 0, remapOp.size());

        final int[] count = {0};
        // define test for future strings
        remapOp.forEach(str -> {
            Assert.assertTrue("unknown future str: " + str, controlGroup.contains(str.toUpperCase()));
            count[0]++;
        });
        // and feed control group
        controlGroup.forEach(getStringConsumer(pump1));
        // and validate executions
        Assert.assertEquals("automated executions", controlGroup.size(), count[0]);
    }

    @NotNull
    private Consumer<String> getStringConsumer(Pump<String> pump) {
        return str -> pump.accept(Reference.constant(str));
    }
}
