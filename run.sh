#!/bin/sh
# Compiles and runs the IT Help Desk console app.
#   ./run.sh
cd "$(dirname "$0")" || exit 1
rm -rf out
javac --release 17 -d out -cp lib/mysql-connector-j-8.4.0.jar $(find src -name "*.java") || exit 1
java -cp "out:lib/mysql-connector-j-8.4.0.jar" helpdesk.Main
