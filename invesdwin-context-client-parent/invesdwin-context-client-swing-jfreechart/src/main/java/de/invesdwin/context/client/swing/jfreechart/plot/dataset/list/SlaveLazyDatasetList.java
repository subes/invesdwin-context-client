package de.invesdwin.context.client.swing.jfreechart.plot.dataset.list;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import de.invesdwin.context.client.swing.jfreechart.panel.InteractiveChartPanel;
import de.invesdwin.context.jfreechart.dataset.XYDataItemOHLC;
import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.time.fdate.FDate;

@ThreadSafe
public class SlaveLazyDatasetList extends ALazyDatasetList<XYDataItemOHLC> implements ISlaveLazyDatasetListener {

    private final ISlaveLazyDatasetProvider provider;
    private final MasterLazyDatasetList master;

    public SlaveLazyDatasetList(final InteractiveChartPanel chartPanel, final ISlaveLazyDatasetProvider provider) {
        this.provider = provider;
        this.master = (MasterLazyDatasetList) chartPanel.getDataset().getData();
        master.registerSlaveDatasetListener(this);
    }

    @Override
    public synchronized void append(final int appendCount) {
        final int masterSizeAfter = master.size();
        final int masterSizeBefore = masterSizeAfter - appendCount;
        int countRemoved = 0;
        //remove at least two elements
        while (data.size() > masterSizeBefore || countRemoved < 2) {
            data.remove(data.size() - 1);
            countRemoved++;
        }
        final int fromIndex = data.size();
        final int toIndex = masterSizeAfter - 1;
        for (int i = fromIndex; i <= toIndex; i++) {
            final FDate key = FDate.valueOf(master.get(i).getDate());
            final XYDataItemOHLC next = provider.getValue(key);
            if (next != null) {
                data.add(next);
            } else {
                //verify that skipping incomplete bars only happens for the last element
                Assertions.checkEquals(i, toIndex);
            }
        }
        assertSameSizeAsMaster();
    }

    @Override
    public synchronized void prepend(final int prependCount) {
        final List<XYDataItemOHLC> prependItems = new ArrayList<>(prependCount);
        for (int i = 0; i < prependCount; i++) {
            final FDate key = FDate.valueOf(master.get(i).getDate());
            final XYDataItemOHLC value = provider.getValue(key);
            Assertions.checkNotNull(value);
            prependItems.add(value);
        }
        data.addAll(0, prependItems);
        assertSameSizeAsMaster();
    }

    private void assertSameSizeAsMaster() {
        if (data.size() != master.size() && data.size() != master.size() - 1) {
            throw new IllegalStateException("data.size [" + data.size() + "] should be between master.size ["
                    + master.size() + "] and master.size-1 [" + (master.size() - 1) + "]");
        }
    }

    @Override
    public synchronized void loadInitial() {
        data = new ArrayList<>(data.size());
        for (int i = 0; i < master.size(); i++) {
            final FDate key = FDate.valueOf(master.get(i).getDate());
            final XYDataItemOHLC value = provider.getValue(key);
            if (value != null) {
                data.add(value);
            } else {
                Assertions.checkEquals(i, master.size() - 1);
            }
        }
    }

    @Override
    public synchronized void removeStart(final int tooManyBefore) {
        for (int i = 0; i < tooManyBefore; i++) {
            data.remove(0);
        }
        assertSameSizeAsMaster();
    }

    @Override
    public synchronized void removeEnd(final int tooManyAfter) {
        for (int i = 0; i < tooManyAfter; i++) {
            data.remove(data.size() - 1);
        }
        assertSameSizeAsMaster();
    }

}