#!/usr/bin/env bash
# Sandbox verification script: compiles the core module + tests with ECJ and
# runs them with the minimal JUnit-compatible runner.
# (On a normal dev machine just use: ./gradlew :core:test)
set -e
cd "$(dirname "$0")/../.."

JH=${JAVA_HOME:-/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime}
ECJ=${ECJ_JAR:-/tmp/ecj-batch.jar}
OUT=tools/out

mkdir -p $OUT/core $OUT/stub $OUT/tests

# 1. Compile the junit stub + runner.
find tools/sandbox/junit-stub -name "*.java" > /tmp/stub.txt
"$JH/bin/java" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
  -17 -proc:none -nowarn -d $OUT/stub @/tmp/stub.txt

# 2. Compile core main sources.
find core/src/main/java -name "*.java" > /tmp/core.txt
"$JH/bin/java" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
  -17 -proc:none -nowarn -d $OUT/core @/tmp/core.txt

# 3. Compile tests against core + stub.
find core/src/test/java -name "*.java" > /tmp/tests.txt
"$JH/bin/java" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
  -17 -proc:none -nowarn -cp "$OUT/core:$OUT/stub" -d $OUT/tests @/tmp/tests.txt

# 4. Run.
"$JH/bin/java" -cp "$OUT/core:$OUT/stub:$OUT/tests" org.junit.runner.SandboxRunner $OUT/tests
