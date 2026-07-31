package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.TicketSale;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.DistributorService;
import com.oktayosman.ticketcenter.util.SessionManager;
import com.oktayosman.ticketcenter.util.SpringContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DistributorDashboardController {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    @FXML private Label totalSalesLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label commissionLabel;
    @FXML private Label distributorLabel;
    @FXML private ListView<String> recentSalesList;
    @FXML private StackPane contentHost;
    @FXML private VBox dashboardOverviewPane;
    @FXML private Label profileUsernameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileCommissionLabel;
    @FXML private Label profileRatingLabel;

    private final DistributorService distributorService;
    private Distributor currentDistributor;

    @Autowired
    public DistributorDashboardController(DistributorService distributorService) {
        this.distributorService = distributorService;
    }

    @FXML
    public void initialize() {
        try {
            User sessionUser = SessionManager.getCurrentUser();
            if (sessionUser == null) {
                distributorLabel.setText("Not logged in");
                return;
            }

            if (sessionUser.getRole() == null || !DISTRIBUTOR_ROLE_NAME.equals(sessionUser.getRole().getName())) {
                distributorLabel.setText("Your account no longer has distributor access. Please log in again.");
                return;
            }

            currentDistributor = distributorService.getDistributorByUserId(sessionUser.getId())
                    .orElse(null);
            if (currentDistributor != null) {
                distributorLabel.setText(currentDistributor.getFirstName() + " " + currentDistributor.getLastName());
                loadDashboardData();
                loadProfileData();
                showDashboardOverview();
            } else {
                distributorLabel.setText("Distributor record not found for: " + sessionUser.getUsername());
            }
        } catch (Exception e) {
            distributorLabel.setText("Error initializing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDashboardData() {
        List<TicketSale> sales = distributorService.getDistributorSales(currentDistributor);

        totalSalesLabel.setText(String.valueOf(sales.size()));

        BigDecimal totalRevenue = sales.stream()
                .map(TicketSale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalRevenueLabel.setText(String.format("€%.2f", totalRevenue));

        double commissionRate = currentDistributor.getCommissionRate();
        commissionLabel.setText(String.format("%.2f %%", commissionRate * 100));

        List<String> recentSalesDisplayList = sales.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .map(sale -> String.format("%s - %s %s - €%.2f",
                        sale.getCreatedAt().toLocalDate(),
                        sale.getBuyerFirstName(),
                        sale.getBuyerLastName(),
                        sale.getTotalAmount()))
                .collect(Collectors.toList());

        recentSalesList.setItems(FXCollections.observableArrayList(recentSalesDisplayList));
    }

    private void loadProfileData() {
        if (currentDistributor == null) {
            return;
        }

        User user = currentDistributor.getUser();
        profileUsernameLabel.setText(user != null && user.getUsername() != null ? user.getUsername() : "-");
        profileEmailLabel.setText(user != null && user.getEmail() != null ? user.getEmail() : "-");
        profileCommissionLabel.setText(String.format("%.2f %%", currentDistributor.getCommissionRate() * 100));
        profileRatingLabel.setText(currentDistributor.getRating() != null
                ? String.format("%.1f / 5", currentDistributor.getRating())
                : "Not rated yet");
    }

    private void showDashboardOverview() {
        contentHost.getChildren().setAll(dashboardOverviewPane);
    }

    @FXML
    public void handleCreateSale() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/distributor_create_sale.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Create Ticket Sale");
            stage.setOnHidden(event -> loadDashboardData());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleViewSales() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/distributor_sales_history.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Sales History");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        Stage stage = (Stage) contentHost.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setTitle("TicketCenter - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
