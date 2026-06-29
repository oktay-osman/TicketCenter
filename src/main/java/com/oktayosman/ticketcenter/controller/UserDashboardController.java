package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventCategory;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.service.EventService;
import com.oktayosman.ticketcenter.util.SessionManager;
import com.oktayosman.ticketcenter.util.SpringContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;

@Controller
public class UserDashboardController {
    @FXML
    public Button cartButton;
    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> genreFilter;

    @FXML
    private TilePane eventsTilePane;

    @FXML
    private Label greetingLabel;

    @FXML
    private Button logoutButton;

    private final EventService eventService;

    public UserDashboardController(EventService eventService) {
        this.eventService = eventService;
    }

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            greetingLabel.setText("Hello, " + currentUser.getUsername());
        } else {
            greetingLabel.setText("Hello, Guest");
        }
        loadEvents();
    }

    @FXML
    private void handleFilterAction() {
        String searchQuery = searchField.getText().trim().toLowerCase();
        String selectedGenre = genreFilter.getValue();
        if (selectedGenre == null) {
            selectedGenre = "All";
        }
        selectedGenre = selectedGenre.trim().toLowerCase();

        eventsTilePane.getChildren().clear();
        if (searchQuery.isEmpty() && selectedGenre.isEmpty() || selectedGenre.equals("all")) {
            loadEvents();
        } else {
            loadFilteredEvents(searchQuery, selectedGenre);
        }
    }

    private void loadFilteredEvents(String searchQuery, String selectedGenre) {
        eventsTilePane.getChildren().clear();
        List<Event> allEvents = eventService.getAllEvents();
        for (Event event : allEvents) {
            boolean matchesSearch = searchQuery.isEmpty() || event.getName().toLowerCase().contains(searchQuery)
                    || event.getDescription().toLowerCase().contains(searchQuery);
            String eventCategoryStr = event.getCategory() != null ? event.getCategory().toString().toLowerCase() : "";
            boolean matchesGenre = selectedGenre.isEmpty() || selectedGenre.equals("all")
                    || eventCategoryStr.equals(selectedGenre);
            if (matchesSearch && matchesGenre) {
                VBox eventCard = createEventCard(event);
                eventsTilePane.getChildren().add(eventCard);
            }
        }
    }

    private void loadEvents() {
        eventsTilePane.getChildren().clear();
        List<Event> events = eventService.getAllEvents();
        for (Event event : events) {
            VBox eventCard = createEventCard(event);
            eventsTilePane.getChildren().add(eventCard);
        }
    }

    private VBox createEventCard(Event event) {
        VBox eventCard = new VBox(10);
        eventCard.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-background-color: #f9f9f9; -fx-border-radius: 5; -fx-background-radius: 5;");

        ImageView imageView;
        try {
            String imagePath = event.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Handle both file URLs and HTTP URLs
                if (!imagePath.startsWith("http")) {
                    imagePath = "file://" + imagePath;
                }
                imageView = new ImageView(new Image(imagePath, 200, 150, true, true));
            } else {
                // Use a placeholder image
                imageView = new ImageView(new Image("https://via.placeholder.com/200x150.png?text=Event+Image", 200, 150, true, true));
            }
        } catch (Exception ex) {
            imageView = new ImageView();
            imageView.setFitWidth(200);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(true);
        }

        Label titleLabel = new Label(event.getName());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        String categoryStr = event.getCategory() != null ? event.getCategory().toString() : "Unknown";
        Label categoryLabel = new Label(categoryStr);
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        Label descriptionLabel = new Label(event.getDescription() != null ? event.getDescription() : "No description available");
        descriptionLabel.setWrapText(true);

        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setOnAction(actionEvent -> handleViewDetails(new ActionEvent()));

        eventCard.getChildren().addAll(imageView, titleLabel, categoryLabel, descriptionLabel, viewDetailsButton);
        return eventCard;
    }

    public void handleViewDetails(ActionEvent actionEvent) {
        System.out.println("View Details button clicked.");
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root));
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Stage currentStage = (Stage) logoutButton.getScene().getWindow();
        currentStage.close();
    }

    public void handleCartClick(ActionEvent actionEvent) {
        System.out.println("Cart button clicked.");
    }
}