package com.adamk33n3r.runelite.watchdog.nodegraph;

import com.adamk33n3r.nodegraph.Graph;
import org.junit.Test;

public class GraphShutdownTest {

    @Test
    public void shutdown_withNullScheduler_doesNotThrow() {
        Graph graph = new Graph();
        graph.shutdown();
    }

    @Test
    public void shutdown_isIdempotent() {
        Graph graph = new Graph();
        graph.shutdown();
        graph.shutdown();
    }
}
