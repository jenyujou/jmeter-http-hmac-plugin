package com.sbux.jmeter.http.hmac.gui;

import com.sbux.jmeter.http.hmac.config.HttpSigManager;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HttpSigManagerGui extends AbstractConfigGui implements ActionListener {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final long serialVersionUID = 1L;
    private final JTextField username = new JTextField(15);
    private final JPasswordField password = new JPasswordField(15);
    private final JTextField algorithm = new JTextField(15);
    private final JTextField headers = new JTextField(15);
    private boolean displayName = true;

    public HttpSigManagerGui() {
        this(true);
    }

    public HttpSigManagerGui(boolean displayName) {
        this.displayName = displayName;
        init();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public String getStaticLabel() {
        return "HTTP Signed Request Defaults";
    }

    @Override
    public String getLabelResource() {
        return "hmac_config_element";
    }

    @Override
    public void configure(TestElement element) {
        log.debug("configure TestElement=" + element.getClass().getCanonicalName());
        super.configure(element);
        username.setText(element.getPropertyAsString(HttpSigManager.KEY_ID));
        password.setText(element.getPropertyAsString(HttpSigManager.SECRET));
        algorithm.setText(element.getPropertyAsString(HttpSigManager.MAC_ALGORITHM, "hmac-sha256"));
        headers.setText(element.getPropertyAsString(HttpSigManager.HEADERS, "(request-target) date digest"));
    }

    @Override
    public TestElement createTestElement() {
        HttpSigManager element = new HttpSigManager();
        modifyTestElement(element);
        return element;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        log.debug("modify TestElement=" + element.getClass().getCanonicalName());
        configureTestElement(element);
        element.setProperty(new StringProperty(HttpSigManager.KEY_ID, username.getText()));
        element.setProperty(new StringProperty(HttpSigManager.SECRET,  new String(password.getPassword())));
        element.setProperty(new StringProperty(HttpSigManager.MAC_ALGORITHM, algorithm.getText()));
        element.setProperty(new StringProperty(HttpSigManager.HEADERS, headers.getText()));
    }

    @Override
    public void clearGui() {
        super.clearGui();

        username.setText(""); //$NON-NLS-1$
        password.setText(""); //$NON-NLS-1$
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)

        algorithm.setText("hmac-sha256");
        headers.setText("(request-target) date digest");

        setLayout(new BorderLayout(0, 5));

        if (displayName) {
            setBorder(makeBorder());
            add(makeTitlePanel(), BorderLayout.NORTH);
        }

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.add(createUsernamePanel());
        mainPanel.add(createPasswordPanel());
        mainPanel.add(createAlgorithmPanel());
        mainPanel.add(createHeadersPanel());
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createUsernamePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel("Key Id"); // $NON-NLS-1$
        label.setLabelFor(username);
        panel.add(label, BorderLayout.WEST);
        panel.add(username, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPasswordPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel("Secret"); // $NON-NLS-1$
        label.setLabelFor(password);
        panel.add(label, BorderLayout.WEST);
        panel.add(password, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAlgorithmPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel("Algorithm"); // $NON-NLS-1$
        label.setLabelFor(algorithm);
        panel.add(label, BorderLayout.WEST);
        panel.add(algorithm, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHeadersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel("Headers"); // $NON-NLS-1$
        label.setLabelFor(headers);
        panel.add(label, BorderLayout.WEST);
        panel.add(headers, BorderLayout.CENTER);
        return panel;
    }
}