package com.kirv.plugin;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.*;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import java.net.URLEncoder;

/**
 * 主面板
 *
 * @author huangxingguang
 * @date 2019-04-21 13:53
 */
class Browser extends JPanel {
    private BrowserView webView;
    private JButton btnRefresh;
    private JProgressBar progressBar;
    private String userHomeDirectory;
    private Path configFilePath;
    private int port = 5000; // default port value
    private Project project;

    Browser(BrowserView webView, Project project) {
        this.webView = webView;
        this.project = project;
        this.initConfig();
        this.initView();
        this.initEvent();
        this.loadApp();
    }

    private void initConfig()
    {
        // Get user home directory (works on Linux, Windows, and macOS)
        userHomeDirectory = System.getProperty("user.home");
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

    private void loadApp()
    {
        loadConfigFile();
        webView.load("about:blank");

        try {
            String projectPath = URLEncoder.encode(project.getBasePath(), "UTF-8").replaceAll("\\+", "%20");
            webView.load("http://localhost:" + port + "/?project=" + projectPath);
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
        gbc.gridx     = 0;                                  // column 0
        gbc.gridy     = 0;                                  // row    0
        gbc.weightx   = 1.0;                                // give this cell all extra horizontal space
        gbc.fill      = GridBagConstraints.NONE;            // do not stretch the button
        gbc.anchor    = GridBagConstraints.WEST;            // align to the left (west) of the cell

        panel.add(btnRefresh = new ControlButton("↻"), gbc);

        // add config file path label
        gbc = new GridBagConstraints();
        gbc.gridx     = 1;
        gbc.gridy     = 0;
        gbc.weightx   = 1.0;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.insets    = new Insets(0, 10, 0, 0);

        JLabel configLabel = new JLabel("Config: " + configFilePath.toString());
        panel.add(configLabel, gbc);

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

        webView.addJSHandler(this::fromPluginCallback);
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

    private String fromPluginCallback(String command) {
        // Check if command starts with file open prefix
        if (command != null && command.startsWith("jide_open_file//")) {
            // Extract file path after '//'
            String filePath = command.substring("jide_open_file//".length());

            // Handle empty path
            if (filePath.isEmpty()) {
                return "error:empty_file_path";
            }

            // Convert to absolute path if relative
            File file = new File(filePath);
            if (!file.isAbsolute()) {
                // Handle relative paths - convert to absolute based on project base
                file = new File(project.getBasePath(), filePath);
            }

            // Check if file exists
            if (!file.exists()) {
                return "error:file_not_found:" + file.getAbsolutePath() + "(" + project.getBasePath() + ") [" + filePath + "]";
            }

            // Open file in IntelliJ editor
            VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file.getAbsolutePath());
            if (vFile == null) throw new IllegalArgumentException("File not found in VFS: " + file.getAbsolutePath());

            ApplicationManager.getApplication().invokeLater(() -> {
                new OpenFileDescriptor(project, vFile).navigate(true);
            });
        }
        
        // Default response for other commands
        return "success";
    }
}