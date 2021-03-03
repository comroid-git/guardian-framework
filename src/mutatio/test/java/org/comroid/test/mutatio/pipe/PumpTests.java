package org.comroid.test.mutatio.pipe;

/*
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
    public void testFull() {
        final Pump<String> pump1 = Pump.create();
        final Pipe<Integer> remapOp = pump1.map(String::hashCode);
        // fill with existing
        controlGroup.forEach(getStringConsumer(pump1));

        // check if pump sizes
        Assert.assertEquals("pump size", controlGroup.size(), pump1.size());
        Assert.assertEquals("remapOp size", controlGroup.size(), remapOp.size());

        // check existing
        for (int i = 0; i < controlGroup.size(); i++)
            Assert.assertEquals("index " + i, controlGroup.get(i).hashCode(), (int) remapOp.requireNonNull(i));

        pump1.clear();
        // check if remap op is empty
        Assert.assertEquals("pump size", 0, pump1.size());
        Assert.assertEquals("remapOp size", 0, remapOp.size());

        final int[] count = {0};
        // define test for future strings
        remapOp.forEach(hash -> {
            Assert.assertTrue("unknown future hash: " + hash, controlGroup.stream()
                    .map(String::hashCode)
                    .anyMatch(hash::equals));
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
} TODO: f i x
*/