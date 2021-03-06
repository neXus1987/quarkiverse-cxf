package io.quarkiverse.cxf;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CxfClientProducer {
    private static final Logger LOGGER = Logger.getLogger(CxfClientProducer.class);

    public CXFClientInfo getInfo() {
        return null;
    }

    public Object loadCxfClient() {

        CXFClientInfo cxfClientInfo = getInfo();
        Class<?> seiClass;
        try {
            seiClass = Class.forName(cxfClientInfo.getSei(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("either webservice interface (client) or implementation (server) is mandatory");
            return null;
        }

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(new QuarkusClientFactoryBean(cxfClientInfo.getClassNames()));

        factory.setServiceClass(seiClass);
        factory.setServiceName(new QName(cxfClientInfo.getWsNamespace(), cxfClientInfo.getWsName()));
        if (cxfClientInfo.getEpName() != null) {
            factory.setEndpointName(new QName(cxfClientInfo.getEpNamespace(), cxfClientInfo.getEpName()));
        }
        factory.setAddress(cxfClientInfo.getEndpointAddress());
        if (cxfClientInfo.getSoapBinding() != null) {
            factory.setBindingId(cxfClientInfo.getSoapBinding());
        }
        if (cxfClientInfo.getWsdlUrl() != null && !cxfClientInfo.getWsdlUrl().isEmpty()) {
            factory.setWsdlURL(cxfClientInfo.getWsdlUrl());
        }
        if (cxfClientInfo.getUsername() != null) {
            factory.setUsername(cxfClientInfo.getUsername());
        }
        if (cxfClientInfo.getPassword() != null) {
            factory.setPassword(cxfClientInfo.getPassword());
        }
        for (String feature : cxfClientInfo.getFeatures()) {
            addToCols(feature, factory.getFeatures());
        }
        for (String inInterceptor : cxfClientInfo.getInInterceptors()) {
            addToCols(inInterceptor, factory.getInInterceptors());
        }
        for (String outInterceptor : cxfClientInfo.getOutInterceptors()) {
            addToCols(outInterceptor, factory.getOutInterceptors());
        }
        for (String outFaultInterceptor : cxfClientInfo.getOutFaultInterceptors()) {
            addToCols(outFaultInterceptor, factory.getOutFaultInterceptors());
        }
        for (String inFaultInterceptor : cxfClientInfo.getInFaultInterceptors()) {
            addToCols(inFaultInterceptor, factory.getInFaultInterceptors());
        }

        LOGGER.info("cxf client loaded for " + cxfClientInfo.getSei());
        return factory.create();
    }

    private <T> void addToCols(String className, List<T> cols) {
        Class<?> cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // silent failed
            return;
        }
        T item = null;
        try {
            Object o = CDI.current().select(cls).get();
            item = (T) o;
            if (item != null) {
                cols.add(item);
            }
        } catch (ClassCastException | UnsatisfiedResolutionException e) {
            //silent fail
        }
        if (item != null) {
            return;
        }
        // if not found with beans just generate it.

        try {
            Object o = cls.getConstructor().newInstance();
            item = (T) o;
            if (item != null) {
                cols.add(item);
            }
        } catch (Exception e) {
        }
    }
}
