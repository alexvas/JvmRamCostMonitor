package jvmram.ui;

import jvmram.model.metrics.MetricType;

import javax.swing.*;

import java.awt.*;

import static jvmram.ui.Utils.COLORS;

public class CustomIcon implements Icon {
    private static final int SIZE = 13;
    private static final Color DISABLED_COLOR = new Color(50, 50, 50);

    private final Color color;
    private final int width = SIZE;
    private final int height = SIZE;

    CustomIcon(MetricType mt) {
        color = COLORS.get(mt);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.translate(x + 6, y + 6);
        var backColor = g.getColor();
        g.setColor(color);
        ButtonModel model = ((JCheckBox) c).getModel();
        if (model.isEnabled()) {
            if (model.isSelected()) {
                g.fillRect(0, 0, SIZE, SIZE);
            } else {
                g.drawRect(0, 0, SIZE, SIZE);
            }
        } else {
            g.setColor(DISABLED_COLOR);
            g.drawRect(0, 0, SIZE, SIZE);
        }
        g.setColor(backColor);
        g.translate(-4, -6);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
