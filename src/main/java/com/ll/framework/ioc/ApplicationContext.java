package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private Reflections reflections;
    private final Map<String, Object> singletonObjects = new HashMap<>();
    private final Map<String, Class<?>> beanDefinitionMap = new HashMap<>();

    public ApplicationContext(String basePackage) {
        reflections = new Reflections(basePackage);
    }

    public void init() {
        Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class);

        for (Class<?> clazz : components) {
            String beanName = clazz.getSimpleName().substring(0, 1).toLowerCase()
                    + clazz.getSimpleName().substring(1);

            beanDefinitionMap.put(beanName, clazz);
        }
    }

    public <T> T genBean(String beanName) {
        Object singleton = singletonObjects.get(beanName);
        if (singleton != null) {
            return (T) singleton;
        }

        Class<?> someClass = beanDefinitionMap.get(beanName);
        if (someClass == null) {
            throw new RuntimeException("Bean not found: " + beanName);
        }

        try {
            Object instance = someClass.getDeclaredConstructor().newInstance();
            singletonObjects.put(beanName, instance);

            return (T) instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
