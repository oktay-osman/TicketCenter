package com.oktayosman.ticketcenter;

import com.oktayosman.ticketcenter.logging.LogUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;



@SpringBootApplication(scanBasePackages = "com.oktayosman.ticketcenter")
public class TicketCenterApplication extends Application {

    private static final Logger logger = LogUtil.getLogger(TicketCenterApplication.class);

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(TicketCenterApplication.class).run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting TicketCenter Application");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        primaryStage.setTitle("Login Page");
        primaryStage.setScene(new Scene(root, 520, 400));
        primaryStage.show();

        logger.info("Login Page displayed successfully");
    }

    @Override
    public void stop() {
        context.close();
    }

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TicketCenterApplication.class);
        builder.run(args);

        launch(args);
    }
}