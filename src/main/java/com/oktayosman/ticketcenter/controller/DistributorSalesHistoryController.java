package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.TicketSale;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.DistributorService;
import com.oktayosman.ticketcenter.service.EventService;
import com.oktayosman.ticketcenter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DistributorSalesHistoryController {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    @FXML private TableView<TicketSale> salesTable;
    @FXML private TableColumn<TicketSale, String> dateColumn;
    @FXML private TableColumn<TicketSale, String> eventColumn;
    @FXML private TableColumn<TicketSale, String> eventCategoryColumn;
    @FXML private TableColumn<TicketSale, String> buyerFirstNameColumn;
    @FXML private TableColumn<TicketSale, String> buyerLastNameColumn;
    @FXML private TableColumn<TicketSale, String> buyerEmailColumn;
    @FXML private TableColumn<TicketSale, String> totalAmountColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventFilterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label ratingLabel;
    @FXML private Label totalSalesCountLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgSaleValueLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private ListView<String> categoryBreakdownList;

    private final DistributorService distributorService;
    private final EventService eventService;
    private Distributor currentDistributor;
    private final ObservableList<TicketSale> masterSales = FXCollections.observableArrayList();
    private final FilteredList<TicketSale> filteredSales = new FilteredList<>(masterSales, sale -> true);
    private final SortedList<TicketSale> sortedSales = new SortedList<>(filteredSales);

    @Autowired
    public DistributorSalesHistoryController(DistributorService distributorService,
                                             EventService eventService) {
        this.distributorService = distributorService;
        this.eventService = eventService;
    }

    @FXML
    public void initialize() {
        resetViewState();

        User sessionUser = SessionManager.getCurrentUser();
        if (sessionUser != null && sessionUser.getRole() != null
                && DISTRIBUTOR_ROLE_NAME.equals(sessionUser.getRole().getName())) {
            currentDistributor = distributorService.getDistributorByUserId(sessionUser.getId())
                    .orElse(null);
        }

        setupTableColumns();
        setupFilters();

        if (currentDistributor != null) {
            loadSales();
            loadRating();
        }
        loadEventFilter();
    }

    private void resetViewState() {
        currentDistributor = null;
        masterSales.clear();
        filteredSales.setPredicate(sale -> true);

        if (searchField != null) searchField.clear();
        if (eventFilterComboBox != null) {
            eventFilterComboBox.getItems().clear();
            eventFilterComboBox.setValue(null);
        }
        if (fromDatePicker != null) fromDatePicker.setValue(null);
        if (toDatePicker != null) toDatePicker.setValue(null);
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt().toLocalDate().toString()));

        eventColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEvent().getName()));

        eventCategoryColumn.setCellValueFactory(cellData -> {
            var category = cellData.getValue().getEvent().getCategory();
            return new SimpleStringProperty(category != null ? category.toString() : "");
        });

        buyerFirstNameColumn.setCellValueFactory(new PropertyValueFactory<>("buyerFirstName"));
        buyerLastNameColumn.setCellValueFactory(new PropertyValueFactory<>("buyerLastName"));
        buyerEmailColumn.setCellValueFactory(new PropertyValueFactory<>("buyerEmail"));

        totalAmountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("€%.2f", cellData.getValue().getTotalAmount())));

        sortedSales.comparatorProperty().bind(salesTable.comparatorProperty());
        salesTable.setItems(sortedSales);
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        eventFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        if (fromDatePicker != null) {
            fromDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        }
        if (toDatePicker != null) {
            toDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        }
    }

    private void applyFilter() {
        filteredSales.setPredicate(sale -> {
            String searchText = searchField.getText().toLowerCase(Locale.ROOT);
            String eventFilter = eventFilterComboBox.getValue();
            LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : null;
            LocalDate to = toDatePicker != null ? toDatePicker.getValue() : null;

            boolean matchesSearch = searchText.isEmpty() ||
                    sale.getBuyerFirstName().toLowerCase(Locale.ROOT).contains(searchText) ||
                    sale.getBuyerLastName().toLowerCase(Locale.ROOT).contains(searchText) ||
                    sale.getBuyerEmail().toLowerCase(Locale.ROOT).contains(searchText);

            boolean matchesEventFilter = eventFilter == null || eventFilter.isEmpty() ||
                    sale.getEvent().getName().equals(eventFilter);

            LocalDate saleDate = sale.getCreatedAt().toLocalDate();
            boolean matchesFrom = from == null || !saleDate.isBefore(from);
            boolean matchesTo = to == null || !saleDate.isAfter(to);

            return matchesSearch && matchesEventFilter && matchesFrom && matchesTo;
        });

        updateSummary();
    }

    private void loadSales() {
        masterSales.clear();
        List<TicketSale> sales = distributorService.getDistributorSales(currentDistributor);
        masterSales.addAll(sales);
        updateSummary();
    }

    private void loadRating() {
        if (ratingLabel == null || currentDistributor == null) return;
        Float rating = currentDistributor.getRating();
        ratingLabel.setText(rating != null ? String.format("%.1f / 5", rating) : "Not rated yet");
    }

    private void loadEventFilter() {
        if (currentDistributor == null) {
            eventFilterComboBox.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Event> assignedEvents = eventService.getEventsByDistributor(currentDistributor);
        List<String> eventNames = assignedEvents.stream()
                .map(Event::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        eventFilterComboBox.setItems(FXCollections.observableArrayList(eventNames));
    }

    private void updateSummary() {
        List<TicketSale> displayedSales = filteredSales.stream().collect(Collectors.toList());

        int totalCount = displayedSales.size();
        totalSalesCountLabel.setText(String.valueOf(totalCount));

        BigDecimal totalRevenue = displayedSales.stream()
                .map(TicketSale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalRevenueLabel.setText(String.format("€%.2f", totalRevenue));

        BigDecimal avgSaleValue = totalCount > 0
                ? totalRevenue.divide(new BigDecimal(totalCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        avgSaleValueLabel.setText(String.format("€%.2f", avgSaleValue));

        int totalTickets = displayedSales.stream()
                .flatMap(sale -> sale.getItems().stream())
                .mapToInt(item -> item.getQuantity())
                .sum();
        totalTicketsLabel.setText(String.valueOf(totalTickets));

        updateCategoryBreakdown(displayedSales);
    }

    private void updateCategoryBreakdown(List<TicketSale> sales) {
        if (categoryBreakdownList == null) return;
        categoryBreakdownList.setItems(FXCollections.observableArrayList(distributorService.getCategoryBreakdownRows(sales)));
    }

    @FXML
    public void handleRefresh() {
        masterSales.clear();
        eventFilterComboBox.getItems().clear();
        searchField.clear();
        if (fromDatePicker != null) fromDatePicker.setValue(null);
        if (toDatePicker != null) toDatePicker.setValue(null);
        loadSales();
        loadEventFilter();
        loadRating();
    }

    @FXML
    public void handleClearFilters() {
        searchField.clear();
        eventFilterComboBox.setValue(null);
        if (fromDatePicker != null) fromDatePicker.setValue(null);
        if (toDatePicker != null) toDatePicker.setValue(null);
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) salesTable.getScene().getWindow();
        stage.close();
    }
}
