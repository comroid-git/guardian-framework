package org.comroid.uniform.adapter.xml.jsoup;

import org.comroid.abstr.AbstractMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public final class JsoupObjectAdapter implements AbstractMap<String, Object> {
    private final Element xml;

    JsoupObjectAdapter(Element xml) {
        this.xml = xml;
    }

    @Override
    public Object get(Object key) {
        return xml.selectFirst(String.valueOf(key));
    }

    @Nullable
    @Override
    public Object put(String key, Object value) {
        return xml.selectFirst(key).html(String.valueOf(value));
    }

    @Override
    public Object remove(Object key) {
        Element element = xml.selectFirst(String.valueOf(key));
        element.remove();
        return element;
    }

    @Override
    public void clear() {
        xml.empty();
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(xml.getAllElements()
                .stream()
                .map(dom -> new java.util.AbstractMap.SimpleEntry<String, Object>(dom.tagName(), dom.html()) {
                    @Override
                    public Object setValue(Object value) {
                        return dom.html(String.valueOf(value));
                    }
                })
                .collect(Collectors.toSet()));
    }

    @Override
    public String toString() {
        return xml.toString();
    }
}
