package jvmram.swing.ui;

import jvmram.model.metrics.MetricType;

import javax.swing.*;

import java.awt.*;

import static jvmram.swing.ui.Utils.COLORS;

public class CustomIcon implements Icon {
    private static final int SIZE = 13;
    private static final Color DISABLED_COLOR = new Color(50, 50, 50);

    private final Color color;

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
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
