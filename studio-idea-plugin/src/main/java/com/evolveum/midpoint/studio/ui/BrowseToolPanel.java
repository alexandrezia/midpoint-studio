package com.evolveum.midpoint.studio.ui;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.studio.action.browse.ComboObjectTypes;
import com.evolveum.midpoint.studio.action.browse.ComboQueryType;
import com.evolveum.midpoint.studio.action.browse.DownloadOptions;
import com.evolveum.midpoint.studio.impl.RestObjectManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.OnePixelSplitter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import static com.evolveum.midpoint.studio.util.MidPointUtils.createAnAction;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BrowseToolPanel extends SimpleToolWindowPanel {

    public enum State {

        DONE, SEARCHING, DOWNLOADING, CANCELING
    }

    private State state = State.DONE;

    private QueryPanel queryPanel;
    private QueryResultsPanel queryResultsPanel;

    private ComboObjectTypes objectType;
    private ComboQueryType queryType;

    private AnAction searchAction;
    private AnAction cancelAction;
    private AnAction pagingAction;

    private AnAction downloadAction;
    private AnAction showAction;
    private AnAction processAction;

    private AnAction pagingText;
    private AnAction previous;
    private AnAction next;

    private boolean rawSearch = true;
    private boolean rawDownload = true;

    private Paging paging = new Paging();


    public BrowseToolPanel() {
        super(false, true);

        initLayout();
    }

    private void initLayout() {
        JComponent queryPanel = initQueryPanel();
        JComponent resultsPanel = initResultsPanel();

        OnePixelSplitter split = new OnePixelSplitter(false);
        split.setProportion(0.3f);
        split.setFirstComponent(queryPanel);
        split.setSecondComponent(resultsPanel);

        setContent(split);
    }

    private JComponent initQueryPanel() {
        JPanel root = new JPanel(new BorderLayout());

        DefaultActionGroup group = createQueryActionGroup();

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("BrowseQueryActions",
                group, true);
        root.add(toolbar.getComponent(), BorderLayout.NORTH);

        queryPanel = new QueryPanel(queryType);
        root.add(queryPanel, BorderLayout.CENTER);

        return root;
    }

    private JComponent initResultsPanel() {
        JPanel results = new JPanel(new BorderLayout());

        DefaultActionGroup resultsActions = createResultsActionGroup();

        ActionToolbar resultsActionsToolbar = ActionManager.getInstance().createActionToolbar("BrowseResultsActions",
                resultsActions, true);
        results.add(resultsActionsToolbar.getComponent(), BorderLayout.NORTH);

        queryResultsPanel = new QueryResultsPanel();
        results.add(queryResultsPanel, BorderLayout.CENTER);

        DefaultActionGroup pagingActions = createPagingActionGroup();

        ActionToolbar pagingActionsToolbar = ActionManager.getInstance().createActionToolbar("BrowseResultsPagingActions",
                pagingActions, true);
        results.add(pagingActionsToolbar.getComponent(), BorderLayout.SOUTH);

        return results;
    }

    private DefaultActionGroup createPagingActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();

        // todo create external actions, they should be able to show progress
        previous = createAnAction("Previous", AllIcons.General.ArrowLeft, null, null);
        group.add(previous);

        next = createAnAction("Next", AllIcons.General.ArrowRight, null, null);
        group.add(next);

        group.addSeparator();

        pagingText = new TextAction() {

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setText("From 1 to 50 of 1234");
            }

            @NotNull
            @Override
            public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
                JComponent comp = super.createCustomComponent(presentation, place);
                comp.setBorder(new CompoundBorder(comp.getBorder(), new EmptyBorder(0, 5, 0, 5)));

                return comp;
            }
        };
        group.add(pagingText);

        return group;
    }

    private DefaultActionGroup createQueryActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        objectType = new ComboObjectTypes();
        group.add(objectType);

        queryType = new ComboQueryType();
        group.add(queryType);

        CheckboxAction rawSearch = new CheckboxAction("Raw") {

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(isSearchEnabled());
                super.update(e);
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return BrowseToolPanel.this.rawSearch;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                BrowseToolPanel.this.rawSearch = state;
            }
        };
        group.add(rawSearch);

        pagingAction = createAnAction("Paging", "Paging Settings", AllIcons.General.GearPlain,
                e -> pagingSettingsPerformed(e),
                e -> e.getPresentation().setEnabled(isSearchEnabled()));
        group.add(pagingAction);

        searchAction = createAnAction("Search", AllIcons.Actions.Find,
                e -> searchPerformed(e),
                e -> e.getPresentation().setEnabled(isSearchEnabled()));
        group.add(searchAction);

        cancelAction = createAnAction("Cancel", AllIcons.Actions.Cancel,
                e -> cancelPerformed(e),
                e -> e.getPresentation().setEnabled(isCancelEnabled()));
        group.add(cancelAction);

        return group;
    }

    private DefaultActionGroup createResultsActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();

        // todo create external xactions, they should be able to show progress
        downloadAction = createAnAction("Download", AllIcons.Actions.Download,
                e -> downloadPerformed(e, false),
                e -> e.getPresentation().setEnabled(isDownloadShowEnabled()));
        group.add(downloadAction);

        showAction = createAnAction("Show", AllIcons.Actions.Show,
                e -> downloadPerformed(e, true),
                e -> e.getPresentation().setEnabled(isDownloadShowEnabled()));
        group.add(showAction);

        CheckboxAction rawSearch = new CheckboxAction("Raw") {

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(isDownloadShowEnabled());
                super.update(e);
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return BrowseToolPanel.this.rawDownload;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                BrowseToolPanel.this.rawDownload = state;
            }
        };
        group.add(rawSearch);

        group.add(new Separator());

        processAction = createAnAction("Process", AllIcons.Actions.RealIntentionBulb,
                e -> processPerformed(e),
                e -> e.getPresentation().setEnabled(isDownloadShowEnabled()));
        group.add(processAction);

        return group;
    }

    private void processPerformed(AnActionEvent evt) {
        ProcessResultsDialog dialog = new ProcessResultsDialog();
        if (!dialog.showAndGet()) {
            return;
        }


        // todo implement
    }

    private void searchPerformed(AnActionEvent evt) {
        state = State.SEARCHING;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            ObjectTypes type = objectType.getSelected();
            ObjectQuery query = queryPanel.buildQuery(evt.getProject());

            RestObjectManager rest = RestObjectManager.getInstance(evt.getProject());

            SearchResultList result = null;
            try {
                result = rest.search(type.getClassDefinition(), query, rawSearch);
            } catch (Exception ex) {
                ex.printStackTrace(); // todo implement
//                printErrorMessage("Couldn't list objects, reason: " + ex.getMessage());
            }

            updateTableModel(result);

            state = State.DONE;
        });
    }

    private void downloadPerformed(AnActionEvent evt, boolean showOnly) {
        ApplicationManager.getApplication().runWriteAction(() -> {

            setState(BrowseToolPanel.State.DOWNLOADING);

            JTable table = queryResultsPanel.getTable();

            RestObjectManager rest = RestObjectManager.getInstance(evt.getProject());

            ObjectQuery objectQuery;
            if (table.getRowCount() == table.getSelectedRowCount() || table.getSelectedRowCount() == 0) {
                // return all
                objectQuery = queryPanel.buildQuery(evt.getProject());
            } else {
                // return only selected objects
                List<String> oids = queryResultsPanel.getSelectedRowsOids();

                PrismContext ctx = rest.getPrismContext();
                QueryFactory qf = ctx.queryFactory();

                InOidFilter inOidFilter = qf.createInOid(oids);

                ItemPath path = ctx.path(ObjectType.F_NAME);
                ObjectPaging paging = qf.createPaging(queryPanel.getOffset(), queryPanel.getMaxSize(),
                        path, OrderDirection.ASCENDING);

                objectQuery = qf.createQuery(inOidFilter, paging);
            }

            ObjectTypes objectTypes = objectType.getSelected();
            VirtualFile[] files = rest.download(objectTypes.getClassDefinition(), objectQuery,
                    new DownloadOptions().showOnly(showOnly).raw(rawDownload));

            if (files != null && files.length == 1) {
                FileEditorManager fem = FileEditorManager.getInstance(evt.getProject());
                fem.openFile(files[0], true, true);
            }
            // todo if files is null show error/warning
        });
    }

    private void cancelPerformed(AnActionEvent evt) {
        state = State.CANCELING;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            // todo implement

            state = State.DONE;
        });
    }

    private void updateTableModel(SearchResultList result) {
        BrowseTableModel tableModel = queryResultsPanel.getTableModel();
        List<ObjectType> data = tableModel.getData();
        data.clear();

        if (result == null || result.isEmpty()) {
            return;
        }

        data.addAll(result);

        ApplicationManager.getApplication().invokeLater(() -> tableModel.fireTableDataChanged());

//        printSuccessMessage("");    // todo add paging/count info to message
    }

    private void pagingSettingsPerformed(AnActionEvent evt) {
        PagingDialog dialog = new PagingDialog(paging);
        if (!dialog.showAndGet()) {
            return;
        }

        this.paging = dialog.getPaging();
    }

    private boolean isDownloadShowEnabled() {
        // todo remove "true" part
        return true || state == State.DONE && !queryResultsPanel.getTable().getSelectionModel().isSelectionEmpty();
    }

    private boolean isSearchEnabled() {
        return state == State.DONE;
    }

    private boolean isCancelEnabled() {
        return state == State.SEARCHING;
    }

    public QueryResultsPanel getQueryResultsPanel() {
        return queryResultsPanel;
    }

    public ObjectTypes getObjectType() {
        return objectType.getSelected();
    }

    public ObjectQuery getObjectQuery(Project project) {
        return queryPanel.buildQuery(project);
    }

    public void setState(State state) {
        this.state = state;
    }
}
