package org.comroid.test.mutatio.pipe;

import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.ref.ReferenceMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BiPipeTest {
    private List<String> controlGroup;

    @Before
    public void setup() {
        controlGroup = Collections.unmodifiableList(IntStream.range(0, 50)
                .mapToObj(txt -> UUID.randomUUID())
                .map(UUID::toString)
                .collect(Collectors.toList()));
    }

    @Test
    public void testSimple() {
        ReferenceList.of(controlGroup)
                .bi(String::hashCode)
                .forEach((hash, str) -> Assert.assertEquals("hash code", (long) hash, str.hashCode()));
    }

    @Test
    public void testMap() {
        final ReferenceMap<Integer, String> map = ReferenceList.of(controlGroup)
                .bi(String::hashCode)
                .mapKey(String::valueOf)
                .map(String::toUpperCase)
                .mapKey(Integer::parseInt)
                .map(String::toLowerCase)
                .distinctKeys();
        controlGroup.forEach(uid -> Assert.assertEquals("map entry", uid, map.get(uid.hashCode())));
    }
}
