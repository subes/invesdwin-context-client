package de.invesdwin.context.client.swing.jfreechart.plot.renderer;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import javax.annotation.concurrent.NotThreadSafe;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.lang.Colors;

@NotThreadSafe
public class DisabledXYItemRenderer extends AbstractXYItemRenderer {

    private final XYItemRenderer enabledRenderer;

    public DisabledXYItemRenderer(final XYItemRenderer enabledRenderer) {
        Assertions.checkNotNull(enabledRenderer);
        if (enabledRenderer instanceof DisabledXYItemRenderer) {
            throw new IllegalArgumentException(
                    "enabledRenderer should not be an instance of " + DisabledXYItemRenderer.class.getSimpleName());
        }
        this.enabledRenderer = enabledRenderer;
    }

    public XYItemRenderer getEnabledRenderer() {
        return enabledRenderer;
    }

    //CHECKSTYLE:OFF
    @Override
    public void drawItem(final Graphics2D g2, final XYItemRendererState state, final Rectangle2D dataArea,
            final PlotRenderingInfo info, final XYPlot plot, final ValueAxis domainAxis, final ValueAxis rangeAxis,
            final XYDataset dataset, final int series, final int item, final CrosshairState crosshairState,
            final int pass) {}
    //CHECKSTYLE:ON

    public static XYItemRenderer maybeUnwrap(final XYItemRenderer renderer) {
        if (renderer instanceof DisabledXYItemRenderer) {
            final DisabledXYItemRenderer cRenderer = (DisabledXYItemRenderer) renderer;
            return cRenderer.getEnabledRenderer();
        } else {
            return renderer;
        }
    }

    @Override
    public void setSeriesStroke(final int series, final Stroke stroke, final boolean notify) {
        enabledRenderer.setSeriesStroke(series, stroke, notify);
    }

    @Override
    public void setSeriesStroke(final int series, final Stroke stroke) {
        enabledRenderer.setSeriesStroke(series, stroke);
    }

    @Override
    public Stroke getSeriesStroke(final int series) {
        return enabledRenderer.getSeriesStroke(series);
    }

    @Override
    public void setSeriesPaint(final int series, final Paint paint, final boolean notify) {
        enabledRenderer.setSeriesPaint(series, paint, notify);
    }

    @Override
    public void setSeriesPaint(final int series, final Paint paint) {
        enabledRenderer.setSeriesPaint(series, paint);
    }

    @Override
    public Paint getSeriesPaint(final int series) {
        return enabledRenderer.getSeriesPaint(series);
    }

    @Override
    public void setSeriesFillPaint(final int series, final Paint paint, final boolean notify) {
        enabledRenderer.setSeriesFillPaint(series, paint, notify);
    }

    @Override
    public void setSeriesFillPaint(final int series, final Paint paint) {
        enabledRenderer.setSeriesFillPaint(series, paint);
    }

    @Override
    public Paint getSeriesFillPaint(final int series) {
        return enabledRenderer.getSeriesFillPaint(series);
    }

    @Override
    public Paint getItemPaint(final int row, final int column) {
        return Colors.INVISIBLE_COLOR;
    }

    @Override
    protected void updateCrosshairValues(final CrosshairState crosshairState, final double x, final double y,
            final int datasetIndex, final double transX, final double transY, final PlotOrientation orientation) {
        //noop
    }

    @Override
    protected void addEntity(final EntityCollection entities, final Shape hotspot, final XYDataset dataset,
            final int series, final int item, final double entityX, final double entityY) {
        //noop
    }

}