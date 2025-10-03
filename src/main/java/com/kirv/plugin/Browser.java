package com.kirv.plugin;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.*;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

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

    Browser(BrowserView webView) {
        this.webView = webView;
        this.initView();
        this.initEvent();
        this.loadApp();
        this.initConfig();
    }

    private void initConfig()
    {
        // Get user home directory (works on Linux, Windows, and macOS)
        userHomeDirectory = System.getProperty("user.home");
        configFilePath = Paths.get(userHomeDirectory, "code_agent_cnfg.env");
    }

    private void loadApp()
    {
        webView.load("about:blank");
        webView.load("http://localhost:5000/");
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
        return "true";
    }
}