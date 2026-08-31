package fr.openmc.core.utils.text.fonts;

import fr.openmc.core.hooks.craftengine.OpenMCContent;
import net.momirealms.craftengine.bukkit.api.CraftEngineImages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Fonts {
    private static final Pattern IMAGE_TOKEN = Pattern.compile(":([a-zA-Z0-9_.-]+):");

    public static String getFont(String namespaceID){
        String glyph = OpenMCContent.image(namespaceID);
        return glyph == null ? "" : "§r" + glyph;
    }

    /** Resolves legacy :image: tokens through CraftEngine's loaded image registry. */
    public static String replaceFontImages(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher matcher = IMAGE_TOKEN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            String glyph = CraftEngineImages.loadedImages().entrySet().stream()
                    .filter(entry -> entry.getKey().value().equals(token))
                    .map(entry -> entry.getValue().miniMessageAt(0, 0))
                    .findFirst().orElse(null);
            matcher.appendReplacement(result, Matcher.quoteReplacement(glyph == null ? matcher.group() : glyph));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
