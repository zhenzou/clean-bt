package me.zzhen.bt;

import jBittorrentAPI.TorrentProcessor;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.zzhen.bt.decoder.*;

import javax.swing.plaf.metal.MetalRootPaneUI;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main extends Application {
    private TorrentFile mTorrent;
    FileChooser chooser = new FileChooser();
    private TorrentProcessor processor;


    @Override
    public void start(Stage primaryStage) throws Exception {
//        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        BorderPane root = new BorderPane();
        MenuBar menuBar = new MenuBar();
        HBox top = new HBox();
        VBox center = new VBox();
        ListView<TextField> texts = new ListView<>();
        Menu fileMenu = new Menu("文件");
        MenuItem openFile = new MenuItem("打开");
        MenuItem saveFile = new MenuItem("保存");
        MenuItem openBitFile = new MenuItem("Bit打开");
        MenuItem saveBitFile = new MenuItem("Bit保存");
        fileMenu.getItems().add(openFile);
        fileMenu.getItems().add(saveFile);
        fileMenu.getItems().add(openBitFile);
        fileMenu.getItems().add(saveBitFile);
        menuBar.getMenus().add(fileMenu);
        top.getChildren().add(menuBar);
        root.setTop(top);
        root.setCenter(center);
        center.getChildren().add(texts);
        openBitFile.setOnAction(event -> {
            chooser.setTitle("选择文件");
            File file = chooser.showOpenDialog(null);
            if (file == null || !file.exists()) {
                System.out.println("没有选择文件");
                return;
            }
            Map map = processor.parseTorrent(file);
            jBittorrentAPI.TorrentFile torrentFile = processor.getTorrentFile(map);
            ArrayList<String> name = torrentFile.name;
            for (int i = 0; i < name.size(); i++) {
                TextField text = new TextField(name.get(i));
//                center.getChildren().add(text);
                texts.getItems().add(text);
            }
        });
        openFile.setOnAction(event -> {
            chooser.setTitle("选择文件");
            File file = chooser.showOpenDialog(null);
            if (file == null || !file.exists()) {
                System.out.println("没有选择文件");
                return;
            }
            try {
                mTorrent = TorrentFile.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Node infoName = mTorrent.getInfoFiles();
//            System.out.println(mTorrent.getInfo());
//            System.out.println(mTorrent.getInfoPieces());
            try {
                OutputStream out = new FileOutputStream("D:/test.test");
                byte[] bytes = mTorrent.getInfoPieces().getBytes();
                for (byte aByte : bytes) {
                    System.out.println((int) aByte);
                    out.write((int) aByte);
                }
//                out.write(mTorrent.getInfoPieces().getBytes());
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<String> names = new ArrayList<>();
            if (infoName instanceof ListNode) {
                List<Node> files = ((ListNode) infoName).getValue();
                List<DictionaryNode> collect = files.stream().map(fileNode -> (DictionaryNode) fileNode).collect(Collectors.toList());

//                collect.forEach(tt -> System.out.println(tt.getNode("path") instanceof ListNode));
                collect.forEach(name -> names.add(((ListNode) name.getNode("path")).getValue().get(0).decode()));

                ListNode list = new ListNode();
                for (Node node : files) {
                    DictionaryNode node3 = (DictionaryNode) node;
                    DictionaryNode dic = new DictionaryNode();
                    ListNode node2 = new ListNode();
                    node2.addNode(new StringNode((((ListNode) node3.getNode("path")).getValue().get(0).decode() + "Test").getBytes()));
                    dic.addNode("path", node2);
                    dic.addNode("length", new IntNode(node3.getNode("length").decode()));
                    list.addNode(dic);
                }
//                value.stream().map(val -> (DictionaryNode) val).forEach(var -> {
//
//                });
                mTorrent.setInfoFiles(list);

//                System.out.println(mTorrent.getInfo());
//
//                mTorrent.setInfoFiles(new ListNode(names.stream().map(name -> {
//
//                }).collect(Collectors.toList())));
            } else {
                names.add(infoName.decode());
            }
//            for (int i = 0; i < names.size(); i++) {
//                TextField text = new TextField(names.get(i));
////                center.getChildren().add(text);
//                texts.getItems().add(text);
//            }
        });
        saveFile.setOnAction((event) -> {
            try {
                File file = chooser.showSaveDialog(null);
                if (file == null) {
                    return;
                } else {
                    OutputStream out = new FileOutputStream(file);
//                    String s = ;
                    out.write(mTorrent.encode());
                    out.flush();
                    out.close();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {

            }
        });
        Scene scene = new Scene(root, 300, 275);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
