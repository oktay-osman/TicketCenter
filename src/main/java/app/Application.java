package app;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        BorderPane root = new BorderPane();
        stage.setScene(new Scene(root, 520, 400));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}