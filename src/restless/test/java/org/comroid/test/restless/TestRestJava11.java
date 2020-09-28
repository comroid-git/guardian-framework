package org.comroid.test.restless;

import org.comroid.restless.REST;
import org.comroid.restless.adapter.jdk.JavaHttpAdapter;
import org.comroid.test.FastJSONLib;
import org.comroid.test.model.NGinXFSNode;
import org.junit.Before;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.comroid.api.Polyfill.url;
import static org.junit.Assert.*;

public class TestRestJava11 {
    public final static URL testUrl = url("https://api.cdn.comroid.org/app/SymBLink/");
    private REST<Void> rest;

    @Before
    public void setup() {
        rest = new REST<>(new JavaHttpAdapter(), FastJSONLib.fastJsonLib);
    }

    @Before
    public void test() {
        final REST<Void>.Request<NGinXFSNode> request = rest.request(NGinXFSNode.class)
                .method(REST.Method.GET);

        try {
            request.execute()
                    .join();

            assertTrue(request.execute()
                    .isDone());

            assertNotNull(request.execute$body()
                    .get(0, TimeUnit.SECONDS));
            assertTrue(request.execute$deserialize()
                    .get(0, TimeUnit.SECONDS)
                    .size() >= 3);
            assertEquals(
                    200,
                    (int) request.execute$statusCode()
                            .get(0, TimeUnit.SECONDS)
            );

            assertTrue(request.execute$map(NGinXFSNode::getType)
                    .get(0, TimeUnit.SECONDS)
                    .stream()
                    .allMatch("directory"::equals));
        } catch (Throwable t) {
            System.out.println("Could not finish Java 11 Test");
            t.printStackTrace(System.out);
        }
    }
}
