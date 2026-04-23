package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {
    private final String basePackage;
    private Map<String, Object> beans = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        try {
            // 패키지 경로를 파일 시스템 경로로 변환
            String path = basePackage.replace(".", "/");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            File directory = new File(resource.toURI());

            scanDirectory(directory, basePackage);  // 탐색 시작
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 재귀로 패키지 하위의 클래스 파일까지 탐색 메서드
    private void scanDirectory(File directory, String packageName) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().replace(".class", "");
                String fullClassName = packageName + "." + className;
                try {
                    Class<?> clazz = Class.forName(fullClassName);
                    if (isComponent(clazz)) {
                        className = Ut.str.lcfirst(className);  // 앞글자를 소문자로 변환
                        System.out.println("Bean 등록: " + className);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Component 어노테이션 여부 반환 메서드
    private boolean isComponent(Class<?> clazz) {
        if(clazz.isAnnotationPresent(Component.class)) return true;

        for(Annotation annotation : clazz.getAnnotations()) {
            if(annotation.annotationType().isAnnotationPresent(Component.class)) {
                return true;
            }
        }

        return false;
    }

    public <T> T genBean(String beanName) {
        return (T) beans.get(beanName);
    }
}