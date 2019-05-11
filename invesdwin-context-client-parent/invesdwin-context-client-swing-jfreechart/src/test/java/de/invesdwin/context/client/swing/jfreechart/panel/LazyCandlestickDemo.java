package de.invesdwin.context.client.swing.jfreechart.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Stroke;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.fife.ui.autocomplete.CompletionProvider;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.OHLCDataItem;

import de.invesdwin.context.client.swing.jfreechart.panel.basis.CustomCombinedDomainXYPlot;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.LineStyleType;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.LineWidthType;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.SeriesRendererType;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.series.SeriesParameterType;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.series.expression.IExpressionSeriesProvider;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.series.indicator.IIndicatorSeriesParameter;
import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.series.indicator.IIndicatorSeriesProvider;
import de.invesdwin.context.client.swing.jfreechart.plot.XYPlots;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.IPlotSourceDataset;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.IndexedDateTimeOHLCDataset;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.IndexedDateTimeXYSeries;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.PlotSourceXYSeriesCollection;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.list.IMasterLazyDatasetProvider;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.list.ISlaveLazyDatasetProvider;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.list.MasterLazyDatasetList;
import de.invesdwin.context.client.swing.jfreechart.plot.dataset.list.SlaveLazyDatasetList;
import de.invesdwin.context.client.swing.rsyntaxtextarea.expression.ExpressionCompletionProvider;
import de.invesdwin.context.client.swing.rsyntaxtextarea.expression.completion.IAliasedCompletion;
import de.invesdwin.context.jfreechart.dataset.XYDataItemOHLC;
import de.invesdwin.context.log.error.Err;
import de.invesdwin.context.system.properties.SystemProperties;
import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.collections.iterable.ICloseableIterable;
import de.invesdwin.util.collections.loadingcache.historical.AHistoricalCache;
import de.invesdwin.util.collections.loadingcache.historical.AIterableGapHistoricalCache;
import de.invesdwin.util.collections.loadingcache.historical.query.IHistoricalCacheQuery;
import de.invesdwin.util.error.UnknownArgumentException;
import de.invesdwin.util.lang.Reflections;
import de.invesdwin.util.lang.UniqueNameGenerator;
import de.invesdwin.util.math.expression.ExpressionParser;
import de.invesdwin.util.math.expression.IExpression;
import de.invesdwin.util.math.expression.eval.BooleanExpression;
import de.invesdwin.util.math.expression.eval.ConstantExpression;
import de.invesdwin.util.math.expression.eval.EnumerationExpression;
import de.invesdwin.util.time.fdate.FDate;

@NotThreadSafe
public class LazyCandlestickDemo extends JFrame {

    private static final String PRICE_PLOT_PANE_ID = "Price";
    private static final UniqueNameGenerator SERIES_ID_GENERATOR = new UniqueNameGenerator();
    private final List<OHLCDataItem> dataItems;
    private final AHistoricalCache<OHLCDataItem> dataItemsCache;

    public LazyCandlestickDemo() {
        super("CandlestickDemo");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        dataItems = loadDataItems();
        dataItemsCache = new AIterableGapHistoricalCache<OHLCDataItem>() {

            @Override
            protected FDate innerExtractKey(final FDate key, final OHLCDataItem value) {
                return FDate.valueOf(value.getDate());
            }

            @Override
            protected Iterable<OHLCDataItem> createDelegate() {
                return dataItems;
            }
        };

        final InteractiveChartPanel chartPanel = new InteractiveChartPanel(getDataSet());
        chartPanel.setPreferredSize(new Dimension(1280, 800));
        add(chartPanel);
        chartPanel.getPlotConfigurationHelper().putIndicatorSeriesProvider(new CustomIndicatorSeriesProvider());
        chartPanel.getPlotConfigurationHelper().putIndicatorSeriesProvider(new ThrowExceptionIndicatorSeriesProvider());

        chartPanel.getPlotConfigurationHelper().setExpressionSeriesProvider(new CustomExpressionSeriesProvider());
        this.pack();

        chartPanel.initialize();
    }

