package com.adamk33n3r.runelite.watchdog;

import com.adamk33n3r.nodegraph.Graph;
import com.adamk33n3r.runelite.watchdog.alerts.AdvancedAlert;
import com.adamk33n3r.runelite.watchdog.alerts.AlertGroup;
import com.adamk33n3r.runelite.watchdog.alerts.ChatAlert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WatchdogPluginShutdownTest extends TestBase {

    @Test
    public void shutdownAdvancedAlertGraph_withAdvancedAlert_shutsDownGraph() {
        AdvancedAlert alert = new AdvancedAlert("test");
        Graph mockGraph = Mockito.mock(Graph.class);
        alert.setGraph(mockGraph);

        this.watchdogPlugin.shutdownAdvancedAlertGraph(alert);

        Mockito.verify(mockGraph).shutdown();
    }

    @Test
    public void shutdownAdvancedAlertGraph_withAlertGroup_shutsDownAllDescendantGraphs() {
        AdvancedAlert aa1 = new AdvancedAlert("aa1");
        AdvancedAlert aa2 = new AdvancedAlert("aa2");
        Graph mockGraph1 = Mockito.mock(Graph.class);
        Graph mockGraph2 = Mockito.mock(Graph.class);
        aa1.setGraph(mockGraph1);
        aa2.setGraph(mockGraph2);

        AlertGroup group = new AlertGroup("group");
        group.getAlerts().add(aa1);
        group.getAlerts().add(aa2);

        this.watchdogPlugin.shutdownAdvancedAlertGraph(group);

        Mockito.verify(mockGraph1).shutdown();
        Mockito.verify(mockGraph2).shutdown();
    }

    @Test
    public void shutdownAdvancedAlertGraph_withNestedAlertGroup_shutsDownAllDescendantGraphs() {
        AdvancedAlert inner = new AdvancedAlert("inner");
        Graph mockGraph = Mockito.mock(Graph.class);
        inner.setGraph(mockGraph);

        AlertGroup innerGroup = new AlertGroup("inner-group");
        innerGroup.getAlerts().add(inner);

        AlertGroup outerGroup = new AlertGroup("outer-group");
        outerGroup.getAlerts().add(innerGroup);

        this.watchdogPlugin.shutdownAdvancedAlertGraph(outerGroup);

        Mockito.verify(mockGraph).shutdown();
    }

    @Test
    public void shutdownAdvancedAlertGraph_withNonAdvancedAlert_doesNothing() {
        ChatAlert chatAlert = new ChatAlert("test");
        this.watchdogPlugin.shutdownAdvancedAlertGraph(chatAlert);
    }
}
