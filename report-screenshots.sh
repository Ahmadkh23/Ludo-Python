#!/usr/bin/env sh
# Re-creates the system-report screenshots in test-results/ui from real gameplay.
# Needs a graphical desktop (two dialogs open and close by themselves). Run sh build.sh first.
set -e
cd "$(dirname "$0")"
SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
java -cp "build/classes${SEP}build/test-classes" com.eduludo.Model.ReportScreenshots test-results/ui
