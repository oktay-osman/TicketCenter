package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.UserService;
import com.oktayosman.ticketcenter.util.SpringContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class LoginController {

    @FXML
    private ImageView brandingImageView;

    @FXML
    private Button cancelButton;

    @FXML
    private PasswordField enterPasswordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private TextField usernameTextField;

    @Autowired
    private UserService userService;

    @FXML
    void cancelButtonOnAction() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    void loginButtonOnAction() {
        String username = usernameTextField.getText().trim();
        String password = enterPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorMessage("Please enter your username and password");
            return;
        }

        try {
            User user = userService.authenticate(username, password);

            if (user != null) {
                showSuccessMessage();
                String fxmlFile = getFxmlFileBasedOnRole(user.getRole().getName());
                loadDashboard(fxmlFile, user.getRole().getName());
            } else {
                showErrorMessage("Invalid username or password");
                enterPasswordField.clear();
            }
        } catch (IllegalArgumentException e) {
            showErrorMessage("Invalid role configuration");
        } catch (IOException e) {
            showErrorMessage("Error loading dashboard");
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        try {
            brandingImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            System.out.println("Error loading logo: " + e.getMessage());
        }
    }

    private String getFxmlFileBasedOnRole(String roleName) {
        return switch (roleName) {
            case "ADMIN" -> "/fxml/admin.fxml";
            case "ORGANIZER" -> "/fxml/organizer.fxml";
            case "DISTRIBUTOR" -> "/fxml/distributor.fxml";
            default -> throw new IllegalArgumentException("Invalid role: " + roleName);
        };
    }

    private void loadDashboard(String fxmlFile, String roleName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        loader.setControllerFactory(SpringContext::getBean);
        Parent root = loader.load();

        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(roleName + " Dashboard");
        stage.show();
    }

    @FXML
    private void registerButtonOnAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Register");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showErrorMessage(String message) {
        loginMessageLabel.setTextFill(Color.RED);
        loginMessageLabel.setText(message);
    }

    private void showSuccessMessage() {
        loginMessageLabel.setTextFill(Color.GREEN);
        loginMessageLabel.setText("Login successful!");
    }
}