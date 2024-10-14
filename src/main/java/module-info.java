module com.oktayosman.ticketcenter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.persistence;


    opens app to javafx.fxml;
    exports app;
    exports controller;
    exports service;
    exports model;

    opens controller to javafx.fxml;
}