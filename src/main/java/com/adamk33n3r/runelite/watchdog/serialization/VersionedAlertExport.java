package com.adamk33n3r.runelite.watchdog.serialization;

import com.adamk33n3r.runelite.watchdog.alerts.Alert;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@AllArgsConstructor
public class VersionedAlertExport {
    private final String version;
    private final List<Alert> alerts;

    /**
     * Tries to deserialize {@code json} as a {@link VersionedAlertExport}.
     *
     * <p>Returns {@code null} when the JSON is a plain alert array or single alert object,
     * so callers can fall back to legacy deserialization.
     */
    @Nullable
    public static VersionedAlertExport tryParse(Gson gson, String json) {
        try {
            VersionedAlertExport result = gson.fromJson(json, VersionedAlertExport.class);
            if (result != null && result.version != null && result.alerts != null) {
                return result;
            }
        } catch (JsonSyntaxException ignored) {
        }
        return null;
    }
}
