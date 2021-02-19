package com.sbux.jmeter.Sampler.gui;

import com.sbux.jmeter.Sampler.SAPDataActions;
import org.apache.jmeter.assertions.gui.AbstractAssertionGui;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.gui.util.TextAreaCellRenderer;
import org.apache.jmeter.gui.util.TextAreaTableCellEditor;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class SAPDataActionsGui extends AbstractAssertionGui {
    private static final long serialVersionUID = 240L;

    /** The name of the table columns in the list of data. */
    private static final String COL_PARAMETERS = "assertion_parameters"; //$NON-NLS-1$

    /** A table of patterns to test against. */
    private JTable stringTable;

    /** Button to delete a set of input data. */
    private JButton deleteData;

    /** Table model for the pattern table. */
    private PowerTableModel tableModel;

    /** Text field for sap function name. */
    private JTextField functionName = new JTextField("SAP Function Name", 20);

    /** Text field for Jmeter parameters. */
    private JTextField jmeterParameters = new JTextField("ADD_JMETER_PARAMS_BY_COMMON",40);

    /** Text field for SAP Host. */
    private JTextField sapHost = new JTextField("SAP_HOST_NAME",20);

    /** Text field for SAP Logon Name. */
    private JTextField sapLogon = new JTextField("SAP_LOGON_NAME",20);

    /** Text field for SAP logon password. */
    private JPasswordField sapPassword = new JPasswordField("SAP_PASSWORD",20);

    public SAPDataActionsGui() {
        init();
    }

    @Override
    public String getStaticLabel() {
        return "SAP Data Actions";
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        SAPDataActions model = (SAPDataActions) element;
        tableModel.clearData();
        CollectionProperty[] jMeterProperties = model.getTestStrings();
        
        for(int i=0; i<jMeterProperties[0].size(); i++){
            tableModel.addRow(new Object[] { jMeterProperties[0].get(i).getStringValue() });
        }

        if (model.getTestStrings()[0].size() == 0) {
            deleteData.setEnabled(false);
        } else {
        	deleteData.setEnabled(true);
        }

        tableModel.fireTableDataChanged();
        
        functionName.setText(model.getFunctionName());
        
        jmeterParameters.setText(model.getJmeterParameters());
        
        sapHost.setText(model.getSAPHost());
        
        sapLogon.setText(model.getSAPLogon());
        
        sapPassword.setText(model.getSAPPassword());
        
        // Now execute the function...
        // model.sample(null);
    }

    @Override
    public TestElement createTestElement() {
    	SAPDataActions sa = new SAPDataActions();
        modifyTestElement(sa);
        return sa;
    }


    @SuppressWarnings("deprecation")
	@Override
    public void modifyTestElement(TestElement el) {
    	super.configureTestElement(el);
    	GuiUtils.stopTableEditing(stringTable);
        configureTestElement(el);
    
    	SAPDataActions sa = (SAPDataActions) el;

        sa.clearTestStrings();
        ArrayList<String> paramsFields = new ArrayList<String>();
        
        for (int i=0; i<tableModel.getRowCount(); i++){
        	tableModel.getData().getClass();
        	paramsFields.add(String.valueOf(tableModel.getValueAt(i, 0))); 
        }

        for (int i=0; i<paramsFields.size(); i++) {
            sa.addTestString(paramsFields.get(i));
        }
        
        if(functionName==null) {
        	functionName.setText("");
        }
        sa.setFunctionName(functionName.getText());
        sa.setJmeterParameters(jmeterParameters.getText());
        sa.setSAPHost(sapHost.getText());
        sa.setSAPLogon(sapLogon.getText());
        sa.setSAPPassword(String.valueOf(sapPassword.getPassword()));
    }


    private void init() {
        setLayout(new BorderLayout());
        Box box = Box.createVerticalBox();
        setBorder(makeBorder());

        box.add(makeTitlePanel());
        box.add(createFunctionNamePanel());
        add(box, BorderLayout.NORTH);
        add(createStringPanel(), BorderLayout.CENTER);
    }

    @Override
    public void clearGui() {
        super.clearGui();
        GuiUtils.stopTableEditing(stringTable);
        tableModel.clearData();
    }

    /**
     * An ActionListener for deleting a set of data.
     *
     */
    private class ClearDataListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            GuiUtils.cancelEditing(stringTable);
            
            int[] rowsSelected = stringTable.getSelectedRows();
            stringTable.clearSelection();
            if (rowsSelected.length > 0) {
                for (int i = rowsSelected.length - 1; i >= 0; i--) {
                    tableModel.removeRow(rowsSelected[i]);
                }
                tableModel.fireTableDataChanged();
            }

            if (stringTable.getModel().getRowCount() == 0) {
            	deleteData.setEnabled(false);
            }
        }
    }

    /**
     * Create a panel allowing the user to enter sap function name.
     * performed.
     *
     * @return a new panel for sap function name
     */
    private JPanel createFunctionNamePanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(JMeterUtils.getResString("Settings"))); //$NON-NLS-1$
        
        panel.add(functionName);
        panel.add(jmeterParameters);
        panel.add(sapHost);
        panel.add(sapLogon);
        panel.add(sapPassword);

        return panel;
    }

    /**
     * Create a panel allowing the user to supply a list of string patterns to
     * test against.
     *
     * @return a new panel for adding string patterns
     */
    private JPanel createStringPanel() {
        tableModel = new PowerTableModel(new String[] { COL_PARAMETERS }, new Class[] { String.class });
        stringTable = new JTable(tableModel);
        stringTable.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
        stringTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JMeterUtils.applyHiDPI(stringTable);

        TextAreaCellRenderer renderer = new TextAreaCellRenderer();
        stringTable.setRowHeight(renderer.getPreferredHeight());
        stringTable.setDefaultRenderer(String.class, renderer);
        stringTable.setDefaultEditor(String.class, new TextAreaTableCellEditor());
        stringTable.setPreferredScrollableViewportSize(new Dimension(100, 70));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Assertion Data Comparison")); //$NON-NLS-1$

        panel.add(new JScrollPane(stringTable), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create a panel with buttons to add and delete string patterns.
     *
     * @return the new panel with add and delete buttons
     */
    private JPanel createButtonPanel() {
        JButton addData = new JButton(JMeterUtils.getResString("add")); //$NON-NLS-1$
        addData.addActionListener(new AddDataListener());
        
        deleteData = new JButton(JMeterUtils.getResString("delete")); //$NON-NLS-1$
        deleteData.addActionListener(new ClearDataListener());
        deleteData.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addData);
        buttonPanel.add(deleteData);
        return buttonPanel;
    }

    /**
     * An ActionListener for adding a set of data.
     */
    private class AddDataListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            GuiUtils.stopTableEditing(stringTable);
            tableModel.addNewRow();
            checkButtonsStatus();
            tableModel.fireTableDataChanged();
        }
    }
    
    
    protected void checkButtonsStatus() {
        // Disable DELETE if there are no rows in the table to delete.
        if (tableModel.getRowCount() == 0) {
        	deleteData.setEnabled(false);
        } else {
        	deleteData.setEnabled(true);
        }
    }

	@Override
	public String getLabelResource() {
		// TODO Auto-generated method stub
		return null;
	}
}
