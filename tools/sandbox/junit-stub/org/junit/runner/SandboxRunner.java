package org.junit.runner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Discovers and runs *Test classes from a class directory (sandbox-only tool). */
public final class SandboxRunner {
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        List<String> classNames = new ArrayList<>();
        scan(root, root, classNames);
        classNames.sort(Comparator.naturalOrder());

        int tests = 0, failures = 0;
        List<String> failureDetails = new ArrayList<>();

        for (String name : classNames) {
            if (!name.endsWith("Test")) continue;
            Class<?> cls = Class.forName(name);
            List<Method> before = new ArrayList<>(), after = new ArrayList<>(), testMethods = new ArrayList<>();
            for (Method m : cls.getMethods()) {
                if (m.isAnnotationPresent(Before.class)) before.add(m);
                if (m.isAnnotationPresent(After.class)) after.add(m);
                if (m.isAnnotationPresent(Test.class)) testMethods.add(m);
            }
            testMethods.sort(Comparator.comparing(Method::getName));
            for (Method test : testMethods) {
                tests++;
                Object instance = cls.getDeclaredConstructor().newInstance();
                try {
                    for (Method b : before) b.invoke(instance);
                    test.invoke(instance);
                    for (Method a : after) a.invoke(instance);
                    System.out.println("  PASS " + cls.getSimpleName() + "." + test.getName());
                } catch (InvocationTargetException e) {
                    failures++;
                    Throwable cause = e.getCause();
                    System.out.println("  FAIL " + cls.getSimpleName() + "." + test.getName()
                        + " -> " + cause);
                    StringBuilder sb = new StringBuilder(cls.getName() + "." + test.getName() + "\n    " + cause);
                    for (StackTraceElement el : cause.getStackTrace()) {
                        if (el.getClassName().startsWith("com.craftworld3d")) {
                            sb.append("\n      at ").append(el);
                        }
                    }
                    failureDetails.add(sb.toString());
                }
            }
        }
        System.out.println();
        System.out.println("Tests run: " + tests + ", Failures: " + failures);
        for (String d : failureDetails) System.out.println("\n" + d);
        System.exit(failures == 0 && tests > 0 ? 0 : 1);
    }

    private static void scan(File root, File dir, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scan(root, f, out);
            } else if (f.getName().endsWith(".class") && !f.getName().contains("$")) {
                String rel = f.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
                out.add(rel.replace(File.separatorChar, '.').replaceAll("\\.class$", ""));
            }
        }
    }
}
