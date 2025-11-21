package com.kirv.plugin;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.ToolWindowManager;

public class BrowserWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        BrowserToolWindowPanel panel = new BrowserToolWindowPanel(project);
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        // Subscribe and tie the connection to the tool window's lifecycle
        MessageBusConnection connection = project.getMessageBus().connect(toolWindow.getDisposable());
        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                // shown/hidden callbacks
                ToolWindow tw = toolWindowManager.getToolWindow("Agent-0.7");
                if (tw != null && !tw.isVisible()) {
                    panel.destroy();
                } else if (tw != null && tw.isVisible()) {
                    panel.init();
                }
            }
        });
    }
}