package de.invesdwin.context.client.swing.api.guiservice;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.TaskService;

import de.invesdwin.context.client.swing.api.view.AView;
import de.invesdwin.context.client.swing.frame.content.IWorkingAreaLocation;

public interface IGuiService {

    StatusBar getStatusBar();

    ContentPane getContentPane();

    TaskService getTaskService();

    boolean hideModalView();

    boolean hideModalView(AView<?, ?> view);

    void showModalView(AView<?, ?> view);

    void showModalView(AView<?, ?> view, Dimension dimension);

    /**
     * If a view is already visible with an equal model, then that is being replaced with the new view. Otherwise this
     * view is just displayed in a new dockable.
     * 
     * This method places the view next to the current focus owning view.
     */
    default void showView(final AView<?, ?> view) {
        showView(view, null);
    }

    /**
     * If a view is already visible with an equal model, then that is being replaced with the new view. Otherwise this
     * view is just displayed in a new dockable.
     * 
     * When location is null, view will be placed next to the current focus owning view.
     */
    void showView(AView<?, ?> view, IWorkingAreaLocation location);

    boolean isModalViewShowing();

    boolean isModalViewShowing(AView<?, ?> view);

    AView<?, ?> getModalViewShowing();

    ResourceMap getResourceMap(Class<?> clazz);

    /**
     * Synchronize binding from models to components for all views in the tree.
     */
    void updateAllViews(AView<?, ?> view);

    /**
     * Synchronize binding from models to components for all views in the tree.
     */
    void updateAllViews(Component component);

    /**
     * Synchronize binding from components to models for all views in the tree. Run validations and update components
     * again accordingly.
     */
    void submitAllViews(AView<?, ?> view);

    /**
     * Synchronize binding from components to models for all views in the tree. Run validations and update components
     * again accordingly.
     */
    void submitAllViews(Component component);

    /**
     * Gets the current modal window or main frame otherwise.
     */
    Window getWindow();

    boolean isControlDown();

    boolean isAltDown();

    boolean isShiftDown();

    boolean isMetaDown();

    boolean isAltGraphDown();

    boolean isModifierDown();

}
