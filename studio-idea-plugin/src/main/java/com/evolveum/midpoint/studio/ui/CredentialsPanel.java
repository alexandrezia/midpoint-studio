package com.evolveum.midpoint.studio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AddEditRemovePanel;
import com.evolveum.midpoint.studio.impl.Credentials;
import com.evolveum.midpoint.studio.impl.CredentialsManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CredentialsPanel extends AddEditRemovePanel<Credentials> {

    private Project project;

    public CredentialsPanel(@NotNull Project project) {
        super(new CredentialsModel(), new ArrayList<>(), null);

        this.project = project;

        initData();

        getTable().setShowColumns(true);
    }

    private void initData() {
        CredentialsManager manager = CredentialsManager.getInstance(project);
        getData().clear();
        getData().addAll(manager.list());
    }

    @Nullable
    @Override
    protected Credentials addItem() {
        return doAddOrEdit(null);
    }

    @Override
    protected boolean removeItem(Credentials o) {
        CredentialsManager manager = CredentialsManager.getInstance(project);
        return manager.delete(o.getKey());
    }

    @Nullable
    @Override
    protected Credentials editItem(Credentials o) {
        return doAddOrEdit(o);
    }

    @Nullable
    private Credentials doAddOrEdit(Credentials credentials) {
        CredentialsEditorDialog dialog = new CredentialsEditorDialog(credentials);
        if (!dialog.showAndGet()) {
            return null;
        }

        Credentials updated = dialog.getCredentials();

        CredentialsManager manager = CredentialsManager.getInstance(project);
        if (credentials == null) {
            // adding
            manager.add(updated);
        } else {
            // editing
            manager.delete(credentials.getKey());
        }

        return updated;
    }

    public AnAction[] createConsoleActions() {
        return new AnAction[]{
                new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {

                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        CredentialsManager manager = CredentialsManager.getInstance(project);
                        manager.reinit();

                        initData();
                    }
                }
        };
    }

    private static class CredentialsModel extends TableModel<Credentials> {

        private static final String[] COLUMN_NAMES = {"Key", "Username", "Password", "Description"};

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Nullable
        @Override
        public String getColumnName(int i) {
            return COLUMN_NAMES[i];
        }

        @Override
        public Object getField(Credentials credentials, int i) {
            switch (i) {
                case 0:
                    return credentials.getKey();
                case 1:
                    return credentials.getUsername();
                case 2:
                    if (credentials.getPassword() == null) {
                        return null;
                    }
                    return StringUtils.repeat("*", credentials.getPassword().length());
                case 3:
                    return credentials.getDescription();
                default:
                    return null;
            }
        }
    }
}
