package com.sbux.jmeter.http.sampler;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpSamplerProxy extends HTTPSamplerBase implements Interruptible {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggingManager.getLoggerForClass();
    protected static final Set<String> APPLIABLE_CONFIG_CLASSES = new HashSet<>(
            Arrays.asList(
                    "org.apache.jmeter.config.gui.LoginConfigGui",
                    "org.apache.jmeter.protocol.http.config.gui.HttpDefaultsGui",
                    "org.apache.jmeter.config.gui.SimpleConfigGui",
                    "org.apache.jmeter.protocol.http.gui.HeaderPanel",
                    "org.apache.jmeter.protocol.http.control.DNSCacheManager",
                    "org.apache.jmeter.protocol.http.gui.DNSCachePanel",
                    "org.apache.jmeter.protocol.http.gui.AuthPanel",
                    "org.apache.jmeter.protocol.http.gui.CacheManagerGui",
                    "org.apache.jmeter.protocol.http.gui.CookiePanel"
            ));

    private HttpAbstractImpl impl;

    private transient volatile boolean notifyFirstSampleAfterLoopRestart;

    public HttpSamplerProxy(){
        super();
    }

    public HttpSamplerProxy(String impl){
        super();
        setImplementation(impl);
    }

    @Override
    public boolean applies(ConfigTestElement configElement) {

        String guiClass = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();
        log.debug("configElement=" + guiClass);
        return true;
    }

    @Override
    protected HTTPSampleResult sample(URL u, String method, boolean areFollowingRedirect, int depth) {
        if (impl == null) {
            impl = new HttpAbstractImpl(getImplementation(), this);
        }
        if(notifyFirstSampleAfterLoopRestart) {
            impl.notifyFirstSampleAfterLoopRestart();
            notifyFirstSampleAfterLoopRestart = false;
        }
        return impl.sample(u, method, areFollowingRedirect, depth);
    }

    @Override
    public void threadFinished(){
        if (impl != null){
            impl.threadFinished(); // Forward to sampler
        }
    }

    @Override
    public boolean interrupt() {
        if (impl != null) {
            return impl.interrupt(); // Forward to sampler
        }
        return false;
    }

    @Override
    public void testIterationStart(LoopIterationEvent event) {
        notifyFirstSampleAfterLoopRestart = true;
    }

}
