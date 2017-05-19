package de.invesdwin.common.client.wicket.cas.page;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.context.security.web.cas.CasProperties;
import de.invesdwin.nowicket.application.AWebPage;

@NotThreadSafe
public class CasLoginPage extends AWebPage {

    public static final String MOUNT_PATH = CasProperties.MOUNT_PATH_CAS_LOGIN;

    public CasLoginPage() {
        super(null);
    }

}
