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
import java.util.*;
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

    private TreeView<TextField> mFileTree = new TreeView<>();
    private TreeItem<TextField> mRootItem = new TreeItem<>();

    private Stage mPrimaryStage;


    private List<Integer> mLengthList = new ArrayList<>();//Temp solution to record mLength

    @Override
    public void start(Stage primaryStage) throws Exception {
//        Parent mRoot = FXMLLoader.load(getClass().getResource("sample.fxml"));

//        mFileTree.setCellFactory();
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
        mRoot.setTop(mTop);
        mRoot.setCenter(mCenter);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Torrent", "*.torrent"));
    }

    private void initMenu() {
        Menu fileMenu = new Menu(Config.MENU_FILE);
        MenuItem openFile = new MenuItem(Config.MENU_FILE_OPEN);
        MenuItem saveFile = new MenuItem(Config.MENU_FILE_SAVE);
        fileMenu.getItems().add(openFile);
        fileMenu.getItems().add(saveFile);
        saveFile.setDisable(true);
        mMenu.getMenus().add(fileMenu);
        mTop.getChildren().add(mMenu);

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
            getFileTree();
        });

        saveFile.setOnAction((event) -> {
            try {
                chooser.setTitle("保存Torrent文件");
                File file = chooser.showSaveDialog(mPrimaryStage);
                if (file == null) {
                    logger.info("没有选择保存文件");
                    return;
                } else {
//                    Node infoName = mTorrent.getInfoFiles();
//                    List<Node> files = ((ListNode) infoName).getValue();
//                    List<DictionaryNode> collect = files.stream().map(fileNode -> (DictionaryNode) fileNode).collect(Collectors.toList());
//                    ListNode list = new ListNode();
//                    for (Node node : files) {
//                        DictionaryNode node3 = (DictionaryNode) node;
//                        DictionaryNode dic = new DictionaryNode();
//                        ListNode node2 = new ListNode();
//                        node2.addNode(new StringNode((((ListNode) node3.getNode("path")).getValue().get(0).decode() + "Test").getBytes()));
//                        dic.addNode("path", node2);
//                        dic.addNode("mLength", new IntNode(node3.getNode("mLength").decode()));
//                        list.addNode(dic);
//                    }
//                    mTorrent.setInfoFiles(list);
//                    mTorrent.setInfoFiles(new ListNode(collect.stream().map(name -> {
//                        ListNode fileNode = new ListNode();
//                        fileNode.addNode(new StringNode(name.encode()));
//                        return fileNode;
//                    }).collect(Collectors.toList())));

                    ListNode infoRoot = new ListNode();
                    final int[] index = {0};
                    mRootItem.getChildren().forEach(item -> {
                        ObservableList<TreeItem<TextField>> children = item.getChildren();
                        IntNode length = new IntNode(mLengthList.get(index[0]) + "");
                        DictionaryNode dic = new DictionaryNode();
                        if (children.size() > 0) {
                            String dir = item.getValue().getText();
                            System.out.println(dir);
                            StringNode dirNode = new StringNode(dir.getBytes());
                            children.forEach(fileItem -> {
                                String name = fileItem.getValue().getText();
                                System.out.println(name);
                                ListNode cur = new ListNode();
                                cur.addNode(dirNode);
                                cur.addNode(new StringNode(name.getBytes()));
                                dic.addNode("path", cur);
                                dic.addNode("length", length);
                                infoRoot.addNode(dic);
                                index[0]++;
                            });
                        } else {
                            String name = item.getValue().getText();
                            ListNode cur = new ListNode();
                            cur.addNode(new StringNode(name.getBytes()));
                            dic.addNode("path", cur);
                            dic.addNode("length", length);
                            infoRoot.addNode(dic);
                            index[0]++;
                        }
                    });
                    System.out.println(mTorrent.getInfoFiles());
                    System.out.println(infoRoot);
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

    /**
     * TODO 采用更加方便的数据结构，做为树的Model
     */
    private void getFileTree() {
        Node infoName = mTorrent.getInfoFiles();
//        mCenter.getChildren().remove(mInitLabel);
        mInitLabel.setVisible(false);
        mFileTree.setVisible(true);
        mRootItem.setValue(new TextField(mTorrent.getInfoName().decode()));
//        mCenter.getChildren().add(mFileTree);
        mFileTree.setRoot(mRootItem);
//        mFileTree.getRoot().getChildren().clear();

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
                        mRootItem.getChildren().get(i).getChildren().add(new TreeItem<>(new TextField(path.get(1).decode())));
                    } else {
                        dirRecord.put(dir, index[0]);
                        TreeItem<TextField> curDir = new TreeItem<>(new TextField(dir));
                        mRootItem.getChildren().add(curDir);
                        curDir.getChildren().add(new TreeItem<>(new TextField(path.get(1).decode())));
                        index[0]++;
                    }
                } else {
                    mRootItem.getChildren().add(new TreeItem<>(new TextField(path.get(0).decode())));
                }
                mLengthList.add(Integer.parseInt(file.getNode("length").decode()));
            });
        } else {
            mRootItem.getChildren().add(new TreeItem<>(new TextField(infoName.decode())));
        }
    }


    private final class TreeFileItemCell extends TreeCell<String> {
        private TextField mTextField;
        private int mLength;

        public TreeFileItemCell() {
        }

        public TreeFileItemCell(int length, String text) {
            mTextField = new TextField(text);
            mLength = length;
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (mTextField == null) {
                mTextField = new TextField();
                mTextField.setText(null);
            }
            setGraphic(mTextField);
            mTextField.selectAll();
        }

        @Override
        public void commitEdit(String s) {
            super.commitEdit(s);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(getTreeItem().getGraphic());
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (mTextField != null) {
                        mTextField.setText(getString());
                    }
                    setText(null);
                    setGraphic(getTreeItem().getGraphic());
                } else {
                    setText(getString());
                    setGraphic(getTreeItem().getGraphic());
                }
            }
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }

    private final class FileTreeItemModel {
        private String mName;
        private String mLength;

        public FileTreeItemModel(String name, String length) {
            mName = name;
            mLength = length;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getLength() {
            return mLength;
        }

        public void setLength(String length) {
            mLength = length;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
