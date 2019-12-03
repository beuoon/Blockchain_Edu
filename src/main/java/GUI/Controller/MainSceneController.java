package GUI.Controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class MainSceneController implements Initializable {
    @FXML
    private Canvas canvas;
    private GraphicsContext gc;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canvas.setOnMouseClicked(this::onMouseEvent);
        canvas.setOnMousePressed(this::onMouseEvent);
        canvas.setOnMouseDragged(this::onMouseEvent);

        gc = canvas.getGraphicsContext2D();
        draw();
    }

    private void onMouseEvent(MouseEvent event) {
        System.out.println(event.getEventType() + " " + event.getX());
    }

    private void draw() {
        gc.setFill(Color.BLACK);
        gc.fillRect(1, 1, 298, 298);

        gc.setStroke(Color.WHITE);
        gc.strokeOval(100, 100, 100, 100);
    }
}
