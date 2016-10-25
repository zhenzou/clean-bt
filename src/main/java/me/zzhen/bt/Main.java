package me.zzhen.bt;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.SubtitleTrack;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.zzhen.bt.decoder.*;
import me.zzhen.bt.log.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
//        Parent mRoot = FXMLLoader.load(getClass().getResource("sample.fxml"));

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
            TreeNode<FileTreeItemModel> fileTree = createFileTree();
            mRootItem.setValue(new FileTreeItemModel(fileTree.getValue().getName(), fileTree.getValue().getLength()));

//            TreeNode.printTree(fileTree);
            getFileTree(mRootItem, fileTree);
            mInitLabel.setVisible(false);
            mFileTree.setVisible(true);
            //TODO 使用更自然的方式
            mFileTree.setRoot(mRootItem.getChildren().get(0));
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
                        if (children.size() > 0) {
                            String dir = item.getValue().getName();
                            logger.debug("cur dir is " + dir);
                            StringNode dirNode = new StringNode(dir.getBytes());
                            children.forEach(fileItem -> {
                                DictionaryNode dic = new DictionaryNode();
                                String name = fileItem.getValue().getName();
                                int length = fileItem.getValue().getLength();
                                logger.debug("length of " + name + " is" + length);
                                ListNode cur = new ListNode();
                                cur.addNode(dirNode);
                                cur.addNode(new StringNode(name.getBytes()));
                                dic.addNode("path", cur);
                                dic.addNode("length", new IntNode(length));
                                infoRoot.addNode(dic);
                            });
                        } else {
                            DictionaryNode dic = new DictionaryNode();
                            String name = item.getValue().getName();
                            logger.debug("cur dir is " + name);
                            ListNode cur = new ListNode();
                            cur.addNode(new StringNode(name.getBytes()));
                            dic.addNode("path", cur);
                            dic.addNode("length", new IntNode(item.getValue().getLength()));
                            infoRoot.addNode(dic);
                        }
                    });
                    mTorrent.setInfoFiles(infoRoot);
                    OutputStream out = new FileOutputStream(file);
                    out.write(mTorrent.encode());
                    out.flush();
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {

            }
        });
    }

    private void getFileTree(TreeItem<FileTreeItemModel> rootItem, TreeNode<FileTreeItemModel> fileTree) {
        List<TreeNode<FileTreeItemModel>> children = fileTree.getChildren();
        TreeItem<FileTreeItemModel> treeItem = new TreeItem<>(new FileTreeItemModel(fileTree.getValue().getName(), fileTree.getValue().getLength()));
        System.out.println(children.size());
        if (children.size() == 0) {
//            rootItem.getChildren().add(treeItem);
        } else {
            children.forEach(node -> getFileTree(treeItem, node));
        }
        rootItem.getChildren().add(treeItem);
    }


    private TreeNode<FileTreeItemModel> createFileTree() {
        ListNode infoName = (ListNode) mTorrent.getInfoFiles();
        List<Node> value = infoName.getValue();
        TreeNode<FileTreeItemModel> treeRoot = new TreeNode<>(new FileTreeItemModel(mTorrent.getInfoName().decode(), 0));
        value.stream().map(item -> (DictionaryNode) item).collect(Collectors.toList()).forEach(item -> addFileToRoot(treeRoot, item));
        return treeRoot;
    }

    private void addFileToRoot(TreeNode<FileTreeItemModel> treeRoot, DictionaryNode file) {
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
     * TODO 采用更加方便的数据结构，做为树的Model
     */
    private void getFileTree() {
        Node infoName = mTorrent.getInfoFiles();
        mInitLabel.setVisible(false);
        mFileTree.setVisible(true);
        mFileTree.setRoot(mRootItem);

        logger.debug(mTorrent.getInfoName().decode());
        logger.debug(mTorrent.getInfoFiles().decode());
        //暂时只管两级文件夹吧，不定级文件夹再说吧
        //TODO 支持不定级文件夹
        //TODO 支持编辑
        Map<String, Integer> dirRecord = new HashMap<>();
        final int[] index = {0};

        if (infoName instanceof ListNode) {
            List<Node> files = ((ListNode) infoName).getValue();
            List<DictionaryNode> collect = files.stream().map(fileNode -> (DictionaryNode) fileNode).collect(Collectors.toList());
            collect.forEach(file -> {
                ListNode path = (ListNode) file.getNode("path");
                if (path.size() > 1) {
                    String dir = path.get(0).decode();
                    if (dirRecord.containsKey(dir)) {
                        int i = dirRecord.get(dir);
                        mRootItem.getChildren().get(i).getChildren().add(new TreeItem<>(new FileTreeItemModel(path.get(1).decode(), file.getNode("length").decode())));
                    } else {
                        dirRecord.put(dir, index[0]);
                        TreeItem<FileTreeItemModel> curDir = new TreeItem<>(new FileTreeItemModel(dir, file.getNode("length").decode()));
                        mRootItem.getChildren().add(curDir);
                        curDir.getChildren().add(new TreeItem<>(new FileTreeItemModel(path.get(1).decode(), file.getNode("length").decode())));
                        index[0]++;
                    }
                } else {
                    mRootItem.getChildren().add(new TreeItem<>(new FileTreeItemModel(path.get(0).decode(), file.getNode("length").decode())));
                }
            });
        } else {
            mRootItem.getChildren().add(new TreeItem<>(new FileTreeItemModel(infoName.decode(), 0)));
        }
    }


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
