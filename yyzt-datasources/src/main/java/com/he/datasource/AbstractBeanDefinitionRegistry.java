package com.he.datasource;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.Environment;

public class AbstractBeanDefinitionRegistry implements EnvironmentAware {
    private Binder binder;
    protected   Environment environment;
    protected ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    protected <T> T getProperties(String prefix,Class<T> classType){
        if(binder==null){
            binder = Binder.get(environment);
        }
        return binder.bind(prefix, Bindable.of(classType)).get();
    }
}
