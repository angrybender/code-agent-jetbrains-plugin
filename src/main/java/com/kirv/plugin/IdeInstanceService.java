package com.kirv.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

@Service(Service.Level.PROJECT)
public final class IdeInstanceService {
    private final Project project;

    IdeInstanceService(Project project) {
        this.project = project;
    }

    void openFile(VirtualFile vFile) {
        if (project == null || project.isDisposed() || vFile == null) return;

        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (!project.isDisposed()) {
                        FileEditorManager.getInstance(project)
                                .openTextEditor(new OpenFileDescriptor(project, vFile), true);
                    }
                },
                com.intellij.openapi.application.ModalityState.nonModal(),
                project.getDisposed() // expire if project closes
        );
    }

    static IdeInstanceService getInstance(Project project) {
        return project.getService(IdeInstanceService.class);
    }
}
