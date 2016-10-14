package me.zzhen.bt;

import jBittorrentAPI.TorrentFile;
import jBittorrentAPI.TorrentProcessor;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class Main extends Application {
    private TorrentFile mTorrent;
    TorrentProcessor processor = new TorrentProcessor();
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
            Map map = processor.parseTorrent(file);
            mTorrent = processor.getTorrentFile(map);
            ArrayList<String> name = mTorrent.name;
            for (int i = 0; i < name.size(); i++) {
                TextField text = new TextField(name.get(i));
//                center.getChildren().add(text);
                texts.getItems().add(text);
            }
        });
        saveFile.setOnAction((event) -> {
            byte[] bytes = processor.generateTorrent(mTorrent);
            try {
                File file1 = chooser.showSaveDialog(null);
                OutputStream out = new FileOutputStream(file1);
                out.write(bytes);
                out.flush();
                out.close();
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

    public void testPrint(boolean bool) {

    }


    public static void main(String[] args) {
        launch(args);
    }
}
