package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;

@Controller
public class EventDetailsController {
    @FXML
    private ImageView eventImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label capacityLabel;
    @FXML
    private Label ticketLimitLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Button closeButton;
    @FXML
    private Button addToCartButton;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Event event;

    public void setEvent(Event event) {
        this.event = event;
        populate();
    }

    private void populate() {
        if (event == null) return;

        titleLabel.setText(event.getName());
        categoryLabel.setText(event.getCategory() != null ? event.getCategory().toString() : "Unknown");
        locationLabel.setText(event.getLocation() != null ? event.getLocation() : "");
        dateLabel.setText(event.getEventDate() != null ? event.getEventDate().format(DATE_TIME_FORMATTER) : "TBA");
        capacityLabel.setText(String.valueOf(event.getCapacity()));
        ticketLimitLabel.setText(String.valueOf(event.getTicketLimit()));
        descriptionLabel.setText(event.getDescription() != null ? event.getDescription() : "No description available");

        try {
            String imagePath = event.getImagePath();
            Image img;
            if (imagePath != null && !imagePath.isBlank()) {
                String resolved = resolveImageUrl(imagePath);
                img = new Image(resolved, 400, 300, true, true);
            } else {
                img = new Image("https://via.placeholder.com/400x300.png?text=Event+Image", 400, 300, true, true);
            }
            eventImageView.setImage(img);
        } catch (Exception ex) {
            // leave empty image view
        }

        // wire up buttons
        closeButton.setOnAction(e -> {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        });

        addToCartButton.setOnAction(e -> {
            // TODO: implement add-to-cart flow later. For now show a placeholder message.
            System.out.println("Add to cart clicked for event id=" + (event.getId() != null ? event.getId() : "<new>"));
        });
    }

    private String resolveImageUrl(String imagePath) {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        java.io.File f = new java.io.File(imagePath);
        if (!f.isAbsolute()) {
            f = new java.io.File(System.getProperty("user.dir"), imagePath);
        }
        return f.toURI().toString();
    }

}

