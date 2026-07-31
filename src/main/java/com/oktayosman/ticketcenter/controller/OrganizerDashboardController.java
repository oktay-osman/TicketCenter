package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventCategory;
import com.oktayosman.ticketcenter.model.EventStatus;
import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.model.SeatType;
import com.oktayosman.ticketcenter.model.SeatCategory;
import com.oktayosman.ticketcenter.service.DistributorService;
import com.oktayosman.ticketcenter.service.EventService;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.service.OrganizerService;
import com.oktayosman.ticketcenter.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import com.oktayosman.ticketcenter.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.ArrayList;

@Controller
public class OrganizerDashboardController {

    // CREATE EVENT TAB
    @FXML private TextField nameField;
    @FXML private ComboBox<EventCategory> categoryCombo;
    @FXML private TextField ticketLimitField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField locationField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<EventStatus> statusCombo;
    @FXML private TextField capacityField;
    @FXML private Button imageButton;
    @FXML private Label imageLabel;
    @FXML private ListView<Distributor> distributorsListView;
    @FXML private Button submitButton;

    // Ticket types UI
    @FXML private VBox ticketTypesContainer;
    @FXML private VBox ticketTypesEntriesBox;
    @FXML private Button addTicketTypeButton;
    @FXML private Label noTicketTypesLabel;

    // MY EVENTS TAB
    @FXML private TabPane mainTabPane;
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> nameColumn;
    @FXML private TableColumn<Event, String> categoryColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, String> dateColumn;
    @FXML private TableColumn<Event, String> statusColumn;
    @FXML private TableColumn<Event, Integer> capacityColumn;
    @FXML private TableColumn<Event, String> updatedAtColumn;
    @FXML private Button refreshButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button logoutButton;

    // when non-null, submits will update the existing event instead of creating a new one
    private Long editingEventId = null;

    @Autowired
    private EventService eventService;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private OrganizerService organizerService;

    @Autowired
    private DistributorService distributorService;

    private File selectedImage;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final List<Distributor> allDistributors = new ArrayList<>();

    @FXML
    public void initialize() {
        // Populate enums and set sensible defaults
        categoryCombo.getItems().setAll(EventCategory.values());
        statusCombo.getItems().setAll(EventStatus.values());

        categoryCombo.getSelectionModel().selectFirst();
        statusCombo.getSelectionModel().select(EventStatus.DRAFT);

        // Setup image chooser
        imageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );

            Stage stage = (Stage) imageButton.getScene().getWindow();
            selectedImage = fileChooser.showOpenDialog(stage);

