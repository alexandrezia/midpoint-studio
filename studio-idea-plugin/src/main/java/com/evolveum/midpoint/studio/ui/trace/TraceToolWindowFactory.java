package com.evolveum.midpoint.studio.ui.trace;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TraceToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();

        Content variablesContent = buildTraceTree(project);
        contentManager.addContent(variablesContent);
        contentManager.setSelectedContent(variablesContent);

        Content tracePerformanceInformation = buildTracePerformance(project);
        contentManager.addContent(tracePerformanceInformation);

        Content traceEntryDetails = buildTraceEntryDetails(project);
        contentManager.addContent(traceEntryDetails);

        Content traceEntryDetailsRaw = buildTraceEntryDetailsRaw(project);
        contentManager.addContent(traceEntryDetailsRaw);

        Content operationResultRaw = buildOperationResultRaw(project);
        contentManager.addContent(operationResultRaw);
    }

    private Content buildTraceEntryDetails(Project project) {
        OpNodeDumpPanel panel = new OpNodeDumpPanel(project);
        return ContentFactory.SERVICE.getInstance().createContent(panel, "Operation Details", false);
    }

    private Content buildTraceEntryDetailsRaw(Project project) {
        OpNodeTraceRawPanel panel = new OpNodeTraceRawPanel(project);
        return ContentFactory.SERVICE.getInstance().createContent(panel, "Trace Entries Raw", false);
    }

    private Content buildOperationResultRaw(Project project) {
        OpNodeOperationResultRawPanel panel = new OpNodeOperationResultRawPanel(project);
        return ContentFactory.SERVICE.getInstance().createContent(panel, "Operation Raw", false);
    }

    private Content buildTraceTree(Project project) {
        OpNodeDetailsPanel variables = new OpNodeDetailsPanel(project);
        return ContentFactory.SERVICE.getInstance().createContent(variables, "Tree View", false);
    }

    private Content buildTracePerformance(Project project) {
        OpNodePerformancePanel perfInformation = new OpNodePerformancePanel(project.getMessageBus());
        //return new HeaderDecorator("Trace Performance Information", new JBScrollPane(perfInformation));
        return ContentFactory.SERVICE.getInstance().createContent(perfInformation, "Performance Information", false);
    }

    @Override
    public void init(ToolWindow window) {
        window.setStripeTitle("Trace");
        window.setTitle("Trace");
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return TraceUtils.shouldBeVisible(project);
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return false;
    }
}