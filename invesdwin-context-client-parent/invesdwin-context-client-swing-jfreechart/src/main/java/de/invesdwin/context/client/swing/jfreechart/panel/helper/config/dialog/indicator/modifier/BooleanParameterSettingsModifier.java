package de.invesdwin.context.client.swing.jfreechart.panel.helper.config.dialog.indicator.modifier;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.series.indicator.IIndicatorSeriesParameter;
import de.invesdwin.util.math.expression.IExpression;
import de.invesdwin.util.math.expression.eval.BooleanConstantExpression;

@NotThreadSafe
public class BooleanParameterSettingsModifier extends AParameterSettingsModifier {

    private final JCheckBox component;

    public BooleanParameterSettingsModifier(final IIndicatorSeriesParameter parameter,
            final Runnable modificationListener) {
        super(parameter);
        component = new JCheckBox();
        component.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                BooleanParameterSettingsModifier.super.setValue(
                        BooleanConstantExpression.valueOf(component.isSelected()));
                modificationListener.run();
            }
        });
    }

    @Override
    public void setValue(final IExpression value) {
        super.setValue(value);
        component.setSelected(value.newEvaluateBoolean().evaluateBoolean());
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

}
