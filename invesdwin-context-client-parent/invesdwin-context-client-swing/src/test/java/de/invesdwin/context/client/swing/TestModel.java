package de.invesdwin.context.client.swing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.springframework.beans.factory.annotation.Configurable;

import de.invesdwin.aspects.EventDispatchThreadUtil;
import de.invesdwin.context.client.swing.api.AModel;
import de.invesdwin.context.client.swing.api.ATask;
import de.invesdwin.context.log.error.Err;
import de.invesdwin.util.lang.Objects;

@SuppressWarnings("serial")
@NotThreadSafe
@Configurable
public class TestModel extends AModel {

    @Inject
    private StatusBar statusBar;

    @Resource
    private String descriptionEnhancement;

    private String name = "init1";
    private String description = "init2";
    private InnerTestModel inner;
    private boolean checkboxTest = true;

    private final List<BeanRow> beanList = new ArrayList<BeanRow>();
    private final List<BeanRow> beanTable = new ArrayList<BeanRow>();

    public TestModel() {
        inner = new InnerTestModel();
        description += descriptionEnhancement;
        for (int i = 0; i < 10; i++) {
            beanList.add(new BeanRow(i));
            beanTable.add(new BeanRow(i + 10));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public InnerTestModel getInner() {
        return inner;
    }

    public void setInner(final InnerTestModel inner) {
        this.inner = inner;
    }

    public List<BeanRow> getBeanList() {
        return beanList;
    }

    public List<BeanRow> getBeanTable() {
        return beanTable;
    }

    public void setCheckboxTest(final boolean checkboxTest) {
        this.checkboxTest = checkboxTest;
    }

    public boolean isCheckboxTest() {
        return checkboxTest;
    }

    @Action
    public void next() {
        statusBar.message("Next has been clicked!");

        final ATask<Object, Object> popupTask = new ATask<Object, Object>(Application.getInstance()) {

            {
                setTitle("Popup Task");
                setDescription("Opens a Dialog and ends as soon as it gets closed.");
            }

            @Override
            protected Object doInBackground() throws Exception {
                setMessage("Dialog opened, waiting for close.");
                EventDispatchThreadUtil.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        Dialogs.showMessageDialog(null, "With this it would continue:\nName: " + getName()
                                + "\nDescription: " + getDescription());
                    }
                });
                throw new IllegalStateException("testError");
            }

            @Override
            protected void finished() {
                statusBar.error("<html>Dialog has been closed!<br>And another line!");
                new Thread() {
                    @Override
                    public void run() {
                        for (int i = 1; i <= 3; i++) {
                            Application.getInstance().getContext().getTaskService().execute(
                                    new ProgressTask(Application.getInstance(), i));
                            try {
                                TimeUnit.SECONDS.sleep(i);
                            } catch (final InterruptedException e) {
                                throw Err.process(e);
                            }
                        }
                    };
                }.start();
            }

        };
        Application.getInstance().getContext().getTaskService().execute(popupTask);
    }

    private class ProgressTask extends ATask<Object, Object> {

        ProgressTask(final Application application, final int id) {
            super(application);
            setTitle("Progress Task " + id);
            setDescription("A sample for a task with progress.");
            setProgress(0);
        }

        @Override
        protected Object doInBackground() throws Exception {
            for (int i = 0; i <= 100; i += 10) {
                if (i <= 100) {
                    setProgress(i);
                    setMessage("Progress at " + i + "%");
                }
                TimeUnit.SECONDS.sleep(1);
            }
            return null;
        }

        @Override
        protected void finished() {
            statusBar.error("<html>Task has been stopped!<br>And another line!");
        }
    }

    public static class InnerTestModel extends AModel {

        private String notice = "notice";

        public String getNotice() {
            return notice;
        }

        public void setNotice(final String notice) {
            this.notice = notice;
        }

        @Action
        public void doNothing() {
            Dialogs.showMessageDialog(null, "Doing nothing!");
        }

    }

    public static class BeanRow extends AModel {
        private final Integer columnInteger;
        private final String columnString;
        private Boolean columnBoolean;

        public BeanRow(final int number) {
            this.columnInteger = number;
            this.columnString = "asd " + number;
        }

        public Integer getColumnInteger() {
            return columnInteger;
        }

        public String getColumnString() {
            return columnString;
        }

        public Boolean getColumnBoolean() {
            return columnBoolean;
        }

        public void setColumnBoolean(final Boolean columnBoolean) {
            this.columnBoolean = columnBoolean;
        }

        @Override
        public String toString() {
            return "asdf: " + Objects.toStringHelper(this)
                    .add("columnInteger", columnInteger)
                    .add("columnString", columnString)
                    .add("columnBoolean", columnBoolean)
                    .toString();
        }

    }

}