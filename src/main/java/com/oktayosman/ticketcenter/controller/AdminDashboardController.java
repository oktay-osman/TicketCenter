package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.logging.LogUtil;
import com.oktayosman.ticketcenter.service.AdminDashboardService;
import com.oktayosman.ticketcenter.util.SessionManager;
import com.oktayosman.ticketcenter.util.SpringContext;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

@Component
public class AdminDashboardController {

    private static final Logger logger = LogUtil.getLogger(AdminDashboardController.class);
    private static final String NL = System.lineSeparator();
    private static final int RECENT_SALES_LIMIT = 15;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label totalUsersLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label adminLabel;
    @FXML private StackPane contentHost;
    @FXML private VBox dashboardOverviewPane;
    @FXML private ListView<String> recentActivityList;

    private final AdminDashboardService adminDashboardService;

    @Autowired
    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @FXML
    public void initialize() {
        recentActivityList.setPlaceholder(new Label("No ticket sales yet."));
        showDashboardOverview();
        String currentUsername = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getUsername() : "Admin";
        adminLabel.setText(currentUsername);
    }

    private void loadDashboardData() {
        AdminDashboardService.DashboardSnapshot snapshot =
                adminDashboardService.getDashboardSnapshot(RECENT_SALES_LIMIT);

        totalUsersLabel.setText(String.valueOf(snapshot.totalUsers()));
        totalEventsLabel.setText(String.valueOf(snapshot.totalEvents()));
        totalTicketsLabel.setText(String.valueOf(snapshot.ticketsSold()));
        totalRevenueLabel.setText(String.format("€%.2f", snapshot.revenue()));

        recentActivityList.setItems(FXCollections.observableArrayList(
                snapshot.recentSales().stream()
                        .map(AdminDashboardController::formatActivityRow)
                        .toList()));
    }

    private static String formatActivityRow(AdminDashboardService.RecentSale sale) {
        return String.format("%s   %d × %s   ·   by %s   ·   for %s   ·   €%.2f",
                DATE_FMT.format(sale.soldAt()), sale.quantity(), sale.eventName(),
                sale.distributorUsername(), sale.buyerName(), sale.amount());
    }

    // Every return to the overview reloads it, so a role change or a sale made elsewhere
    // is reflected rather than leaving stale numbers on screen.
    private void showDashboardOverview() {
        loadDashboardData();
        contentHost.getChildren().setAll(dashboardOverviewPane);
    }

    private void showUsersManagement() {
        showView("/fxml/admin_users.fxml", "user management",
                (AdminUsersController controller) -> controller.setOnBackToDashboard(this::showDashboardOverview));
    }

    /**
     * Loads an FXML view into the content host. Any failure - including one thrown from the
     * new controller's initialize(), which FXMLLoader rethrows as a LoadException rather than
     * an IOException - is logged and shown to the admin instead of dying on the FX thread and
     * leaving the button looking inert.
     */
    private <C> void showView(String fxmlPath, String description, Consumer<C> configureController) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            configureController.accept(loader.getController());

            contentHost.getChildren().setAll(root);
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to load {} view from {}", description, fxmlPath, e);
            showError("Could not open " + description + "." + NL + NL + rootCauseMessage(e));
        }
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message != null && !message.isBlank() ? message : cause.getClass().getSimpleName();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    @FXML
    public void handleManageUsers(ActionEvent actionEvent) {
        showUsersManagement();
    }

    @FXML
    public void handleManageEvents(ActionEvent actionEvent) {
        showView("/fxml/admin_events.fxml", "event management",
                (AdminEventsController controller) -> controller.setOnBack(this::showDashboardOverview));
    }

    @FXML
    public void handleCreateAccount(ActionEvent actionEvent) {
        showView("/fxml/admin_create_account.fxml", "account creation",
                (AdminCreateAccountController controller) -> controller.setOnBack(this::showDashboardOverview));
    }

    @FXML
    public void handleManageProfiles(ActionEvent actionEvent) {
        showView("/fxml/admin_profiles.fxml", "profile management",
                (AdminProfilesController controller) -> controller.setOnBack(this::showDashboardOverview));
    }

    @FXML
    public void handleViewReports(ActionEvent actionEvent) {
        showView("/fxml/admin_reports.fxml", "reports",
                (AdminReportsController controller) -> controller.setOnBack(this::showDashboardOverview));
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
        } catch (IOException | RuntimeException e) {
            // Keep the dashboard window open: closing it with no login window to replace it
            // would leave the user with nothing on screen.
            logger.error("Failed to load login view after logout", e);
            showError("Could not return to the login screen." + NL + NL + rootCauseMessage(e));
            return;
        }

        Node source = (Node) actionEvent.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();
        currentStage.close();
    }
}
