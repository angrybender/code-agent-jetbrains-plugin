package com.kirv.plugin;

import javax.swing.*;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.ToolWindowManager;

public class BrowserWindowFactory implements ToolWindowFactory {
    private static final Map<Project, BrowserToolWindowPanel> PANELS = new WeakHashMap<>();

    public static @Nullable BrowserToolWindowPanel getPanel(@NotNull Project project) {
        synchronized (PANELS) {
            return PANELS.get(project);
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        BrowserToolWindowPanel panel = new BrowserToolWindowPanel(project);
        synchronized (PANELS) {
            PANELS.put(project, panel);
        }
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        com.intellij.openapi.util.Disposer.register(toolWindow.getDisposable(), () -> {
            synchronized (PANELS) {
                PANELS.remove(project);
            }
        });

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