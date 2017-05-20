package de.invesdwin.context.client.swing.api;

import java.beans.Beans;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import de.invesdwin.context.log.Log;
import de.invesdwin.norva.beanpath.annotation.Hidden;
import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.bean.AValueObject;

@SuppressWarnings("serial")
@ThreadSafe
public abstract class AModel extends AValueObject {

    public static final String IGNORE = "IGNORE";
    public static final String TEXT_RESOURCE_SUFFIX = ".text";

    protected final Log log = new Log(this);

    private final ResourceMap resourceMap;
    private final Object actionMapLock = new Object();
    @GuardedBy("actionMapLock")
    private ActionMap actionMap;

    static {
        final boolean prevDesignTime = Beans.isDesignTime();
        Beans.setDesignTime(true);
        //maybe initialize DesignTimeApplication to grant access to resourcemap and other stuff when application itself is not needed actually here
        Assertions.assertThat(Application.getInstance()).isNotNull();
        Beans.setDesignTime(prevDesignTime);
    }

    public AModel() {

        resourceMap = Application.getInstance().getContext().getResourceMap(this.getClass());
        resourceMap.injectFields(this);
    }

    @Hidden
    public ResourceMap getResourceMap() {
        return resourceMap;
    }

    /**
     * ActionMaps must be initialized lazy because else the ProxyActions don't get configured properly on startup!
     */
    @Hidden
    public ActionMap getActionMap() {
        synchronized (actionMapLock) {
            if (actionMap == null) {
                actionMap = Application.getInstance().getContext().getActionMap(this);
            }
            return actionMap;
        }
    }

}