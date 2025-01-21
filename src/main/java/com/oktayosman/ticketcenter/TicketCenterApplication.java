package com.oktayosman.ticketcenter;

import com.oktayosman.ticketcenter.logging.LogUtil;
import com.oktayosman.ticketcenter.util.SpringContext;
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
        logger.info("Initializing Spring Application Context");

        context = new SpringApplicationBuilder(TicketCenterApplication.class).run();
        SpringContext.setContext(context);

        if (SpringContext.getContext() == null) {
            throw new IllegalStateException("Spring ApplicationContext initialization failed");
        }
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
        logger.info("Closing Spring Application Context");
        if (context != null) {
            context.close();
        }
    }

    public static void main(String[] args) {
        logger.info("Launching TicketCenter Application");
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TicketCenterApplication.class);
        builder.run(args);
        logger.info("TicketCenter Application launched successfully");
        launch(args);
    }
}