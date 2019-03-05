package de.invesdwin.context.client.swing.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.context.client.swing.api.AView;

@NotThreadSafe
public class UpdateAllViewsHelper {

    private static final UpdateAllViewsHelper DEFAULT_INSTANCE = new UpdateAllViewsHelper();

    public void process(final Component component) {
        final List<AView<?, ?>> views = getViews(component);
        update(views);
    }

    protected void update(final List<AView<?, ?>> views) {
        for (int i = 0; i < views.size(); i++) {
            views.get(i).getBindingGroup().update();
        }
    }

    protected List<AView<?, ?>> getViews(final Component component) {
        final List<AView<?, ?>> views = new ArrayList<>();
        new AViewVisitor() {
            @Override
            protected void visit(final AView<?, ?> view) {
                if (view.getBindingGroup() != null) {
                    views.add(view);
                }
            }
        }.visitAll(getRootComponent(component));
        return views;
    }

    protected Component getRootComponent(final Component component) {
        return Views.getRootComponentInDockable(component);
    }

    public static void update(final Component component) {
        DEFAULT_INSTANCE.process(component);
    }

}