    private static List<OHLCDataItem> loadDataItems() {
        final List<OHLCDataItem> dataItems = new ArrayList<OHLCDataItem>();
        try {
            /*
             * String strUrl=
             * "https://query1.finance.yahoo.com/v7/finance/download/MSFT?period1=1493801037&period2=1496479437&interval=1d&events=history&crumb=y/oR8szwo.9";
             */
            final File f = new File("src/test/java/"
                    + LazyCandlestickDemo.class.getPackage().getName().replace(".", "/") + "/MSFTlong.csv");
            final BufferedReader in = new BufferedReader(new FileReader(f));

            final DateFormat df = new java.text.SimpleDateFormat("y-M-d");

            String inputLine;
            in.readLine();
            while ((inputLine = in.readLine()) != null) {
                final StringTokenizer st = new StringTokenizer(inputLine, ",");

                final Date date = df.parse(st.nextToken());
                final double open = Double.parseDouble(st.nextToken());
                final double high = Double.parseDouble(st.nextToken());
                final double low = Double.parseDouble(st.nextToken());
                @SuppressWarnings("unused")
                final double close = Double.parseDouble(st.nextToken());
                final double adjClose = Double.parseDouble(st.nextToken());
                final double volume = Double.parseDouble(st.nextToken());

                final OHLCDataItem item = new OHLCDataItem(date, open, high, low, adjClose, volume);
                dataItems.add(item);
            }
            in.close();
        } catch (final Exception e) {
            throw Err.process(e);
        }
        return dataItems;
    }

    protected IndexedDateTimeOHLCDataset getDataSet() {

        //This is where we go get the data, replace with your own data source
        final List<OHLCDataItem> data = getData();

        //Create a dataset, an Open, High, Low, Close dataset
        final IndexedDateTimeOHLCDataset result = new IndexedDateTimeOHLCDataset("MSFT", data);
        result.setPrecision(2);
        result.setRangeAxisId(PRICE_PLOT_PANE_ID);

        return result;
    }

    //This method uses yahoo finance to get the OHLC data
    protected List<OHLCDataItem> getData() {
        final IMasterLazyDatasetProvider provider = new IMasterLazyDatasetProvider() {
            private final IHistoricalCacheQuery<OHLCDataItem> query = dataItemsCache.query();

            @Override
            public FDate getFirstAvailableKey() {
                return FDate.valueOf(dataItems.get(0).getDate());
            }

            @Override
            public FDate getLastAvailableKey() {
                return FDate.valueOf(dataItems.get(dataItems.size() - 1).getDate());
            }

            @Override
            public ICloseableIterable<OHLCDataItem> getPreviousValues(final FDate key, final int count) {
                return query.getPreviousValues(key, count);
            }

            @Override
            public ICloseableIterable<? extends OHLCDataItem> getValues(final FDate from, final FDate to) {
                return query.getValues(from, to);
            }

        };
        return new MasterLazyDatasetList(provider);
    }

    //CHECKSTYLE:OFF
    public static void main(final String[] args) {
        if (Reflections.JAVA_VERSION < 12) {
            new SystemProperties().setInteger("jdk.gtk.version", 2);
        }
        //CHECKSTYLE:ON
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
        new LazyCandlestickDemo().setVisible(true);
    }

