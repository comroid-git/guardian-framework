package org.comroid.test.varbind;

import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.multipart.PartialBind;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static org.comroid.test.varbind.FastJSONLib.fastJsonLib;

public class BindBuilderTest {
    private GroupBind<Dummy, Dummy> group;
    private UniObjectNode objData;

    public static String reverse(String str) {
        return str.chars()
                .mapToObj(x -> String.valueOf((char) x))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.joining());
    }

    @Before
    public void setup() {
        this.group = new GroupBind<>(fastJsonLib, "dummy");
        this.objData = UniObjectNode.ofMap(fastJsonLib, Map.of("name", "lucas"));
    }

    @Test
    public void testSingleIdentities() {
        final VarBind<Object, String, String, String> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .asIdentities()
                .onceEach()
                .build();

        validateMembers(bind);

        final Span<String> extract = bind.extract(objData);

        Assert.assertTrue(extract.isSingle());
        Assert.assertEquals("lucas", extract.get());

        final Span<String> process = bind.remapAll(null, extract);

        Assert.assertTrue(process.isSingle());
        Assert.assertEquals("lucas", process.get());

        final String finish = bind.finish(process);

        Assert.assertEquals("lucas", finish);
    }

    @Test
    public void testSingleRemapped() {
        final VarBind<Object, String, String, String> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .andRemap(BindBuilderTest::reverse)
                .onceEach()
                .build();

        validateMembers(bind);

        final Span<String> extract = bind.extract(objData);

        Assert.assertTrue(extract.isSingle());
        Assert.assertEquals("lucas", extract.get());

        final Span<String> process = bind.remapAll(null, extract);

        Assert.assertTrue(process.isSingle());
        Assert.assertEquals(reverse("lucas"), process.get());

        final String finish = bind.finish(process);

        Assert.assertEquals(reverse("lucas"), finish);
    }

    @Test
    public void testSingleResolved() {
        final VarBind<Object, String, String, String> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .andResolve((str, dummy) -> dummy.modify(str))
                .onceEach()
                .build();

        validateMembers(bind);

        final Dummy dummy = new Dummy();

        final Span<String> extract = bind.extract(objData);

        Assert.assertTrue(extract.isSingle());
        Assert.assertEquals("lucas", extract.get());

        final Span<String> process = bind.remapAll(dummy, extract);

        Assert.assertTrue(process.isSingle());
        Assert.assertEquals(dummy.modify("lucas"), process.get());

        final String finish = bind.finish(process);

        Assert.assertEquals(dummy.modify("lucas"), finish);
    }

    @Test
    public void testListedSimple() {
        final VarBind<Object, String, String, ArrayList<String>> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .asIdentities()
                .intoCollection(ArrayList::new)
                .build();

        validateMembers(bind);

        // todo: Create test
    }

    @Test
    public void testListedRemapped() {
        final VarBind<Object, String, String, ArrayList<String>> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .asIdentities()
                .intoCollection(ArrayList::new)
                .build();

        validateMembers(bind);

        // todo: Create test
    }

    @Test
    public void testListedResolved() {
        final VarBind<Object, String, String, ArrayList<String>> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .asIdentities()
                .intoCollection(ArrayList::new)
                .build();

        validateMembers(bind);

        // todo: Create test
    }

    @Test(expected = NullPointerException.class)
    public void testNullAsDependency() {
        final VarBind<Object, String, String, String> bind
                = group.createBind("name")
                .extractAs(ValueType.STRING)
                .andResolve((str, dummy) -> dummy.modify(str))
                .onceEach()
                .build();

        validateMembers(bind);

        bind.remap("", null);
    }

    public void validateMembers(final VarBind<Object, ?, ?, ?> bind) {
        Assert.assertTrue(bind.as(PartialBind.Base.class).isPresent());
        Assert.assertTrue(bind.as(PartialBind.Grouped.class).isPresent());
        Assert.assertTrue(bind.as(PartialBind.Extractor.class).isPresent());
        Assert.assertTrue(bind.as(PartialBind.Remapper.class).isPresent());
        Assert.assertTrue(bind.as(PartialBind.Finisher.class).isPresent());
    }
}
