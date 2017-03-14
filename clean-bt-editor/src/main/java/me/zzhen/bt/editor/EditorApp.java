
package me.zzhen.bt.editor;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.zzhen.bt.bencode.*;
import me.zzhen.bt.common.TorrentFile;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/17.
 *         Version :
 *         Description:
 */
public class EditorApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(EditorApp.class.getName());

    private final BorderPane mainArea = new BorderPane();
    private final MenuBar menuBar = new MenuBar();
    private final HBox topArea = new HBox();
    private final VBox centerArea = new VBox();
    private final FileChooser chooser = new FileChooser();
    private final Button centerBtn = new Button("点击选择Torrent文件");
    private final Alert errorDialog = new Alert(Alert.AlertType.ERROR);
    private final Alert infoDialog = new Alert(Alert.AlertType.INFORMATION);
    private final TreeView<FileTreeItemModel> fileTree = new TreeView<>();
    private TreeItem<FileTreeItemModel> itemRoot = new TreeItem<>();
    private Stage primaryStage;

    private ObjectProperty<TorrentFile> torrentProperty = new SimpleObjectProperty<>(null);

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        initView();
        initMenu();
        Scene scene = new Scene(mainArea, EditorConfig.WINDOW_WIDTH, EditorConfig.WINDOW_HEIGHT);
        primaryStage.setResizable(false);
        primaryStage.setTitle(EditorConfig.APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initView() {
        centerArea.getChildren().add(centerBtn);
        centerArea.setAlignment(Pos.CENTER);
        fileTree.setEditable(true);
        fileTree.setCellFactory((TreeView<FileTreeItemModel> tree) -> new FileTreeItemCell());
        mainArea.setTop(topArea);
        mainArea.setCenter(centerArea);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Torrent", "*.torrent"));

        //将菜单栏添加到顶部
        topArea.getChildren().add(menuBar);
        errorDialog.setTitle("错误");
        infoDialog.setTitle("提示");

        centerBtn.setOnAction(event -> openFile());
    }

    private void initMenu() {
        Menu fileMenu = new Menu(EditorConfig.MENU_FILE);
        MenuItem openFile = new MenuItem(EditorConfig.MENU_FILE_OPEN);
        FontAwesomeIconView openView = new FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN);
        openFile.setGraphic(openView);
        MenuItem saveFile = new MenuItem(EditorConfig.MENU_FILE_SAVE);
        FontAwesomeIconView saveView = new FontAwesomeIconView(FontAwesomeIcon.SAVE);
        saveFile.setGraphic(saveView);
        fileMenu.getItems().addAll(openFile, saveFile);

        Menu toolMenu = new Menu(EditorConfig.MENU_TOOL);
        MenuItem randomName = new MenuItem(EditorConfig.MENU_TOOL_RANDOM);

        BooleanBinding when = new When(Bindings.createBooleanBinding(() -> torrentProperty.getValue() == null, torrentProperty)).then(true).otherwise(false);

        randomName.disableProperty().bind(when);
        saveFile.disableProperty().bind(when);
        centerBtn.visibleProperty().bind(when);

        fileTree.visibleProperty().bind(when.not());

        toolMenu.getItems().add(randomName);

        menuBar.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));

        menuBar.getMenus().addAll(fileMenu, toolMenu);
        openFile.setOnAction(event -> openFile());
        saveFile.setOnAction((event) -> saveFile());
        randomName.setOnAction(event -> randomNameAll(itemRoot));

    }

    private void saveFile() {
        chooser.setTitle("保存Torrent文件");
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            ListNode infoRoot = new ListNode();
            itemRoot.getChildren().forEach(item -> {
                ListNode path = new ListNode();
                transformTreeItemNode(infoRoot, item, path);
            });
            torrentProperty.getValue().setInfoFiles(infoRoot);
            try {
                torrentProperty.getValue().save(file);
                infoDialog.setContentText("保存文件" + file.getName() + "成功");
                infoDialog.showAndWait();
            } catch (IOException e) {
                errorDialog.setContentText("保存文件" + file.getName() + "失败\n" + e.getMessage());
                errorDialog.showAndWait();
            }
        }
    }

    private boolean isFirst = true;

    private void openFile() {
        chooser.setTitle("选择文件");
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null || !file.exists()) {
            return;
        }
        try {
            torrentProperty.setValue(TorrentFile.fromFile(file));
        } catch (IOException | DecoderException e) {
            errorDialog.setContentText(e.getMessage());
            errorDialog.showAndWait();
        }
        TreeNode<FileTreeItemModel> nodeRoot = buildNodeTree();
        itemRoot = buildItemTree(nodeRoot);
        fileTree.setRoot(itemRoot);
        if (isFirst) {
            centerArea.getChildren().add(fileTree);
            isFirst = false;
        }
    }

    /**
     * 重命名所有文件
     *
     * @param item
     */
    private void randomNameAll(TreeItem<FileTreeItemModel> item) {
        item.getValue().setName(randomName(item.getValue().getName()));
        ObservableList<TreeItem<FileTreeItemModel>> children = item.getChildren();
        for (TreeItem<FileTreeItemModel> child : children) {
            randomNameAll(child);
        }
        fileTree.refresh();
    }


    /**
     * 将UI上的文件树结构恢复成BT文件中的扁平的文件树
     *
     * @param infoRoot BT 中的 infoFiles节点
     * @param item     当前树的根节点
     * @param path     BT文件的path节点
     */
    private void transformTreeItemNode(ListNode infoRoot, TreeItem<FileTreeItemModel> item, ListNode path) {
        ObservableList<TreeItem<FileTreeItemModel>> children = item.getChildren();
        path.addNode(new StringNode(item.getValue().getName().getBytes()));
        if (children.size() == 0) {
            int size = path.size();
            DictionaryNode dic = new DictionaryNode();
            StringNode[] nodes = new StringNode[size];
            System.arraycopy(path.getValue().toArray(), 0, nodes, 0, size);
            dic.addNode("path", new ListNode(Arrays.asList(nodes)));
            dic.addNode("length", new IntNode(item.getValue().getLength()));
            infoRoot.addNode(dic);
            path.removeNode(size - 1);//回退
        } else {
            children.forEach(node -> transformTreeItemNode(infoRoot, node, path));
            path.removeNode(path.size() - 1);//回退
        }
    }

    /**
     * 将文件树转换成FX中的TreeView的节点
     *
     * @param fileTree
     */
    private TreeItem<FileTreeItemModel> buildItemTree(TreeNode<FileTreeItemModel> fileTree) {
        TreeItem<FileTreeItemModel> root = new TreeItem<>(fileTree.getValue());
        List<TreeItem<FileTreeItemModel>> collect = fileTree.getChildren().stream().map(this::buildItemTree).collect(Collectors.toList());
        if (fileTree.isLeaf()) {
            //TODO 支持更多文件类型图标显示
            //TODO 研究，是否可以共享图标
            root.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILE_VIDEO_ALT));
        } else {
            root.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FOLDER));
        }
        root.getChildren().addAll(collect);
        return root;
    }

    /**
     * 将BT扁平的文件结构重新组装成树结构
     *
     * @return 文件树的根节点
     */
    private TreeNode<FileTreeItemModel> buildNodeTree() {
        ListNode infoName = (ListNode) torrentProperty.getValue().getInfoFiles();
        List<Node> value = infoName.getValue();
        TreeNode<FileTreeItemModel> treeRoot = new TreeNode<>(new FileTreeItemModel(torrentProperty.getValue().getInfoName().toString(), 0));
        value.stream().map(item -> (DictionaryNode) item).collect(Collectors.toList()).forEach(item -> addNodeToTree(treeRoot, item));
        return treeRoot;
    }

    /**
     * 递归的将节点添加到树中
     *
     * @param treeRoot 当前子树的根节点
     * @param file     当前的文件信息
     */
    private void addNodeToTree(TreeNode<FileTreeItemModel> treeRoot, DictionaryNode file) {
        int index = 0;
        ListNode path = (ListNode) file.getNode("path");
        long length = Long.parseLong(file.getNode("length").toString());
        int size = path.size();
        while (index < size) {
            treeRoot = treeRoot.getOrAdd(new FileTreeItemModel(path.get(index).toString(), (index == size - 1) ? length : 0));
            index++;
        }
    }

    /**
     * 文件的UI显示
     */
    private final class FileTreeItemCell extends TreeCell<FileTreeItemModel> {

        private final EditorViewHolder editorViewHolder = EditorViewHolder.EDITOR_VIEW_HOLDER;
        private boolean isEdited = false;

        public FileTreeItemCell() {
        }

        @Override
        public void startEdit() {
            super.startEdit();
//            if (getTreeItem() == itemRoot) {
//                return;
//            }
            editorViewHolder.setCell(this);
            setGraphic(editorViewHolder);
            isEdited = true;
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem().getOriginalName());
            setGraphic(getTreeItem().getGraphic());
            isEdited = false;
        }

        @Override
        protected void updateItem(FileTreeItemModel item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText("");
                setGraphic(null);
            } else {
                if (isEditing()) {
                    editorViewHolder.editor.setText(item.getName());
                    setText(null);
                } else {
                    if (isEdited) {
                        setText(editorViewHolder.editor.getText());
                        item.setName(editorViewHolder.editor.getText());
                    } else {
                        setText(item.getName());
                    }
                }
                setGraphic(getTreeItem().getGraphic());
            }
        }

        @Override
        public void commitEdit(FileTreeItemModel newValue) {
            super.commitEdit(newValue);
            isEdited = false;
        }
    }

    /**
     * 文件信息的Model，主要保存文件名和文件大小
     */
    private final class FileTreeItemModel {

        private String originalName;
        private String name;
        private long length;// 0 文件夹

        public FileTreeItemModel(String name, long length) {
            this.name = name;
            this.length = length;
            originalName = name;
        }

        public FileTreeItemModel(String name, String length) {
            this.name = name;
            this.length = Long.parseLong(length);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            if (originalName == null) {
                originalName = this.name;
            }
            this.name = name;
        }

        public String getOriginalName() {
            return originalName;
        }

        public long getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        @Override
        public int hashCode() {
            return name.hashCode() + Objects.hashCode(length);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FileTreeItemModel other = (FileTreeItemModel) obj;
            return name.equals(other.name) && length == other.length;
        }

        @Override
        public String toString() {
            return name + ":" + length;
        }

    }


    /**
     * 节点编辑的UI，共享
     */
    static class EditorViewHolder extends HBox {
        final static EditorViewHolder EDITOR_VIEW_HOLDER = new EditorViewHolder();
        final Button okBtn;
        final Button cancelBtn;
        final Button randomBtn;
        final TextField editor;

        private FileTreeItemCell cell;

        void setCell(FileTreeItemCell cell) {
            this.cell = cell;
            editor.setText(cell.getItem().getName());
        }

        EditorViewHolder() {
            okBtn = new Button("确定");
            cancelBtn = new Button("取消");
            randomBtn = new Button("随机");
            editor = new TextField();
            getChildren().addAll(editor, randomBtn, okBtn, cancelBtn);

            okBtn.setOnAction(event -> cell.commitEdit(cell.getItem()));

            cancelBtn.setOnAction(event -> {
                String text = cell.getItem().getOriginalName();
                if (text != null) {
                    cell.getItem().setName(text);
                }
                editor.setText(text);
                cell.commitEdit(cell.getItem());
            });

            randomBtn.setOnAction(event -> {
                editor.setText(randomName(cell.getItem().getName()));
            });
        }
    }

    static String randomName(String origin) {
        String extName = Utils.getExtName(origin);
        return Utils.uuid() + "." + extName;
    }


    public static void main(String[] args) {
        launch(args);
    }
}