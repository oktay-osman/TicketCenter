package com.oktayosman.ticketcenter.util;

import org.springframework.context.ApplicationContext;

public class SpringContext {
    private static ApplicationContext context;

    public static void setContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext is not initialized");
        }
        return context.getBean(beanClass);
    }

    public static Object getBean(String beanName) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext is not initialized");
        }
        return context.getBean(beanName);
    }
}