    private final class CustomExpressionSeriesProvider implements IExpressionSeriesProvider {
        @Override
        public IPlotSourceDataset newInstance(final InteractiveChartPanel chartPanel, final String expression,
                final String plotPaneId, final SeriesRendererType rendererType) {
            final Stroke stroke = chartPanel.getPlotConfigurationHelper().getPriceInitialSettings().getSeriesStroke();
            final LineStyleType lineStyleType = LineStyleType.valueOf(stroke);
            final LineWidthType lineWidthType = LineWidthType.valueOf(stroke);
            final Color color = Color.GREEN;
            final boolean priceLineVisible = false;
            final boolean priceLabelVisible = false;

            final String seriesId = SERIES_ID_GENERATOR.get(plotPaneId);
            final PlotSourceXYSeriesCollection dataset = new PlotSourceXYSeriesCollection(seriesId);
            dataset.setSeriesTitle(expression);
            final XYPlot plot = chartPanel.getOhlcPlot();
            dataset.setPlot(plot);
            dataset.setPrecision(4);
            dataset.setInitialPlotPaneId(plotPaneId);
            dataset.setRangeAxisId(plotPaneId);
            final IndexedDateTimeXYSeries series = newSeriesMaster(chartPanel, expression, seriesId);
            final int datasetIndex = plot.getDatasetCount();
            dataset.addSeries(series);
            final XYItemRenderer renderer = rendererType.newRenderer(dataset, lineStyleType, lineWidthType, color,
                    priceLineVisible, priceLabelVisible);
            plot.setDataset(datasetIndex, dataset);
            plot.setRenderer(datasetIndex, renderer);
            XYPlots.updateRangeAxes(plot);
            chartPanel.update();

            if (!chartPanel.getCombinedPlot().isSubplotVisible(plot)) {
                chartPanel.getCombinedPlot().add(plot, CustomCombinedDomainXYPlot.INITIAL_PLOT_WEIGHT);
            }

            return dataset;
        }

        @Override
        public void modifyDataset(final InteractiveChartPanel chartPanel, final IPlotSourceDataset dataset,
                final String expression) {
            final PlotSourceXYSeriesCollection cDataset = (PlotSourceXYSeriesCollection) dataset;
            final String seriesId = dataset.getSeriesId();
            final IndexedDateTimeXYSeries series = newSeriesMaster(chartPanel, expression, seriesId);
            cDataset.setSeriesTitle(expression);
            cDataset.setNotify(false);
            cDataset.removeAllSeries();
            cDataset.addSeries(series);
            cDataset.setNotify(true);
        }

        private IndexedDateTimeXYSeries newSeriesMaster(final InteractiveChartPanel chartPanel,
                final String expressionStr, final String seriesId) {
            final IExpression expression = parseExpression(expressionStr);
            final IHistoricalCacheQuery<OHLCDataItem> sourceQuery = dataItemsCache.query();
            final ISlaveLazyDatasetProvider provider = new ISlaveLazyDatasetProvider() {

                @Override
                public XYDataItemOHLC getValue(final FDate key) {
                    final OHLCDataItem ohlc = sourceQuery.getValue(key);
                    final FDate time = new FDate(ohlc.getDate());
                    final double value = expression.evaluateDouble(time);
                    final XYDataItemOHLC item = new XYDataItemOHLC(
                            new OHLCDataItem(time.dateValue(), Double.NaN, Double.NaN, Double.NaN, value, Double.NaN));
                    return item;
                }

            };
            final IndexedDateTimeXYSeries series = new IndexedDateTimeXYSeries(seriesId,
                    new SlaveLazyDatasetList(chartPanel, provider));
            return series;
        }

        @Override
        public IExpression parseExpression(final String expression) {
            return new ExpressionParser(expression).parse();
        }

        @Override
        public CompletionProvider newCompletionProvider() {
            final Set<String> duplicateExpressionFilter = new HashSet<>();
            final Map<String, IAliasedCompletion> name_completion = new HashMap<>();
            final ExpressionCompletionProvider provider = new ExpressionCompletionProvider();
            provider.addDefaultCompletions(duplicateExpressionFilter, name_completion);
            return provider;
        }

    }

    private final class ThrowExceptionIndicatorSeriesProvider implements IIndicatorSeriesProvider {
        @Override
        public IPlotSourceDataset newInstance(final InteractiveChartPanel chartPanel, final IExpression[] args) {
            throw new RuntimeException("This should be displayed in a dialog.");
        }

        @Override
        public String getPlotPaneId() {
            return PRICE_PLOT_PANE_ID;
        }

        @Override
        public IIndicatorSeriesParameter[] getParameters() {
            return NO_PARAMETERS;
        }

        @Override
        public String getName() {
            return "throw exception";
        }

