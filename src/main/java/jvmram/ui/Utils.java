package jvmram.ui;

import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    private Utils() {}


    public static int[] toArray(List<Integer> input) {
        return input.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private static final Pattern PID_NUMBER_REGEX = Pattern.compile("\\d++");

    public static long parsePidFromProcessDisplayName(String selectedItem) {
        var matcher = PID_NUMBER_REGEX.matcher(selectedItem);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unexpected selected item: %s".formatted(selectedItem));
        }
        var matchedGroup = matcher.group();
        return Long.parseLong(matchedGroup);
    }
}
