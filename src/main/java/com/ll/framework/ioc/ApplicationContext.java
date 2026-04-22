package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

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
            Constructor<?>[] constructors = someClass.getDeclaredConstructors();

            Constructor<?> targetConstructor = null;

            for (Constructor<?> constructor : constructors) {

                Class<?>[] paramTypes = constructor.getParameterTypes();

                boolean canCreate = true;

                for (Class<?> paramType : paramTypes) {
                    if (!canResolve(paramType)) {   // 이 타입 Bean을 만들 수 있는지 확인
                        canCreate = false;
                        break;
                    }
                }

                if (canCreate) {
                    if (targetConstructor == null ||
                            constructor.getParameterCount() > targetConstructor.getParameterCount()) {
                        targetConstructor = constructor;
                    }
                }
            }

            if (targetConstructor == null) {
                throw new RuntimeException("적절한 생성자를 찾을 수 없음: " + someClass.getName());
            }

            targetConstructor.setAccessible(true);

            Class<?>[] paramTypes = targetConstructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = getBeanByType(paramTypes[i]);
            }

            Object instance = targetConstructor.newInstance(params);

            singletonObjects.put(beanName, instance);

            return (T) instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getBeanByType(Class<?> type) {
        for (Object bean : singletonObjects.values()) {
            if (type.isAssignableFrom(bean.getClass())) {
                return bean;
            }
        }

        for (Map.Entry<String, Class<?>> entry : beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue())) {
                return genBean(entry.getKey());
            }
        }

        throw new RuntimeException("No bean found for type: " + type);
    }

    private boolean canResolve(Class<?> type) {
        for (Object bean : singletonObjects.values()) {
            if (type.isAssignableFrom(bean.getClass())) {
                return true;
            }
        }

        for (Class<?> clazz : beanDefinitionMap.values()) {
            if (type.isAssignableFrom(clazz)) {
                return true;
            }
        }

        return false;
    }
}
