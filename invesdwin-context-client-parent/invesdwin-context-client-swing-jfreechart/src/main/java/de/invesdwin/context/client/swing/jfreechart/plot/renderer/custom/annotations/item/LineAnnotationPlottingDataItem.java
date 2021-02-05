package de.invesdwin.context.client.swing.jfreechart.plot.renderer.custom.annotations.item;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.context.client.swing.jfreechart.plot.renderer.custom.annotations.AnnotationPlottingDataset;
import de.invesdwin.util.time.fdate.FDate;

@NotThreadSafe
public class LineAnnotationPlottingDataItem extends AAnnotationPlottingDataItem {

    private final double startPrice;
    private final FDate startTime;
    private final FDate endTime;
    private final double endPrice;
    private boolean itemLoaded;
    private int startTimeLoadedIndex = Integer.MIN_VALUE;
    private int endTimeLoadedIndex = Integer.MIN_VALUE;

    public LineAnnotationPlottingDataItem(final String annotationId, final double startPrice, final FDate startTime,
            final FDate endTime, final double endPrice) {
        super(annotationId);
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.endPrice = endPrice;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public FDate getStartTime() {
        return startTime;
    }

    public FDate getEndTime() {
        return endTime;
    }

    public double getEndPrice() {
        return endPrice;
    }

    @Override
    public boolean isItemLoaded() {
        return itemLoaded;
    }

    @Override
    public void updateItemLoaded(final long firstLoadedKeyMillis, final long lastLoadedKeyMillis,
            final boolean trailingLoaded, final AnnotationPlottingDataset dataset) {
        if (!trailingLoaded && getStartTime().millisValue() > lastLoadedKeyMillis
                || getEndTime() != null && getEndTime().millisValue() < firstLoadedKeyMillis) {
            if (itemLoaded) {
                itemLoaded = false;
                startTimeLoadedIndex = Integer.MIN_VALUE;
                endTimeLoadedIndex = Integer.MIN_VALUE;
            }
        } else {
            this.startTimeLoadedIndex = dataset.getDateTimeAsItemIndex(0, startTime);
            if (endTime != null) {
                this.endTimeLoadedIndex = dataset.getDateTimeAsItemIndex(0, endTime);
            } else {
                this.endTimeLoadedIndex = dataset.getItemCount(0) - 1;
            }
            itemLoaded = true;
        }
    }

    @Override
    public void modifyItemLoadedIndexes(final int fromIndex, final int addend) {
        if (itemLoaded) {
            if (startTimeLoadedIndex >= fromIndex) {
                startTimeLoadedIndex += addend;
            }
            if (endTimeLoadedIndex >= fromIndex) {
                endTimeLoadedIndex += addend;
            }
        }
    }

    @Override
    public int innerGetStartTimeLoadedIndex() {
        return startTimeLoadedIndex;
    }

    @Override
    public int innerGetEndTimeLoadedIndex() {
        return endTimeLoadedIndex;
    }

}
