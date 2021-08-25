package org.comroid.test.mutatio.pipe;

import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.ref.ReferenceList;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PipeTest {
    private List<String> controlGroup;

    @Before
    public void setup() {
        controlGroup = Collections.unmodifiableList(IntStream.range(0, 50)
                .mapToObj(txt -> UUID.randomUUID())
                .map(UUID::toString)
                .collect(Collectors.toList()));
    }

    //@Test
    public void testBasicOperations() {
        final ReferenceList<String> strings = ReferenceList.of(controlGroup);

        final RefContainer<@NotNull Integer, Object> remapOp = strings.map(String::toLowerCase);
        for (int i = 0; i < controlGroup.size(); i++)
            Assert.assertEquals("index " + i, controlGroup.get(i).toLowerCase(), remapOp.getReference(i).get());

        final RefContainer<@NotNull Integer, String> filterOp = strings.filter(str -> str.chars()
                .map(Character::toLowerCase)
                .allMatch(c -> c != 'a'));
        for (int i = 0; i < filterOp.size(); i++)
            filterOp.getReference(i)
                    .wrap()
                    .map(String::toLowerCase)
                    .ifPresent(str -> Assert.assertFalse(str.contains("a")));

        final RefContainer<@NotNull Integer, String> filterMapOp = strings
                .map(String::toLowerCase)
                .filter(str -> str.chars()
                        .allMatch(c -> c != 'a'));
        for (int i = 0; i < filterOp.size(); i++)
            filterOp.getReference(i)
                    .wrap()
                    .ifPresent(str -> Assert.assertFalse(str.contains("a")));
    }
}
