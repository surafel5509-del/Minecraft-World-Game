package org.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Minimal JUnit4-compatible stub used ONLY for sandbox verification where
 * Maven Central is unreachable. The real project resolves junit:junit:4.13.2
 * through Gradle; this stub mirrors the subset of the API the tests use.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
    Class<? extends Throwable> expected() default None.class;

    class None extends Throwable {
        private None() {}
    }
}
