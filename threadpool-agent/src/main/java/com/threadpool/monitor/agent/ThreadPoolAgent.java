package com.threadpool.monitor.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.jar.JarFile;


/**
 * @Date 2021/1/30 下午2:04
 * @Created by haoyann
 */
public class ThreadPoolAgent {
    
    private static final String THREAD_POOL_EXECUTOR_NAME = "java.util.concurrent.ThreadPoolExecutor";
    
    private static final String WEB_ENDPOINT_DISCOVERER_NAME = "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer";
    
    public static void premain(String arguments, Instrumentation instrumentation) {
        
        try {
            instrumentation
                    .appendToBootstrapClassLoaderSearch(new JarFile(findAgentPath() + "/threadpool-manager.jar"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("net.bytebuddy."))
                .type(ElementMatchers.is(ThreadPoolExecutor.class)
                        .or(ElementMatchers.named(WEB_ENDPOINT_DISCOVERER_NAME)))
                .transform((builder, typeDescription, classLoader, module) -> {
                    if (THREAD_POOL_EXECUTOR_NAME.equals(typeDescription.getName())) {
                        return builder.visit(Advice.to(ThreadConstructorInterceptor.class)
                                .on(ElementMatchers.isConstructor().and(ElementMatchers.takesArguments(7))))
                                .visit(Advice.to(ThreadPoolRejectInterceptor.class)
                                        .on(ElementMatchers.named("reject")));
                    }
                    return builder;
                }).transform((builder, typeDescription, classLoader, module) -> {
            if (WEB_ENDPOINT_DISCOVERER_NAME.equals(typeDescription.getName())) {
                return builder
                        .visit(Advice.to(EndpointDiscovererInterceptor.class).on(ElementMatchers.isConstructor()));
            }
            return builder;
        }).with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION).with(new Listener()).installOn(instrumentation);
    }
    
    private static String findAgentPath() {
        String classResourcePath = ThreadPoolAgent.class.getName().replaceAll("\\.", "/") + ".class";
        
        URL resource = ClassLoader.getSystemClassLoader().getResource(classResourcePath);
        if (resource != null) {
            String urlString = resource.toString();
            String jarPath = urlString.substring(urlString.indexOf("file:"), urlString.indexOf("!"));
            File agentJarFile;
            try {
                agentJarFile = new File(new URL(jarPath).toURI());
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException("agent jar path can`t find", e);
            }
            
            return agentJarFile.getParent();
        }
        throw new RuntimeException("agent jar path can`t find");
    }
    
    private static class Listener implements AgentBuilder.Listener {
        
        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        
        }
        
        @Override
        public void onTransformation(final TypeDescription typeDescription, final ClassLoader classLoader,
                final JavaModule module, final boolean loaded, final DynamicType dynamicType) {
            
            System.out.println("On Transformation class: " + typeDescription.getName());
            
        }
        
        @Override
        public void onIgnored(final TypeDescription typeDescription, final ClassLoader classLoader,
                final JavaModule module, final boolean loaded) {
            
        }
        
        @Override
        public void onError(final String typeName, final ClassLoader classLoader, final JavaModule module,
                final boolean loaded, final Throwable throwable) {
            System.out.println("Enhance class " + typeName + " error.");
        }
        
        @Override
        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        
        }
    }
    
    
}
