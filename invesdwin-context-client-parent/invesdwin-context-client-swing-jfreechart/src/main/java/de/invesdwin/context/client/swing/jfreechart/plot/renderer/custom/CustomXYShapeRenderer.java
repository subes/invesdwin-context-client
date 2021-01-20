package de.invesdwin.context.client.swing.jfreechart.plot.renderer.custom;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.annotation.concurrent.NotThreadSafe;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.SerialUtils;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.XYDataset;

/**
 * Adapted from CustomXYShapeRenderer
 */
@NotThreadSafe
public class CustomXYShapeRenderer extends AbstractXYItemRenderer implements XYItemRenderer, Cloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -3271351259436865995L;

    /**
     * A table of flags that control (per series) whether or not shapes are filled.
     */
    private BooleanList seriesShapesFilled;

    /** The default value returned by the getShapeFilled() method. */
    private boolean baseShapesFilled = true;

    /**
     * The shape that is used to represent a line in the legend. This should never be set to {@code null}.
     */
    private transient Shape legendLine;

    /**
     * Constructs a new renderer.
     */
    public CustomXYShapeRenderer() {
        this(null);
    }

    /**
     * Constructs a new renderer.
     *
     * @param toolTipGenerator
     *            the item label generator ({@code null} permitted).
     */
    public CustomXYShapeRenderer(final XYToolTipGenerator toolTipGenerator) {
        this(toolTipGenerator, null);
    }

    /**
     * Constructs a new renderer.
     *
     * @param toolTipGenerator
     *            the item label generator ({@code null} permitted).
     * @param urlGenerator
     *            the URL generator.
     */
    public CustomXYShapeRenderer(final XYToolTipGenerator toolTipGenerator, final XYURLGenerator urlGenerator) {

        super();
        setDefaultToolTipGenerator(toolTipGenerator);
        setURLGenerator(urlGenerator);

        this.seriesShapesFilled = new BooleanList();
        this.baseShapesFilled = true;
        this.legendLine = new Line2D.Double(-7.0, 0.0, 7.0, 0.0);

        System.out.println("use a shapebuilder as param and adjust on stroke width changes");
    }

    /**
     * Returns the flag used to control whether or not the shape for an item is filled.
     * <p>
     * The default implementation passes control to the {@code getSeriesShapesFilled()} method. You can override this
     * method if you require different behaviour.
     *
     * @param series
     *            the series index (zero-based).
     * @param item
     *            the item index (zero-based).
     *
     * @return A boolean.
     *
     * @see #getSeriesShapesFilled(int)
     */
    public boolean getItemShapeFilled(final int series, final int item) {

        // otherwise look up the paint table
        final Boolean flag = this.seriesShapesFilled.getBoolean(series);
        if (flag != null) {
            return flag.booleanValue();
        } else {
            return this.baseShapesFilled;
        }
    }

    /**
     * Returns the flag used to control whether or not the shapes for a series are filled.
     *
     * @param series
     *            the series index (zero-based).
     *
     * @return A boolean.
     */
    public Boolean getSeriesShapesFilled(final int series) {
        return this.seriesShapesFilled.getBoolean(series);
    }

    /**
     * Sets the 'shapes filled' flag for a series and sends a {@link RendererChangeEvent} to all registered listeners.
     *
     * @param series
     *            the series index (zero-based).
     * @param flag
     *            the flag.
     *
     * @see #getSeriesShapesFilled(int)
     */
    public void setSeriesShapesFilled(final int series, final Boolean flag) {
        this.seriesShapesFilled.setBoolean(series, flag);
        fireChangeEvent();
    }

    /**
     * Returns the base 'shape filled' attribute.
     *
     * @return The base flag.
     *
     * @see #setBaseShapesFilled(boolean)
     */
    public boolean getBaseShapesFilled() {
        return this.baseShapesFilled;
    }

    /**
     * Sets the base 'shapes filled' flag and sends a {@link RendererChangeEvent} to all registered listeners.
     *
     * @param flag
     *            the flag.
     *
     * @see #getBaseShapesFilled()
     */
    public void setBaseShapesFilled(final boolean flag) {
        this.baseShapesFilled = flag;
    }

    /**
     * Returns the shape used to represent a line in the legend.
     *
     * @return The legend line (never {@code null}).
     *
     * @see #setLegendLine(Shape)
     */
    public Shape getLegendLine() {
        return this.legendLine;
    }

    /**
     * Sets the shape used as a line in each legend item and sends a {@link RendererChangeEvent} to all registered
     * listeners.
     *
     * @param line
     *            the line ({@code null} not permitted).
     *
     * @see #getLegendLine()
     */
    public void setLegendLine(final Shape line) {
        Args.nullNotPermitted(line, "line");
        this.legendLine = line;
        fireChangeEvent();
    }

    /**
     * Returns a legend item for a series.
     *
     * @param datasetIndex
     *            the dataset index (zero-based).
     * @param series
     *            the series index (zero-based).
     *
     * @return A legend item for the series.
     */
    @Override
    public LegendItem getLegendItem(final int datasetIndex, final int series) {
        final XYPlot plot = getPlot();
        if (plot == null) {
            return null;
        }
        LegendItem result = null;
        final XYDataset dataset = plot.getDataset(datasetIndex);
        if (dataset != null) {
            if (getItemVisible(series, 0)) {
                final String label = getLegendItemLabelGenerator().generateLabel(dataset, series);
                final String description = label;
                String toolTipText = null;
                if (getLegendItemToolTipGenerator() != null) {
                    toolTipText = getLegendItemToolTipGenerator().generateLabel(dataset, series);
                }
                String urlText = null;
                if (getLegendItemURLGenerator() != null) {
                    urlText = getLegendItemURLGenerator().generateLabel(dataset, series);
                }
                final Shape shape = lookupLegendShape(series);
                final boolean shapeFilled = getItemShapeFilled(series, 0);
                final Paint paint = lookupSeriesPaint(series);
                final Paint linePaint = paint;
                final Stroke lineStroke = lookupSeriesStroke(series);
                result = new LegendItem(label, description, toolTipText, urlText, true, shape, shapeFilled, paint,
                        !shapeFilled, paint, lineStroke, false, this.legendLine, lineStroke, linePaint);
                result.setLabelFont(lookupLegendTextFont(series));
                final Paint labelPaint = lookupLegendTextPaint(series);
                if (labelPaint != null) {
                    result.setLabelPaint(labelPaint);
                }
                result.setDataset(dataset);
                result.setDatasetIndex(datasetIndex);
                result.setSeriesKey(dataset.getSeriesKey(series));
                result.setSeriesIndex(series);
            }
        }
        return result;
    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2
     *            the graphics device.
     * @param state
     *            the renderer state.
     * @param dataArea
     *            the area within which the data is being drawn.
     * @param info
     *            collects information about the drawing.
     * @param plot
     *            the plot (can be used to obtain standard color information etc).
     * @param domainAxis
     *            the domain axis.
     * @param rangeAxis
     *            the range axis.
     * @param dataset
     *            the dataset.
     * @param series
     *            the series index (zero-based).
     * @param item
     *            the item index (zero-based).
     * @param crosshairState
     *            crosshair information for the plot ({@code null} permitted).
     * @param pass
     *            the pass index.
     */
    //CHECKSTYLE:OFF
    @Override
    public void drawItem(final Graphics2D g2, final XYItemRendererState state, final Rectangle2D dataArea,
            final PlotRenderingInfo info, final XYPlot plot, final ValueAxis domainAxis, final ValueAxis rangeAxis,
            final XYDataset dataset, final int series, final int item, final CrosshairState crosshairState,
            final int pass) {
        //CHECKSTYLE:ON

        boolean itemVisible = getItemVisible(series, item);

        // get the data point...
        final double x1 = dataset.getXValue(series, item);
        final double y1 = dataset.getYValue(series, item);
        if (Double.isNaN(x1) || Double.isNaN(y1)) {
            itemVisible = false;
        }

        if (!itemVisible) {
            return;
        }

        final PlotOrientation orientation = plot.getOrientation();
        final Paint paint = getItemPaint(series, item);
        final Stroke seriesStroke = getItemStroke(series, item);
        g2.setPaint(paint);
        g2.setStroke(seriesStroke);

        final RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        final RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
        final double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
        final double transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);

        //plot shape
        Shape shape = getItemShape(series, item);
        if (orientation == PlotOrientation.HORIZONTAL) {
            shape = ShapeUtils.createTranslatedShape(shape, transY1, transX1);
        } else if (orientation == PlotOrientation.VERTICAL) {
            shape = ShapeUtils.createTranslatedShape(shape, transX1, transY1);
        }
        if (shape.intersects(dataArea)) {
            if (getItemShapeFilled(series, item)) {
                g2.fill(shape);
            } else {
                g2.draw(shape);
            }
        }
    }

    /**
     * Tests this renderer for equality with another object.
     *
     * @param obj
     *            the object ({@code null} permitted).
     *
     * @return A boolean.
     */
    //CHECKSTYLE:OFF
    @Override
    public boolean equals(final Object obj) {
        //CHECKSTYLE:ON

        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CustomXYShapeRenderer)) {
            return false;
        }
        final CustomXYShapeRenderer that = (CustomXYShapeRenderer) obj;
        if (!this.seriesShapesFilled.equals(that.seriesShapesFilled)) {
            return false;
        }
        if (this.baseShapesFilled != that.baseShapesFilled) {
            return false;
        }
        if (!ShapeUtils.equal(this.legendLine, that.legendLine)) {
            return false;
        }
        return super.equals(obj);

    }

    /**
     * Returns a clone of the renderer.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException
     *             if the renderer cannot be cloned.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final CustomXYShapeRenderer clone = (CustomXYShapeRenderer) super.clone();
        clone.seriesShapesFilled = (BooleanList) this.seriesShapesFilled.clone();
        clone.legendLine = ShapeUtils.clone(this.legendLine);
        return clone;
    }

    ////////////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    // These provide the opportunity to subclass the standard renderer and
    // create custom effects.
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the image used to draw a single data item.
     *
     * @param plot
     *            the plot (can be used to obtain standard color information etc).
     * @param series
     *            the series index.
     * @param item
     *            the item index.
     * @param x
     *            the x value of the item.
     * @param y
     *            the y value of the item.
     *
     * @return The image.
     *
     * @see #getPlotImages()
     */
    protected Image getImage(final Plot plot, final int series, final int item, final double x, final double y) {
        // this method must be overridden if you want to display images
        return null;
    }

    /**
     * Returns the hotspot of the image used to draw a single data item. The hotspot is the point relative to the top
     * left of the image that should indicate the data item. The default is the center of the image.
     *
     * @param plot
     *            the plot (can be used to obtain standard color information etc).
     * @param image
     *            the image (can be used to get size information about the image)
     * @param series
     *            the series index
     * @param item
     *            the item index
     * @param x
     *            the x value of the item
     * @param y
     *            the y value of the item
     *
     * @return The hotspot used to draw the data item.
     */
    protected Point getImageHotspot(final Plot plot, final int series, final int item, final double x, final double y,
            final Image image) {

        final int height = image.getHeight(null);
        final int width = image.getWidth(null);
        return new Point(width / 2, height / 2);

    }

    /**
     * Provides serialization support.
     *
     * @param stream
     *            the input stream.
     *
     * @throws IOException
     *             if there is an I/O error.
     * @throws ClassNotFoundException
     *             if there is a classpath problem.
     */
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.legendLine = SerialUtils.readShape(stream);
    }

    /**
     * Provides serialization support.
     *
     * @param stream
     *            the output stream.
     *
     * @throws IOException
     *             if there is an I/O error.
     */
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtils.writeShape(this.legendLine, stream);
    }

}
