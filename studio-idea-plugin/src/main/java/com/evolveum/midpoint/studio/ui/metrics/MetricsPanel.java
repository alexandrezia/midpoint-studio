package com.evolveum.midpoint.studio.ui.metrics;

import com.evolveum.midpoint.studio.impl.metrics.MetricsService;
import com.evolveum.midpoint.studio.impl.metrics.MetricsSession;
import com.evolveum.midpoint.studio.impl.metrics.Node;
import com.evolveum.midpoint.studio.ui.HeaderDecorator;
import com.evolveum.midpoint.studio.util.MidPointUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MetricsPanel extends BorderLayoutPanel {

    private JLabel initializing;

    private JBSplitter splitter;

    public MetricsPanel() {
        initLayout();
    }

    private void initLayout() {
        initializing = new JLabel("Initializing");
        initializing.setAlignmentX(Component.CENTER_ALIGNMENT);
        initializing.setAlignmentY(Component.CENTER_ALIGNMENT);
        add(initializing, BorderLayout.CENTER);
    }

    public void init(MetricsService metricsService, UUID sessionId) {
        MetricsSession session = metricsService.getSession(sessionId);
        if (session == null) {
            return;
        }

        remove(initializing);

        JComponent toolbar = initMainToolbar();
        add(toolbar, BorderLayout.NORTH);

        OnePixelSplitter vertical = new OnePixelSplitter();
        vertical.setProportion(0.2f);
        add(vertical, BorderLayout.CENTER);

        OnePixelSplitter horizontal = new OnePixelSplitter(true);

        vertical.setFirstComponent(horizontal);
        vertical.setSecondComponent(initChartsPanel());

        horizontal.setFirstComponent(initNodesPanel(session.listNodes()));
        horizontal.setSecondComponent(initOptionsPanel());
    }

    private JPanel initNodesPanel(List<Node> nodes) {
        NodesPanel panel = new NodesPanel(nodes);
        return new HeaderDecorator("Nodes", new JBScrollPane(panel));
    }

    private JPanel initOptionsPanel() {
        OptionsPanel panel = new OptionsPanel();
        return new HeaderDecorator("Metrics", new JScrollPane(panel));
    }

    private JPanel initChartsPanel() {
        ChartsPanel panel = new ChartsPanel();
        return new HeaderDecorator(" ", panel);
    }

    private JComponent initMainToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        AnAction start = MidPointUtils.createAnAction("Start", AllIcons.Actions.Run_anything, e -> startClicked(e));
        group.add(start);
//
//        AnAction collapseAll = MidPointUtils.createAnAction("Collapse All", AllIcons.Actions.Collapseall, e -> main.collapseAll());
//        group.add(collapseAll);

        ActionToolbar resultsActionsToolbar = ActionManager.getInstance().createActionToolbar("MetricsMainToolbar", group, true);
        return resultsActionsToolbar.getComponent();
    }

    public void startClicked(AnActionEvent evt) {

    }
}
