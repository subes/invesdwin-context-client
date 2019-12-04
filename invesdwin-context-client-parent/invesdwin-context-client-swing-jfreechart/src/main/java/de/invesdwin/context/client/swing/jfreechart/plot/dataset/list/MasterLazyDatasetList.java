package de.invesdwin.context.client.swing.jfreechart.plot.dataset.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jfree.data.Range;
import org.jfree.data.xy.OHLCDataItem;

import com.github.benmanes.caffeine.cache.Caffeine;

import de.invesdwin.context.client.swing.jfreechart.panel.InteractiveChartPanel;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.PlotZoomHelper;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.listener.IRangeListener;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.list.item.MasterOHLCDataItem;
import de.invesdwin.util.collections.iterable.ICloseableIterable;
import de.invesdwin.util.collections.iterable.ICloseableIterator;
import de.invesdwin.util.concurrent.WrappedExecutorService;
import de.invesdwin.util.concurrent.priority.IPriorityRunnable;
import de.invesdwin.util.math.Integers;
import de.invesdwin.util.time.fdate.FDate;

@ThreadSafe
public class MasterLazyDatasetList extends ALazyDatasetList<MasterOHLCDataItem> implements IChartPanelAwareDatasetList {

    private static final int STEP_ITEM_COUNT_MULTIPLIER = 10;
    private static final int PRELOAD_RANGE_MULTIPLIER = 2;
    /*
     * keep a few more items because we need to keep some buffer to the left and right and we don't want to load data on
     * all the smaller panning actions
     */
    private static final int MAX_STEP_ITEM_COUNT = PlotZoomHelper.MAX_ZOOM_ITEM_COUNT;
    private static final int MAX_ITEM_COUNT = MAX_STEP_ITEM_COUNT * 5;
    private static final int TRIM_ITEM_COUNT = MAX_STEP_ITEM_COUNT * 7;
    private final WrappedExecutorService executor;
    private final IMasterLazyDatasetProvider provider;
    private final Set<ISlaveLazyDatasetListener> slaveDatasetListeners;
    private final Set<IRangeListener> rangeListeners;
    private final FDate firstAvailableKey;
    private InteractiveChartPanel chartPanel;
    @GuardedBy("this")
    private FDate lastUpdateTime;

    public MasterLazyDatasetList(final IMasterLazyDatasetProvider provider, final WrappedExecutorService executor) {
        this.provider = provider;

        final ConcurrentMap<ISlaveLazyDatasetListener, Boolean> slaveDatasetListeners = Caffeine.newBuilder()
                .weakKeys()
                .<ISlaveLazyDatasetListener, Boolean> build()
                .asMap();
        this.slaveDatasetListeners = Collections.newSetFromMap(slaveDatasetListeners);
        final ConcurrentMap<IRangeListener, Boolean> limitRangeListeners = Caffeine.newBuilder()
                .weakKeys()
                .<IRangeListener, Boolean> build()
                .asMap();
        this.rangeListeners = Collections.newSetFromMap(limitRangeListeners);
        this.firstAvailableKey = provider.getFirstAvailableKey();
        this.executor = executor;
    }

    @Override
    protected MasterOHLCDataItem dummyValue() {
        return MasterOHLCDataItem.DUMMY_VALUE;
    }

    public WrappedExecutorService getExecutor() {
        return executor;
    }

    @Override
    public synchronized void setChartPanel(final InteractiveChartPanel chartPanel) {
        this.chartPanel = chartPanel;
        chartPanel.getPlotZoomHelper().getRangeListeners().add(new LimitRangeListenerImpl());
    }

