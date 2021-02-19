package com.sbux.jmeter.http.hmac.gui;

import com.sbux.jmeter.http.hmac.sampler.HttpSigSampler;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.protocol.http.config.gui.UrlConfigGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public final class HttpSigSamplerGui extends AbstractSamplerGui {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private UrlConfigGui urlConfigGui;
    private JCheckBox retrieveEmbeddedResources;
    private JCheckBox concurrentDwn;
    private JTextField concurrentPool;
    private JCheckBox isMon;
    private JCheckBox useMD5;
    private JLabeledTextField embeddedRE; // regular expression used to match against embedded resource URLs
    private JTextField sourceIpAddr; // does not apply to Java implementation
    private JComboBox<String> sourceIpType = new JComboBox<>(HTTPSamplerBase.getSourceTypeList());

    private final boolean isAJP;

    public HttpSigSamplerGui() {
        isAJP = false;
        init();
    }

    protected HttpSigSamplerGui(boolean ajp) {
        isAJP = ajp;
        init();
    }

    @Override
    public String getStaticLabel() {
        return "HTTP Signed Request";
    }

    @Override
    public void configure(TestElement element) {
        log.debug("configure TestElement=" + element.getClass().getCanonicalName());
        super.configure(element);
        final HTTPSamplerBase samplerBase = (HTTPSamplerBase) element;
        urlConfigGui.configure(element);
        retrieveEmbeddedResources.setSelected(samplerBase.isImageParser());
        concurrentDwn.setSelected(samplerBase.isConcurrentDwn());
        concurrentPool.setText(samplerBase.getConcurrentPool());
        isMon.setSelected(samplerBase.isMonitor());
        useMD5.setSelected(samplerBase.useMD5());
        embeddedRE.setText(samplerBase.getEmbeddedUrlRE());
        if (!isAJP) {
            sourceIpAddr.setText(samplerBase.getIpSource());
            sourceIpType.setSelectedIndex(samplerBase.getIpSourceType());
        }
    }

    @Override
    public TestElement createTestElement() {
        HTTPSamplerBase sampler = new HttpSigSampler();
        modifyTestElement(sampler);
        return sampler;
    }


    @Override
    public void modifyTestElement(TestElement element) {
        log.debug("modify TestElement=" + element.getClass().getCanonicalName());
        element.clear();
        urlConfigGui.modifyTestElement(element);
        final HTTPSamplerBase samplerBase = (HTTPSamplerBase) element;
        samplerBase.setImageParser(retrieveEmbeddedResources.isSelected());
        enableConcurrentDwn(retrieveEmbeddedResources.isSelected());
        samplerBase.setConcurrentDwn(concurrentDwn.isSelected());
        samplerBase.setConcurrentPool(concurrentPool.getText());
        samplerBase.setMonitor(isMon.isSelected());
        samplerBase.setMD5(useMD5.isSelected());
        samplerBase.setEmbeddedUrlRE(embeddedRE.getText());
        if (!isAJP) {
            samplerBase.setIpSource(sourceIpAddr.getText());
            samplerBase.setIpSourceType(sourceIpType.getSelectedIndex());
        }
        super.configureTestElement(element);
    }

    @Override
    public String getLabelResource() {
        return "web_testing_title"; // $NON-NLS-1$
    }


    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        // URL CONFIG
        urlConfigGui = new UrlConfigGui(true, true, true);

        // AdvancedPanel (embedded resources, source address and optional tasks)
        JPanel advancedPanel = new VerticalPanel();
        advancedPanel.add(createEmbeddedRsrcPanel());
        advancedPanel.add(createSourceAddrPanel());
        advancedPanel.add(createOptionalTasksPanel());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add(JMeterUtils
                .getResString("web_testing_basic"), urlConfigGui);
        tabbedPane.add(JMeterUtils
                .getResString("web_testing_advanced"), advancedPanel);

        JPanel emptyPanel = new JPanel();
        emptyPanel.setMaximumSize(new Dimension());

        add(makeTitlePanel(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(emptyPanel, BorderLayout.SOUTH);
    }

    protected JPanel createEmbeddedRsrcPanel() {
        // retrieve Embedded resources
        retrieveEmbeddedResources = new JCheckBox(JMeterUtils.getResString("web_testing_retrieve_images")); // $NON-NLS-1$
        // add a listener to activate or not concurrent dwn.
        retrieveEmbeddedResources.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    enableConcurrentDwn(true);
                } else {
                    enableConcurrentDwn(false);
                }
            }
        });
        // Download concurrent resources
        concurrentDwn = new JCheckBox(JMeterUtils.getResString("web_testing_concurrent_download")); // $NON-NLS-1$
        concurrentDwn.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (retrieveEmbeddedResources.isSelected() && e.getStateChange() == ItemEvent.SELECTED) {
                    concurrentPool.setEnabled(true);
                } else {
                    concurrentPool.setEnabled(false);
                }
            }
        });
        concurrentPool = new JTextField(2); // 2 column size
        concurrentPool.setMinimumSize(new Dimension(10, (int) concurrentPool.getPreferredSize().getHeight()));
        concurrentPool.setMaximumSize(new Dimension(30, (int) concurrentPool.getPreferredSize().getHeight()));

        final JPanel embeddedRsrcPanel = new HorizontalPanel();
        embeddedRsrcPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils
                .getResString("web_testing_retrieve_title"))); // $NON-NLS-1$
        embeddedRsrcPanel.add(retrieveEmbeddedResources);
        embeddedRsrcPanel.add(concurrentDwn);
        embeddedRsrcPanel.add(concurrentPool);

        // Embedded URL match regex
        embeddedRE = new JLabeledTextField(JMeterUtils.getResString("web_testing_embedded_url_pattern"), 20); // $NON-NLS-1$
        embeddedRsrcPanel.add(embeddedRE);

        return embeddedRsrcPanel;
    }

    protected JPanel createOptionalTasksPanel() {
        // OPTIONAL TASKS
        final JPanel checkBoxPanel = new VerticalPanel();
        checkBoxPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils
                .getResString("optional_tasks"))); // $NON-NLS-1$

        // Use MD5
        useMD5 = new JCheckBox(JMeterUtils.getResString("response_save_as_md5")); // $NON-NLS-1$

        // Is monitor
        isMon = new JCheckBox(JMeterUtils.getResString("monitor_is_title")); // $NON-NLS-1$
        checkBoxPanel.add(useMD5);
        checkBoxPanel.add(isMon);

        return checkBoxPanel;
    }

    protected JPanel createSourceAddrPanel() {
        final JPanel sourceAddrPanel = new HorizontalPanel();
        sourceAddrPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), JMeterUtils
                .getResString("web_testing_source_ip"))); // $NON-NLS-1$

        if (!isAJP) {
            // Add a new field source ip address (for HC implementations only)
            sourceIpType.setSelectedIndex(HTTPSamplerBase.SourceType.HOSTNAME.ordinal()); //default: IP/Hostname
            sourceAddrPanel.add(sourceIpType);

            sourceIpAddr = new JTextField();
            sourceAddrPanel.add(sourceIpAddr);
        }
        return sourceAddrPanel;
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public void clearGui() {
        super.clearGui();
        retrieveEmbeddedResources.setSelected(false);
        concurrentDwn.setSelected(false);
        concurrentPool.setText(String.valueOf(HTTPSamplerBase.CONCURRENT_POOL_SIZE));
        enableConcurrentDwn(false);
        isMon.setSelected(false);
        useMD5.setSelected(false);
        urlConfigGui.clear();
        embeddedRE.setText(""); // $NON-NLS-1$
        if (!isAJP) {
            sourceIpAddr.setText(""); // $NON-NLS-1$
            sourceIpType.setSelectedIndex(HTTPSamplerBase.SourceType.HOSTNAME.ordinal()); //default: IP/Hostname
        }
    }

    private void enableConcurrentDwn(boolean enable) {
        if (enable) {
            concurrentDwn.setEnabled(true);
            embeddedRE.setEnabled(true);
            if (concurrentDwn.isSelected()) {
                concurrentPool.setEnabled(true);
            }
        } else {
            concurrentDwn.setEnabled(false);
            concurrentPool.setEnabled(false);
            embeddedRE.setEnabled(false);
        }
    }
}
