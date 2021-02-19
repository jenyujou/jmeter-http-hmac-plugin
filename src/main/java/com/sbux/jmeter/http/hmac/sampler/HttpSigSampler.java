package com.sbux.jmeter.http.hmac.sampler;

import com.sbux.jmeter.http.hmac.config.HttpSigManager;
import com.sbux.jmeter.http.hmac.gui.HttpSigManagerGui;
import com.sbux.jmeter.http.sampler.HttpSamplerProxy;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

public final class HttpSigSampler extends HttpSamplerProxy {
    private static final Logger log = LoggingManager.getLoggerForClass();
    public static final String HTTP_SIG_MANAGER = "HTTPSigSampler.Sig_Manager";

    public HttpSigSampler() {
        APPLIABLE_CONFIG_CLASSES.add(HttpSigManagerGui.class.getCanonicalName());
    }

    @Override
    protected HTTPSampleResult sample(java.net.URL u, String method, boolean areFollowingRedirect, int depth) {
        if (getHeaderManager() == null) {
            setHeaderManager(new HeaderManager());
        } else {
            getHeaderManager().removeHeaderNamed("(request-target)");
            getHeaderManager().removeHeaderNamed("Digest");
            getHeaderManager().removeHeaderNamed("Date");
            getHeaderManager().removeHeaderNamed("Authorization");
        }

        for (Header header : getHttpSigManager().getHeaders(getBody(), getMethod(), getPath())) {
            getHeaderManager().add(header);
        }

        return super.sample(u, method, areFollowingRedirect, depth);
    }

    @Override
    public void addTestElement(TestElement el) {
        log.debug("addTestElement TestElement=" + el);

        if (el instanceof HttpSigManager) {
            setHttpSigManager((HttpSigManager) el);
        } else {
            super.addTestElement(el);
        }
    }

    public HttpSigManager getHttpSigManager() {
        return (HttpSigManager) getProperty(HTTP_SIG_MANAGER).getObjectValue();
    }

    public void setHttpSigManager(HttpSigManager httpSigManager) {
        HttpSigManager mgr = getHttpSigManager();
        if (mgr != null) {
            log.warn("Existing AuthManager " + mgr.getName() + " superseded by " + httpSigManager.getName());
        }

        setProperty(new TestElementProperty(HTTP_SIG_MANAGER, httpSigManager));
    }

    private String getBody() {
        if (getArguments().getArgumentsAsMap().size() == 1) {
            return getArguments().getArgument(0).getValue();
        } else {
            return "";
        }
    }
}
