package com.kirv.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import com.intellij.ide.dnd.DnDEvent;
import com.intellij.ide.dnd.DnDSupport;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.intellij.util.ReflectionUtil;

public final class BrowserToolWindowPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(BrowserToolWindowPanel.class);
    private final Project project;
    private Browser browser;

    public BrowserToolWindowPanel(Project project) {
        this.project = project;
        setLayout(new java.awt.BorderLayout());
        add(createBrowserComponent(), java.awt.BorderLayout.CENTER);
        panelEvents();
    }

    public void destroy() {
        if (browser != null) {
            browser.onHide();
        }
    }

    public void init() {
        if (browser != null) {
            browser.onShow();
        }
    }

    public void addFilesToContext(ArrayList<File> files) {
        if (browser == null || files == null || files.isEmpty()) {
            return;
        }
        browser.onFilesDrag(files);
    }

    private boolean isSupportedJCEF() {
        try {
            Method method = ReflectionUtil.getDeclaredMethod(
                    Class.forName("com.intellij.ui.jcef.JBCefApp"), "isSupported"
            );
            return Objects.nonNull(method) && (boolean) method.invoke(null);
        } catch (Exception e) {
            return false;
        }
    }

    private JComponent createBrowserComponent() {
        try {
            if (isSupportedJCEF()) {
                BrowserView view = new JcefBrowser();
                browser = new Browser(view, project);
                return browser;
            }
        } catch (Exception e) {
            LOG.error(e);
        }

        JLabel label = new JLabel("JCEF is not supported in running IDE");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setBorder(JBUI.Borders.emptyTop(10));
        return label;
    }

    private void panelEvents() {
        if (browser == null) {
            return;
        }

        DnDSupport.createBuilder(browser)
                .setTargetChecker(event -> {
                    // Return true when you want to accept the drag/drop at the current location.
                    return true;
                })
                .setDropHandler(this::handleIdeDrop)
                .install();
    }

    private void handleIdeDrop(DnDEvent event) {
        if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                List<File> droppedFiles = (List<File>) event.getTransferData(DataFlavor.javaFileListFlavor);
                addFilesToContext(new ArrayList<>(droppedFiles));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            } catch (UnsupportedFlavorException e) {
                throw new RuntimeException(e);
            }
        }
    }
}