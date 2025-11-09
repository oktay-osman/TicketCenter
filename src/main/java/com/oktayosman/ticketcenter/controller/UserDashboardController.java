package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.model.User;
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
        String selectedGenre = genreFilter.getValue().trim().toLowerCase();

        eventsTilePane.getChildren().clear();
        if (searchQuery.isEmpty() && (selectedGenre.isEmpty() || selectedGenre.equals("all"))) {
            loadEvents();
        } else {
            loadFilteredPlaceholderEvents(searchQuery, selectedGenre);
        }
    }

    private void loadFilteredPlaceholderEvents(String searchQuery, String selectedGenre) {
        eventsTilePane.getChildren().clear();
        for (MockEvent e : getMockEvents()) {
            boolean matchesSearch = searchQuery.isEmpty() || e.getTitle().toLowerCase().contains(searchQuery);
            boolean matchesGenre = (selectedGenre == null || selectedGenre.equalsIgnoreCase("All")
                    || e.getCategory().equalsIgnoreCase(selectedGenre));
            if (matchesSearch && matchesGenre) {
                VBox eventCard = createEventCard(e.getTitle(), e.getCategory(), e.getDescription(), e.getImageUrl());
                eventsTilePane.getChildren().add(eventCard);
            }
        }
    }

    private void loadEvents() {
        eventsTilePane.getChildren().clear();
        for (MockEvent e : getMockEvents()) {
            VBox eventCard = createEventCard(e.getTitle(), e.getCategory(), e.getDescription(), e.getImageUrl());
            eventsTilePane.getChildren().add(eventCard);
        }
    }

    private VBox createEventCard(String title, String category, String description, String imageUrl) {
        VBox eventCard = new VBox(10);
        eventCard.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-background-color: #f9f9f9; -fx-border-radius: 5; -fx-background-radius: 5;");

        ImageView imageView;
        try {
            imageView = new ImageView(new Image(imageUrl, 200, 150, true, true));
        } catch (Exception ex) {
            imageView = new ImageView();
            imageView.setFitWidth(200);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(true);
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label categoryLabel = new Label(category);
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);

        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setOnAction(event -> handleViewDetails(new ActionEvent()));

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

    private java.util.List<MockEvent> getMockEvents() {
        java.util.List<MockEvent> events = new java.util.ArrayList<>();
        events.add(new MockEvent("Musical Night", "Musicals", "A fantastic evening of music.", "https://via.placeholder.com/200x150.png?text=Musical+Night"));
        events.add(new MockEvent("Rock Concert", "Concerts", "Loud guitars and bright lights.", "https://via.placeholder.com/200x150.png?text=Rock+Concert"));
        events.add(new MockEvent("Boxing Match", "Fights", "High-energy title fight.", "https://via.placeholder.com/200x150.png?text=Boxing+Match"));
        events.add(new MockEvent("Shakespeare Play", "Theater", "Classic drama on stage.", "https://via.placeholder.com/200x150.png?text=Shakespeare+Play"));
        return events;
    }

    private static class MockEvent {
        private final String title;
        private final String category;
        private final String description;
        private final String imageUrl;

        MockEvent(String title, String category, String description, String imageUrl) {
            this.title = title;
            this.category = category;
            this.description = description;
            this.imageUrl = imageUrl;
        }

        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
    }
}