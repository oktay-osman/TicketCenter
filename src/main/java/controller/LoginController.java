package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    public ImageView brandingImageView;

    @FXML
    public Button cancelButton;

    @FXML
    public PasswordField enterPasswordField;

    @FXML
    public Button loginButton;

    @FXML
    public Label loginMessageLabel;

    @FXML
    public TextField usernameTextField;

    @FXML
    void cancelButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    void loginButtonOnAction(ActionEvent event) {
        if (!usernameTextField.getText().isEmpty() &&
                !enterPasswordField.getText().isEmpty()) {
            checkLogin();
        } else {
            loginMessageLabel.setTextFill(Color.RED);
            loginMessageLabel.setText("Please enter a valid username/password");
        }
    }

    public void checkLogin() {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        File brandingFile = new File("images/Logo.png");
        Image brandingImage = new Image(brandingFile.toURI().toString());
        brandingImageView.setImage(brandingImage);
    }
}
