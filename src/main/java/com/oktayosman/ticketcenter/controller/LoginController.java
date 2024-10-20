package app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

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
        String username = usernameTextField.getText();
        String password = enterPasswordField.getText();
//        User user = userService.authenticate(username, password);

//        if (null != null) {
//            try {
////                FXMLLoader loader = new FXMLLoader((getClass().getResource("/fxml")))
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                loginMessageLabel.setText("Error loading dashboard");
//            }
//        } else {
//            loginMessageLabel.setTextFill(Color.RED);
//            loginMessageLabel.setText("Please enter a valid username/password");
//        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Image logo = new Image(getClass().getResourceAsStream("/images/Logo.png"));

        if (logo.isError()) {
            System.out.println("Error loading image: " + logo.getException());
        } else {
            System.out.println("Image loaded successfully.");
            brandingImageView.setImage(logo);
        }
    }
}
