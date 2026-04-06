package com.kirv.plugin;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class AddToContextAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ArrayList<File> files = getSelectedLocalFiles(e);
        boolean enabled = project != null && !files.isEmpty();
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        BrowserToolWindowPanel panel = BrowserWindowFactory.getPanel(project);
        if (panel == null) {
            return;
        }

        ArrayList<File> files = getSelectedLocalFiles(e);
        if (files.isEmpty()) {
            return;
        }

        panel.addFilesToContext(files);
    }

    private ArrayList<File> getSelectedLocalFiles(@NotNull AnActionEvent e) {
        ArrayList<File> files = new ArrayList<>();
        VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles != null && virtualFiles.length > 0) {
            for (VirtualFile virtualFile : virtualFiles) {
                addIfLocal(files, virtualFile);
            }
            return files;
        }

        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        addIfLocal(files, virtualFile);
        return files;
    }

    private void addIfLocal(ArrayList<File> files, VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            return;
        }
        files.add(new File(virtualFile.getPath()));
    }
}