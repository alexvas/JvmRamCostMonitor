package jvmram.ui;

import jvmram.Config;
import jvmram.controller.GraphController;
import jvmram.controller.GraphRenderer;
import jvmram.metrics.GraphPoint;
import jvmram.model.graph.GraphPointQueues;
import jvmram.model.graph.MetricVisibility;
import jvmram.model.graph.Utils;
import jvmram.model.metrics.MetricType;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GraphPanel extends JPanel implements GraphRenderer {

    private static final int PADDING = 60;
    private static final int LABEL_PADDING = 40;
    private static final int GRAPH_STROKE_WIDTH = 1;
    private static final Duration GRAPH_MIN_DURATION = Duration.ofMinutes(2);

    private final GraphPointQueues graphPointQueues = GraphPointQueues.getInstance();
    private final MetricVisibility metricVisibility = MetricVisibility.getInstance();
    private final GraphController graphController = GraphController.getInstance();

    private static final Map<MetricType, Color> COLORS = new EnumMap<>(MetricType.class);

    static {
        for (MetricType mt : MetricType.values()) {
            Color color = switch (mt) {
                case RSS -> Color.RED;
                case PSS -> Color.GREEN;
                case USS -> Color.BLUE;
                case WS -> new Color(97, 110, 18);
                case PWS -> new Color(118, 68, 1);
                case PB -> Color.ORANGE;
                case HEAP_USED -> Color.MAGENTA;
                case HEAP_COMMITTED -> Color.CYAN;
                case NMT_USED -> new Color(128, 0, 255);
                case NMT_COMMITTED -> new Color(32, 42, 69);
            };
            COLORS.put(mt, color);
        }
    }

    public GraphPanel() {
        graphController.addRenderer(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        var g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Очистка фона
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        if (graphPointQueues.isEmpty()) {
            drawEmptyGraph(g2, width, height);
            return;
        }

        int graphWidth = width - 2 * PADDING - LABEL_PADDING;
        int graphHeight = height - 2 * PADDING;

        // Определение диапазона данных
        long minValue = 0;
        long maxBytes = graphPointQueues.maxBytes();

        var leftTimeBond = graphPointQueues.minMoment();
        var rightTimeBond = graphPointQueues.maxMoment();

        // Добавляем небольшой отступ к диапазону
        maxBytes = Math.round(maxBytes * 1.1d);

        var graphDuration = Utils.max(
                Duration.between(leftTimeBond, rightTimeBond),
                GRAPH_MIN_DURATION
        );

        // Отрисовка сетки и осей
        drawGrid(g2, width, height, graphWidth, graphHeight, minValue, maxBytes, graphDuration);

        // Отрисовка графиков
        var oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(GRAPH_STROKE_WIDTH));

        var keys = graphPointQueues.keys();

        for (var key : keys) {
            var metricColor = COLORS.get(key.type());
            g2.setColor(metricColor);

            var points = graphPointQueues.getPoints(key);
            drawLine(g2, points, maxBytes, leftTimeBond, graphDuration, graphWidth, graphHeight);
        }

        g2.setStroke(oldStroke);

        // Отрисовка легенды
        drawLegend(g2, width);

        // Заголовок
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Потребление памяти", width / 2 - 80, 20);
    }

    private void drawEmptyGraph(Graphics2D g2, int width, int height) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        String message = "Нет данных для отображения";
        int messageWidth = g2.getFontMetrics().stringWidth(message);
        g2.drawString(message, (width - messageWidth) / 2, height / 2);
    }

    private void drawGrid(Graphics2D g2, int width, int height, int graphWidth, int graphHeight,
                          long minValue, long maxValue, Duration graphDuration) {
        g2.setColor(Color.LIGHT_GRAY);
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1));

        // Вертикальные линии сетки
        int xAxisStart = PADDING + LABEL_PADDING;
        for (int i = 0; i <= 10; i++) {
            int x = xAxisStart + (i * graphWidth / 10);
            g2.drawLine(x, PADDING, x, height - PADDING);
        }

        // Горизонтальные линии сетки
        for (int i = 0; i <= 10; i++) {
            int y = height - PADDING - (i * graphHeight / 10);
            g2.drawLine(xAxisStart, y, width - PADDING, y);
        }

        // Оси
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        // Ось X
        g2.drawLine(xAxisStart, height - PADDING, width - PADDING, height - PADDING);
        // Ось Y
        g2.drawLine(xAxisStart, PADDING, xAxisStart, height - PADDING);

        // Подписи осей
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        // Ось X
        g2.drawString("Время (сек)", width / 2 - 40, height - 10);
        // Ось Y
        Graphics2D g2Rotated = (Graphics2D) g2.create();
        g2Rotated.rotate(-Math.PI / 2);
        g2Rotated.drawString("Память (байты)", -height / 2 - 40, 20);
        g2Rotated.dispose();

        // Подписи значений на осях
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        // Ось X
        for (int i = 0; i <= 10; i++) {
            var duration = graphDuration.toSeconds();
            double timeValue = duration * i / 10.0;
            String label = String.format("%.1f", timeValue);
            int x = xAxisStart + (i * graphWidth / 10) - 15;
            g2.drawString(label, x, height - PADDING + 20);
        }
        // Ось Y
        for (int i = 0; i <= 10; i++) {
            double value = minValue + (maxValue - minValue) * (10 - i) / 10.0;
            String label = formatValue(value);
            int labelWidth = g2.getFontMetrics().stringWidth(label);
            int y = PADDING + (i * graphHeight / 10) + 5;
            g2.drawString(label, xAxisStart - labelWidth - 5, y);
        }

        g2.setStroke(oldStroke);
    }

    private record CanvasPoint(int x, int y) {
    }

    private void drawLine(
            Graphics2D g2,
            Collection<GraphPoint> points,
            long maxBytes,
            Instant startTime,
            Duration graphDuration,
            int graphWidth,
            int graphHeight
    ) {
        if (points.size() < 2) {
            return;
        }

        int xAxisStart = PADDING + LABEL_PADDING;
        int yAxisStart = getHeight() - PADDING;

        long fullMillis = graphDuration.toMillis();

        List<CanvasPoint> canvasPoints = points.stream()
                .map(graphPoint ->
                        convertToCanvasPoint(
                                maxBytes,
                                startTime,
                                graphWidth,
                                graphHeight,
                                graphPoint,
                                fullMillis,
                                xAxisStart,
                                yAxisStart)
                ).toList();

        CanvasPoint prev = null;
        for (var cp: canvasPoints) {
            if (prev == null) {
                prev = cp;
                continue;
            }
            g2.drawLine(prev.x, prev.y, cp.x, cp.y);
            prev = cp;
        }
    }

    private static CanvasPoint convertToCanvasPoint(long maxBytes, Instant startTime, int graphWidth, int graphHeight, GraphPoint graphPoint, long fullMillis, int xAxisStart, int yAxisStart) {
        long elapsedMillis = Duration.between(startTime, graphPoint.moment()).toMillis();
        double xRelative = ((double) elapsedMillis) / fullMillis;
        double xDelta = xRelative * graphWidth;
        double yRelative = ((double) graphPoint.bytes()) / maxBytes;
        double yDelta = yRelative * graphHeight;
        int xAbsolute = xAxisStart + (int) Math.round(xDelta);
        int yAbsolute = yAxisStart - (int) Math.round(yDelta);
        return new CanvasPoint(xAbsolute, yAbsolute);
    }

    private void drawLegend(Graphics2D g2, int width) {
        int legendX = width - PADDING - 150;
        int legendY = PADDING + 20;
        int lineHeight = 20;

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        int index = 0;

        for (var mt1 : MetricType.values()) {
            if (!mt1.isApplicable(Config.os) || !metricVisibility.isVisible(mt1)) {
                continue;
            }
            var legendMetricsColor = COLORS.get(mt1);
            var metricsName = mt1.getDisplayName();

            int itemTop = legendY + index * lineHeight;

            g2.setColor(legendMetricsColor);
            g2.fillRect(legendX, itemTop - 8, 15, 2);
            g2.setColor(Color.BLACK);
            g2.drawString(metricsName, legendX + 20, itemTop);
            ++index;
        }
    }

    private String formatValue(double value) {
        if (value >= 1e9) {
            return String.format("%.2fG", value / 1e9);
        } else if (value >= 1e6) {
            return String.format("%.2fM", value / 1e6);
        } else if (value >= 1e3) {
            return String.format("%.2fK", value / 1e3);
        } else {
            return String.format("%.0f", value);
        }
    }

    @Override
    public void repaintAsync() {
        SwingUtilities.invokeLater(this::repaint);
    }
}
