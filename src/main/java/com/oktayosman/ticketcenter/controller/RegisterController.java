package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Role;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class RegisterController {

    @FXML
    private TextField firstnameTextField;

    @FXML
    private TextField lastnameTextField;

    @FXML
    private TextField emailTextField;

    @FXML
    private TextField usernameTextField;

    @FXML
    private PasswordField setPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label registrationMessageLabel;

    @FXML
    private Label confirmPasswordLabel;

    @FXML
    private Button registerButton;

    @FXML
    private Button closeButton;

    @Autowired
    private UserService userService;

    @FXML
    void registerButtonOnAction(ActionEvent event) {
        String firstName = firstnameTextField.getText();
        String lastName = lastnameTextField.getText();
        String email = emailTextField.getText();
        String username = usernameTextField.getText();
        String password = setPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            registrationMessageLabel.setText("Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLabel.setText("Passwords do not match.");
            return;
        }

        try{
            User newUser = new User(firstName, lastName, email, username, password, null);
            boolean success = userService.registerUser(newUser);

            if (success) {
                registrationMessageLabel.setText("Registration successful!");
                clearFields();
            } else {
                registrationMessageLabel.setText("Registration failed. Please try again.");
            }
        } catch (Exception e) {
            registrationMessageLabel.setText("Registration failed. Please try again.");
        }

    }

    @FXML
    void closeButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    void clearFields() {
        firstnameTextField.clear();
        lastnameTextField.clear();
        emailTextField.clear();
        usernameTextField.clear();
        setPasswordField.clear();
        confirmPasswordField.clear();
    }
}