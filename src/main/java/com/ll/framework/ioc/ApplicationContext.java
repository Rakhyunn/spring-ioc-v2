package com.ll.framework.ioc;

import java.io.File;
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
            String path = basePackage.replace(".", "/");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            File directory = new File(resource.toURI());

            scanDirectory(directory, basePackage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File directory, String packageName) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().replace(".class", "");
                String fullClassName = packageName + "." + className;
                System.out.println("스캔된 클래스: " + fullClassName);
            }
        }
    }

    public <T> T genBean(String beanName) {
        return (T) beans.get(beanName);
    }
}