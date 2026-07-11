package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.service.AdminDashboardService;
import com.oktayosman.ticketcenter.util.SessionManager;
import com.oktayosman.ticketcenter.util.SpringContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminDashboardController {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label adminLabel;
    @FXML private StackPane contentHost;
    @FXML private VBox dashboardOverviewPane;

    private final AdminDashboardService adminDashboardService;

    @Autowired
    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @FXML
    public void initialize() {
        loadDashboardData();
        showDashboardOverview();
        String currentUsername = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getUsername() : "Admin";
        adminLabel.setText(currentUsername);
    }

    private void loadDashboardData() {
        totalUsersLabel.setText(String.valueOf(adminDashboardService.getTotalUsers()));
        totalEventsLabel.setText(String.valueOf(adminDashboardService.getTotalEvents()));
    }

    private void showDashboardOverview() {
        contentHost.getChildren().setAll(dashboardOverviewPane);
    }

    private void showUsersManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_users.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent usersRoot = loader.load();

            AdminUsersController controller = loader.getController();
            controller.setOnBackToDashboard(this::showDashboardOverview);

            contentHost.getChildren().setAll(usersRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load admin users view", e);
        }
    }

    @FXML
    public void handleManageUsers(ActionEvent actionEvent) {
        showUsersManagement();
    }

    @FXML
    public void handleManageEvents(ActionEvent actionEvent) {
        // TODO: Implement manage events functionality
    }

    @FXML
    public void handleViewReports(ActionEvent actionEvent) {
        // TODO: Implement view reports functionality
    }

    @FXML
    public void handleLogout(ActionEvent actionEvent) {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root));
            loginStage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load login view", e);
        }

        Node source = (Node) actionEvent.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();
        currentStage.close();
    }
}
