package com.sbux.jmeter.http.sampler;

import org.apache.jmeter.protocol.http.sampler.HTTPAbstractImpl;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerFactory;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class HttpAbstractImpl {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private final HTTPAbstractImpl impl;

    public HttpAbstractImpl(String implementation, HTTPSamplerBase base) {
        impl = HTTPSamplerFactory.getImplementation(implementation, base);
    }

    protected HTTPSampleResult sample(final URL url, final String httpMethod, final boolean areFollowingRedirect, final int frameDepth) {

        Class[] argTypes = new Class[]{URL.class, String.class, boolean.class, int.class};
        Object[] args = new Object[]{url, httpMethod, areFollowingRedirect, frameDepth};
        return (HTTPSampleResult) invoke("sample", argTypes, args);
    }

    public boolean interrupt() {
        return (boolean) invoke("interrupt");
    }

    protected void notifyFirstSampleAfterLoopRestart() {
        invoke("notifyFirstSampleAfterLoopRestart");
    }

    protected void threadFinished() {
        invoke("threadFinished");
    }

    private Object invoke(final String methodName) {
        //DAM don't be evil like me
        try {
            Method method = impl.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(impl);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            log.debug(e.getMessage(), e);
            return null;
        }

    }

    private Object invoke(final String methodName, final Class[] argTypes, final Object[] args) {
        try {
            Method method = impl.getClass().getDeclaredMethod(methodName, argTypes);
            method.setAccessible(true);
            return method.invoke(impl, args);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
