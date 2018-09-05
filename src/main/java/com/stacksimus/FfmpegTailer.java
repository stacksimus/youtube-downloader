package com.stacksimus;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.StringUtils;

public class FfmpegTailer extends TailerListenerAdapter {

    private Label downloadStatusLabel;

    private float duration;

    public FfmpegTailer(Label downloadStatusLabel, float duration) {
        this.downloadStatusLabel = downloadStatusLabel;
        this.duration = duration;
    }

    @Override
    public void handle(String line) {
        try {
            float time = Float.valueOf(StringUtils.substringBetween(line, "time=", "br=").trim());
            Platform.runLater(() -> downloadStatusLabel.setText(String.format("%.2f%% converted.\n",time/duration*100.0)) );
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(Exception ex) {
        ex.printStackTrace();
    }
}