        @Override
        public String getExpressionName() {
            return "throwException";
        }

        @Override
        public String getDescription() {
            return "handles the exception";
        }

        @Override
        public void modifyDataset(final InteractiveChartPanel chartPanel, final IPlotSourceDataset dataset,
                final IExpression[] args) {
            throw new RuntimeException("This should never happen.");
        }
    }

    private final class CustomIndicatorSeriesProvider implements IIndicatorSeriesProvider {
        @Override
        public IPlotSourceDataset newInstance(final InteractiveChartPanel chartPanel, final IExpression[] args) {
            final Stroke stroke = chartPanel.getPlotConfigurationHelper().getPriceInitialSettings().getSeriesStroke();
            final LineStyleType lineStyleType = LineStyleType.valueOf(stroke);
            final LineWidthType lineWidthType = LineWidthType.valueOf(stroke);
            final Color color = Color.GREEN;
            final boolean priceLineVisible = false;
            final boolean priceLabelVisible = false;

            final PlotSourceXYSeriesCollection dataset = new PlotSourceXYSeriesCollection(getExpressionString(args));
            final XYPlot plot = chartPanel.getOhlcPlot();
            dataset.setPlot(plot);
            dataset.setPrecision(4);
            dataset.setInitialPlotPaneId(getPlotPaneId());
            dataset.setRangeAxisId(getPlotPaneId());
            final String seriesId = SERIES_ID_GENERATOR.get(getPlotPaneId());
            final IndexedDateTimeXYSeries series = newSeriesSlave(chartPanel, args, seriesId);
            final int datasetIndex = plot.getDatasetCount();
            dataset.addSeries(series);
            final SeriesRendererType seriesRendererType = SeriesRendererType.Line;
            final XYItemRenderer renderer = seriesRendererType.newRenderer(dataset, lineStyleType, lineWidthType, color,
                    priceLineVisible, priceLabelVisible);
            plot.setDataset(datasetIndex, dataset);
            plot.setRenderer(datasetIndex, renderer);
            XYPlots.updateRangeAxes(plot);
            chartPanel.update();

            if (!chartPanel.getCombinedPlot().isSubplotVisible(plot)) {
                chartPanel.getCombinedPlot().add(plot, CustomCombinedDomainXYPlot.INITIAL_PLOT_WEIGHT);
            }

            return dataset;
        }

        private IndexedDateTimeXYSeries newSeriesSlave(final InteractiveChartPanel chartPanel, final IExpression[] args,
                final String seriesId) {
            Assertions.checkEquals(4, args.length);
            final boolean invertAddition = args[0].evaluateBoolean();
            final int lagBars = args[1].evaluateInteger();
            Assertions.assertThat(lagBars).isNotNegative();
            double addition = args[2].evaluateDouble();
            if (invertAddition) {
                addition = -addition;
            }
            final double finalAdditon = addition;
            final OhlcValueType ohlcValueType = OhlcValueType.parseString(args[3].toString());

            final IHistoricalCacheQuery<OHLCDataItem> sourceQuery = dataItemsCache.query();
            final ISlaveLazyDatasetProvider provider = new ISlaveLazyDatasetProvider() {
                @Override
                public XYDataItemOHLC getValue(final FDate key) {
                    final OHLCDataItem ohlcItem = sourceQuery.getPreviousValue(key, lagBars);
                    final FDate time = new FDate(ohlcItem.getDate());
                    final double value = ohlcValueType.getValue(ohlcItem) + finalAdditon;
                    final XYDataItemOHLC item = new XYDataItemOHLC(
                            new OHLCDataItem(time.dateValue(), Double.NaN, Double.NaN, Double.NaN, value, Double.NaN));
                    return item;
                }
            };
            final IndexedDateTimeXYSeries series = new IndexedDateTimeXYSeries(getExpressionName(),
                    new SlaveLazyDatasetList(chartPanel, provider));
            return series;
        }

