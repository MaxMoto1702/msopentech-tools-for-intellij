package com.microsoftopentechnologies.intellij.helpers.storage;


import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoftopentechnologies.intellij.model.storage.Queue;
import com.microsoftopentechnologies.intellij.model.storage.StorageAccount;
import com.microsoftopentechnologies.intellij.model.storage.Table;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class TableExplorerFileEditorProvider implements FileEditorProvider, DumbAware {

    public static Key<StorageAccount> STORAGE_KEY = new Key<StorageAccount>("storageAccount");
    public static Key<Table> TABLE_KEY = new Key<Table>("table");

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        StorageAccount storageAccount = virtualFile.getUserData(STORAGE_KEY);
        Table table = virtualFile.getUserData(TABLE_KEY);

        return (storageAccount != null && table != null);
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        TableFileEditor tableFileEditor = new TableFileEditor();

        StorageAccount storageAccount = virtualFile.getUserData(STORAGE_KEY);
        Table table = virtualFile.getUserData(TABLE_KEY);

        tableFileEditor.setTable(table);
        tableFileEditor.setStorageAccount(storageAccount);
        tableFileEditor.setProject(project);

        tableFileEditor.fillGrid();

        return tableFileEditor;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor fileEditor) {
        Disposer.dispose(fileEditor);
    }

    @NotNull
    @Override
    public FileEditorState readState(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void writeState(@NotNull FileEditorState fileEditorState, @NotNull Project project, @NotNull Element element) {}

    @NotNull
    @Override
    public String getEditorTypeId() {
        return "Azure-Storage-Table-Editor";
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

}
