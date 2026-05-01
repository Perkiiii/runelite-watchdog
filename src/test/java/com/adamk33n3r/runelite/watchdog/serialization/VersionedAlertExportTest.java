package com.adamk33n3r.runelite.watchdog.serialization;

import com.adamk33n3r.runelite.watchdog.AlertManager;
import com.adamk33n3r.runelite.watchdog.alerts.Alert;
import com.adamk33n3r.runelite.watchdog.alerts.ChatAlert;

import com.google.gson.Gson;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class VersionedAlertExportTest {

    private final Gson gson = new WatchdogGsonFactory().create(RuneLiteAPI.GSON);

    // ── tryParse detection ───────────────────────────────────────────────────

    @Test
    public void tryParse_returnsEnvelope_whenVersionedJson() {
        VersionedAlertExport export = new VersionedAlertExport("3.5.0", List.of(new ChatAlert("test")));
        String json = this.gson.toJson(export);

        VersionedAlertExport parsed = VersionedAlertExport.tryParse(this.gson, json);

        assertNotNull(parsed);
        assertEquals("3.5.0", parsed.getVersion());
        assertEquals(1, parsed.getAlerts().size());
    }

    @Test
    public void tryParse_returnsNull_forPlainAlertList() {
        List<Alert> alerts = List.of(new ChatAlert("test"));
        String json = this.gson.toJson(alerts, AlertManager.ALERT_LIST_TYPE);

        assertNull(VersionedAlertExport.tryParse(this.gson, json));
    }

    @Test
    public void tryParse_returnsNull_forSingleAlert() {
        ChatAlert alert = new ChatAlert("test");
        String json = this.gson.toJson(alert, AlertManager.ALERT_TYPE);

        assertNull(VersionedAlertExport.tryParse(this.gson, json));
    }

    // ── round-trip ───────────────────────────────────────────────────────────

    @Test
    public void roundTrip_preservesVersionAndAlertName() {
        ChatAlert alert = new ChatAlert("harvest");
        alert.setMessage("*ready to harvest*");
        VersionedAlertExport export = new VersionedAlertExport("4.0.0", List.of(alert));

        String json = this.gson.toJson(export);
        VersionedAlertExport parsed = VersionedAlertExport.tryParse(this.gson, json);

        assertNotNull(parsed);
        assertEquals("4.0.0", parsed.getVersion());
        assertEquals(1, parsed.getAlerts().size());
        ChatAlert parsedAlert = (ChatAlert) parsed.getAlerts().get(0);
        assertEquals("harvest", parsedAlert.getName());
        assertEquals("*ready to harvest*", parsedAlert.getMessage());
    }

    @Test
    public void roundTrip_preservesMultipleAlerts() {
        List<Alert> alerts = List.of(new ChatAlert("first"), new ChatAlert("second"));
        VersionedAlertExport export = new VersionedAlertExport("4.1.0", alerts);

        String json = this.gson.toJson(export);
        VersionedAlertExport parsed = VersionedAlertExport.tryParse(this.gson, json);

        assertNotNull(parsed);
        assertEquals(2, parsed.getAlerts().size());
        assertEquals("first", parsed.getAlerts().get(0).getName());
        assertEquals("second", parsed.getAlerts().get(1).getName());
    }
}
