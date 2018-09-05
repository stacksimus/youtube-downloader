package com.stacksimus;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class YoutubeDLTask extends Task<Void> {

    private static final String YOUTUBE_DL_EXE = "youtube-dl";

    private static final String FFMPEG_EXE = "ffmpeg";

    private String url;

    private String artist;

    private String title;

    private boolean extractAudio;

    private boolean extractVideo;

    private Label downloadStatusLabel;

    private Button button;

    private Process downloadProcess;

    private Process convertProcess;

    private String videoFile;

    public YoutubeDLTask(String url, String artist, String title, boolean extractAudio, boolean extractVideo,
                         Label downloadStatusLabel, Button button) {
        this.url = url;
        this.artist = artist;
        this.title = title;
        this.extractAudio = extractAudio;
        this.extractVideo = extractVideo;
        this.downloadStatusLabel = downloadStatusLabel;
        this.button = button;
    }

    public Void call() {
        try {
            // Get the duration of the video
            float duration = getDuration();

            // Run youtube-dl
            downloadProcess = new ProcessBuilder(
                    buildCommand(url, artist, title, extractAudio, extractVideo)).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(downloadProcess.getInputStream()));

            Platform.runLater(() -> downloadStatusLabel.setText("Downloading files") );

            //Temp files that will be deleted
            List<String> deleteFiles = new ArrayList<>();

            // Start a new thread to parse input stream from youtube-dl process and retrieve
            // the video file and files to delete
            new Thread(() -> {
                try {
                    gatherFiles(reader, deleteFiles);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Start a new thread to wait for download process to finish
            new Thread(() -> {
                try {
                    downloadProcess.waitFor();
                    Platform.runLater(() -> downloadStatusLabel.setText("Download complete") );

                    //Convert any files to mp4 format if necessary
                    if (convertNeeded(videoFile, extractVideo)) {
                        Platform.runLater(() -> downloadStatusLabel.setText("Converting video file.") );
                        String convertedFileName = FilenameUtils.getBaseName(videoFile) + ".mp4";
                        convertProcess = new ProcessBuilder(FFMPEG_EXE,
                                FfmpegOptions.OVERWRITE, FfmpegOptions.INPUT, videoFile,
                                FfmpegOptions.VSTATS, FfmpegOptions.VSTATS_FILE, FfmpegOptions.VSTATS_FILE_NAME,
                                convertedFileName).inheritIO().start();

                        //Create a new thread to read ffmpeg output log file
                        TailerListener listener = new FfmpegTailer(downloadStatusLabel,duration);
                        String vstatsFile = System.getProperty("user.dir") + File.separator + FfmpegOptions.VSTATS_FILE_NAME;
                        Tailer tailer = Tailer.create(new File(vstatsFile), listener, 1000);

                        //Create a new thread to wait for process to finish
                        new Thread(() -> {
                            try {
                                convertProcess.waitFor();
                                File file = new File(videoFile);
                                file.delete();
                                Platform.runLater(() -> downloadStatusLabel.setText("Conversion complete.") );
                                //Re-enable the button
                                button.setDisable(false);

                                tailer.stop();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                    else {
                        //Re-enable the button
                        button.setDisable(false);
                    }

                    //Delete temp files if necessary
                    if (extractAudio && extractVideo) {
                        deleteTempFiles(deleteFiles);
                    }

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> buildCommand(String url, String artist, String title, boolean extractAudio, boolean extractVideo) {
        List<String> commands = new ArrayList<>();
        commands.add(YOUTUBE_DL_EXE);
        commands.add(url);
        commands.add(YoutubeOptions.OUTPUT);
        commands.add(getRenamedFile(artist, title));
        if (extractAudio) {
            commands.add(YoutubeOptions.EXTRACT_AUDIO);
            commands.add(YoutubeOptions.AUDIO_FORMAT_MP3);
            if (extractVideo) {
                commands.add(YoutubeOptions.KEEP_VIDEO);
            }
        }
        return commands;
    }

    private void gatherFiles(BufferedReader reader, List<String> deleteFiles) throws IOException {
        String line;
        while ( (line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains("%")) {
                final String percent = String.format("%s%% downloaded",
                        StringUtils.substringBetween(line, "[download]", "%"));
                Platform.runLater(() -> downloadStatusLabel.setText(percent));
            }
            if (line.contains("[ffmpeg]") && line.contains("Merging")) {
                videoFile = StringUtils.substringBetween(line,"\"");
            }
            else if (line.contains("[download]") && line.contains("Destination:")) {
                deleteFiles.add(StringUtils.substringAfterLast(line, ": "));
            }
        }
    }

    private float getDuration() throws IOException {
        Process getDurationProcess = new ProcessBuilder(YOUTUBE_DL_EXE, url, YoutubeOptions.GET_DURATION).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(getDurationProcess.getInputStream()));
        String[] durationSplit = reader.readLine().split(":");
        if (durationSplit.length == 1) {
            return Float.valueOf(durationSplit[0]);
        }
        if (durationSplit.length == 2) {
            return Float.valueOf(durationSplit[0]) * 60 + Float.valueOf(durationSplit[1]);
        }
        if (durationSplit.length == 3) {
            return Float.valueOf(durationSplit[0]) * 3600 + Float.valueOf(durationSplit[1]) * 60 + Float.valueOf(durationSplit[2]);
        }
        return -1;
    }

    private boolean convertNeeded(String videoFile, boolean extractVideo) {
        if (extractVideo) {
            String extension = FilenameUtils.getExtension(videoFile);
            if (extension.equals("webm") || extension.equals("mkv")) {
                return true;
            }
        }
        return false;
    }

    private String getRenamedFile(String artist, String title) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!StringUtils.isEmpty(artist)) {
            stringBuilder.append(artist);
            stringBuilder.append(" - ");
        }
        stringBuilder.append(title + ".%(ext)s");
        return stringBuilder.toString();
    }

    private void deleteTempFiles(List<String> deleteFiles) {
        deleteFiles.stream().forEach(deleteFile -> {
            File file = new File(deleteFile);
            boolean success = file.delete();
            String message = String.format("%s temp file %s", success ? "Deleted" : "Unable to delete", file.getName());
            System.out.println(message);
        });
    }
}
