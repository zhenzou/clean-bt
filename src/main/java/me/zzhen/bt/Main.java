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

import java.io.*;
import java.util.List;
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
        primaryStage.setResizable(false);
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
    }

    private void initMenu() {
        Menu fileMenu = new Menu(Config.MENU_FILE);
        MenuItem openFile = new MenuItem(Config.MENU_FILE_OPEN);
        MenuItem saveFile = new MenuItem(Config.MENU_FILE_SAVE);
        fileMenu.getItems().add(openFile);
        fileMenu.getItems().add(saveFile);
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
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
            saveFile.setDisable(false);
            TreeNode<FileTreeItemModel> fileTree = createFileNodeTree();
            mRootItem.setValue(new FileTreeItemModel(fileTree.getValue().getName(), fileTree.getValue().getLength()));
            getFileTree(mRootItem, fileTree);
            mInitLabel.setVisible(false);
            mFileTree.setVisible(true);
            //TODO 使用更自然的方式
            mRootItem = mRootItem.getChildren().get(0);
            mFileTree.setRoot(mRootItem);
        });

        saveFile.setOnAction((event) -> {
            try {
                chooser.setTitle("保存Torrent文件");
                File file = chooser.showSaveDialog(mPrimaryStage);
                if (file == null) {
                    logger.info("没有选择保存文件");
                    return;
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
                }
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                logger.error(e.getMessage());
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
            dic.addNode("path", new ListNode(List.of(nodes)));
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
        int length = Integer.parseInt(file.getNode("length").decode());
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

        private HBox mHBox;
        private Button mOkButton;
        private Button mCancelButton;

        private TextField mTextField;

        public FileTreeItemCell() {
        }

        @Override
        public void startEdit() {
            super.startEdit();
//            if (mHBox == null) {
//                mHBox = new HBox();
//                mOkButton = new Button("确定");
//                mCancelButton = new Button("取消");
//                mHBox.getChildren();
//                mTextField = new TextField();
//                mTextField.setText(getString());
//            }
            if (mTextField == null) {
                mTextField = new TextField();
                mTextField.setText(getString());
            }
            setGraphic(mTextField);
            mTextField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem().getName());
            setGraphic(getTreeItem().getGraphic());
//            getTreeItem().setGraphic(mTextField);
        }

        @Override
        protected void updateItem(FileTreeItemModel item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText("");
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (mTextField != null) {
                        mTextField.setText(item.getName());
                    }
                    setText(null);
                    setGraphic(getTreeItem().getGraphic());
                } else {
                    if (mTextField != null) {
                        setText(mTextField.getText());
                        item.setName(mTextField.getText());
                    } else {
                        setText(item.getName());
                    }
                    setGraphic(getTreeItem().getGraphic());
                }
            }
        }

        private String getString() {
            return getItem() == null ? "" : getItem().getName();
        }
    }

    /**
     * 文件信息的Model，主要保存文件名和文件大小
     */
    private final class FileTreeItemModel {
        private String mOriginalName;
        private String mName;
        private int mLength;

        public FileTreeItemModel(String name, int length) {
            mName = name;
            mLength = length;
        }

        public FileTreeItemModel(String name, String length) {
            mName = name;
            mLength = Integer.parseInt(length);
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            if (mOriginalName == null) {
                mOriginalName = mName;
            }
            mName = name;
        }

        public String getOriginalName() {
            return mOriginalName;
        }

        public int getLength() {
            return mLength;
        }

        public void setLength(int length) {
            mLength = length;
        }


        @Override
        public int hashCode() {
            return mName.hashCode() + mLength;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FileTreeItemModel) {
                FileTreeItemModel other = (FileTreeItemModel) obj;
                if (mName.equals(other.mName) && mLength == other.mLength) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return mName + ":" + mLength;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
