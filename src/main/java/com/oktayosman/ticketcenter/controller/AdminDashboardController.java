package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.AdminDashboardService;
import com.oktayosman.ticketcenter.util.SessionManager;
import com.oktayosman.ticketcenter.util.SpringContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;


@Component
public class AdminDashboardController {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private ListView<String> recentActivityList;
    @FXML private ListView<String> notificationsList;
    @FXML private Button markNotificationsReadButton;
    @FXML private BorderPane mainPane;
    @FXML private VBox userManagementPanel;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button logoutButton;

    @FXML private VBox usersCard;
    @FXML private VBox eventsCard;
    @FXML private VBox ticketsCard;
    @FXML private VBox revenueCard;

    private final AdminDashboardService adminDashboardService;
    private ObservableList<User> users = FXCollections.observableArrayList();

    @Autowired
    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @FXML
    public void initialize() {
        loadDashboardData();

        if (userManagementPanel != null) {
            userManagementPanel.setVisible(false);
        }

        if (userTable != null) {
            usernameColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getUsername()));
            emailColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getEmail()));
            roleColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getRole().getName()));
        }

        if (usersCard != null) {
            usersCard.setOnMouseClicked(e -> handleManageUsers(null));
        }
        if (eventsCard != null) {
            eventsCard.setOnMouseClicked(e -> handleManageEvents(null));
        }
        if (ticketsCard != null) {
            ticketsCard.setOnMouseClicked(e -> handleViewReports(null));
        }
        if (revenueCard != null) {
            revenueCard.setOnMouseClicked(e -> handleViewReports(null));
        }
    }

    private void loadDashboardData() {
        int totalUsers = adminDashboardService.getTotalUsers();
        int totalEvents = adminDashboardService.getTotalEvents();

        totalUsersLabel.setText(String.valueOf(totalUsers));
        totalEventsLabel.setText(String.valueOf(totalEvents));
    }


    @FXML
    public void handleManageUsers(ActionEvent actionEvent) {
        if (userManagementPanel != null) {
            boolean isVisible = userManagementPanel.isVisible();
            userManagementPanel.setVisible(!isVisible);

            if (!isVisible) {
                loadUsers();
            }
        }
    }

    private void loadUsers() {
        users.setAll(adminDashboardService.getAllUsers());
        userTable.setItems(users);
    }

    @FXML
    private void handleSearchUsers() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            userTable.setItems(users);
        } else {
            ObservableList<User> filteredUsers = users.filtered(user ->
                    user.getUsername().toLowerCase().contains(searchText.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(searchText.toLowerCase()) ||
                            user.getRole().getName().toLowerCase().contains(searchText.toLowerCase()));
            searchField.clear(
            );
            userTable.setItems(filteredUsers);
        }
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
            e.printStackTrace();
        }
        Node source = (Node) actionEvent.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();
        currentStage.close();
    }

}