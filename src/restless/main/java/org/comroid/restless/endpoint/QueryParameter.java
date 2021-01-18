package org.comroid.restless.endpoint;

import org.intellij.lang.annotations.Language;

public final class QueryParameter {
    private QueryParameter() {
        throw new AbstractMethodError();
    }

    @Language("RegExp")
    public static String regex(@Language("RegExp") String valueRegex) {
        return regex(true, valueRegex);
    }

    @Language("RegExp")
    public static String regex(boolean first, @Language("RegExp") String valueRegex) {
        return String.format("(%s[\\w\\S]+?=%s)?",
                first ? "\\?" : '&',
                valueRegex
        );
    }

    public static String param(String name, Object value) {
        return param(true, name, value);
    }

    public static String param(boolean first, String name, Object value) {
        return String.format("%s%s=%s", first ? '?' : '&', name, value);
    }
}