        @Override
        public void modifyDataset(final InteractiveChartPanel chartPanel, final IPlotSourceDataset dataset,
                final IExpression[] args) {
            final PlotSourceXYSeriesCollection cDataset = (PlotSourceXYSeriesCollection) dataset;
            final String seriesId = dataset.getSeriesId();
            final IndexedDateTimeXYSeries series = newSeriesSlave(chartPanel, args, seriesId);
            cDataset.setNotify(false);
            cDataset.removeAllSeries();
            cDataset.addSeries(series);
            cDataset.setNotify(true);
        }

        @Override
        public String getPlotPaneId() {
            return PRICE_PLOT_PANE_ID;
        }

        @Override
        public IIndicatorSeriesParameter[] getParameters() {
            return new IIndicatorSeriesParameter[] { new IIndicatorSeriesParameter() {

                @Override
                public SeriesParameterType getType() {
                    return SeriesParameterType.Boolean;
                }

                @Override
                public String getExpressionName() {
                    return "invertAddition";
                }

                @Override
                public String getName() {
                    return "Invert Addition";
                }

                @Override
                public IExpression[] getEnumerationValues() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return "boolean description";
                }

                @Override
                public IExpression getDefaultValue() {
                    return new BooleanExpression(false);
                }
            }, new IIndicatorSeriesParameter() {

                @Override
                public SeriesParameterType getType() {
                    return SeriesParameterType.Integer;
                }

                @Override
                public String getExpressionName() {
                    return "lagBars";
                }

                @Override
                public String getName() {
                    return "Lag Bars";
                }

                @Override
                public IExpression[] getEnumerationValues() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return "integer description";
                }

                @Override
                public IExpression getDefaultValue() {
                    return new ConstantExpression(0);
                }
            }, new IIndicatorSeriesParameter() {

                @Override
                public SeriesParameterType getType() {
                    return SeriesParameterType.Double;
                }

                @Override
                public String getExpressionName() {
                    return "addition";
                }

                @Override
                public String getName() {
                    return "Addition";
                }

                @Override
                public IExpression[] getEnumerationValues() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return "double description";
                }

                @Override
                public IExpression getDefaultValue() {
                    return new ConstantExpression(0);
                }
            }, new IIndicatorSeriesParameter() {

                @Override
                public SeriesParameterType getType() {
                    return SeriesParameterType.Enumeration;
                }

                @Override
                public String getExpressionName() {
                    return "ohlcValue";
                }

                @Override
                public String getName() {
                    return "OHLC Value";
                }

                @Override
                public IExpression[] getEnumerationValues() {
                    return EnumerationExpression.valueOf(OhlcValueType.values());
                }

                @Override
                public String getDescription() {
                    return "enum description";
                }

                @Override
                public IExpression getDefaultValue() {
                    return EnumerationExpression.valueOf(OhlcValueType.Close);
                }
            } };
        }

        @Override
        public String getName() {
            return "name";
        }

        @Override
        public String getExpressionName() {
            return "expression";
        }

        @Override
        public String getDescription() {
            return "description";
        }
    }

    private enum OhlcValueType {
        Open {
            @Override
            public double getValue(final OHLCDataItem item) {
                return item.getOpen().doubleValue();
            }
        },
        High {
            @Override
            public double getValue(final OHLCDataItem item) {
                return item.getHigh().doubleValue();
            }
        },
        Low {
            @Override
            public double getValue(final OHLCDataItem item) {
                return item.getLow().doubleValue();
            }
        },
        Close {
            @Override
            public double getValue(final OHLCDataItem item) {
                return item.getClose().doubleValue();
            }
        };

        public abstract double getValue(OHLCDataItem item);

        public static OhlcValueType parseString(final String str) {
            final String strClean = str.trim().toLowerCase();
            switch (strClean) {
            case "open":
                return OhlcValueType.Open;
            case "high":
                return OhlcValueType.High;
            case "low":
                return OhlcValueType.Low;
            case "close":
                return OhlcValueType.Close;
            default:
                throw UnknownArgumentException.newInstance(String.class, strClean);
            }
        }
    }
}