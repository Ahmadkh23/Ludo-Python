package com.eduludo.testing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs every method annotated with {@link Test} in the given test classes and prints a report.
 * Exits with status 1 if any test fails, so the build scripts can stop.
 *
 * <pre>java -cp build/classes;build/test-classes com.eduludo.testing.TestRunner --scan build/test-classes</pre>
 */
public final class TestRunner {

    public static void main(String[] args) throws Exception {
        List<String> classes = new ArrayList<>();
        if (args.length == 2 && args[0].equals("--scan")) {
            classes.addAll(scan(Path.of(args[1])));
        } else {
            classes.addAll(Arrays.asList(args));
        }
        int passed = 0;
        List<String> failures = new ArrayList<>();
        for (String className : classes) {
            Class<?> cls = Class.forName(className);
            Method[] methods = cls.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            System.out.println(cls.getSimpleName());
            for (Method m : methods) {
                if (!m.isAnnotationPresent(Test.class) || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Object instance = cls.getDeclaredConstructor().newInstance();
                m.setAccessible(true);
                try {
                    m.invoke(instance);
                    passed++;
                    System.out.println("  PASS  " + m.getName());
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    failures.add(cls.getSimpleName() + "." + m.getName() + ": " + cause);
                    System.out.println("  FAIL  " + m.getName() + " -> " + cause);
                }
            }
        }
        System.out.println();
        System.out.println("Tests run: " + (passed + failures.size()) + ", passed: " + passed + ", failed: " + failures.size());
        if (!failures.isEmpty()) {
            failures.forEach(f -> System.out.println("  " + f));
            System.exit(1);
        }
    }

    /** Finds every compiled class named *Test under the folder (top-level classes only). */
    static List<String> scan(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith("Test.class") && !name.equals("Test.class"); // skip the @Test annotation
                    })
                    .map(p -> root.relativize(p).toString().replace('\\', '/').replace('/', '.'))
                    .map(s -> s.substring(0, s.length() - ".class".length()))
                    .sorted()
                    .toList();
        }
    }
}

