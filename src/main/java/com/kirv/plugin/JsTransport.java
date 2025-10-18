package com.kirv.plugin;

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
        String result = "";
        try {
            result = onCallback(request);
        } catch (IllegalArgumentException e) {
            result = "error:" + e.getMessage();
        }

        callback.success(result);

        return true;
    }

    public String onCallback(String command) {
        // Check if command starts with file open prefix
        if (command == null) {
            throw new IllegalArgumentException("wrong_command");
        }

        // open file in the IDE
        String[] commandAndArgs = command.split("/\\/");
        if (commandAndArgs.length == 0) {
            throw new IllegalArgumentException("wrong_command_arguments");
        }

        String opCode = commandAndArgs[0];

        if (opCode.equals("jide_open_file") || opCode.equals("jide_open_diff_file") && commandAndArgs.length == 2) {
            String filePath = commandAndArgs[1];

            VirtualFile vFile = getProjectFile(filePath);
            IdeInstanceService.getInstance(project).openFile(vFile);
        } else if (opCode.equals("jide_open_diff_file")) {
            String filePath = commandAndArgs[1];
            String sourceFilePath = commandAndArgs[2];

            VirtualFile left = getProjectFile(sourceFilePath);
            VirtualFile right = getProjectFile(filePath);

            IdeInstanceService.getInstance(project).openDiffFiles(left, right);
        }

        // Default response for other commands
        return "success";
    }

    private VirtualFile getProjectFile(String filePath) {
        if (filePath.isEmpty()) {
            throw new IllegalArgumentException("empty_file_path");
        }

        // Convert to absolute path if relative
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(project.getBasePath(), filePath);
        }

        if (!file.exists()) {
            throw new IllegalArgumentException("file_not_found: " + file.getAbsolutePath() + "\n(" + project.getBasePath() + ")\n[" + filePath + "]");
        }

        // Open file in IntelliJ editor
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file.getAbsolutePath());
        if (vFile == null) throw new IllegalArgumentException("File not found in VFS: " + file.getAbsolutePath());

        return vFile;
    }
}
