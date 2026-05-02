package com.adamk33n3r.runelite.watchdog;

import com.adamk33n3r.nodegraph.Graph;
import com.adamk33n3r.nodegraph.nodes.ActionNode;
import com.adamk33n3r.nodegraph.nodes.TriggerNode;
import com.adamk33n3r.nodegraph.nodes.flow.DelayNode;
import com.adamk33n3r.runelite.watchdog.alerts.AdvancedAlert;
import com.adamk33n3r.runelite.watchdog.alerts.AlertGroup;
import com.adamk33n3r.runelite.watchdog.alerts.ChatAlert;
import com.adamk33n3r.runelite.watchdog.notifications.GameMessage;
import com.adamk33n3r.runelite.watchdog.serialization.WatchdogGsonFactory;
import com.google.gson.Gson;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AlertConverterTest {
    private AlertConverter converter;

    @Before
    public void setUp() {
        converter = new AlertConverter(RuneLiteAPI.GSON);
    }

    // --- name ---

    @Test
    public void name_isSuffixedWithAdvanced() {
        assertEquals("My Alert (Advanced)", converter.convert(new ChatAlert("My Alert")).getName());
    }

    // --- alert-level properties ---

    @Test
    public void enabled_false_isCarriedToResult() {
        ChatAlert source = new ChatAlert("Test");
        source.setEnabled(false);
        assertFalse(converter.convert(source).isEnabled());
    }

    @Test
    public void debounceTime_isCarriedToResult() {
        ChatAlert source = new ChatAlert("Test");
        source.setDebounceTime(3000);
        assertEquals(3000, converter.convert(source).getDebounceTime());
    }

    // --- node structure: single alert with no notifications ---

    @Test
    public void singleAlert_noNotifications_producesTriggerNodeOnly() {
        Graph graph = converter.convert(new ChatAlert("Test")).getGraph();

        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.getNodes().get(0) instanceof TriggerNode);
        assertEquals(0, graph.getConnections().size());
    }

    // --- node structure: single alert with notifications ---

    @Test
    public void singleAlert_oneNotification_producesTwoNodesOneConnection() {
        ChatAlert source = new ChatAlert("Test");
        source.getNotifications().add(new GameMessage());
        Graph graph = converter.convert(source).getGraph();

        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.getNodes().get(0) instanceof TriggerNode);
        assertTrue(graph.getNodes().get(1) instanceof ActionNode);
        assertEquals(1, graph.getConnections().size());
    }

    @Test
    public void singleAlert_twoNotifications_producesLinearChain() {
        ChatAlert source = new ChatAlert("Test");
        source.getNotifications().add(new GameMessage());
        source.getNotifications().add(new GameMessage());
        Graph graph = converter.convert(source).getGraph();

        assertEquals(3, graph.getNodes().size());
        assertEquals(2, graph.getConnections().size());
    }

    // --- delay injection ---

    @Test
    public void notification_withDelay_injectsDelayNodeBeforeActionNode() {
        ChatAlert source = new ChatAlert("Test");
        GameMessage delayed = new GameMessage();
        delayed.setDelayMilliseconds(500);
        source.getNotifications().add(delayed);
        Graph graph = converter.convert(source).getGraph();

        assertEquals(3, graph.getNodes().size());
        assertTrue(graph.getNodes().get(0) instanceof TriggerNode);
        assertTrue(graph.getNodes().get(1) instanceof DelayNode);
        assertTrue(graph.getNodes().get(2) instanceof ActionNode);
        assertEquals(2, graph.getConnections().size());
    }

    @Test
    public void notification_withDelay_delayNodeCarriesCorrectValue() {
        ChatAlert source = new ChatAlert("Test");
        GameMessage delayed = new GameMessage();
        delayed.setDelayMilliseconds(750);
        source.getNotifications().add(delayed);
        Graph graph = converter.convert(source).getGraph();

        DelayNode delayNode = (DelayNode) graph.getNodes().get(1);
        assertEquals(750, delayNode.getDelayMs().getValue().intValue());
    }

    @Test
    public void notification_withDelay_delayMillisecondsZeroedOnActionNode() {
        ChatAlert source = new ChatAlert("Test");
        GameMessage delayed = new GameMessage();
        delayed.setDelayMilliseconds(500);
        source.getNotifications().add(delayed);
        Graph graph = converter.convert(source).getGraph();

        ActionNode actionNode = (ActionNode) graph.getNodes().get(2);
        assertEquals(0, actionNode.getNotification().getDelayMilliseconds());
    }

    @Test
    public void notification_withoutDelay_noDelayNodeCreated() {
        ChatAlert source = new ChatAlert("Test");
        source.getNotifications().add(new GameMessage());
        Graph graph = converter.convert(source).getGraph();

        assertFalse(graph.getNodes().stream().anyMatch(n -> n instanceof DelayNode));
    }

    @Test
    public void notification_withDelay_secondNotificationChainsContinues() {
        ChatAlert source = new ChatAlert("Test");
        GameMessage delayed = new GameMessage();
        delayed.setDelayMilliseconds(200);
        source.getNotifications().add(delayed);
        source.getNotifications().add(new GameMessage());
        Graph graph = converter.convert(source).getGraph();

        // TriggerNode + DelayNode + ActionNode + ActionNode
        assertEquals(4, graph.getNodes().size());
        assertEquals(3, graph.getConnections().size());
    }

    // --- AlertGroup flattening ---

    @Test
    public void alertGroup_flat_producesOneRowPerChildAlert() {
        AlertGroup group = new AlertGroup("Group");
        group.getAlerts().add(new ChatAlert("A1"));
        group.getAlerts().add(new ChatAlert("A2"));
        Graph graph = converter.convert(group).getGraph();

        assertEquals(2, graph.getNodes().size());
        assertEquals(2, graph.getNodes().stream().filter(n -> n instanceof TriggerNode).count());
    }

    @Test
    public void alertGroup_nested_flattensToLeafAlertsOnly() {
        AlertGroup outer = new AlertGroup("Outer");
        AlertGroup inner = new AlertGroup("Inner");
        inner.getAlerts().add(new ChatAlert("Leaf2"));
        outer.getAlerts().add(new ChatAlert("Leaf1"));
        outer.getAlerts().add(inner);
        Graph graph = converter.convert(outer).getGraph();

        assertEquals(2, graph.getNodes().size());
        assertEquals(2, graph.getNodes().stream().filter(n -> n instanceof TriggerNode).count());
    }

    @Test
    public void alertGroup_advancedAlertChild_isSkipped() {
        AlertGroup group = new AlertGroup("Group");
        group.getAlerts().add(new ChatAlert("Regular"));
        group.getAlerts().add(new AdvancedAlert("Skip me"));
        Graph graph = converter.convert(group).getGraph();

        assertEquals(1, graph.getNodes().size());
    }

    @Test
    public void convertingAdvancedAlert_directlyProducesEmptyGraph() {
        Graph graph = converter.convert(new AdvancedAlert("Already Advanced")).getGraph();
        assertEquals(0, graph.getNodes().size());
    }

    // --- layout ---

    @Test
    public void triggerNode_isAtStartPosition() {
        Graph graph = converter.convert(new ChatAlert("Test")).getGraph();

        TriggerNode trigger = (TriggerNode) graph.getNodes().get(0);
        assertEquals(AlertConverter.START_X, trigger.getX());
        assertEquals(AlertConverter.START_Y, trigger.getY());
    }

    @Test
    public void multipleRows_haveDistinctYPositions() {
        AlertGroup group = new AlertGroup("Group");
        group.getAlerts().add(new ChatAlert("A1"));
        group.getAlerts().add(new ChatAlert("A2"));
        Graph graph = converter.convert(group).getGraph();

        List<Integer> yPositions = graph.getNodes().stream()
            .map(n -> n.getY())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        assertEquals(2, yPositions.size());
        assertTrue(yPositions.get(1) - yPositions.get(0) >= AlertConverter.NODE_ROW_STEP);
    }

    @Test
    public void actionNode_isToTheRightOfTriggerNode() {
        ChatAlert source = new ChatAlert("Test");
        source.getNotifications().add(new GameMessage());
        Graph graph = converter.convert(source).getGraph();

        TriggerNode trigger = (TriggerNode) graph.getNodes().get(0);
        ActionNode action = (ActionNode) graph.getNodes().get(1);
        assertTrue(action.getX() > trigger.getX());
    }

    // --- deep copy ---

    @Test
    public void deepCopy_modifyingSourceNotificationDoesNotAffectConverted() {
        ChatAlert source = new ChatAlert("Test");
        GameMessage original = new GameMessage();
        source.getNotifications().add(original);

        AdvancedAlert result = converter.convert(source);

        original.setDelayMilliseconds(999);

        ActionNode actionNode = (ActionNode) result.getGraph().getNodes().get(1);
        assertEquals(0, actionNode.getNotification().getDelayMilliseconds());
    }

    // --- hasNestedGroups ---

    @Test
    public void hasNestedGroups_returnsFalse_forPlainAlert() {
        assertFalse(AlertConverter.hasNestedGroups(new ChatAlert("Test")));
    }

    @Test
    public void hasNestedGroups_returnsFalse_forFlatGroup() {
        AlertGroup group = new AlertGroup("Group");
        group.getAlerts().add(new ChatAlert("Alert"));
        assertFalse(AlertConverter.hasNestedGroups(group));
    }

    @Test
    public void hasNestedGroups_returnsTrue_forGroupWithNestedGroup() {
        AlertGroup outer = new AlertGroup("Outer");
        outer.getAlerts().add(new AlertGroup("Inner"));
        assertTrue(AlertConverter.hasNestedGroups(outer));
    }
}
