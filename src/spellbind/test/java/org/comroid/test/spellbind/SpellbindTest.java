package org.comroid.test.spellbind;

import org.comroid.api.Invocable;
import org.comroid.api.UUIDContainer;
import org.comroid.spellbind.SpellCore;
import org.comroid.spellbind.model.TypeFragmentProvider;
import org.comroid.test.model.AnotherPartialAbstract;
import org.comroid.test.model.FullAbstract;
import org.comroid.test.model.NonAbstract;
import org.comroid.test.model.PartialAbstract;
import org.junit.Assert;
import org.junit.Test;

public class SpellbindTest {
    @Test
    public void testDirectBound() {
        final ImplementingClass implementingClass = new ImplementingClass();

        MainInterface proxy = SpellCore.builder(MainInterface.class, implementingClass)
                .addFragment(MainInterface.PROVIDER)
                .build();
        Assert.assertSame(proxy, proxy.self());

        Assert.assertTrue(proxy.cast(PartialAbstract.class)
                .isPresent());
        Assert.assertFalse(proxy.string()
                .isPresent());

        Assert.assertEquals(47, proxy.add(5));
        Assert.assertEquals(42, proxy.getValue());

        Assert.assertEquals("some class", proxy.name());
        Assert.assertFalse(proxy.invert(true));
    }

    @Test
    public void testMultipleBound() {
        final ImplementingClass implementingClass = new ImplementingClass();

        HyperInterface proxy = SpellCore.builder(HyperInterface.class, implementingClass)
                .addFragment(HyperInterface.PROVIDER)
                .build();
        Assert.assertSame(proxy, proxy.self());

        Assert.assertTrue(proxy.cast(PartialAbstract.class)
                .isPresent());
        Assert.assertFalse(proxy.string()
                .isPresent());

        Assert.assertEquals(47, proxy.add(5));
        Assert.assertEquals(42, proxy.getValue());

        Assert.assertEquals("some class", proxy.name());
        Assert.assertFalse(proxy.invert(true));

        Assert.assertTrue(proxy instanceof CharSequence);
        Assert.assertEquals(5, proxy.length());
        Assert.assertEquals('e', proxy.charAt(1));
        Assert.assertEquals('l', proxy.charAt(3));
        Assert.assertEquals("ello", proxy.subSequence(1, 5));

        Assert.assertTrue(proxy instanceof AnotherPartialAbstract);
        Assert.assertEquals(6.2d, proxy.getAnother(), 0.0d);
        Assert.assertEquals(-6d, proxy.sub(0.2d), 0.0d);
    }

    public interface HyperInterface extends MainInterface, CharSequence, AnotherPartialAbstract {
        TypeFragmentProvider<HyperInterface> PROVIDER = new TypeFragmentProvider<HyperInterface>() {
            @Override
            public Class<HyperInterface> getInterface() {
                return HyperInterface.class;
            }

            @Override
            public Invocable<? extends HyperInterface> getInstanceSupplier() {
                return Invocable.ofConstructor(SubImpl.class);
            }
        };

        @SuppressWarnings("NullableProblems")
        class SubImpl extends ImplementingClass implements HyperInterface {
            @Override
            public double getAnother() {
                return 6.2d;
            }

            @Override
            public int length() {
                return 5;
            }

            @Override
            public char charAt(int index) {
                return "hello".charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return "hello".subSequence(start, end);
            }
        }
    }

    public interface MainInterface extends NonAbstract, PartialAbstract, FullAbstract {
        TypeFragmentProvider<MainInterface> PROVIDER = new TypeFragmentProvider<MainInterface>() {
            @Override
            public Class<MainInterface> getInterface() {
                return MainInterface.class;
            }

            @Override
            public Invocable<? extends MainInterface> getInstanceSupplier() {
                return Invocable.ofConstructor(ImplementingClass.class);
            }
        };
    }

    public static class ImplementingClass extends UUIDContainer.Base implements MainInterface {
        @Override
        public int getValue() {
            return 42;
        }

        @Override
        public boolean invert(boolean val) {
            return !val;
        }

        @Override
        public String name() {
            return "some class";
        }
    }
}
