package org.comroid.uniform.adapter.json.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.comroid.abstr.AbstractList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ListIterator;

public class ArrayAdapter implements AbstractList<Object> {
    private final JsonArray node;

    public ArrayAdapter(JsonArray node) {
        this.node = node;
    }

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public boolean add(Object o) {
        if (o instanceof Boolean)
            node.add((boolean) o);
        else if (o instanceof String)
            node.add((String) o);
        else if (o instanceof Number)
            node.add((Number) o);
        else return false;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public void clear() {
        node
    }

    @Override
    public Object get(int index) {
        return null;
    }

    @Override
    public Object set(int index, Object element) {
        return null;
    }

    @Override
    public void add(int index, Object element) {

    }

    @Override
    public Object remove(int index) {
        return null;
    }

    @NotNull
    @Override
    public ListIterator<Object> listIterator(int index) {
        return null;
    }

    @NotNull
    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return null;
    }
}
