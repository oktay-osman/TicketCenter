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
        loadPlaceholderEvents();
    }

    @FXML
    private void handleFilterAction() {
        String searchQuery = searchField.getText().trim().toLowerCase();
        String selectedGenre = genreFilter.getValue();

        eventsTilePane.getChildren().clear();

        if (searchQuery.isEmpty() && (selectedGenre == null || selectedGenre.equals("All"))) {
            loadPlaceholderEvents();
        } else {
            loadFilteredPlaceholderEvents(searchQuery, selectedGenre);
        }
    }

    private void loadPlaceholderEvents() {
        eventsTilePane.getChildren().clear();

        for (int i = 1; i <= 3; i++) {
            VBox eventCard = createPlaceholderEventCard("Event " + i, "Category " + i, "Description for Event " + i);
            eventsTilePane.getChildren().add(eventCard);
        }
    }

    private void loadFilteredPlaceholderEvents(String searchQuery, String selectedGenre) {
        for (int i = 1; i <= 3; i++) {
            if (("Event " + i).toLowerCase().contains(searchQuery) || ("Category " + i).equals(selectedGenre)) {
                VBox eventCard = createPlaceholderEventCard("Event " + i, "Category " + i, "Description for Event " + i);
                eventsTilePane.getChildren().add(eventCard);
            }
        }
    }

    private VBox createPlaceholderEventCard(String title, String category, String description) {
        VBox eventCard = new VBox(10);
        eventCard.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-background-color: #f9f9f9; -fx-border-radius: 5; -fx-background-radius: 5;");

        ImageView imageView = new ImageView(new Image("https://via.placeholder.com/200x150.png?text=Event+Image"));
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label categoryLabel = new Label(category);
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);

        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setOnAction(event -> {
            System.out.println("View Details clicked for " + title);
        });

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
}