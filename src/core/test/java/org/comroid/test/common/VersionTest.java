package org.comroid.test.common;

import org.comroid.common.Version;
import org.junit.Assert;
import org.junit.Test;

public class VersionTest {
    @Test
    public void testVersion() {
        final Version version1 = new Version("1.1.3");
        final Version version2 = new Version("1.2");
        final Version version3 = new Version("1.2-12");

        Assert.assertTrue(version1.compareTo(version2) < 0);
        Assert.assertTrue(version3.compareTo(version2) > 0);

        final Version version4 = new Version("1.2.3-A");
        final Version version5 = new Version("1.2.3-RC");
        final Version version6 = new Version("1.2.3-RC2");

        Assert.assertTrue(version5.compareTo(version4) > 0);
        Assert.assertTrue(version6.compareTo(version4) > 0);

        Assert.assertTrue(version4.compareTo(version5) < 0);
        Assert.assertTrue(version4.compareTo(version6) < 0);
    }
}
