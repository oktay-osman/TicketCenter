package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.service.AdminDashboardService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminCreateAccountController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;

    // Organizer-specific fields
    @FXML private GridPane organizerFieldsPane;
    @FXML private TextField organizationNameField;
    @FXML private TextField organizerCommissionField;

    // Distributor-specific fields
    @FXML private GridPane distributorFieldsPane;
    @FXML private TextField commissionRateField;

    @FXML private Label messageLabel;

    private final AdminDashboardService adminDashboardService;
    private Runnable onBack;

    @Autowired
    public AdminCreateAccountController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList("USER", "ORGANIZER", "DISTRIBUTOR"));
        roleComboBox.setValue("USER");
        updateRoleFields("USER");

        roleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateRoleFields(newVal);
        });
    }

    private void updateRoleFields(String role) {
        organizerFieldsPane.setVisible("ORGANIZER".equals(role));
        organizerFieldsPane.setManaged("ORGANIZER".equals(role));
        distributorFieldsPane.setVisible("DISTRIBUTOR".equals(role));
        distributorFieldsPane.setManaged("DISTRIBUTOR".equals(role));
    }

    @FXML
    public void handleSave() {
        messageLabel.setStyle("-fx-text-fill: #dc2626;");
        messageLabel.setText("");

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        // Basic validation
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("All required fields must be filled in.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format.");
            return;
        }

        // Role-specific values
        String organizationName = null;
        Double commission = null;
        Double commissionRate = null;

        if ("ORGANIZER".equals(role)) {
            organizationName = organizationNameField.getText().trim();
            String commStr = organizerCommissionField.getText().trim();
            if (!commStr.isEmpty()) {
                try {
                    commission = Double.parseDouble(commStr);
                } catch (NumberFormatException ex) {
                    messageLabel.setText("Commission must be a valid number.");
                    return;
                }
            }
        } else if ("DISTRIBUTOR".equals(role)) {
            String rateStr = commissionRateField.getText().trim();
            if (!rateStr.isEmpty()) {
                try {
                    commissionRate = Double.parseDouble(rateStr);
                    if (commissionRate < 0 || commissionRate > 1) {
                        messageLabel.setText("Commission rate must be between 0 and 1 (e.g. 0.10 for 10%).");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    messageLabel.setText("Commission rate must be a valid number.");
                    return;
                }
            }
        }

        try {
            adminDashboardService.createUserAccount(
                    firstName, lastName, username, email, password, role,
                    organizationName, commission, commissionRate);

            messageLabel.setStyle("-fx-text-fill: #059669;");
            messageLabel.setText("Account created successfully for '" + username + "'.");
            clearForm();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            messageLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            messageLabel.setText("Error creating account: " + ex.getMessage());
        }
    }

    @FXML
    public void handleClear() {
        clearForm();
    }

    @FXML
    public void handleBack() {
        if (onBack != null) onBack.run();
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleComboBox.setValue("USER");
        organizationNameField.clear();
        organizerCommissionField.clear();
        commissionRateField.clear();
    }
}

