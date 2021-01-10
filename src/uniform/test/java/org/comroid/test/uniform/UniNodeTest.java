package org.comroid.test.uniform;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.comroid.uniform.adapter.json.fastjson.FastJSONLib.fastJsonLib;

public class UniNodeTest {
    private final Random rng = new Random();
    private UniObjectNode object;
    private UniArrayNode array;
    private Map<String, Integer> randomMap;
    private List<Integer> randomInts;

    @Before
    public void setup() {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();

        randomInts = IntStream.range(0, 50)
                .mapToObj(x -> rng.nextInt(500))
                .distinct()
                .collect(Collectors.toList());
        randomMap = randomInts.stream()
                .collect(Collectors.toMap(x -> String.valueOf(x * x), x -> x));

        object.putAll(randomMap);
        array.addAll(randomInts);

        this.object = fastJsonLib.createUniObjectNode(object);
        this.array = fastJsonLib.createUniArrayNode(array);

        Assert.assertNotNull("object node", object);
        Assert.assertNotNull("arry node", array);
    }

    @Test
    public void testObject() {
        randomMap.forEach((key, value) -> {
            UniNode valueNode = object.get(key);
            Assert.assertTrue("valueNode type: " + valueNode.getClass(), valueNode instanceof UniValueNode);
            Assert.assertEquals("valueNode value", (int) value, valueNode.asInt(0));
        });
        System.out.println("object.toString() = " + object.toString());
    }

    @Test
    public void testArray() {
        for (int i = 0; i < array.size(); i++) {
            Integer value = array.get(i)
                    .asInt(0);

            Assert.assertEquals(randomInts.get(i), value);
        }
        System.out.println("array.toString() = " + array.toString());
    }
}
