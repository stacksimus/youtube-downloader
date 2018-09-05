package com.stacksimus;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {

    private static final int SCENE_WIDTH = 300;

    private static final int SCENE_HEIGHT = 200;

    private static final int GRIDPANE_GAP = 10;

    private static final int GRIDPANE_PADDING = 25;

    private GridPane gridPane;

    //GUI Components
    private TextField urlTextField = new TextField();

    private TextField titleTextField = new TextField();

    private TextField artistTextField = new TextField();

    CheckBox audioCheckbox = new CheckBox("Audio");
    CheckBox videoCheckbox = new CheckBox("Video");

    private Label downloadStatusLabel = new Label();

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Youtube Downloader");

        gridPane = createGridPane();

        createControls();

        Scene scene = new Scene(gridPane, SCENE_WIDTH, SCENE_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.TOP_CENTER);
        gridPane.setHgap(GRIDPANE_GAP);
        gridPane.setVgap(GRIDPANE_GAP);
        gridPane.setPadding(new Insets(GRIDPANE_PADDING, GRIDPANE_PADDING, GRIDPANE_PADDING, GRIDPANE_PADDING));

        return gridPane;
    }

    private void createControls() {
        int rowIndex = 0;

        gridPane.add(new Label("URL:"), 0, rowIndex);
        gridPane.add(urlTextField, 1, rowIndex);
        rowIndex++;

        gridPane.add(new Label("Title:"), 0, rowIndex);
        gridPane.add(titleTextField, 1, rowIndex);
        rowIndex++;

        gridPane.add(new Label("Artist:"), 0, rowIndex);
        gridPane.add(artistTextField, 1, rowIndex);
        rowIndex++;

        gridPane.add(new Label("Format"), 0, rowIndex);
        audioCheckbox.setSelected(true);
        videoCheckbox.setSelected(true);
        HBox checkboxHBox = new HBox(10, audioCheckbox,videoCheckbox);
        gridPane.add(checkboxHBox, 1, rowIndex);
        rowIndex++;

        Button button = new Button("Download");
        button.setOnAction((event) -> {
            try {
                String url = urlTextField.getText();
                boolean extractAudio = audioCheckbox.isSelected();
                boolean extractVideo = videoCheckbox.isSelected();
                String artist = artistTextField.getText();
                String title = titleTextField.getText();

                if (!validateInputs(url,title,extractAudio,extractVideo)) {
                    return;
                }

                downloadStatusLabel.setText("");
                button.setDisable(true);

                YoutubeDLTask runner = new YoutubeDLTask(url,artist, title, extractAudio, extractVideo, downloadStatusLabel, button);
                Thread youtubeDlThread = new Thread(runner);
                youtubeDlThread.setDaemon(true);
                youtubeDlThread.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        gridPane.add(button, 0, rowIndex);

        gridPane.add(downloadStatusLabel, 1,rowIndex);

    }

    private boolean validateInputs(String url, String title, boolean extractAudio, boolean extractVideo) {
        if (url.isEmpty()) {
            showAlert("Missing URL", "Please enter a URL");
            return false;
        }
        if (title.isEmpty()) {
            showAlert("Missing Title", "Please enter a Title");
            return false;
        }
        if (!extractAudio && !extractVideo) {
            showAlert("Missing Format", "Please enter a Format");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(gridPane.getScene().getWindow());
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
