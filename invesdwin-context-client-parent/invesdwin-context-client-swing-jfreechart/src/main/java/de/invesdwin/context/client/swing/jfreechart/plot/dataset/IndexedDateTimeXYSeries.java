package de.invesdwin.context.client.swing.jfreechart.plot.dataset;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.context.jfreechart.dataset.ListXYSeriesOHLC;
import de.invesdwin.context.jfreechart.dataset.MutableXYDataItemOHLC;

@NotThreadSafe
public class IndexedDateTimeXYSeries extends ListXYSeriesOHLC {

    public IndexedDateTimeXYSeries(final String seriesKey, final List<? extends MutableXYDataItemOHLC> data) {
        super(seriesKey, data);
    }

    @Override
    public Number getX(final int index) {
        return index;
    }

}
