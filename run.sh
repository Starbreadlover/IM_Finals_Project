#!/bin/sh
# Compiles and runs the IT Help Desk app.
#   ./run.sh        -> GUI (Swing) version
#   ./run.sh cli    -> console version
cd "$(dirname "$0")" || exit 1
rm -rf out
javac --release 17 -d out -cp lib/mysql-connector-j-8.4.0.jar $(find src -name "*.java") || exit 1
if [ "$1" = "cli" ]; then
    java -cp "out:lib/mysql-connector-j-8.4.0.jar" helpdesk.Main
else
    java -cp "out:lib/mysql-connector-j-8.4.0.jar" helpdesk.gui.GuiMain
fi
