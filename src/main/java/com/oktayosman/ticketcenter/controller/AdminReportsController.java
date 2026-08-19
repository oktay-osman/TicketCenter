package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.service.AdminDashboardService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdminReportsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Events Report
    @FXML private TableView<EventReportRow> eventsTable;
    @FXML private TableColumn<EventReportRow, String> eventNameColumn;
    @FXML private TableColumn<EventReportRow, String> eventCategoryColumn;
    @FXML private TableColumn<EventReportRow, String> eventDateColumn;
    @FXML private TableColumn<EventReportRow, String> eventStatusColumn;
    @FXML private TableColumn<EventReportRow, String> eventLocationColumn;
    @FXML private TableColumn<EventReportRow, String> eventTicketsSoldColumn;
    @FXML private TableColumn<EventReportRow, String> eventRevenueColumn;
    @FXML private Label eventsTotalTicketsLabel;
    @FXML private Label eventsTotalRevenueLabel;

    // Distributors Report
    @FXML private TableView<DistributorReportRow> distributorsTable;
    @FXML private TableColumn<DistributorReportRow, String> distNameColumn;
    @FXML private TableColumn<DistributorReportRow, String> distUsernameColumn;
    @FXML private TableColumn<DistributorReportRow, String> distCommissionColumn;
    @FXML private TableColumn<DistributorReportRow, String> distRatingColumn;
    @FXML private TableColumn<DistributorReportRow, String> distTicketsSoldColumn;
    @FXML private TableColumn<DistributorReportRow, String> distRevenueColumn;

    private final AdminDashboardService adminDashboardService;
    private Runnable onBack;

    @Autowired
    public AdminReportsController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    public void initialize() {
        setupEventsTable();
        setupDistributorsTable();
        loadData();
    }

    private void setupEventsTable() {
        eventNameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        eventCategoryColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        eventDateColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));
        eventStatusColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        eventLocationColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocation()));
        eventTicketsSoldColumn.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTicketsSold())));
        eventRevenueColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRevenue()));
    }

    private void setupDistributorsTable() {
        distNameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        distUsernameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        distCommissionColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCommission()));
        distRatingColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRating()));
        distTicketsSoldColumn.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTicketsSold())));
        distRevenueColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRevenue()));
    }

    private void loadData() {
        loadEventsReport();
        loadDistributorsReport();
    }

    private void loadEventsReport() {
        List<Event> events = adminDashboardService.getAllEvents();
        List<EventReportRow> rows = new ArrayList<>();
        long totalTickets = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Event event : events) {
            long tickets = adminDashboardService.getTicketsSoldForEvent(event);
            BigDecimal revenue = adminDashboardService.getRevenueForEvent(event);
            rows.add(new EventReportRow(event, tickets, revenue));
            totalTickets += tickets;
            totalRevenue = totalRevenue.add(revenue);
        }

        eventsTable.setItems(FXCollections.observableArrayList(rows));
        if (eventsTotalTicketsLabel != null) eventsTotalTicketsLabel.setText(String.valueOf(totalTickets));
        if (eventsTotalRevenueLabel != null) eventsTotalRevenueLabel.setText(String.format("€%.2f", totalRevenue));
    }

    private void loadDistributorsReport() {
        List<Distributor> distributors = adminDashboardService.getAllDistributors();
        List<DistributorReportRow> rows = new ArrayList<>();

        for (Distributor distributor : distributors) {
            long tickets = adminDashboardService.getTicketsSoldForDistributor(distributor);
            BigDecimal revenue = adminDashboardService.getRevenueForDistributor(distributor);
            rows.add(new DistributorReportRow(distributor, tickets, revenue));
        }

        distributorsTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void handleBack() {
        if (onBack != null) onBack.run();
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    // ---- Inner data classes ----

    public class EventReportRow {
        private final String name;
        private final String category;
        private final String date;
        private final String status;
        private final String location;
        private final long ticketsSold;
        private final String revenue;

        public EventReportRow(Event event, long ticketsSold, BigDecimal revenue) {
            this.name = event.getName() != null ? event.getName() : "";
            this.category = event.getCategory() != null ? event.getCategory().toString() : "";
            this.date = event.getEventDate() != null ? event.getEventDate().format(DATE_FMT) : "";
            this.status = event.getStatus() != null ? event.getStatus().toString() : "";
            this.location = event.getLocation() != null ? event.getLocation() : "";
            this.ticketsSold = ticketsSold;
            this.revenue = String.format("€%.2f", revenue);
        }

        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
        public String getLocation() { return location; }
        public long getTicketsSold() { return ticketsSold; }
        public String getRevenue() { return revenue; }
    }

    public class DistributorReportRow {
        private final String name;
        private final String username;
        private final String commission;
        private final String rating;
        private final long ticketsSold;
        private final String revenue;

        public DistributorReportRow(Distributor distributor, long ticketsSold, BigDecimal revenue) {
            String firstName = distributor.getFirstName() != null ? distributor.getFirstName() : "";
            String lastName = distributor.getLastName() != null ? distributor.getLastName() : "";
            this.name = (firstName + " " + lastName).trim();
            this.username = distributor.getUsername() != null ? distributor.getUsername() : "";
            this.commission = String.format("%.2f%%", distributor.getCommissionRate() * 100);
            Float r = distributor.getRating();
            this.rating = r != null ? String.format("%.1f / 5", r) : "—";
            this.ticketsSold = ticketsSold;
            this.revenue = String.format("€%.2f", revenue);
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getCommission() { return commission; }
        public String getRating() { return rating; }
        public long getTicketsSold() { return ticketsSold; }
        public String getRevenue() { return revenue; }
    }
}
