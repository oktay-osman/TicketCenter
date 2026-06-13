package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventCategory;
import com.oktayosman.ticketcenter.model.EventStatus;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.service.EventService;
import com.oktayosman.ticketcenter.repository.UserRepository;
import com.oktayosman.ticketcenter.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class OrganizerDashboardController {

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
    @FXML private Button submitButton;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.oktayosman.ticketcenter.service.OrganizerService organizerService;

    private File selectedImage;

    @FXML
    public void initialize() {

        // Populate enums and set sensible defaults
        categoryCombo.getItems().setAll(EventCategory.values());
        statusCombo.getItems().setAll(EventStatus.values());

        // Select defaults: first category and DRAFT status
        categoryCombo.getSelectionModel().selectFirst();
        statusCombo.getSelectionModel().select(EventStatus.DRAFT);

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
    }

    private void handleCreateEvent() {
        try {
            String name = nameField.getText();
            EventCategory category = categoryCombo.getValue();
            int ticketLimit = Integer.parseInt(ticketLimitField.getText());
            var localDate = eventDatePicker.getValue();
            LocalDateTime date = localDate != null ? localDate.atStartOfDay() : null;
            String location = locationField.getText();
            String description = descriptionArea.getText();
            EventStatus status = statusCombo.getValue();
            int capacity = Integer.parseInt(capacityField.getText());

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

                    // store relative path (uploads/events/filename)
                    imagePath = uploadsDir.resolve(filename).toString();
                } catch (IOException io) {
                    throw new RuntimeException("Failed to save uploaded image", io);
                }
            }

            // Basic validation
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
            if (category == null) throw new IllegalArgumentException("Category is required");
            if (status == null) throw new IllegalArgumentException("Status is required");
            if (date == null) throw new IllegalArgumentException("Event date is required");

            // Ensure an organizer is logged in; try to resolve Organizer entity from session
            var user = SessionManager.getCurrentUser();
            Organizer organizer = null;
            if (user instanceof Organizer) {
                organizer = (Organizer) user;
            } else if (user != null) {
                // try to reload from repository and cast
                organizer = userRepository.findById(user.getId()).filter(u -> u instanceof Organizer).map(u -> (Organizer) u).orElse(null);
            }

            if (organizer == null) {
                throw new IllegalStateException("Only organizers can create events. No organizer is logged in.");
            }

            // Build Event and save
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

            // debug: ensure organizer id and type
            System.out.println("[DEBUG] current session user id=" + user.getId() + " class=" + user.getClass().getName());
            System.out.println("[DEBUG] resolved organizer id=" + organizer.getId() + " class=" + organizer.getClass().getName());

            // resolve legacy organizer_id from organizers table (if present) and set on event
            Integer legacyId = organizerService.findLegacyOrganizerIdByUserId(user.getId());
            if (legacyId != null) {
                event.setOrganizerLegacyId(legacyId);
                System.out.println("[DEBUG] resolved legacy organizer_id=" + legacyId);
            } else {
                System.out.println("[DEBUG] no legacy organizer_id found for user=" + user.getId());
            }

            Event saved = eventService.createEvent(event, organizer);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setContentText("Event created successfully (ID: " + saved.getId() + ")");
            success.showAndWait();

            // clear form
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

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Invalid input: " + ex.getMessage());
            alert.show();
        }
    }
}