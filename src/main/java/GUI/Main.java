package GUI;

import java.io.File;

import GUI.Controller.MainSceneController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(new File("resources/mainScene.fxml").toURL());
        Parent root = loader.load();
        MainSceneController controller = loader.getController();

        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(e -> controller.shutdown());
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}