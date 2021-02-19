package com.sbux.jmeter.assertions.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.jmeter.assertions.gui.AbstractAssertionGui;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.gui.util.TextAreaCellRenderer;
import org.apache.jmeter.gui.util.TextAreaTableCellEditor;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;

import com.sbux.jmeter.assertions.ResponseJsonDataAssertion;

/**
 * GUI interface for a {@link ResponseJsonDataAssertionGui}.
 * 
 * @author: jjou
 *
 */
public class ResponseJsonDataAssertionGui extends AbstractAssertionGui {
    private static final long serialVersionUID = 240L;

    /** The name of the table columns in the list of data. */
    private static final String COL_RESPONSE_JSON_FIELDS = "assertion_json_data"; //$NON-NLS-1$
    private static final String COL_TARGET_JSON_FIELDS = "assertion_jmeter_variables"; //$NON-NLS-1$
    private static final String COL_REQUIRED_FIELD = "assertion_required_field"; //$NON-NLS-1$

    /** A table of patterns to test against. */
    private JTable stringTable;

    /** Button to delete a set of input data. */
    private JButton deleteData;

    /** Table model for the pattern table. */
    private PowerTableModel tableModel;

    /**
     * Radio button indicating to test if the field contains one of the
     * patterns.
     */
    private JRadioButton containsBox;

    /**
     * Radio button indicating to test if the field matches one of the patterns.
     */
    private JRadioButton ignorecaseBox;

    /**
     * Radio button indicating if the field equals the string.
     */
    private JRadioButton equalsBox;

    /**
     * Create a new AssertionGui panel.
     */
    public ResponseJsonDataAssertionGui() {
        init();
    }

    @Override
    public String getStaticLabel() {
        return "Response JSON Data Assertions";
    }

    @Override
    public String getLabelResource() {
        return "assertion_title"; // $NON-NLS-1$
    }

    /* Implements JMeterGUIComponent.createTestElement() */
    @Override
    public TestElement createTestElement() {
    	ResponseJsonDataAssertion el = new ResponseJsonDataAssertion();
        modifyTestElement(el);
        return el;
    }

    /* Implements JMeterGUIComponent.modifyTestElement(TestElement) */
    @Override
    public void modifyTestElement(TestElement el) {
        GuiUtils.stopTableEditing(stringTable);
        configureTestElement(el);
    
        ResponseJsonDataAssertion ra = (ResponseJsonDataAssertion) el;
        saveScopeSettings(ra);

        ra.clearTestStrings();
        ArrayList<String> responseFields = new ArrayList<String>();
        ArrayList<String> jmeterVariables = new ArrayList<String>();
        ArrayList<String> requiredFields = new ArrayList<String>();
        
        for (int i=0; i<tableModel.getRowCount(); i++){
        	tableModel.getData().getClass();
        	responseFields.add(String.valueOf(tableModel.getValueAt(i, 0))); 
        	jmeterVariables.add(String.valueOf(tableModel.getValueAt(i, 1))); 
        	requiredFields.add(String.valueOf(tableModel.getValueAt(i, 2))); 
        }

        if (containsBox.isSelected()) {
            ra.setToContainsType();
        } else if (equalsBox.isSelected()) {
            ra.setToEqualsType();
        } else if (ignorecaseBox.isSelected()) {
            ra.setToIgnoreCaseType();
        } else {
            ra.setToEqualsType();
        }        
        
        for (int i=0; i<responseFields.size(); i++) {
            ra.addTestString(responseFields.get(i),jmeterVariables.get(i),requiredFields.get(i));
        }
    }

    /**
     * Implements JMeterGUIComponent.clearGui
     */
    @Override
    public void clearGui() {
        super.clearGui();
        GuiUtils.stopTableEditing(stringTable);
        tableModel.clearData();
    }

    /**
     * A newly created component can be initialized with the contents of a Test
     * Element object by calling this method. The component is responsible for
     * querying the Test Element object for the relevant information to display
     * in its GUI.
     *
     * @param el
     *            the TestElement to configure
     */
    @Override
    public void configure(TestElement el) {
        super.configure(el);
        ResponseJsonDataAssertion model = (ResponseJsonDataAssertion) el;
        showScopeSettings(model, true);

        if (model.isContainsType()) {
            containsBox.setSelected(true);
        } else if (model.isEqualsType()) {
            equalsBox.setSelected(true);
        } else {
            ignorecaseBox.setSelected(true);
        }

        tableModel.clearData();
        CollectionProperty[] jMeterProperties = model.getTestStrings();
        
        for(int i=0; i<jMeterProperties[0].size(); i++){
	    		// By default, all fields are defined as required.
	    		String requiredField = jMeterProperties[2].size()==0 ? "1" : jMeterProperties[2].get(i).getStringValue();
	        tableModel.addRow(new Object[] { jMeterProperties[0].get(i).getStringValue(), jMeterProperties[1].get(i).getStringValue(), requiredField });
        }

        if (model.getTestStrings()[0].size() == 0) {
            deleteData.setEnabled(false);
        } else {
        	deleteData.setEnabled(true);
        }

        tableModel.fireTableDataChanged();
    }

    /**
     * Initialize the GUI components and layout.
     */
    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new BorderLayout());
        Box box = Box.createVerticalBox();
        setBorder(makeBorder());

        box.add(makeTitlePanel());
        box.add(createScopePanel(true));
        box.add(createTypePanel());
        add(box, BorderLayout.NORTH);
        add(createStringPanel(), BorderLayout.CENTER);
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
     * Create a panel allowing the user to choose what type of test should be
     * performed.
     *
     * @return a new panel for selecting the type of assertion test
     */
    private JPanel createTypePanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(JMeterUtils.getResString("assertion_pattern_match_rules"))); //$NON-NLS-1$

        ButtonGroup group = new ButtonGroup();

        containsBox = new JRadioButton(JMeterUtils.getResString("assertion_contains")); //$NON-NLS-1$
        group.add(containsBox);
        containsBox.setSelected(true);
        panel.add(containsBox);

        ignorecaseBox = new JRadioButton(JMeterUtils.getResString("assertion_ignorecase")); //$NON-NLS-1$
        group.add(ignorecaseBox);
        panel.add(ignorecaseBox);

        equalsBox = new JRadioButton(JMeterUtils.getResString("assertion_equals")); //$NON-NLS-1$
        group.add(equalsBox);
        panel.add(equalsBox);

        return panel;
    }

    /**
     * Create a panel allowing the user to supply a list of string patterns to
     * test against.
     *
     * @return a new panel for adding string patterns
     */
    private JPanel createStringPanel() {
        tableModel = new PowerTableModel(new String[] { COL_RESPONSE_JSON_FIELDS, COL_TARGET_JSON_FIELDS, COL_REQUIRED_FIELD }, new Class[] { String.class, String.class, String.class });
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
        panel.setBorder(BorderFactory.createTitledBorder("Assertion JSON Data Comparison")); //$NON-NLS-1$

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
}
