package com.kirv.plugin;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.*;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;

final class Browser extends JPanel {
    private BrowserView webView;
    private JButton btnRefresh;
    private JButton btnOpenConfigFile;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private Path configFilePath;
    private int port = 5000; // default port value
    private final Project project;

    Browser(BrowserView webView, @NotNull Project project) {
        this.webView = webView;
        this.project = project;
        initConfigPath();
        initView();
        initEvent();
        loadApp();
    }

    public void destroy() {
        webView.onHide();
    }

    public void init() {
        initBrowserEvent();
    }

    private void initConfigPath() {
        String userHomeDirectory = System.getProperty("user.home");
        configFilePath = Paths.get(userHomeDirectory, "code_agent_cnfg.env");
    }

    private void loadConfigFile() {
        Properties properties = new Properties();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath.toFile()))) {
            properties.load(reader);

            // Read PORT parameter with default value 5000
            String portStr = properties.getProperty("PORT", "5000");
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                // If PORT value is invalid, use default 5000
                port = 5000;
                System.err.println("Invalid PORT value in config file, using default: 5000");
            }

        } catch (IOException e) {
            // Config file not found or cannot be read - use default port
            port = 5000;
        }
    }

    private void loadApp() {
        loadConfigFile();
        webView.load("about:blank");

        try {
            String projectPath = URLEncoder.encode(project.getBasePath(), "UTF-8").replaceAll("\\+", "%20");
            webView.load("http://localhost:" + port + "/?project=" + projectPath);
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
        panel.add(btnOpenConfigFile = new ControlButton(".env"), gbc);

        // add config file path label
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

        webView.onProgressChange(e -> swingInvokeLater(() -> {
            progressBar.setVisible(e != 1.0 && e != 0);
            progressBar.setValue((int) (e * 100));
        }));
    }

    private void initBrowserEvent() {
        webView.addJSHandler(new JsTransport(project, statusLabel));
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