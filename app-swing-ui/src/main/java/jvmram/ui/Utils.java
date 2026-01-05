package jvmram.ui;

import jvmram.model.metrics.MetricType;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Utils {
    private Utils() {}

    static final Map<MetricType, Color> COLORS = new EnumMap<>(MetricType.class);

    static {
        for (MetricType mt : MetricType.values()) {
            Color color = switch (mt) {
                case RSS, WS -> Color.RED;
                case PSS -> Color.GREEN;
                case USS, PB -> Color.BLUE;
                case HEAP_USED -> Color.MAGENTA;
                case HEAP_COMMITTED -> Color.CYAN;
                case NMT_USED -> new Color(128, 0, 255);
                case NMT_COMMITTED -> new Color(32, 42, 69);
            };
            COLORS.put(mt, color);
        }
    }

    public static int[] toArray(List<Integer> input) {
        return input.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private static final Pattern PID_NUMBER_REGEX = Pattern.compile("\\d++");

    public static long parsePidFromProcessDisplayName(String selectedItem) {
        var matcher = PID_NUMBER_REGEX.matcher(selectedItem);
        if (!matcher.find()) {
            throw new IllegalStateException("Unexpected selected item: %s".formatted(selectedItem));
        }
        var matchedGroup = matcher.group();
        return Long.parseLong(matchedGroup);
    }

    public static JPanel createGroup(String title) {
        JPanel group = new JPanel();
        group.setBorder(BorderFactory.createTitledBorder(title));
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        return group;
    }
}
