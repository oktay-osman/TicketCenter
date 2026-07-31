package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.service.EventService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class AdminEventsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> nameColumn;
    @FXML private TableColumn<Event, String> categoryColumn;
    @FXML private TableColumn<Event, String> organizerColumn;
    @FXML private TableColumn<Event, String> dateColumn;
    @FXML private TableColumn<Event, String> statusColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, String> capacityColumn;
    @FXML private TextField searchField;
    @FXML private Label totalEventsLabel;

    private final EventService eventService;
    private final ObservableList<Event> masterEvents = FXCollections.observableArrayList();
    private final FilteredList<Event> filteredEvents = new FilteredList<>(masterEvents, e -> true);
    private final SortedList<Event> sortedEvents = new SortedList<>(filteredEvents);

    private Runnable onBack;

    @Autowired
    public AdminEventsController(EventService eventService) {
        this.eventService = eventService;
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    public void initialize() {
        setupColumns();
        sortedEvents.comparatorProperty().bind(eventsTable.comparatorProperty());
        eventsTable.setItems(sortedEvents);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        loadEvents();
    }

    private void setupColumns() {
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        categoryColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategory() != null ? c.getValue().getCategory().toString() : ""));
        organizerColumn.setCellValueFactory(c -> {
            Event e = c.getValue();
            if (e.getOrganizer() != null && e.getOrganizer().getUser() != null
                    && e.getOrganizer().getUser().getUsername() != null
                    && !e.getOrganizer().getUser().getUsername().isBlank()) {
                return new SimpleStringProperty(e.getOrganizer().getUser().getUsername());
            }
            if (e.getOrganizerLegacyId() != null) {
                return new SimpleStringProperty("Organizer #" + e.getOrganizerLegacyId());
            }
            return new SimpleStringProperty("-");
        });
        dateColumn.setCellValueFactory(c -> {
            Event e = c.getValue();
            return new SimpleStringProperty(e.getEventDate() != null ? e.getEventDate().format(DATE_FMT) : "TBA");
        });
        statusColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus() != null ? c.getValue().getStatus().toString() : ""));
        locationColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));
        capacityColumn.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getCapacity())));
    }

    private void loadEvents() {
        List<Event> events = eventService.getAllEventsWithOrganizerUser();
        masterEvents.setAll(events);
        applyFilter();
    }

    private void applyFilter() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filteredEvents.setPredicate(e -> {
            if (text.isEmpty()) return true;
            return contains(e.getName(), text)
                    || contains(e.getLocation(), text)
                    || (e.getCategory() != null && contains(e.getCategory().toString(), text))
                    || (e.getStatus() != null && contains(e.getStatus().toString(), text))
                    || (e.getOrganizer() != null && e.getOrganizer().getUser() != null
                        && contains(e.getOrganizer().getUser().getUsername(), text))
                    || (e.getOrganizerLegacyId() != null && contains(String.valueOf(e.getOrganizerLegacyId()), text));
        });
        totalEventsLabel.setText("Total: " + filteredEvents.size());
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    @FXML
    public void handleRefresh() {
        searchField.clear();
        loadEvents();
    }

    @FXML
    public void handleBack() {
        if (onBack != null) onBack.run();
    }
}
