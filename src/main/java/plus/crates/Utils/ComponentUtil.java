package plus.crates.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;

/** Boundary helpers for old ampersand/section configuration while Bukkit APIs use Components. */
public final class ComponentUtil {
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private ComponentUtil() {
    }

    public static Component legacy(String text) {
        if (text == null) {
            return Component.empty();
        }
        return text.indexOf('§') >= 0 ? SECTION.deserialize(text) : AMPERSAND.deserialize(text);
    }

    public static List<Component> legacy(List<String> lines) {
        return lines.stream().map(ComponentUtil::legacy).toList();
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String legacy(Component component) {
        return AMPERSAND.serialize(component);
    }

    public static String legacyString(String text) {
        return legacy(legacy(text));
    }
}
