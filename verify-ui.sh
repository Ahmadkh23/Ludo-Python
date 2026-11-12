#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"
java -Djava.awt.headless=true -cp build/classes:build/test-classes com.eduludo.Model.Iteration1Check
