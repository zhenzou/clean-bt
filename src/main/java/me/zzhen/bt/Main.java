
package me.zzhen.bt;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.zzhen.bt.decoder.*;
import me.zzhen.bt.log.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
public class Main extends Application {

    private static Logger logger = Logger.getLogger(Main.class.getName());

    private TorrentFile mTorrent;
    private BorderPane mRoot = new BorderPane();
    private MenuBar mMenu = new MenuBar();
    private HBox mTop = new HBox();
    private VBox mCenter = new VBox();
    private FileChooser chooser = new FileChooser();

    private Alert mErrorDialog = new Alert(Alert.AlertType.ERROR);
    private Alert mMessageDialog = new Alert(Alert.AlertType.INFORMATION);

    private Label mInitLabel = new Label("请选择Torrent文件");

    private TreeView<FileTreeItemModel> mFileTree = new TreeView<>();
    private TreeItem<FileTreeItemModel> mRootItem = new TreeItem<>();

    private Stage mPrimaryStage;


    @Override
    public void start(Stage primaryStage) throws Exception {
        mPrimaryStage = primaryStage;
        initView();
        initMenu();
        Scene scene = new Scene(mRoot, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        primaryStage.setResizable(true);
        primaryStage.setTitle(Config.APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initView() {
        mCenter.getChildren().add(mInitLabel);
        mCenter.setAlignment(Pos.CENTER);
        mCenter.getChildren().add(mFileTree);
        mFileTree.setVisible(false);
        mFileTree.setEditable(true);
        mFileTree.setCellFactory((TreeView<FileTreeItemModel> tree) -> new FileTreeItemCell());
        mRoot.setTop(mTop);
        mRoot.setCenter(mCenter);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Torrent", "*.torrent"));

        //将菜单栏添加到顶部
        mTop.getChildren().add(mMenu);
        mErrorDialog.setTitle("错误");
        mMessageDialog.setTitle("提示");
    }

    private void initMenu() {
        Menu fileMenu = new Menu(Config.MENU_FILE);
        MenuItem openFile = new MenuItem(Config.MENU_FILE_OPEN);
        MenuItem saveFile = new MenuItem(Config.MENU_FILE_SAVE);
        fileMenu.getItems().addAll(openFile, saveFile);
        saveFile.setDisable(true);

        mMenu.getMenus().add(fileMenu);
        openFile.setOnAction(event -> {
            chooser.setTitle("选择文件");
            File file = chooser.showOpenDialog(mPrimaryStage);
            if (file == null || !file.exists()) {
                logger.info("没有选择文件");
                return;
            }
            try {
                mTorrent = TorrentFile.fromFile(file);
            } catch (IOException | DecoderExecption e) {
                logger.error(e.getMessage());
                e.printStackTrace();
                mErrorDialog.setContentText(e.getMessage());
                mErrorDialog.showAndWait();
            }
            saveFile.setDisable(false);
            TreeNode<FileTreeItemModel> fileTree = createFileNodeTree();
            //将原来的节点清空
            mRootItem.getChildren().clear();
            getFileTree(mRootItem, fileTree);
            mInitLabel.setVisible(false);
            mFileTree.setVisible(true);
            //TODO 使用更自然的方式
            mRootItem = mRootItem.getChildren().get(0);
            mFileTree.setRoot(mRootItem);
            mFileTree.refresh();
        });

        //TODO 异步？
        saveFile.setOnAction((event) -> {
            try {
                chooser.setTitle("保存Torrent文件");
                File file = chooser.showSaveDialog(mPrimaryStage);
                if (file == null) {
                    logger.info("没有选择保存文件");
                } else {
                    ListNode infoRoot = new ListNode();
                    mRootItem.getChildren().forEach(item -> {
                        ObservableList<TreeItem<FileTreeItemModel>> children = item.getChildren();
                        children.forEach(tt -> System.out.println(tt.getValue().getName()));
                        ListNode path = new ListNode();
                        getFileTreeItem(infoRoot, item, path);
                    });
                    mTorrent.setInfoFiles(infoRoot);

                    OutputStream out = new FileOutputStream(file);
                    out.write(mTorrent.encode());
                    out.flush();
                    out.close();
                    mMessageDialog.setContentText("保存文件" + file.getName() + "成功");
                    mMessageDialog.showAndWait();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                mErrorDialog.setContentText(e.getMessage());
                mErrorDialog.showAndWait();
                e.printStackTrace();
            }
        });
    }

    /**
     * 将UI上的文件树结构恢复成BT文件中的扁平的文件树
     *
     * @param infoRoot BT 中的 infoFiles节点
     * @param item     当前树的根节点
     * @param path     BT文件的path节点
     */
    private void getFileTreeItem(ListNode infoRoot, TreeItem<FileTreeItemModel> item, ListNode path) {
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
            children.forEach(node -> getFileTreeItem(infoRoot, node, path));
            path.removeNode(path.size() - 1);//回退
        }
    }

    /**
     * 将文件树转换成FX中的TreeView的节点
     *
     * @param rootItem
     * @param fileTree
     */
    private void getFileTree(TreeItem<FileTreeItemModel> rootItem, TreeNode<FileTreeItemModel> fileTree) {
        List<TreeNode<FileTreeItemModel>> children = fileTree.getChildren();
        TreeItem<FileTreeItemModel> treeItem = new TreeItem<>(new FileTreeItemModel(fileTree.getValue().getName(), fileTree.getValue().getLength()));
        logger.debug(children.size());
        if (children.size() == 0) {
//            rootItem.getChildren().add(treeItem);
        } else {
            children.forEach(node -> getFileTree(treeItem, node));
        }
        rootItem.getChildren().add(treeItem);
    }

    /**
     * 将BT扁平的文件结构重新组装成树结构
     *
     * @return 文件树的根节点
     */
    private TreeNode<FileTreeItemModel> createFileNodeTree() {
        ListNode infoName = (ListNode) mTorrent.getInfoFiles();
        List<Node> value = infoName.getValue();
        TreeNode<FileTreeItemModel> treeRoot = new TreeNode<>(new FileTreeItemModel(mTorrent.getInfoName().decode(), 0));
        value.stream().map(item -> (DictionaryNode) item).collect(Collectors.toList()).forEach(item -> addFileToTree(treeRoot, item));
        return treeRoot;
    }

    /**
     * 递归的将节点添加到树中
     *
     * @param treeRoot 当前子树的根节点
     * @param file     当前的文件信息
     */
    private void addFileToTree(TreeNode<FileTreeItemModel> treeRoot, DictionaryNode file) {
        int index = 0;
        ListNode path = (ListNode) file.getNode("path");
        long length = Long.parseLong(file.getNode("length").decode());
        int size = path.size();
        while (index < size) {
            treeRoot = treeRoot.getOrAdd(new FileTreeItemModel(path.get(index).decode(), (index == size - 1) ? length : 0));
            index++;
        }
    }

    /**
     * 文件的UI显示
     */
    private final class FileTreeItemCell extends TreeCell<FileTreeItemModel> {

        private EditorViewHolder editorViewHolder = EditorViewHolder.EDITOR_VIEW_HOLDER;
        private boolean isEdited = false;

        //TODO 增加 随机文件夹以及全部文件随机命名
        public FileTreeItemCell() {
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (getTreeItem() == mRootItem) {
                return;
            }
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

        private String getString() {
            return getItem() == null ? "" : getItem().getName();
        }
    }

    /**
     * 文件信息的Model，主要保存文件名和文件大小
     */
    private final class FileTreeItemModel {

        private String originalName;
        private String name;
        private long leagth;// 0 文件夹

        public FileTreeItemModel(String name, long length) {
            this.name = name;
            leagth = length;
        }

        public FileTreeItemModel(String name, String length) {
            this.name = name;
            leagth = Long.parseLong(length);
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
            return leagth;
        }

        public void setLength(int length) {
            leagth = length;
        }

        @Override
        public int hashCode() {
            return name.hashCode() + Objects.hashCode(leagth);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FileTreeItemModel other = (FileTreeItemModel) obj;
            return name.equals(other.name) && leagth == other.leagth;
        }

        @Override
        public String toString() {
            return name + ":" + leagth;
        }

    }


    static class EditorViewHolder extends HBox {
        static final EditorViewHolder EDITOR_VIEW_HOLDER = new EditorViewHolder();

        Button okBtn;
        Button cancelBtn;
        Button random;
        TextField editor;

        FileTreeItemCell cell;

        public void setCell(FileTreeItemCell cell) {
            this.cell = cell;
            editor.setText(cell.getItem().getName());
        }

        EditorViewHolder() {
            okBtn = new Button("确定");
            cancelBtn = new Button("取消");
            random = new Button("随机");
            editor = new TextField();
            getChildren().addAll(editor, random, okBtn, cancelBtn);

            okBtn.setOnAction(event -> cell.commitEdit(cell.getItem()));

            cancelBtn.setOnAction(event -> {
                String text = cell.getItem().getOriginalName();
                if (text != null) {
                    cell.getItem().setName(text);
                }
                editor.setText(text);
                cell.commitEdit(cell.getItem());
            });

            random.setOnAction(event -> {
                String extName = Utils.getExtName(cell.getItem().getName());
                editor.setText(Utils.randomDigitalName() + "." + extName);
            });
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}