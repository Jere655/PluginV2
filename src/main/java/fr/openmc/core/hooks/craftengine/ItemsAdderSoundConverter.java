package fr.openmc.core.hooks.craftengine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit les sons ItemsAdder (sounds) en configuration CraftEngine.
 * Chaque son devient un événement sonore avec ses fichiers audio.
 */
public final class ItemsAdderSoundConverter {
    private final String namespace;
    private final File namespaceDir;
    private final ConversionReport report;
    private final Map<String, Object> sounds = new LinkedHashMap<>();

    public ItemsAdderSoundConverter(String namespace, File namespaceDir, ConversionReport report) {
        this.namespace = namespace;
        this.namespaceDir = namespaceDir;
        this.report = report;
    }

    public Map<String, Object> getSounds() {
        return sounds;
    }

    public Map<String, Object> legacyCompatibleContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>(content);
        sanitized.remove("sounds");
        return sanitized;
    }

    public void read(String fileName, Map<String, Object> content) {
        Map<String, Object> section = asSection(content.get("sounds"));
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String id = entry.getKey();
            Map<String, Object> definition = asSection(entry.getValue());
            if (definition.isEmpty()) continue;

            convertSound(fileName, id, definition);
        }
    }

    private void convertSound(String fileName, String id, Map<String, Object> definition) {
        String fullId = namespace + ":" + id;

        if (!(definition.get("path") instanceof String path) || path.isBlank()) {
            report.unsupported(fullId, "son ItemsAdder sans chemin");
            return;
        }

        Map<String, Object> settings = asSection(definition.get("settings"));

        Map<String, Object> sound = new LinkedHashMap<>();
        sound.put("replace", false);

        if (settings.get("subtitle") instanceof String subtitle && !subtitle.isBlank()) {
            sound.put("subtitle", subtitle);
        }

        String soundPath = path.endsWith(".ogg") ? path.substring(0, path.length() - ".ogg".length()) : path;
        sound.put("sounds", List.of(soundPath));

        reportUnsupportedSoundFields(fullId, fileName, definition);

        sounds.put(fullId, sound);
        report.getSoundIDs().add(fullId);
    }

    private void reportUnsupportedSoundFields(String fullId, String fileName, Map<String, Object> definition) {
        for (String key : definition.keySet()) {
            if (!List.of("path", "settings", "category").contains(key)) {
                report.unsupported(fullId, "propriété son ItemsAdder non convertie : " + key);
            }
        }
    }

    private Map<String, Object> asSection(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>();
        map.forEach((key, entry) -> section.put(String.valueOf(key), entry));
        return section;
    }
}
