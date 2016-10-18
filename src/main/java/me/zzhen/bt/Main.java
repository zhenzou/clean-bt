package me.zzhen.bt;

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
        fileMenu.getItems().add(openFile);
        fileMenu.getItems().add(saveFile);
        menuBar.getMenus().add(fileMenu);
        top.getChildren().add(menuBar);
        root.setTop(top);
        root.setCenter(center);
        center.getChildren().add(texts);
        openFile.setOnAction(event -> {
            chooser.setTitle("选择文件");
            File file = chooser.showOpenDialog(null);
            if (file == null || !file.exists()) {
                System.out.println("没有选择文件");
                return;
            }

            try {
                mTorrent = TorrentFile.fromFile(file.getAbsolutePath());
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
                collect.forEach(name -> names.add(name.getNode("path").decode()));
//                collect.forEach(name -> System.out.println(name.decode()));
            } else {
                System.out.println();
                names.add(infoName.decode());
            }
            for (int i = 0; i < names.size(); i++) {
                TextField text = new TextField(names.get(i));
//                center.getChildren().add(text);
                texts.getItems().add(text);
            }
        });
//        saveFile.setOnAction((event) -> {
//            byte[] bytes = processor.generateTorrent(mTorrent);
//            try {
//                File file1 = chooser.showSaveDialog(null);
//                OutputStream out = new FileOutputStream(file1);
//                out.write(bytes);
//                out.flush();
//                out.close();
//            } catch (FileNotFoundException e) {
//
//                e.printStackTrace();
//            } catch (IOException e) {
//
//            }
//        });
        Scene scene = new Scene(root, 300, 275);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
