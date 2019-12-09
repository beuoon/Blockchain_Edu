package GUI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import GUI.Controller.MainSceneController;
import GUI.Controller.SendDialogController;
import blockchainCore.node.Node;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

public class Main extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/mainScene.fxml"));
        Parent root = loader.load();
        MainSceneController controller = loader.getController();
        controller.setSendDialog(this::showSendDialog);

        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Blockchain Education");
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        primaryStage.setOnCloseRequest(e -> controller.shutdown());
        primaryStage.show();
    }

    private Pair<Pair<String, String>, Integer> showSendDialog(String nodeId, ArrayList<Node> nodes) {
        Pair<Pair<String, String>, Integer> result = null;

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/sendDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Send");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setScene(new Scene(root));

            SendDialogController controller = loader.getController();
            controller.setNodeInfo(nodeId, nodes);

            dialogStage.showAndWait();

            result = controller.getResult();

        } catch (Exception ignored) { }

        return result;
    }

    public static void main(String[] args) { Application.launch(args); }
}