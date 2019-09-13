package com.fnproject.fn.testing;

import com.fnproject.fn.runtime.EntryPoint;
import com.fnproject.fn.runtime.EventCodec;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Testing classloader that loads all classes afresh when needed, otherwise delegates shared classes to the parent classloader
 */
public class FnTestingClassLoader extends ClassLoader {
    private final List<String> sharedPrefixes;
    private final Map<String, Class<?>> loaded = new HashMap<>();

    private interface NotForked {
    }

    private static Class<?> UNFORKED_TYPE = NotForked.class;

    public FnTestingClassLoader(ClassLoader parent, List<String> sharedPrefixes) {
        super(parent);
        this.sharedPrefixes = sharedPrefixes;
    }

    boolean isShared(String classOrPackageName) {
        for (String prefix : sharedPrefixes) {
            if (("=" + classOrPackageName).equals(prefix) || classOrPackageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public Class<?> getClass(Map<String, Class<?>> loaded, String className, byte[] clsBytes) {
        Class<?> cls = defineClass(className, clsBytes, 0, clsBytes.length);
        resolveClass(cls);
        loaded.put(className,cls);
        return cls;
    };

    public void loadInClass(Map<String, Class<?>> loaded, String className, byte[] clsBytes) {
        Class<?> cls = defineClass(className, clsBytes, 0, clsBytes.length);
        resolveClass(cls);
        loaded.put(className,cls);
    };



    @Override
    public synchronized Class<?> loadClass(String className) throws ClassNotFoundException {

        Class<?> definedClass = loaded.get(className);
        if (definedClass != null) {
            return definedClass;
        }




        if (className.equals(ResourcePrincipalAuthenticationDetailsProvider.class.getName())) {
            byte[] clsBytes ;
            DynamicType.Unloaded<ResourcePrincipalAuthenticationDetailsProvider> builderType;
            try {
               builderType = new ByteBuddy()
                        .rebase(ResourcePrincipalAuthenticationDetailsProvider.class, ClassFileLocator.ForClassLoader.of(this))
                        .constructor(ElementMatchers.any())
                        .intercept(MethodDelegation.to(FakeAuthenticationDetailsProvider.class.getDeclaredConstructor()).andThen(SuperMethodCall.INSTANCE))
                        .method(named("builder"))
                        .intercept(MethodDelegation.to(FakeAuthenticationDetailsProvider.class))
                        .make();
                clsBytes = builderType
                        .getBytes();
//                FileUtils.writeByteArrayToFile(new File("/tmp/osx.classByteFile"), clsBytes);


            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for(Map.Entry<TypeDescription,byte[]> e : builderType.getAuxiliaryTypes().entrySet()){
                byte[] auxBytes = e.getValue();
                loadInClass(loaded, e.getKey().getName(), auxBytes);
            }

            return getClass(loaded, className, clsBytes);

        }

        if (className.equals(ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder.class.getName())) {
            DynamicType.Unloaded<ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder> buildtype = null;
            try {
                try {
                    buildtype = new ByteBuddy()
                            .rebase(ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder.class, ClassFileLocator.ForClassLoader.of(this))
                            .constructor(ElementMatchers.any())
                            .intercept(MethodDelegation.to((FakeAuthenticationDetailsProvider.builder())))
                            .method(named("build"))
                            .intercept(MethodDelegation.to(FakeAuthenticationDetailsProvider.FakeBuilder.class))
                            .make();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] clsBytes = buildtype
                    .getBytes();

            for(Map.Entry<TypeDescription,byte[]> e : buildtype.getAuxiliaryTypes().entrySet()){
                byte[] auxBytes = e.getValue();
                loadInClass(loaded, e.getKey().getName(), auxBytes);
            }

            return getClass(loaded, className, clsBytes);

        }

        Class<?> cls = null;
        if (isShared(className) || className.contains("auxiliary")) {
            cls = getParent().loadClass(className);
        }

        if (cls == null) {
            try {
                InputStream in = getResourceAsStream(className.replace('.', '/') + ".class");
                if (in == null) {
                    throw new ClassNotFoundException("Class not found :" + className);
                }

                byte[] clsBytes = IOUtils.toByteArray(in);
                cls = defineClass(className, clsBytes, 0, clsBytes.length);
                resolveClass(cls);

            } catch (IOException e) {
                throw new ClassNotFoundException(className, e);
            }
        }
        loaded.put(className, cls);
        return cls;
    }


    public int run(Map<String, String> mutableEnv, EventCodec codec, PrintStream functionErr, String... s) {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {


            Thread.currentThread().setContextClassLoader(this);

            Class<?> entryPoint_class = loadClass(EntryPoint.class.getName());
            Object entryPoint = entryPoint_class.newInstance();

            return (int) getMethodInClassLoader(entryPoint, "run", Map.class, EventCodec.class, PrintStream.class, String[].class)
                    .invoke(entryPoint, mutableEnv, codec, functionErr, s);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private Method getMethodInClassLoader(Object target, String method, Class... types) throws NoSuchMethodException {
        Class<?> targetClass;
        if (target instanceof Class) {
            targetClass = (Class) target;
        } else {
            targetClass = target.getClass();
        }
        return targetClass.getMethod(method, types);
    }
}