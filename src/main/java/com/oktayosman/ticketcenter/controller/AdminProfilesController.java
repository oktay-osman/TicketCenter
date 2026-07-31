package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.service.AdminDashboardService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminProfilesController {

    // --- Organizers tab ---
    @FXML private TableView<Organizer> organizersTable;
    @FXML private TableColumn<Organizer, String> orgNameColumn;
    @FXML private TableColumn<Organizer, String> orgUsernameColumn;
    @FXML private TableColumn<Organizer, String> orgOrgNameColumn;
    @FXML private TableColumn<Organizer, String> orgCommissionColumn;

    @FXML private Label selectedOrganizerLabel;
    @FXML private TextField orgNameField;
    @FXML private TextField orgCommissionField;
    @FXML private Label orgMessageLabel;

    // --- Distributors tab ---
    @FXML private TableView<Distributor> distributorsTable;
    @FXML private TableColumn<Distributor, String> distNameColumn;
    @FXML private TableColumn<Distributor, String> distUsernameColumn;
    @FXML private TableColumn<Distributor, String> distCommissionColumn;
    @FXML private TableColumn<Distributor, String> distRatingColumn;

    @FXML private Label selectedDistributorLabel;
    @FXML private TextField distCommissionField;
    @FXML private Label distMessageLabel;

    private final AdminDashboardService adminDashboardService;
    private final ObservableList<Organizer> organizerList = FXCollections.observableArrayList();
    private final ObservableList<Distributor> distributorList = FXCollections.observableArrayList();

    private Organizer selectedOrganizer;
    private Distributor selectedDistributor;
    private Runnable onBack;

    @Autowired
    public AdminProfilesController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    public void initialize() {
        setupOrganizerTable();
        setupDistributorTable();
        loadData();
    }

    private void setupOrganizerTable() {
        orgNameColumn.setCellValueFactory(c -> {
            Organizer o = c.getValue();
            if (o.getUser() == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(o.getUser().getFirstName() + " " + o.getUser().getLastName());
        });
        orgUsernameColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUser() != null ? c.getValue().getUser().getUsername() : "-"));
        orgOrgNameColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getOrganizationName() != null ? c.getValue().getOrganizationName() : ""));
        orgCommissionColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCommission() != null
                        ? String.format("%.2f%%", c.getValue().getCommission()) : "-"));

        organizersTable.setItems(organizerList);
        organizersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newOrg) -> {
            selectedOrganizer = newOrg;
            if (newOrg != null) {
                selectedOrganizerLabel.setText(newOrg.getUser() != null
                        ? newOrg.getUser().getFirstName() + " " + newOrg.getUser().getLastName() : "-");
                orgNameField.setText(newOrg.getOrganizationName() != null ? newOrg.getOrganizationName() : "");
                orgCommissionField.setText(newOrg.getCommission() != null
                        ? String.valueOf(newOrg.getCommission()) : "");
                orgMessageLabel.setText("");
            } else {
                clearOrganizerSelection();
            }
        });
    }

    private void setupDistributorTable() {
        distNameColumn.setCellValueFactory(c -> {
            Distributor d = c.getValue();
            if (d.getUser() == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(d.getUser().getFirstName() + " " + d.getUser().getLastName());
        });
        distUsernameColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUser() != null ? c.getValue().getUser().getUsername() : "-"));
        distCommissionColumn.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.2f%%", c.getValue().getCommissionRate() * 100)));
        distRatingColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRating() != null
                        ? String.format("%.1f", c.getValue().getRating()) : "Not rated"));

        distributorsTable.setItems(distributorList);
        distributorsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newDist) -> {
            selectedDistributor = newDist;
            if (newDist != null) {
                selectedDistributorLabel.setText(newDist.getUser() != null
                        ? newDist.getUser().getFirstName() + " " + newDist.getUser().getLastName() : "-");
                distCommissionField.setText(String.valueOf(newDist.getCommissionRate()));
                distMessageLabel.setText("");
            } else {
                clearDistributorSelection();
            }
        });
    }

    private void loadData() {
        organizerList.setAll(adminDashboardService.getAllOrganizers());
        distributorList.setAll(adminDashboardService.getAllDistributors());
    }

    // --- Organizer actions ---

    @FXML
    public void handleSaveOrganizer() {
        if (selectedOrganizer == null) {
            orgMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            orgMessageLabel.setText("Select an organizer first.");
            return;
        }
        String orgName = orgNameField.getText().trim();
        String commStr = orgCommissionField.getText().trim();
        Double commission = null;
        if (!commStr.isEmpty()) {
            try {
                commission = Double.parseDouble(commStr);
            } catch (NumberFormatException ex) {
                orgMessageLabel.setStyle("-fx-text-fill: #dc2626;");
                orgMessageLabel.setText("Commission must be a valid number.");
                return;
            }
        }
        try {
            adminDashboardService.updateOrganizerProfile(selectedOrganizer.getId(), orgName, commission);
            orgMessageLabel.setStyle("-fx-text-fill: #059669;");
            orgMessageLabel.setText("Organizer profile updated.");
            loadData();
        } catch (Exception ex) {
            orgMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            orgMessageLabel.setText("Error: " + ex.getMessage());
        }
    }

    @FXML
    public void handleClearOrganizerSelection() {
        organizersTable.getSelectionModel().clearSelection();
        clearOrganizerSelection();
    }

    private void clearOrganizerSelection() {
        selectedOrganizer = null;
        selectedOrganizerLabel.setText("No organizer selected");
        orgNameField.clear();
        orgCommissionField.clear();
        orgMessageLabel.setText("");
    }

    // --- Distributor actions ---

    @FXML
    public void handleSaveDistributor() {
        if (selectedDistributor == null) {
            distMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            distMessageLabel.setText("Select a distributor first.");
            return;
        }
        String rateStr = distCommissionField.getText().trim();
        if (rateStr.isEmpty()) {
            distMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            distMessageLabel.setText("Commission rate is required.");
            return;
        }
        double rate;
        try {
            rate = Double.parseDouble(rateStr);
            if (rate < 0 || rate > 1) {
                distMessageLabel.setStyle("-fx-text-fill: #dc2626;");
                distMessageLabel.setText("Rate must be between 0 and 1 (e.g. 0.10 = 10%).");
                return;
            }
        } catch (NumberFormatException ex) {
            distMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            distMessageLabel.setText("Commission rate must be a valid number.");
            return;
        }
        try {
            adminDashboardService.updateDistributorCommission(selectedDistributor.getId(), rate);
            distMessageLabel.setStyle("-fx-text-fill: #059669;");
            distMessageLabel.setText("Commission rate updated.");
            loadData();
        } catch (Exception ex) {
            distMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            distMessageLabel.setText("Error: " + ex.getMessage());
        }
    }

    @FXML
    public void handleClearDistributorSelection() {
        distributorsTable.getSelectionModel().clearSelection();
        clearDistributorSelection();
    }

    private void clearDistributorSelection() {
        selectedDistributor = null;
        selectedDistributorLabel.setText("No distributor selected");
        distCommissionField.clear();
        distMessageLabel.setText("");
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    @FXML
    public void handleBack() {
        if (onBack != null) onBack.run();
    }
}

