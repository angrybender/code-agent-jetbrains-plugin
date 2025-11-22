package com.kirv.plugin;

import com.intellij.ui.jcef.JBCefBrowser;

import java.util.function.Consumer;

import javax.swing.*;

public interface BrowserView {
    JComponent getBrowser();

    public JBCefBrowser getJBCefBrowser();

    public void addJSHandler(JsTransport instance);

    void load(String url);

    void onUrlChange(Consumer<String> consumer);

    void onProgressChange(Consumer<Double> consumer);

    void back();

    void forward();

    boolean canBack();

    boolean canForward();

    void openDevTools();

    void executeScript(String script);

    void onHide();

    void onShow();

    Type type();

    enum Type {
        JAVAFX, JCEF
    }
}
