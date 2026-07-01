package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.Event;
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
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javafx.collections.FXCollections;

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

        // Populate genre filter dynamically from database events
        populateGenreFilter();

        loadEvents();

        // Add dynamic search listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> performSearch());

        // Add category filter listener for dynamic updates
        genreFilter.valueProperty().addListener((observable, oldValue, newValue) -> performSearch());
    }

    private void populateGenreFilter() {
        // Get all unique categories from events
        List<Event> allEvents = eventService.getAllEvents();
        Set<String> uniqueCategories = new TreeSet<>();
        uniqueCategories.add("All"); // Always add "All" as first option

        for (Event event : allEvents) {
            if (event.getCategory() != null) {
                uniqueCategories.add(event.getCategory().toString());
            }
        }

        // Set the items in the ComboBox
        genreFilter.setItems(FXCollections.observableArrayList(uniqueCategories));
        genreFilter.setValue("All"); // Set default to "All"
    }



    private void performSearch() {
        String searchQuery = searchField.getText().trim().toLowerCase();
        String selectedGenre = genreFilter.getValue();
        if (selectedGenre == null) {
            selectedGenre = "All";
        }

        eventsTilePane.getChildren().clear();
        if (searchQuery.isEmpty() && selectedGenre.equals("All")) {
            loadEvents();
        } else {
            loadFilteredEvents(searchQuery, selectedGenre);
        }
    }

    private void loadFilteredEvents(String searchQuery, String selectedGenre) {
        eventsTilePane.getChildren().clear();
        List<Event> allEvents = eventService.getAllEvents();
        for (Event event : allEvents) {
            // Search in event name and description
            boolean matchesSearch = searchQuery.isEmpty() ||
                    event.getName().toLowerCase().contains(searchQuery) ||
                    (event.getDescription() != null && event.getDescription().toLowerCase().contains(searchQuery));

            // Filter by genre/category
            String eventCategoryStr = event.getCategory() != null ? event.getCategory().toString() : "";
            boolean matchesGenre = selectedGenre.equals("All") || eventCategoryStr.equals(selectedGenre);

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
        // Card container with fixed width and height for consistent layout
        VBox eventCard = new VBox(8);
        eventCard.setAlignment(Pos.TOP_LEFT);
        eventCard.setPrefWidth(240);
        eventCard.setPrefHeight(360);
        eventCard.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-background-color: #fff; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Image (thumbnail)
        ImageView imageView = new ImageView();
        imageView.setFitWidth(220);
        imageView.setFitHeight(130);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        try {
            String imagePath = event.getImagePath();
            if (imagePath != null && !imagePath.isBlank()) {
                String resolved = resolveImageUrl(imagePath);
                imageView.setImage(new Image(resolved, 220, 130, true, true));
            } else {
                imageView.setImage(new Image("https://via.placeholder.com/220x130.png?text=Event+Image", 220, 130, true, true));
            }
        } catch (Exception ex) {
            imageView.setImage(new Image("https://via.placeholder.com/220x130.png?text=Event+Image", 220, 130, true, true));
        }

        // Title
        Label titleLabel = new Label(event.getName() != null ? event.getName() : "Untitled Event");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(220);
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Category / meta
        String categoryStr = event.getCategory() != null ? event.getCategory().toString() : "Unknown";
        Label categoryLabel = new Label(categoryStr);
        categoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Short description (preview)
        String fullDesc = event.getDescription() != null ? event.getDescription() : "No description available";
        String descPreview = fullDesc.length() > 120 ? fullDesc.substring(0, 117).trim() + "..." : fullDesc;
        Label descriptionLabel = new Label(descPreview);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(220);
        descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #444;");

        // View details button (consistent across cards)
        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setPrefWidth(200);
        viewDetailsButton.setOnAction(e -> openEventDetails(event));

        // Wrap button in HBox to center it
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().add(viewDetailsButton);

        // Spacer to push the button to the bottom so all cards align
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        eventCard.getChildren().addAll(imageView, titleLabel, categoryLabel, descriptionLabel, spacer, buttonContainer);
        return eventCard;
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

    private void openEventDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/event_details.fxml"));
            loader.setControllerFactory(SpringContext::getBean);
            Parent root = loader.load();

            EventDetailsController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle(event.getName());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
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