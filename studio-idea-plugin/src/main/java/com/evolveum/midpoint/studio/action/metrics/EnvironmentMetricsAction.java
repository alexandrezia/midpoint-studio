package com.evolveum.midpoint.studio.action.metrics;

import com.evolveum.midpoint.studio.impl.Environment;
import com.evolveum.midpoint.studio.impl.EnvironmentService;
import com.evolveum.midpoint.studio.impl.metrics.MetricsService;
import com.evolveum.midpoint.studio.impl.metrics.MetricsSession;
import com.evolveum.midpoint.studio.ui.metrics.MetricsEditor;
import com.evolveum.midpoint.studio.ui.metrics.MetricsEditorState;
import com.evolveum.midpoint.studio.util.RunnableUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class EnvironmentMetricsAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        EnvironmentService em = EnvironmentService.getInstance(project);
        Environment selected = em.getSelected();

        e.getPresentation().setEnabled(selected != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        EnvironmentService em = EnvironmentService.getInstance(e.getProject());
        Environment env = em.getSelected();

        MetricsService service = project.getService(MetricsService.class);
        MetricsSession session = service.createSession(env);

        MetricsEditorState state = new MetricsEditorState();
        state.setId(session.getId());

        RunnableUtils.runWriteActionAndWait(() -> {

            FileEditorManager fem = FileEditorManager.getInstance(project);
            FileEditor[] editors = fem.openFile(session.getFile(), true, true);
            if (editors[0] instanceof MetricsEditor) {
                MetricsEditor editor = (MetricsEditor) editors[0];
                editor.setState(state);
            }
        });

        session.start();
    }
}
