package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.AdminDashboardService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminUsersController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label selectedUserLabel;
    @FXML private Label currentRoleLabel;
    @FXML private Label messageLabel;
    @FXML private Button saveRoleButton;

    private final AdminDashboardService adminDashboardService;
    private final ObservableList<User> masterUsers = FXCollections.observableArrayList();
    private final FilteredList<User> filteredUsers = new FilteredList<>(masterUsers, user -> true);
    private final SortedList<User> sortedUsers = new SortedList<>(filteredUsers);

    private Runnable onBackToDashboard;
    private User selectedUser;

    @Autowired
    public AdminUsersController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @FXML
    public void initialize() {
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRole().getName()));

        sortedUsers.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedUsers);

        roleComboBox.setItems(FXCollections.observableArrayList(adminDashboardService.getAssignableRoleNames()));
        saveRoleButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> selectedUser == null || roleComboBox.getValue() == null,
                userTable.getSelectionModel().selectedItemProperty(),
                roleComboBox.valueProperty()));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> updateSelection(newUser));
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
            }
        });

        loadUsers();
        clearSelection();
    }

    public void setOnBackToDashboard(Runnable onBackToDashboard) {
        this.onBackToDashboard = onBackToDashboard;
    }

    @FXML
    private void handleBack() {
        if (onBackToDashboard != null) {
            onBackToDashboard.run();
        }
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
        showMessage("Users refreshed.", true);
    }

    @FXML
    private void handleClearSelection() {
        userTable.getSelectionModel().clearSelection();
        clearSelection();
    }

    @FXML
    private void handleSaveRole() {
        if (selectedUser == null) {
            showMessage("Select a user first.", false);
            return;
        }

        String roleName = roleComboBox.getValue();
        if (roleName == null) {
            showMessage("Choose a role to assign.", false);
            return;
        }

        Long userId = selectedUser.getId();
        try {
            adminDashboardService.updateUserRole(userId, roleName);
            loadUsers();
            selectUserById(userId);
            showMessage("Role updated successfully.", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            showMessage(e.getMessage(), false);
        }
    }

    private void loadUsers() {
        Long previouslySelectedUserId = selectedUser != null ? selectedUser.getId() : null;
        masterUsers.setAll(adminDashboardService.getAllUsers());
        applyFilter();

        if (previouslySelectedUserId != null) {
            if (!selectUserById(previouslySelectedUserId)) {
                clearSelection();
            }
        } else {
            clearSelection();
        }
    }

    private void applyFilter() {
        String filterText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filteredUsers.setPredicate(user -> {
            if (filterText.isEmpty()) {
                return true;
            }

            return contains(user.getFirstName(), filterText)
                    || contains(user.getLastName(), filterText)
                    || contains(user.getUsername(), filterText)
                    || contains(user.getEmail(), filterText)
                    || contains(user.getRole().getName(), filterText);
        });
    }

    private boolean contains(String value, String filterText) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filterText);
    }

    private void updateSelection(User user) {
        selectedUser = user;
        if (user == null) {
            clearSelection();
            return;
        }

        selectedUserLabel.setText(user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")");
        currentRoleLabel.setText(user.getRole().getName());

        if (adminDashboardService.getAssignableRoleNames().contains(user.getRole().getName())) {
            roleComboBox.setValue(user.getRole().getName());
        } else {
            roleComboBox.setValue(null);
        }

        messageLabel.setText("");
    }

    private void clearSelection() {
        selectedUser = null;
        selectedUserLabel.setText("No user selected");
        currentRoleLabel.setText("-");
        roleComboBox.setValue(null);
        messageLabel.setText("");
    }

    private boolean selectUserById(Long userId) {
        if (userId == null) {
            return false;
        }

        return userTable.getItems().stream()
                .filter(user -> userId.equals(user.getId()))
                .findFirst()
                .map(user -> {
                    userTable.getSelectionModel().select(user);
                    return true;
                })
                .orElse(false);
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success ? "-fx-text-fill: #059669;" : "-fx-text-fill: #dc2626;");
    }
}
