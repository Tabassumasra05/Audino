package com.audino.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Lightweight inline Canvas sparkline rendering trend curves for historical clinical metrics.
 */
public class Sparkline extends Canvas {

    public Sparkline(List<Double> points, double width, double height, Color strokeColor, Color fillColor) {
        super(width, height);
        draw(points, strokeColor != null ? strokeColor : Color.web("#0F766E"), fillColor);
    }

    public Sparkline(List<Double> points, double width, double height) {
        this(points, width, height, Color.web("#0F766E"), Color.web("#0F766E", 0.15));
    }

    public void setPoints(List<Double> points, Color strokeColor, Color fillColor) {
        draw(points, strokeColor, fillColor);
    }

    private void draw(List<Double> points, Color strokeColor, Color fillColor) {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);

        if (points == null || points.size() < 2) {
            // Draw placeholder dotted line
            gc.setStroke(Color.web("#CBD5E1"));
            gc.setLineWidth(1.0);
            gc.strokeLine(0, h / 2, w, h / 2);
            return;
        }

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double p : points) {
            if (p < min) min = p;
            if (p > max) max = p;
        }

        double range = max - min;
        if (range <= 0.0001) range = 1.0;

        double paddingY = 4.0;
        double usableH = h - (paddingY * 2);
        double stepX = w / (points.size() - 1);

        double[] xCoords = new double[points.size()];
        double[] yCoords = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            xCoords[i] = i * stepX;
            double norm = (points.get(i) - min) / range;
            yCoords[i] = h - paddingY - (norm * usableH);
        }

        // Fill area below sparkline if requested
        if (fillColor != null) {
            gc.beginPath();
            gc.moveTo(xCoords[0], yCoords[0]);
            for (int i = 1; i < points.size(); i++) {
                gc.lineTo(xCoords[i], yCoords[i]);
            }
            gc.lineTo(w, h);
            gc.lineTo(0, h);
            gc.closePath();
            gc.setFill(fillColor);
            gc.fill();
        }

        // Draw line
        gc.beginPath();
        gc.moveTo(xCoords[0], yCoords[0]);
        for (int i = 1; i < points.size(); i++) {
            gc.lineTo(xCoords[i], yCoords[i]);
        }
        gc.setStroke(strokeColor);
        gc.setLineWidth(2.0);
        gc.stroke();

        // Draw endpoint dot
        int last = points.size() - 1;
        gc.setFill(strokeColor);
        gc.fillOval(xCoords[last] - 3, yCoords[last] - 3, 6, 6);
    }
}
