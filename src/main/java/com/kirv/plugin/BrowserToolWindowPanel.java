package com.kirv.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.lang.reflect.Method;
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
    }

    public void destroy() {
        if (browser != null) {
            browser.destroy();
        }
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
                BrowserView view = (BrowserView) Class
                        .forName("com.kirv.plugin.JcefBrowser")
                        .getDeclaredConstructor()
                        .newInstance();
                browser = new Browser(view, project); // pass this window's Project down
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
}
