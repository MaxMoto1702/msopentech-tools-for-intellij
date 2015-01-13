package com.microsoftopentechnologies.intellij.wizards.createvm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.sdk.AzureSDKManagerImpl;
import com.microsoftopentechnologies.intellij.model.Subscription;
import com.microsoftopentechnologies.intellij.model.vm.AffinityGroup;
import com.microsoftopentechnologies.intellij.model.vm.CloudService;
import com.microsoftopentechnologies.intellij.model.vm.Location;
import com.microsoftopentechnologies.intellij.model.vm.StorageAccount;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.util.*;


public class CreateStorageAccountForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox subscriptionComboBox;
    private JTextField nameTextField;
    private JComboBox regionOrAffinityGroupComboBox;
    private JComboBox replicationComboBox;
    private JProgressBar createProgressBar;

    private Runnable onCreate;
    private Subscription subscription;
    private StorageAccount storageAccount;

    private enum ReplicationTypes {
        Standard_LRS,
        Standard_GRS,
        Standard_RAGRS;

        public String getDescription() {
            switch (this) {
                case Standard_GRS:
                    return "Geo-Redundant";
                case Standard_LRS:
                    return "Locally Redundant";
                case Standard_RAGRS:
                    return "Read_Access Geo-Redundant";
            }

            return super.toString();
        }
    }

    public CreateStorageAccountForm() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setTitle("Create Storage Account");

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        regionOrAffinityGroupComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {

                return (o instanceof String) ?
                        super.getListCellRendererComponent(jList, o, i, b, b1)
                        : super.getListCellRendererComponent(jList, "  " + o.toString(), i, b, b1);
            }
        });

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }
        });

        regionOrAffinityGroupComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                validateEmptyFields();
            }
        });

        replicationComboBox.setModel(new DefaultComboBoxModel(ReplicationTypes.values()));
        replicationComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                return super.getListCellRendererComponent(jList, ((ReplicationTypes) o).getDescription(), i, b, b1);
            }
        });
    }


    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                nameTextField.getText().isEmpty() || regionOrAffinityGroupComboBox.getSelectedObjects().length == 0);

        buttonOK.setEnabled(allFieldsCompleted);
    }

    private void onOK() {

        if (nameTextField.getText().length() < 3
                || nameTextField.getText().length() > 24
                || !nameTextField.getText().matches("[a-z0-9]+")) {
            JOptionPane.showMessageDialog(this, "Invalid storage account name. The name should be between 3 and 24 characters long and \n" +
                    "can contain only lowercase letters and numbers.", "Error creating the storage account", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        createProgressBar.setVisible(true);

        try {
            String name = nameTextField.getText();
            String region = (regionOrAffinityGroupComboBox.getSelectedItem() instanceof Location) ? regionOrAffinityGroupComboBox.getSelectedItem().toString() : "";
            String affinityGroup = (regionOrAffinityGroupComboBox.getSelectedItem() instanceof AffinityGroup) ? regionOrAffinityGroupComboBox.getSelectedItem().toString() : "";
            String replication = replicationComboBox.getSelectedItem().toString();

            storageAccount = new StorageAccount(name, replication, region, affinityGroup, subscription.getId().toString());
            AzureSDKManagerImpl.getManager().createStorageAccount(storageAccount);

            onCreate.run();
        } catch (AzureCmdException e) {
            UIHelper.showException("Error creating cloud service", e);
        }

        setCursor(Cursor.getDefaultCursor());

        this.setVisible(false);
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public void fillFields(final Subscription subscription, Project project) {
        this.subscription = subscription;

        subscriptionComboBox.addItem(subscription.getName());

        regionOrAffinityGroupComboBox.addItem("<Loading...>");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading regions...", false) {
            @Override
            public void run(ProgressIndicator progressIndicator) {

                try {
                    final java.util.List<AffinityGroup> affinityGroups = AzureSDKManagerImpl.getManager().getAffinityGroups(subscription.getId().toString());
                    final java.util.List<Location> locations = AzureSDKManagerImpl.getManager().getLocations(subscription.getId().toString());

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final Vector<Object> vector = new Vector<Object>();
                            vector.add("Regions");
                            vector.addAll(locations);
                            if (affinityGroups.size() > 0) {
                                vector.add("Affinity Groups");
                                vector.addAll(affinityGroups);
                            }

                            regionOrAffinityGroupComboBox.removeAllItems();
                            regionOrAffinityGroupComboBox.setModel(new DefaultComboBoxModel(vector) {
                                public void setSelectedItem(Object o) {
                                    if (!(o instanceof String)) {
                                        super.setSelectedItem(o);
                                    }
                                }
                            });

                            regionOrAffinityGroupComboBox.setSelectedIndex(1);
                        }
                    });

                } catch (AzureCmdException e) {
                    UIHelper.showException("Error getting regions", e);
                }
            }
        });

    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }
}
