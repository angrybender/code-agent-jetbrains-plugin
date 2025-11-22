package com.kirv.plugin;

import java.awt.*;
import java.io.*;
import javax.swing.*;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;

final class Browser extends JPanel {
    private BrowserView webView;
    private ConfigService configService;
    private final Project project;
    private JButton btnRefresh;
    private JButton btnOpenConfigFile;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private Boolean isStartEvent = false;

    Browser(BrowserView webView, @NotNull Project project) {
        this.webView = webView;
        this.project = project;
        this.configService = new ConfigService();
        initView();
        initEvent();
        loadApp();
        initBrowserEvent();
        isStartEvent = true;
    }

    public void onHide() {
        webView.onHide();
        isStartEvent = false;
    }

    public void onShow() {
        if (!isStartEvent) {
            initBrowserEvent();
        }

        webView.onShow();
    }

    private void loadApp() {
        configService.loadConfigFile();
        webView.load("about:blank");


        try {
            String projectPath = URLEncoder.encode(project.getBasePath(), "UTF-8").replaceAll("\\+", "%20");
            webView.load("http://localhost:" + configService.getPort() + "/?project=" + projectPath);
            statusLabel.setText("Connection...");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void initView() {
        setLayout(new BorderLayout());
        add(topControls(), BorderLayout.NORTH);
        add(centerContent(), BorderLayout.CENTER);
    }

    private JPanel topControls() {
        JPanel panel = new JPanel(new GridBagLayout());

        // make the button hug the left side
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;                                 // column 0
        gbc.gridy = 0;                                 // row    0
        gbc.weightx = 0;                               // give this cell all extra horizontal space
        gbc.fill = GridBagConstraints.NONE;            // do not stretch the button
        gbc.anchor = GridBagConstraints.WEST;          // align to the left (west) of the cell
        panel.add(btnRefresh = new ControlButton("↻"), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(btnOpenConfigFile = new ControlButton("\uD83D\uDD27"), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 10, 0, 0);
        statusLabel = new JLabel("...");
        panel.add(statusLabel, gbc);

        // filler (push everything to the left)
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // take remaining space
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    private JPanel centerContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(0, 5));
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(webView.getBrowser(), BorderLayout.CENTER);
        return panel;
    }

    private void initEvent() {
        btnRefresh.addActionListener(e -> loadApp());
        btnOpenConfigFile.addActionListener(e -> openConfigFile());

        webView.onProgressChange(e -> swingInvokeLater(() -> {
            progressBar.setVisible(e != 1.0 && e != 0);
            progressBar.setValue((int) (e * 100));
        }));
    }

    private void initBrowserEvent() {
        webView.addJSHandler(new JsTransport(project, statusLabel));
    }

    private void openConfigFile() {
        File configFile = configService.getConfigFilePath().toFile();
        if (!configFile.exists()) {
            configService.createConfigFromDefault();
        }

        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(configFile.getAbsolutePath());
        IdeInstanceService.getInstance(project).openFile(vFile);
    }

    private static class ControlButton extends JButton {
        ControlButton(String text) {
            super(text);
            setMaximumSize(new Dimension(40, 25));
            setMinimumSize(new Dimension(40, 25));
            setPreferredSize(new Dimension(40, 25));
        }
    }

    private void swingInvokeLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}