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
import me.zzhen.bt.decoder.DictionaryNode;
import me.zzhen.bt.decoder.ListNode;
import me.zzhen.bt.decoder.Node;
import me.zzhen.bt.decoder.StringNode;

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
            List<String> names = new ArrayList<>();
            if (infoName instanceof ListNode) {
                System.out.println();
                ListNode node = (ListNode) infoName;
                List<Node> value = node.getValue();
                List<DictionaryNode> collect = value.stream().map(fileNode -> (DictionaryNode) fileNode).collect(Collectors.toList());
                collect.forEach(tt -> System.out.println(tt.getNode("path") instanceof ListNode));
                collect.forEach(name -> names.add(name.getNode("path").decode()));
                mTorrent.setInfoFiles(new ListNode(names.stream().map(name -> {
                    ListNode node1 = new ListNode();
                    node.addNode(new StringNode(name));
                    return node1;
                }).collect(Collectors.toList())));
            } else {
                names.add(infoName.decode());
            }
            for (int i = 0; i < names.size(); i++) {
                TextField text = new TextField(names.get(i));
//                center.getChildren().add(text);
                texts.getItems().add(text);
            }
        });
        saveFile.setOnAction((event) -> {
            try {
                File file = chooser.showSaveDialog(null);
                if (file == null) {
                    return;
                } else {
                    OutputStream out = new FileOutputStream(file);
                    String s = mTorrent.toString();
                    out.write(s.getBytes());
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
