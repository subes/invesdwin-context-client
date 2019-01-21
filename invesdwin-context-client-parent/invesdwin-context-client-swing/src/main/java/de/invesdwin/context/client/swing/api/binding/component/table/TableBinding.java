package de.invesdwin.context.client.swing.api.binding.component.table;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import de.invesdwin.context.client.swing.api.binding.BindingGroup;
import de.invesdwin.context.client.swing.api.binding.component.AComponentBinding;
import de.invesdwin.norva.beanpath.spi.element.ATableBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.modifier.IBeanPathPropertyModifier;

@NotThreadSafe
public class TableBinding extends AComponentBinding<JTable, List<?>> {

    private final ATableBeanPathElement element;
    private final TableModelBinding tableModel;

    public TableBinding(final JTable component, final ATableBeanPathElement element, final BindingGroup bindingGroup) {
        super(component, element, bindingGroup);
        this.element = element;
        this.tableModel = new TableModelBinding(element.getColumns());
        configureSelectionMode(component);
    }

    protected void configureSelectionMode(final JTable component) {
        //model selection is handled via a special checkbox column
        component.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        component.setCellSelectionEnabled(false);
        component.setRowSelectionAllowed(true);
    }

    @Override
    protected boolean isModifiable() {
        return false;
    }

    @Override
    protected IBeanPathPropertyModifier<List<?>> getModifier() {
        return element.getChoiceModifier();
    }

    @Override
    protected void fromModelToComponent(final List<?> modelValue) {
        tableModel.update(modelValue);
    }

    @Override
    protected List<?> fromComponentToModel() {
        throw new UnsupportedOperationException("not modifiable");
    }

}
