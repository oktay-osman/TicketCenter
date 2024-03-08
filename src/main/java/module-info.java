module com.oktayosman.ticketcenter {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.oktayosman.ticketcenter to javafx.fxml;
    exports com.oktayosman.ticketcenter;
}