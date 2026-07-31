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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DistributorSalesHistoryController {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    @FXML private TableView<TicketSale> salesTable;
    @FXML private TableColumn<TicketSale, String> dateColumn;
    @FXML private TableColumn<TicketSale, String> eventColumn;
    @FXML private TableColumn<TicketSale, String> buyerFirstNameColumn;
    @FXML private TableColumn<TicketSale, String> buyerLastNameColumn;
    @FXML private TableColumn<TicketSale, String> buyerEmailColumn;
    @FXML private TableColumn<TicketSale, String> totalAmountColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventFilterComboBox;
    @FXML private Label totalSalesCountLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgSaleValueLabel;
    @FXML private Label totalTicketsLabel;

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
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt().toLocalDate().toString()));

        eventColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEvent().getName()));

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
    }

    private void applyFilter() {
        filteredSales.setPredicate(sale -> {
            String searchText = searchField.getText().toLowerCase(Locale.ROOT);
            String eventFilter = eventFilterComboBox.getValue();

            boolean matchesSearch = searchText.isEmpty() ||
                    sale.getBuyerFirstName().toLowerCase(Locale.ROOT).contains(searchText) ||
                    sale.getBuyerLastName().toLowerCase(Locale.ROOT).contains(searchText) ||
                    sale.getBuyerEmail().toLowerCase(Locale.ROOT).contains(searchText);

            boolean matchesEventFilter = eventFilter == null || eventFilter.isEmpty() ||
                    sale.getEvent().getName().equals(eventFilter);

            return matchesSearch && matchesEventFilter;
        });

        updateSummary();
    }

    private void loadSales() {
        masterSales.clear();
        List<TicketSale> sales = distributorService.getDistributorSales(currentDistributor);
        masterSales.addAll(sales);


        updateSummary();
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

        BigDecimal avgSaleValue = totalCount > 0 ? totalRevenue.divide(new BigDecimal(totalCount), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        avgSaleValueLabel.setText(String.format("€%.2f", avgSaleValue));

        int totalTickets = displayedSales.stream()
                .flatMap(sale -> sale.getItems().stream())
                .mapToInt(item -> item.getQuantity())
                .sum();
        totalTicketsLabel.setText(String.valueOf(totalTickets));
    }

    @FXML
    public void handleRefresh() {
        masterSales.clear();
        eventFilterComboBox.getItems().clear();
        searchField.clear();
        loadSales();
        loadEventFilter();
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) salesTable.getScene().getWindow();
        stage.close();
    }
}
