#!/usr/bin/env sh
# =============================================================================
#  EduLudo build (macOS / Linux / Git Bash). Needs only a JDK 17 or newer.
#    ./build.sh        compile, run all tests, create dist/EduLudo.jar
# =============================================================================
set -e
cd "$(dirname "$0")"

command -v javac >/dev/null 2>&1 || { echo "[ERROR] javac not found. Install a JDK 17+."; exit 1; }

echo "=== Cleaning"
rm -rf build dist/EduLudo.jar
mkdir -p build/classes build/test-classes dist

SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac

echo "=== Compiling application"
find src/main/java -name "*.java" | sed 's/.*/"&"/' > build/main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes @build/main-sources.txt
cp -R src/main/resources/. build/classes/

echo "=== Compiling tests"
find src/test/java -name "*.java" | sed 's/.*/"&"/' > build/test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes @build/test-sources.txt

echo "=== Running tests"
java -Djava.awt.headless=true -cp "build/classes${SEP}build/test-classes" com.eduludo.testing.TestRunner --scan build/test-classes

echo "=== Creating dist/EduLudo.jar"
jar --create --file dist/EduLudo.jar --main-class com.eduludo.App -C build/classes .
echo "Done: dist/EduLudo.jar  (run with: java -jar dist/EduLudo.jar)"

