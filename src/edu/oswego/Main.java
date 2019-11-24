package edu.oswego;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.io.IOException;

public class Main extends Application {

    private static String GUI = "GUI.fxml";
    private static double xOffset = 0;
    private static double yOffset = 0;



    @Override
    public void start(final Stage primaryStage) throws IOException {


        FXMLLoader loader = new FXMLLoader(getClass().getResource(GUI));
        Parent root = loader.load();
        Scene frame = new Scene(root);

        primaryStage.isResizable();
        primaryStage.setTitle("test");
        primaryStage.initStyle(StageStyle.UNDECORATED);
        frame.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = primaryStage.getX() - event.getScreenX();
                yOffset = primaryStage.getY() - event.getScreenY();
            }
        });
        frame.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                primaryStage.setX(event.getScreenX() + xOffset);
                primaryStage.setY(event.getScreenY() + yOffset);
            }
        });
        frame.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>
                () {

            @Override
            public void handle(KeyEvent t) {
                if(t.getCode()== KeyCode.ESCAPE)
                {
                    System.out.println("Escape pressed, program closing...");
                    Stage sb = (Stage)primaryStage.getScene().getWindow();//use any one object
                    sb.close();
                }
            }
        });
        primaryStage.setScene(frame);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

}

