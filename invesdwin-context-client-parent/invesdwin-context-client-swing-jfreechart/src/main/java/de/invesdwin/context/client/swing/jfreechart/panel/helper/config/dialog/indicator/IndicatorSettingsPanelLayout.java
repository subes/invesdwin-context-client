package de.invesdwin.context.client.swing.jfreechart.panel.helper.config.dialog.indicator;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.invesdwin.context.client.swing.jfreechart.panel.helper.config.dialog.indicator.modifier.IParameterSettingsModifier;
import de.invesdwin.util.swing.Components;

@NotThreadSafe
public class IndicatorSettingsPanelLayout extends JPanel {

    //CHECKSTYLE:OFF
    public final IParameterSettingsModifier[] modifiers;
    //CHECKSTYLE:ON

    public IndicatorSettingsPanelLayout(final IParameterSettingsModifier[] modifiers) {
        this.modifiers = modifiers;

        setLayout(new FlowLayout());
        int rows = 0;
        for (int i = 0; i < modifiers.length; i++) {
            final IParameterSettingsModifier modifier = modifiers[i];
            final JLabel label = new JLabel(modifier.getParameter().getName());
            Components.setToolTipText(label, modifier.getParameter().getDescription(), false);
            add(label);
            final JComponent modifierComponent = modifier.getComponent();
            add(modifierComponent);
            rows++;
        }

        setLayout(new GridLayout(rows, 2, 5, 5));

    }

}