            if (selectedImage != null) {
                imageLabel.setText(selectedImage.getName());
            }
        });

        submitButton.setOnAction(e -> handleCreateEvent());

        // Setup ticket types UI
        addTicketTypeButton.setOnAction(e -> addTicketTypeRow(null, null, null));
        updateNoTicketTypesLabel();

        // Load distributor selection options for assignment
        loadDistributors();

        editButton.setOnAction(e -> handleEditSelected());
        deleteButton.setOnAction(e -> handleDeleteSelected());
        logoutButton.setOnAction(e -> handleLogout());

        // Setup table columns for My Events tab
        setupEventsTable();

        // Load events when tab is selected
        mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 1) { // My Events tab
                loadOrganizerEvents();
            }
        });

        // Setup refresh button
        refreshButton.setOnAction(e -> loadOrganizerEvents());
    }

    private void setupEventsTable() {
        // Setup table columns with cell value factories
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        categoryColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory().toString()));
        locationColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLocation()));
        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime eventDate = cellData.getValue().getEventDate();
            String formatted = eventDate != null ? eventDate.format(DATE_TIME_FORMATTER) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));
        capacityColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCapacity()).asObject());
        updatedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime updatedAt = cellData.getValue().getUpdatedAt();
            String formatted = updatedAt != null ? updatedAt.format(DATE_TIME_FORMATTER) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
    }

    private void loadOrganizerEvents() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) {
                showError("No user logged in");
                return;
            }

            Organizer organizer = organizerRepository.findById(user.getId()).orElse(null);
            if (organizer == null) {
                showError("No organizer profile found");
                return;
            }

            List<Event> events = eventService.getEventsByOrganizer(organizer);
            ObservableList<Event> observableEvents = FXCollections.observableArrayList(events);
            eventsTable.setItems(observableEvents);

        } catch (Exception ex) {
            showError("Failed to load events: " + ex.getMessage());
        }
    }

    private void handleCreateEvent() {
        try {
            // Validate required fields
            String name = nameField.getText();
            EventCategory category = categoryCombo.getValue();
            String ticketLimitStr = ticketLimitField.getText();
            var localDate = eventDatePicker.getValue();
            String location = locationField.getText();
            String description = descriptionArea.getText();
            EventStatus status = statusCombo.getValue();
            String capacityStr = capacityField.getText();

            // Build validation error messages
            StringBuilder errors = new StringBuilder();

            if (name == null || name.isBlank()) {
                errors.append("• Event name is required\n");
            }
            if (category == null) {
                errors.append("• Category is required\n");
            }
            if (location == null || location.isBlank()) {
                errors.append("• Location is required\n");
            }
            if (localDate == null) {
                errors.append("• Event date is required\n");
            }
            if (description == null || description.isBlank()) {
                errors.append("• Description is required\n");
            }
            if (status == null) {
                errors.append("• Status is required\n");
            }

            // Validate numeric fields
            int ticketLimit = 0;
            if (ticketLimitStr == null || ticketLimitStr.isBlank()) {
                errors.append("• Ticket limit is required\n");
            } else {
                try {
                    ticketLimit = Integer.parseInt(ticketLimitStr);
                    if (ticketLimit <= 0) {
                        errors.append("• Ticket limit must be greater than 0\n");
                    }
                } catch (NumberFormatException ex) {
                    errors.append("• Ticket limit must be a valid number\n");
                }
            }

            int capacity = 0;
            if (capacityStr == null || capacityStr.isBlank()) {
                errors.append("• Capacity is required\n");
            } else {
                try {
                    capacity = Integer.parseInt(capacityStr);
                    if (capacity <= 0) {
                        errors.append("• Capacity must be greater than 0\n");
                    }
                } catch (NumberFormatException ex) {
                    errors.append("• Capacity must be a valid number\n");
                }
            }

            // Validate and collect seat types
            List<SeatType> seatTypesList = new ArrayList<>();
            if (ticketTypesEntriesBox != null) {
                for (Node child : ticketTypesEntriesBox.getChildren()) {
                    if (child == noTicketTypesLabel) continue;
                    if (child instanceof HBox) {
                        List<Node> rowChildren = ((HBox) child).getChildren();
                        if (rowChildren.size() >= 3 && rowChildren.get(0) instanceof ComboBox && rowChildren.get(1) instanceof TextField && rowChildren.get(2) instanceof TextField) {
                            SeatCategory seatCat = (SeatCategory) ((ComboBox<?>) rowChildren.get(0)).getValue();
                            String priceStr = ((TextField) rowChildren.get(1)).getText();
                            String totalStr = ((TextField) rowChildren.get(2)).getText();

                            if (seatCat == null) {
                                errors.append("• Seat type category is required\n");
                            }
                            if (priceStr == null || priceStr.isBlank()) {
                                errors.append("• Seat type price is required\n");
                            }
                            if (totalStr == null || totalStr.isBlank()) {
                                errors.append("• Total seats is required for seat type\n");
                            }

                            if (seatCat != null && priceStr != null && !priceStr.isBlank() && totalStr != null && !totalStr.isBlank()) {
                                try {
                                    BigDecimal price = new BigDecimal(priceStr);
                                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                                        errors.append("• Price must be non-negative for ").append(seatCat.getDisplayName()).append("\n");
                                    } else {
                                        Integer totalSeats = Integer.parseInt(totalStr);
                                        if (totalSeats < 0) {
                                            errors.append("• Total seats must be non-negative for ").append(seatCat.getDisplayName()).append("\n");
                                        } else {
                                            SeatType st = new SeatType();
                                            st.setSeatCategory(seatCat);
                                            st.setPrice(price);
                                            st.setTotalSeats(totalSeats);
                                            seatTypesList.add(st);
                                        }
                                    }
                                } catch (NumberFormatException ex) {
                                    errors.append("• Invalid price or total seats for ").append(seatCat.getDisplayName()).append("\n");
                                }
                            }
                        }
                    }
                }
            }

            if (!errors.isEmpty()) {
                showError("Please fill in all required fields:\n\n" + errors);
                return;
            }

            LocalDateTime date = localDate.atStartOfDay();

            // Handle image upload
            String imagePath = null;
            if (selectedImage != null) {
                try {
                    Path uploadsDir = Paths.get("uploads", "events");
                    Files.createDirectories(uploadsDir);

                    String original = selectedImage.getName();
                    String ext = "";
                    int idx = original.lastIndexOf('.');
                    if (idx > 0) ext = original.substring(idx);

                    String filename = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;
                    Path target = uploadsDir.resolve(filename);
                    Files.copy(selectedImage.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

                    imagePath = uploadsDir.resolve(filename).toString();
                } catch (IOException io) {
                    throw new RuntimeException("Failed to save uploaded image", io);
                }
            }

            // Ensure organizer is logged in
            User user = SessionManager.getCurrentUser();
            if (user == null) {
                throw new IllegalStateException("No user is logged in.");
            }

            Organizer organizer = organizerRepository.findById(user.getId()).orElse(null);
            if (organizer == null) {
                throw new IllegalStateException("Only organizers can create events. No organizer profile found.");
            }

            // Build and save event
            Event event = new Event();
            event.setName(name);
            event.setCategory(category);
            event.setTicketLimit(ticketLimit);
            event.setEventDate(date);
            event.setLocation(location);
            event.setDescription(description);
            event.setStatus(status);
            event.setCapacity(capacity);
            event.setImagePath(imagePath);

            // Attach seat types to event
            if (!seatTypesList.isEmpty()) {
                for (SeatType st : seatTypesList) {
                    st.setEvent(event);
                }
                event.setSeatTypes(seatTypesList);
            }

            System.out.println("[DEBUG] current session user id=" + user.getId() + " class=" + user.getClass().getName());
            System.out.println("[DEBUG] resolved organizer id=" + organizer.getId() + " class=" + organizer.getClass().getName());

            // Resolve legacy organizer_id
            Integer legacyId = organizerService.findLegacyOrganizerIdByUserId(user.getId());
            if (legacyId != null) {
                event.setOrganizerLegacyId(legacyId);
                System.out.println("[DEBUG] resolved legacy organizer_id=" + legacyId);
            } else {
                System.out.println("[DEBUG] no legacy organizer_id found for user=" + user.getId());
            }

            Event saved;
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Success");
            List<Distributor> selectedDistributors = getSelectedDistributors();

            if (editingEventId == null) {
                saved = eventService.createEvent(event, organizer, selectedDistributors);
                success.setHeaderText("Event Created");
                success.setContentText("Event '" + name + "' created successfully (ID: " + saved.getId() + ")");
            } else {
                saved = eventService.updateEvent(editingEventId, event, organizer, selectedDistributors);
                success.setHeaderText("Event Updated");
                success.setContentText("Event '" + name + "' updated successfully (ID: " + saved.getId() + ")");
                // reset editing state
                editingEventId = null;
                submitButton.setText("Create Event");
            }
            success.showAndWait();

            // Clear form
            clearForm();

            // Reload events in My Events tab if it exists
            loadOrganizerEvents();

        } catch (Exception ex) {
            showError("Error creating event: " + ex.getMessage());
        }
    }

    private void clearForm() {
        nameField.clear();
        categoryCombo.getSelectionModel().selectFirst();
        ticketLimitField.clear();
        eventDatePicker.setValue(null);
        locationField.clear();
        descriptionArea.clear();
        statusCombo.getSelectionModel().select(EventStatus.DRAFT);
        capacityField.clear();
        imageLabel.setText("No file selected");
        selectedImage = null;
        if (distributorsListView != null) {
            distributorsListView.getSelectionModel().clearSelection();
        }
        // Clear seat types
        if (ticketTypesEntriesBox != null) {
            ticketTypesEntriesBox.getChildren().clear();
            updateNoTicketTypesLabel();
        }
        editingEventId = null;
        submitButton.setText("Create Event");
    }

    private void handleEditSelected() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an event to edit.");
            return;
        }

        // populate form fields
        nameField.setText(selected.getName());
        categoryCombo.setValue(selected.getCategory());
        ticketLimitField.setText(String.valueOf(selected.getTicketLimit()));
        if (selected.getEventDate() != null) {
            eventDatePicker.setValue(selected.getEventDate().toLocalDate());
        } else {
            eventDatePicker.setValue(null);
        }
        locationField.setText(selected.getLocation());
        descriptionArea.setText(selected.getDescription());
        statusCombo.setValue(selected.getStatus());
        capacityField.setText(String.valueOf(selected.getCapacity()));
        imageLabel.setText(selected.getImagePath() != null ? java.nio.file.Paths.get(selected.getImagePath()).getFileName().toString() : "No file selected");

        // Populate seat types if any
        ticketTypesEntriesBox.getChildren().clear();
        if (selected.getSeatTypes() != null && !selected.getSeatTypes().isEmpty()) {
            for (SeatType st : selected.getSeatTypes()) {
                addTicketTypeRow(st.getSeatCategory(), st.getPrice() != null ? st.getPrice().toString() : null, st.getTotalSeats() != null ? st.getTotalSeats().toString() : null);
            }
        } else {
            updateNoTicketTypesLabel();
        }

        // Pre-select assigned distributors in edit mode
        if (distributorsListView != null) {
            distributorsListView.getSelectionModel().clearSelection();
            List<Distributor> assigned = eventService.getAssignedDistributors(selected.getId());
            for (Distributor distributor : assigned) {
                for (int i = 0; i < allDistributors.size(); i++) {
                    Distributor available = allDistributors.get(i);
                    if (available.getId() != null && available.getId().equals(distributor.getId())) {
                        distributorsListView.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        }

        // set editing state and switch to create tab
        editingEventId = selected.getId();
        submitButton.setText("Save Changes");
        mainTabPane.getSelectionModel().select(0);
    }

    private void handleDeleteSelected() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an event to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete event '" + selected.getName() + "'? This action cannot be undone.");
        var res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                eventService.deleteEventById(selected.getId());
                loadOrganizerEvents();
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Deleted");
                info.setHeaderText(null);
                info.setContentText("Event deleted successfully.");
                info.showAndWait();
            } catch (Exception ex) {
                showError("Failed to delete event: " + ex.getMessage());
            }
        }
    }

    private void handleLogout() {
        try {
            SessionManager.logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            showError("Failed to load login screen: " + e.getMessage());
        }
    }

    private void addTicketTypeRow(SeatCategory category, String price, String totalSeats) {
        if (ticketTypesEntriesBox == null) return;
        ticketTypesEntriesBox.getChildren().remove(noTicketTypesLabel);

        HBox row = new HBox(8);
        row.setStyle("-fx-padding: 5; -fx-border-color: #ddd; -fx-background-color: #f5f5f5;");

        ComboBox<SeatCategory> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().setAll(SeatCategory.values());
        categoryCombo.setStyle("-fx-min-width: 120;");
        if (category != null) {
            categoryCombo.setValue(category);
        } else {
            categoryCombo.getSelectionModel().selectFirst();
        }

        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        priceField.setPrefWidth(100);
        if (price != null) priceField.setText(price);

        TextField totalSeatsField = new TextField();
        totalSeatsField.setPromptText("Total Seats");
        totalSeatsField.setPrefWidth(100);
        if (totalSeats != null) totalSeatsField.setText(totalSeats);

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-padding: 5;");
        removeBtn.setOnAction(e -> {
            ticketTypesEntriesBox.getChildren().remove(row);
            updateNoTicketTypesLabel();
        });

        row.getChildren().addAll(categoryCombo, priceField, totalSeatsField, removeBtn);
        ticketTypesEntriesBox.getChildren().add(row);
        updateNoTicketTypesLabel();
    }

    private void updateNoTicketTypesLabel() {
        if (ticketTypesEntriesBox == null) return;
        boolean hasRows = ticketTypesEntriesBox.getChildren().stream().anyMatch(n -> n != noTicketTypesLabel);
        if (!hasRows) {
            if (!ticketTypesEntriesBox.getChildren().contains(noTicketTypesLabel)) {
                ticketTypesEntriesBox.getChildren().add(noTicketTypesLabel);
            }
        } else {
            ticketTypesEntriesBox.getChildren().remove(noTicketTypesLabel);
        }
    }

    private void loadDistributors() {
        if (distributorsListView == null) {
            return;
        }

        allDistributors.clear();
        allDistributors.addAll(distributorService.getAllDistributors());

        distributorsListView.setItems(FXCollections.observableArrayList(allDistributors));
        distributorsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        distributorsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Distributor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String fullName = (item.getFirstName() == null ? "" : item.getFirstName())
                            + " "
                            + (item.getLastName() == null ? "" : item.getLastName());
                    setText(fullName.trim() + " (" + item.getUsername() + ")");
                }
            }
        });
    }

    private List<Distributor> getSelectedDistributors() {
        if (distributorsListView == null) {
            return List.of();
        }
        return new ArrayList<>(distributorsListView.getSelectionModel().getSelectedItems());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}