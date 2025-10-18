package com.kirv.plugin;

import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.awt.*;
import java.io.*;

import javax.swing.*;

public class JsTransport extends CefMessageRouterHandlerAdapter {
    private final Project project;

    public JsTransport(Project project) {
        this.project = project;
    }

    @Override
    public boolean onQuery(CefBrowser cefBrowser,
                           CefFrame frame,
                           long queryId,
                           String request,
                           boolean persistent,
                           CefQueryCallback callback) {

        //System.out.println("JS called with: " + request + ", for project: " + project.getBasePath());
        callback.success(onCallback(request));

        return true;
    }

    public String onCallback(String command) {
        // Check if command starts with file open prefix
        if (command == null) {
            return "error:wrong_command";
        }

        // open file in the IDE
        String[] commandAndArgs = command.split("/\\/");
        if (commandAndArgs.length == 0) {
            return "error:wrong_command_arguments";
        }

        String opCode = commandAndArgs[0];

        if (opCode.equals("jide_open_file") || opCode.equals("jide_open_diff_file") && commandAndArgs.length == 2) {
            String filePath = commandAndArgs[1];

            try {
                VirtualFile vFile = getProjectFile(filePath);
                IdeInstanceService.getInstance(project).openFile(vFile);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        } else if (opCode.equals("jide_open_diff_file")) {
            String filePath = commandAndArgs[1];
            String sourceFilePath = commandAndArgs[2];

            try {
                VirtualFile left = getProjectFile(sourceFilePath);
                VirtualFile right = getProjectFile(filePath);

                com.intellij.openapi.project.DumbService.getInstance(project).runWhenSmart(() ->
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (project.isDisposed()) return;
                            if (left == null || right == null || !left.isValid() || !right.isValid()) return;

                            var request = DiffRequestFactory.getInstance().createFromFiles(project, left, right);
                            DiffManager.getInstance().showDiff(project, request);
                        }, com.intellij.openapi.application.ModalityState.nonModal())
                );
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        }

        // Default response for other commands
        return "success";
    }

    private VirtualFile getProjectFile(String filePath)
    {
        if (filePath.isEmpty()) {
            throw new IllegalArgumentException("error:empty_file_path");
        }

        // Convert to absolute path if relative
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(project.getBasePath(), filePath);
        }

        if (!file.exists()) {
            throw new IllegalArgumentException("error:file_not_found: " + file.getAbsolutePath() + "\n(" + project.getBasePath() + ")\n[" + filePath + "]");
        }

        // Open file in IntelliJ editor
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file.getAbsolutePath());
        if (vFile == null) throw new IllegalArgumentException("error:File not found in VFS: " + file.getAbsolutePath());

        return vFile;
    }
}
