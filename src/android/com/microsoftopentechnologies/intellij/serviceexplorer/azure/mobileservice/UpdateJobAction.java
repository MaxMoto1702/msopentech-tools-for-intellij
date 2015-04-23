package com.microsoftopentechnologies.intellij.serviceexplorer.azure.mobileservice;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.components.DefaultLoader;
import com.microsoftopentechnologies.intellij.helpers.Name;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.ms.MobileService;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionEvent;
import com.microsoftopentechnologies.intellij.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

@Name("Update job")
public class UpdateJobAction extends NodeActionListener {
    private ScheduledJobNode scheduledJobNode;

    public UpdateJobAction(ScheduledJobNode scheduledJobNode) {
        this.scheduledJobNode = scheduledJobNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        // get the parent MobileServiceNode node
        MobileServiceNode mobileServiceNode = (MobileServiceNode) scheduledJobNode.findParentByType(MobileServiceNode.class);
        final MobileService mobileService = mobileServiceNode.getMobileService();

        VirtualFile editorFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(scheduledJobNode.job.getLocalFilePath(mobileService.getName())));
        if (editorFile != null) {
            FileEditor[] fe = FileEditorManager.getInstance((Project) scheduledJobNode.getProject()).getAllEditors(editorFile);

            if (fe.length > 0 && fe[0].isModified()) {
                int i = JOptionPane.showConfirmDialog(null, "The file is modified. Do you want to save pending changes?", "Service Explorer", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (i) {
                    case JOptionPane.YES_OPTION:
                        ApplicationManager.getApplication().saveAll();
                        break;
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;
                }
            }

            ProgressManager.getInstance().run(new Task.Backgroundable((Project) scheduledJobNode.getProject(), "Uploading job script", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        AzureRestAPIManagerImpl.getManager().uploadJobScript(
                                mobileService.getSubcriptionId(), mobileService.getName(),
                                scheduledJobNode.job.getName(), scheduledJobNode.job.getLocalFilePath(mobileService.getName()));
                    } catch (AzureCmdException e) {
                        DefaultLoader.getUIHelper().showException("Error uploading script", e);
                    }
                }
            });
        }
    }
}