    @Override
    public synchronized void resetRange() {
        final boolean empty = getData().isEmpty();
        if (empty || getLastLoadedKey().isBefore(getResetReferenceTime())) {
            if (!empty) {
                newData();
            }
            final Runnable task = newSyncTask(new Runnable() {
                @Override
                public void run() {
                    loadInitialDataMaster();
                    if (!slaveDatasetListeners.isEmpty()) {
                        for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                            slave.loadIinitialItems(false);
                        }
                    }
                }
            });
            task.run();
            if (!slaveDatasetListeners.isEmpty()) {
                executor.execute(new IPriorityRunnable() {
                    @Override
                    public void run() {
                        final List<MasterOHLCDataItem> data = getData();
                        for (int i = 0; i < data.size(); i++) {
                            final FDate key = FDate.valueOf(data.get(i).getDate());
                            data.get(i).loadSlaveItems(key);
                        }
                        for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                            slave.afterLoadItems(true);
                        }
                    }

                    @Override
                    public double getPriority() {
                        return 0;
                    }
                });
            }
        }
    }

    public synchronized void reloadData() {
        if (getData().isEmpty()) {
            return;
        }
        final Runnable task = newSyncTask(new Runnable() {
            @Override
            public void run() {
                reloadDataMaster();
                if (!slaveDatasetListeners.isEmpty()) {
                    for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                        slave.loadIinitialItems(false);
                    }
                }
            }
        });
        task.run();
        if (!slaveDatasetListeners.isEmpty()) {
            executor.execute(new IPriorityRunnable() {
                @Override
                public void run() {
                    final List<MasterOHLCDataItem> data = getData();
                    for (int i = 0; i < data.size(); i++) {
                        final FDate key = FDate.valueOf(data.get(i).getDate());
                        data.get(i).loadSlaveItems(key);
                    }
                    for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                        slave.afterLoadItems(true);
                    }
                }

                @Override
                public double getPriority() {
                    return 0;
                }
            });
        }
    }

    protected Runnable newSyncTask(final Runnable syncTask) {
        return syncTask;
    }

    private void reloadDataMaster() {
        final ICloseableIterable<? extends OHLCDataItem> initialValues = provider.getValues(getFirstLoadedKey(),
                getLastLoadedKey());
        final List<MasterOHLCDataItem> data = newData();
        try (ICloseableIterator<? extends OHLCDataItem> it = initialValues.iterator()) {
            while (true) {
                final OHLCDataItem next = it.next();
                data.add(new MasterOHLCDataItem(next));
            }
        } catch (final NoSuchElementException e) {
            //end reached
        }
    }

    private FDate getResetReferenceTime() {
        if (lastUpdateTime != null && isTrailing(chartPanel.getDomainAxis().getRange())) {
            return lastUpdateTime;
        } else {
            return provider.getLastAvailableKey();
        }
    }

    public synchronized boolean isTrailing(final Range range) {
        return range.getUpperBound() >= (getData().size() - 1);
    }

    private void loadInitialDataMaster() {
        final int initialVisibleItemCount = chartPanel.getInitialVisibleItemCount() * PRELOAD_RANGE_MULTIPLIER;
        final ICloseableIterable<? extends OHLCDataItem> initialValues = provider
                .getPreviousValues(provider.getLastAvailableKey(), initialVisibleItemCount);
        final List<MasterOHLCDataItem> data = getData();
        try (ICloseableIterator<? extends OHLCDataItem> it = initialValues.iterator()) {
            while (true) {
                final OHLCDataItem next = it.next();
                data.add(new MasterOHLCDataItem(next));
            }
        } catch (final NoSuchElementException e) {
            //end reached
        }
    }

    private synchronized Range maybeTrimDataRange(final Range range, final MutableBoolean rangeChanged) {
        Range updatedRange = range;
        final List<MasterOHLCDataItem> data = getData();
        if (data.size() > TRIM_ITEM_COUNT) {
            //trim both ends based on center
            final int centralValueAdj = Integers.max(0, (int) range.getLowerBound())
                    + Integers.min(data.size() - 1, (int) range.getUpperBound()) / 2;
            int tooManyBefore = centralValueAdj - MAX_ITEM_COUNT / 2;
            int tooManyAfter = data.size() - centralValueAdj - MAX_ITEM_COUNT / 2;
            if (tooManyBefore < 0) {
                tooManyAfter += tooManyBefore;
            }
            if (tooManyAfter < 0) {
                tooManyBefore += tooManyAfter;
            }
            if (tooManyBefore > 0) {
                updatedRange = removeTooManyBefore(range, rangeChanged, data, tooManyBefore);
            }
            if (tooManyAfter > 0) {
                removeTooManyAfter(data, tooManyAfter);
            }
        }
        return updatedRange;
    }

    private void removeTooManyAfter(final List<MasterOHLCDataItem> data, final int tooManyAfter) {
        chartPanel.incrementUpdatingCount();
        try {
            for (int i = 0; i < tooManyAfter; i++) {
                data.remove(data.size() - 1);
            }
            if (!slaveDatasetListeners.isEmpty()) {
                for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                    slave.removeEndItems(tooManyAfter);
                }
            }
        } finally {
            chartPanel.decrementUpdatingCount();
        }
    }

    private Range removeTooManyBefore(final Range range, final MutableBoolean rangeChanged,
            final List<MasterOHLCDataItem> data, final int tooManyBefore) {
        chartPanel.incrementUpdatingCount();
        try {
            for (int i = 0; i < tooManyBefore; i++) {
                data.remove(0);
            }
            final Range updatedRange = new Range(range.getLowerBound() - tooManyBefore,
                    range.getUpperBound() - tooManyBefore);
            rangeChanged.setTrue();
            if (!slaveDatasetListeners.isEmpty()) {
                for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                    slave.removeStartItems(tooManyBefore);
                }
            }
            return updatedRange;
        } finally {
            chartPanel.decrementUpdatingCount();
        }
    }

    public synchronized Range maybeLoadDataRange(final Range range, final MutableBoolean rangeChanged) {
        final List<MasterOHLCDataItem> data = getData();
        if (executor.getPendingCount() > 0) {
            //wait for lazy loading to finish
            return limitLoadedRange(range, rangeChanged, data);
        }
        final boolean isTrailing = isTrailing(range);
        Range updatedRange = range;
        final int preloadLowerBound = (int) (range.getLowerBound() - range.getLength());
        if (preloadLowerBound < 0) {
            final FDate firstLoadedKey = getFirstLoadedKey();
            if (firstAvailableKey.isBefore(firstLoadedKey)) {
                //prepend a whole screen additional to the requested items
                final int prependCount = Integers.min(MAX_STEP_ITEM_COUNT,
                        Integers.abs(preloadLowerBound) * STEP_ITEM_COUNT_MULTIPLIER);
                System.out.println("prepend " + prependCount);
                final List<MasterOHLCDataItem> prependItems;
                chartPanel.incrementUpdatingCount(); //prevent flickering
                try {
                    prependItems = prependMaster(prependCount);
                    prependSlaves(prependCount);
                } finally {
                    chartPanel.decrementUpdatingCount();
                }
                updatedRange = new Range(range.getLowerBound() + prependCount, range.getUpperBound() + prependCount);
                rangeChanged.setTrue();

                final ICloseableIterable<? extends OHLCDataItem> masterPrependValues = provider
                        .getPreviousValues(firstLoadedKey.addMilliseconds(-1), prependCount);
                loadItems(data, prependItems, masterPrependValues);
            }
        }
        //wait for update instead if trailing
        if (!isTrailing) {
            final int preloadUpperBound = (int) (range.getUpperBound() + range.getLength());
            if (preloadUpperBound > data.size()) {
                final FDate lastLoadedKey = getLastLoadedKey();
                if (provider.getLastAvailableKey().isAfter(lastLoadedKey)) {
                    //append a whole screen additional to the requested items
                    final int appendCount = Integers.min(MAX_STEP_ITEM_COUNT,
                            (preloadUpperBound - data.size()) * STEP_ITEM_COUNT_MULTIPLIER);
                    if (appendCount > 0) {
                        System.out.println("append " + appendCount);
                        final List<MasterOHLCDataItem> appendItems;
                        chartPanel.incrementUpdatingCount(); //prevent flickering
                        try {
                            appendItems = appendMaster(appendCount);
                            appendSlaves(appendCount);
                        } finally {
                            chartPanel.decrementUpdatingCount();
                        }

                        final ICloseableIterable<? extends OHLCDataItem> appendMasterValues = provider
                                .getNextValues(FDate.valueOf(appendItems.get(0).getDate()), appendItems.size());
                        loadItems(data, appendItems, appendMasterValues);
                    }
                }
            }
        }
        return updatedRange;
    }

    private Range limitLoadedRange(final Range range, final MutableBoolean rangeChanged,
            final List<MasterOHLCDataItem> data) {
        if (data.isEmpty()) {
            return range;
        }
        final int from = Integers.max(0, (int) range.getLowerBound());
        final int central = (int) range.getCentralValue();
        final int to = Integers.min((int) range.getUpperBound(), data.size() - 1);
        if (central <= from || central >= to || !data.get(central).isSlaveItemsLoaded()) {
            return range;
        }
        Range modifiedRange = range;
        //scrolling higher
        for (int i = central + 1; i <= to; i++) {
            if (!data.get(i).isSlaveItemsLoaded()) {
                modifiedRange = new Range(modifiedRange.getLowerBound(), i - 1);
                rangeChanged.setTrue();
                break;
            }
        }
        //scrolling lower
        for (int i = central - 1; i >= from; i--) {
            if (!data.get(i).isSlaveItemsLoaded()) {
                modifiedRange = new Range(i + 1, modifiedRange.getUpperBound());
                rangeChanged.setTrue();
                break;
            }
        }
        if (rangeChanged.isTrue()) {
            System.out.println(range + " -> " + modifiedRange);
        }
        return modifiedRange;
    }

    private void loadItems(final List<MasterOHLCDataItem> data, final List<MasterOHLCDataItem> items,
            final ICloseableIterable<? extends OHLCDataItem> masterValues) {
        final double priority = items.get(0).getDate().getTime();
        executor.execute(new IPriorityRunnable() {
            @Override
            public void run() {
                int nextItemsIndex = 0;
                try (ICloseableIterator<? extends OHLCDataItem> it = masterValues.iterator()) {
                    while (true) {
                        final OHLCDataItem next = it.next();
                        final FDate key = FDate.valueOf(next.getDate());
                        final MasterOHLCDataItem appendItem = items.get(nextItemsIndex);
                        appendItem.setOHLC(next);
                        appendItem.loadSlaveItems(key);
                        nextItemsIndex++;
                    }
                } catch (final NoSuchElementException e) {
                    //end reached
                }
                chartPanel.incrementUpdatingCount();
                try {
                    final int tooManyAfter = items.size() - nextItemsIndex;
                    if (tooManyAfter > 0) {
                        System.out.println("tooManyAfter " + tooManyAfter);
                        final int removeMasterIndex = data.indexOf(items.get(nextItemsIndex));
                        if (removeMasterIndex >= 0) {
                            synchronized (MasterLazyDatasetList.this) {
                                for (int i = 0; i < tooManyAfter; i++) {
                                    data.remove(removeMasterIndex);
                                }
                                if (!slaveDatasetListeners.isEmpty()) {
                                    for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                                        slave.removeMiddleItems(removeMasterIndex, tooManyAfter);
                                    }
                                }
                            }
                        }
                    }
                    if (!slaveDatasetListeners.isEmpty()) {
                        for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                            slave.afterLoadItems(true);
                        }
                    }
                    chartPanel.update();
                } finally {
                    chartPanel.decrementUpdatingCount();
                }
            }

            @Override
            public double getPriority() {
                return priority;
            }

        });
    }

    private List<MasterOHLCDataItem> appendMaster(final int appendCount) {
        //invalidate two elements to reload them
        final List<MasterOHLCDataItem> appendItems = new ArrayList<>(appendCount);
        final List<MasterOHLCDataItem> data = getData();
        for (int i = Math.max(0, data.size() - 2); i <= data.size() - 1; i++) {
            final MasterOHLCDataItem existingItem = data.get(i);
            appendItems.add(existingItem);
        }
        final Date lastLoadedKey = data.get(data.size() - 1).getDate();
        for (int i = 0; i < appendCount; i++) {
            final MasterOHLCDataItem appendItem = new MasterOHLCDataItem(lastLoadedKey);
            appendItems.add(appendItem);
            data.add(appendItem);
        }
        return appendItems;
    }

    private void appendSlaves(final int appendCount) {
        if (!slaveDatasetListeners.isEmpty()) {
            for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                slave.appendItems(appendCount);
            }
        }
    }

    private List<MasterOHLCDataItem> prependMaster(final int prependCount) {
        final List<MasterOHLCDataItem> prependItems = new ArrayList<>(prependCount);
        final List<MasterOHLCDataItem> data = getData();
        final Date firstLoadedKey = data.get(0).getDate();
        for (int i = 0; i < prependCount; i++) {
            final MasterOHLCDataItem prependItem = new MasterOHLCDataItem(firstLoadedKey);
            prependItems.add(prependItem);
        }
        getData().addAll(0, prependItems);
        return prependItems;
    }

    private void prependSlaves(final int prependCount) {
        if (!slaveDatasetListeners.isEmpty()) {
            for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                slave.prependItems(prependCount);
            }
        }
    }

    public synchronized FDate getFirstLoadedKey() {
        return getLoadedKey(0);
    }

    public synchronized FDate getLastLoadedKey() {
        return getLoadedKey(getData().size() - 1);
    }

    private FDate getLoadedKey(final int i) {
        return FDate.valueOf(getData().get(i).getDate());
    }

    private final class LimitRangeListenerImpl implements IRangeListener {

        @Override
        public Range beforeLimitRange(final Range range, final MutableBoolean rangeChanged) {
            Range updatedRange = maybeLoadDataRange(range, rangeChanged);
            if (!rangeListeners.isEmpty()) {
                for (final IRangeListener l : rangeListeners) {
                    updatedRange = l.beforeLimitRange(updatedRange, rangeChanged);
                }
            }
            return updatedRange;
        }

        @Override
        public Range afterLimitRange(final Range range, final MutableBoolean rangeChanged) {
            Range updatedRange = maybeTrimDataRange(range, rangeChanged);
            if (!rangeListeners.isEmpty()) {
                for (final IRangeListener l : rangeListeners) {
                    updatedRange = l.afterLimitRange(updatedRange, rangeChanged);
                }
            }
            return updatedRange;
        }

        @Override
        public void onRangeChanged(final Range range) {
            if (!rangeListeners.isEmpty()) {
                for (final IRangeListener l : rangeListeners) {
                    l.onRangeChanged(range);
                }
            }
        }
    }

    public synchronized void registerSlaveDatasetListener(final ISlaveLazyDatasetListener slaveDatasetListener) {
        slaveDatasetListeners.add(slaveDatasetListener);

        executor.execute(new Runnable() { //show task info and load data async
            @Override
            public void run() {
                //sync slave data with master
                slaveDatasetListener.loadIinitialItems(true);
            }
        });
    }

    public synchronized void registerRangeListener(final IRangeListener rangeListener) {
        rangeListeners.add(rangeListener);
    }

    public synchronized boolean update(final FDate lastTickTime) {
        if (executor.getPendingCount() > 0) {
            //wait for lazy loading to finish
            return false;
        }
        final List<MasterOHLCDataItem> data = getData();
        if (data.isEmpty()) {
            resetRange();
            return !data.isEmpty();
        }
        final Range rangeBefore = chartPanel.getDomainAxis().getRange();
        final boolean isTrailing = isTrailing(rangeBefore);
        if (!isTrailing) {
            return false;
        }
        chartPanel.incrementUpdatingCount();
        try {
            return updateTrailingItems(lastTickTime, data, rangeBefore);
        } finally {
            chartPanel.decrementUpdatingCount();
        }
    }

    private boolean updateTrailingItems(final FDate lastTickTime, final List<MasterOHLCDataItem> data,
            final Range rangeBefore) {
        //remove at least two elements
        int lastItemIndex = Math.max(0, data.size() - 3);
        OHLCDataItem lastItem = data.get(lastItemIndex);
        final ICloseableIterable<? extends OHLCDataItem> history = provider.getValues(new FDate(lastItem.getDate()),
                lastTickTime);
        final int firstAppendIndex = lastItemIndex;
        int appendCount = 0;
        try (ICloseableIterator<? extends OHLCDataItem> it = history.iterator()) {
            while (true) {
                final OHLCDataItem item = it.next();
                final MasterOHLCDataItem newItem = new MasterOHLCDataItem(item);
                if (lastItemIndex < data.size()) {
                    lastItem = data.get(lastItemIndex);
                    if (!item.equals(lastItem)) {
                        data.set(lastItemIndex, newItem);
                        lastItem = newItem;
                        appendCount++;
                    }
                } else if (item.getDate().after(lastItem.getDate())) {
                    data.add(newItem);
                    appendCount++;
                    lastItem = newItem;
                }
                lastItemIndex++;
            }
        } catch (final NoSuchElementException ex) {
            // end reached
        }
        if (appendCount > 0) {
            /*
             * we need to replace at least the last two elements, otherwise if the slave does not draw incomplete bars,
             * the NaN bar will always be appended without the real value appearing
             */
            appendSlaves(appendCount);
            lastUpdateTime = getLastLoadedKey();

            //trail range
            final Range updatedRange = new Range(rangeBefore.getLowerBound() + appendCount,
                    rangeBefore.getUpperBound() + appendCount);
            chartPanel.getDomainAxis().setRange(updatedRange);

            //load slave items
            for (int i = firstAppendIndex; i < lastItemIndex; i++) {
                final MasterOHLCDataItem item = data.get(i);
                item.loadSlaveItems(FDate.valueOf(item.getDate()));
            }
            if (!slaveDatasetListeners.isEmpty()) {
                for (final ISlaveLazyDatasetListener slave : slaveDatasetListeners) {
                    slave.afterLoadItems(false);
                }
            }
            return true;
        } else {
            return false;
        }
    }

}
