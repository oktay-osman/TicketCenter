package com.oktayosman.ticketcenter.controller;

import com.oktayosman.ticketcenter.exception.TicketInventoryException;
import com.oktayosman.ticketcenter.model.*;
import com.oktayosman.ticketcenter.service.DistributorService;
import com.oktayosman.ticketcenter.service.EventService;
import com.oktayosman.ticketcenter.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DistributorCreateSaleController {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> eventComboBox;
    @FXML private VBox ticketItemsContainer;
    @FXML private Label totalAmountLabel;
    @FXML private Label messageLabel;

    private final DistributorService distributorService;
    private final EventService eventService;
    private Distributor currentDistributor;
    private Map<String, Event> eventMap = new HashMap<>();
    private List<TicketItemRow> ticketItemRows = new ArrayList<>();

    @Autowired
    public DistributorCreateSaleController(DistributorService distributorService,
                                           EventService eventService) {
        this.distributorService = distributorService;
        this.eventService = eventService;
    }

    @FXML
    public void initialize() {
        User sessionUser = SessionManager.getCurrentUser();
        if (sessionUser == null) {
            showErrorMessage("You are not logged in. Please log in again.");
        } else if (sessionUser.getRole() == null || !DISTRIBUTOR_ROLE_NAME.equals(sessionUser.getRole().getName())) {
            showErrorMessage("Your account no longer has distributor access. Please log in again.");
        } else {
            currentDistributor = distributorService.getDistributorByUserId(sessionUser.getId())
                    .orElse(null);
        }
        if (currentDistributor == null) {
            showErrorMessage("Distributor account not found. Please log in again.");
        }
        loadEvents();
        addMessageListener();
    }

    private void loadEvents() {
        if (currentDistributor == null) {
            eventMap = new HashMap<>();
            eventComboBox.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Event> assignedEvents = eventService.getEventsByDistributor(currentDistributor);

        eventMap = assignedEvents.stream()
                .collect(Collectors.toMap(Event::getName, event -> event, (left, right) -> left));

        List<String> eventNames = assignedEvents.stream()
                .map(Event::getName)
                .collect(Collectors.toList());

        eventComboBox.setItems(FXCollections.observableArrayList(eventNames));

        if (eventNames.isEmpty()) {
            showErrorMessage("No events are assigned to your distributor account yet.");
        }
    }

    @FXML
    public void handleAddTicketType() {
        if (eventComboBox.getValue() == null) {
            showErrorMessage("Please select an event first");
            return;
        }

        Event selectedEvent = eventMap.get(eventComboBox.getValue());
        if (selectedEvent == null
                || selectedEvent.getSeatTypes() == null
                || selectedEvent.getSeatTypes().isEmpty()) {
            showErrorMessage("No ticket types available for this event. Please add seat types to the event first.");
            return;
        }

        TicketItemRow row = new TicketItemRow(selectedEvent, this);
        ticketItemRows.add(row);
        ticketItemsContainer.getChildren().add(row.getContainer());
    }

    @FXML
    public void handleSave() {
        if (currentDistributor == null) {
            showErrorMessage("Distributor session is invalid. Please close and log in again.");
            return;
        }
        if (!validateForm()) {
            return;
        }

        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            Event selectedEvent = eventMap.get(eventComboBox.getValue());

            // Create ticket sale
            TicketSale ticketSale = new TicketSale(selectedEvent, currentDistributor,
                    firstName, lastName, email, BigDecimal.ZERO);

            // Add items to sale
            List<TicketSaleItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (TicketItemRow row : ticketItemRows) {
                if (row.getQuantity() > 0) {
                    SeatType seatType = row.getSelectedSeatType();
                    Integer quantity = row.getQuantity();
                    BigDecimal unitPrice = seatType.getPrice();
                    BigDecimal subtotal = unitPrice.multiply(new BigDecimal(quantity));

                    TicketSaleItem item = new TicketSaleItem(ticketSale, seatType, quantity, unitPrice, subtotal);
                    items.add(item);
                    totalAmount = totalAmount.add(subtotal);
                }
            }

            if (items.isEmpty()) {
                showErrorMessage("Please add at least one ticket");
                return;
            }

            ticketSale.setItems(items);
            ticketSale.setTotalAmount(totalAmount);

            distributorService.createTicketSale(ticketSale);

            showSuccessMessage("Ticket sale created successfully!");
            clearForm();
            
            // Close dialog after 1 second
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(this::handleClose);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (TicketInventoryException e) {
            // Covers TicketLimitExceededException too. These are expected outcomes and
            // already carry a message written for the seller, so show it unprefixed.
            showErrorMessage(e.getMessage());
        } catch (Exception e) {
            showErrorMessage("Error creating ticket sale: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleClear() {
        clearForm();
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        eventComboBox.setValue(null);
        ticketItemsContainer.getChildren().clear();
        ticketItemRows.clear();
        totalAmountLabel.setText("€0.00");
        messageLabel.setText("");
    }

    private boolean validateForm() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();

        if (firstName.isEmpty()) {
            showErrorMessage("First name is required");
            return false;
        }
        if (lastName.isEmpty()) {
            showErrorMessage("Last name is required");
            return false;
        }
        if (email.isEmpty()) {
            showErrorMessage("Email is required");
            return false;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showErrorMessage("Invalid email format");
            return false;
        }
        if (eventComboBox.getValue() == null) {
            showErrorMessage("Event is required");
            return false;
        }
        if (ticketItemRows.isEmpty() || ticketItemRows.stream().allMatch(r -> r.getQuantity() == 0)) {
            showErrorMessage("At least one ticket with quantity > 0 is required");
            return false;
        }

        return true;
    }

    private void showErrorMessage(String message) {
        messageLabel.setTextFill(Color.RED);
        messageLabel.setText(message);
    }

    private void showSuccessMessage(String message) {
        messageLabel.setTextFill(Color.GREEN);
        messageLabel.setText(message);
    }

    private void addMessageListener() {
        messageLabel.setText("");
    }

    public void updateTotal() {
        BigDecimal total = ticketItemRows.stream()
                .map(row -> {
                    SeatType st = row.getSelectedSeatType();
                    if (st != null && row.getQuantity() > 0) {
                        return st.getPrice().multiply(new BigDecimal(row.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalAmountLabel.setText(String.format("€%.2f", total));
    }

    public void removeTicketRow(TicketItemRow row) {
        ticketItemRows.remove(row);
        ticketItemsContainer.getChildren().remove(row.getContainer());
        updateTotal();
    }

    // Inner class for ticket item rows
    public static class TicketItemRow {
        private static final int DEFAULT_MAX_QUANTITY = 1000;

        private final Event event;
        private final DistributorCreateSaleController parent;
        private final HBox container;
        private final ComboBox<SeatType> seatTypeComboBox;
        private final Spinner<Integer> quantitySpinner;
        private final Label priceLabel;

        public TicketItemRow(Event event, DistributorCreateSaleController parent) {
            this.event = event;
            this.parent = parent;

            container = new HBox(10);
            container.setPadding(new Insets(10));
            container.setStyle("-fx-border-color: #e5e5e5; -fx-border-radius: 4; -fx-padding: 8;");
            container.setAlignment(Pos.CENTER_LEFT);

            // Seat Type ComboBox
            seatTypeComboBox = new ComboBox<>();
            if (event.getSeatTypes() != null) {
                seatTypeComboBox.setItems(FXCollections.observableArrayList(event.getSeatTypes()));
            }
            seatTypeComboBox.setPrefWidth(150);
            seatTypeComboBox.setPromptText("Select ticket type");
            seatTypeComboBox.setConverter(new javafx.util.StringConverter<SeatType>() {
                @Override
                public String toString(SeatType seatType) {
                    if (seatType == null) return "";
                    String label = seatType.getSeatCategory() + " - €" + seatType.getPrice();
                    if (seatType.getTotalSeats() != null) {
                        label += " (" + Math.max(seatType.getAvailableSeats(), 0) + " left)";
                    }
                    return label;
                }

                @Override
                public SeatType fromString(String s) {
                    return null;
                }
            });

            // Quantity Spinner — disabled until a seat type is chosen
            quantitySpinner = new Spinner<>();
            quantitySpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, DEFAULT_MAX_QUANTITY, 0, 1));
            quantitySpinner.setPrefWidth(80);
            quantitySpinner.setEditable(true);
            quantitySpinner.setDisable(true);

            // Price Label
            priceLabel = new Label("€0.00");
            priceLabel.setPrefWidth(80);

            // Update listeners
            seatTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                quantitySpinner.setDisable(newVal == null);
                if (newVal == null) {
                    // Reset spinner and price when deselected
                    setQuantityMax(DEFAULT_MAX_QUANTITY);
                    quantitySpinner.getValueFactory().setValue(0);
                    priceLabel.setText("€0.00");
                    parent.updateTotal();
                } else {
                    // Cap the spinner at what is still on sale. The service re-checks
                    // this against the database, so this is guidance, not enforcement.
                    setQuantityMax(newVal.getTotalSeats() == null
                            ? DEFAULT_MAX_QUANTITY
                            : Math.max(newVal.getAvailableSeats(), 0));
                }
                updatePrice();
            });
            quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                updatePrice();
                parent.updateTotal();
            });

            // Remove Button
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(evt -> parent.removeTicketRow(this));

            container.getChildren().addAll(
                    new Label("Type:"), seatTypeComboBox,
                    new Label("Qty:"), quantitySpinner,
                    new Label("Subtotal:"), priceLabel,
                    removeButton
            );

            HBox.setHgrow(seatTypeComboBox, javafx.scene.layout.Priority.ALWAYS);
        }

        private void setQuantityMax(int max) {
            SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) quantitySpinner.getValueFactory();
            factory.setMax(max);
            if (factory.getValue() != null && factory.getValue() > max) {
                factory.setValue(max);
            }
        }

        private void updatePrice() {
            if (seatTypeComboBox.getValue() != null) {
                BigDecimal unitPrice = seatTypeComboBox.getValue().getPrice();
                Integer quantity = quantitySpinner.getValue();
                BigDecimal subtotal = unitPrice.multiply(new BigDecimal(quantity));
                priceLabel.setText(String.format("€%.2f", subtotal));
            } else {
                priceLabel.setText("€0.00");
            }
        }

        public HBox getContainer() {
            return container;
        }

        public Integer getQuantity() {
            return quantitySpinner.getValue();
        }

        public SeatType getSelectedSeatType() {
            return seatTypeComboBox.getValue();
        }
    }
}
