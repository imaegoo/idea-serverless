#!/bin/sh
# Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

# ---------------------------------------------------------------------
# IntelliJ IDEA startup script.
# ---------------------------------------------------------------------

message()
{
  TITLE="Cannot start IntelliJ IDEA"
  if [ -n "$(command -v zenity)" ]; then
    zenity --error --title="$TITLE" --text="$1" --no-wrap
  elif [ -n "$(command -v kdialog)" ]; then
    kdialog --error "$1" --title "$TITLE"
  elif [ -n "$(command -v notify-send)" ]; then
    notify-send "ERROR: $TITLE" "$1"
  elif [ -n "$(command -v xmessage)" ]; then
    xmessage -center "ERROR: $TITLE: $1"
  else
    printf "ERROR: %s\n%s\n" "$TITLE" "$1"
  fi
}

if [ -z "$(command -v uname)" ] || [ -z "$(command -v realpath)" ] || [ -z "$(command -v dirname)" ] || [ -z "$(command -v cat)" ] || \
   [ -z "$(command -v egrep)" ]; then
  TOOLS_MSG="Required tools are missing:"
  for tool in uname realpath egrep dirname cat ; do
     test -z "$(command -v $tool)" && TOOLS_MSG="$TOOLS_MSG $tool"
  done
  message "$TOOLS_MSG (SHELL=$SHELL PATH=$PATH)"
  exit 1
fi

# shellcheck disable=SC2034
GREP_OPTIONS=''
OS_TYPE=$(uname -s)
OS_ARCH=$(uname -m)

# ---------------------------------------------------------------------
# Ensure $IDE_HOME points to the directory where the IDE is installed.
# ---------------------------------------------------------------------
IDE_BIN_HOME=$(dirname "$(realpath "$0")")
IDE_HOME=$(dirname "${IDE_BIN_HOME}")
CONFIG_HOME="${XDG_CONFIG_HOME:-${HOME}/.config}"

# ---------------------------------------------------------------------
# Locate a JRE installation directory command -v will be used to run the IDE.
# Try (in order): $IDEA_JDK, .../idea.jdk, .../jbr, $JDK_HOME, $JAVA_HOME, "java" in $PATH.
# ---------------------------------------------------------------------
JRE=""

# shellcheck disable=SC2154
if [ -n "$IDEA_JDK" ] && [ -x "$IDEA_JDK/bin/java" ]; then
  JRE="$IDEA_JDK"
fi

BITS=""
if [ -z "$JRE" ] && [ -s "${CONFIG_HOME}/JetBrains/IdeaIC2021.3/idea.jdk" ]; then
  USER_JRE=$(cat "${CONFIG_HOME}/JetBrains/IdeaIC2021.3/idea.jdk")
  if [ -x "$USER_JRE/bin/java" ]; then
    JRE="$USER_JRE"
  fi
fi

if [ -z "$JRE" ] && [ "$OS_TYPE" = "Linux" ] && [ "$OS_ARCH" = "x86_64" ] && [ -d "$IDE_HOME/jbr" ]; then
  JRE="$IDE_HOME/jbr"
fi

# shellcheck disable=SC2153
if [ -z "$JRE" ]; then
  if [ -n "$JDK_HOME" ] && [ -x "$JDK_HOME/bin/java" ]; then
    JRE="$JDK_HOME"
  elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JRE="$JAVA_HOME"
  fi
fi

if [ -z "$JRE" ]; then
  JAVA_BIN=$(command -v java)
else
  JAVA_BIN="$JRE/bin/java"
fi

if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
  message "No JRE found. Please make sure \$IDEA_JDK, \$JDK_HOME, or \$JAVA_HOME point to valid JRE installation."
  exit 1
fi

# ---------------------------------------------------------------------
# Collect JVM options and IDE properties.
# ---------------------------------------------------------------------
# shellcheck disable=SC2154
if [ -n "$IDEA_PROPERTIES" ]; then
  IDE_PROPERTIES_PROPERTY="-Didea.properties.file=$IDEA_PROPERTIES"
fi

BITS="64"
VM_OPTIONS_FILE=""
USER_VM_OPTIONS_FILE=""
# shellcheck disable=SC2154
if [ -n "$IDEA_VM_OPTIONS" ] && [ -r "$IDEA_VM_OPTIONS" ]; then
  # 1. $<IDE_NAME>_VM_OPTIONS
  VM_OPTIONS_FILE="$IDEA_VM_OPTIONS"
else
  # 2. <IDE_HOME>/bin/[<os>/]<bin_name>.vmoptions ...
  if [ -r "${IDE_BIN_HOME}/idea${BITS}.vmoptions" ]; then
    VM_OPTIONS_FILE="${IDE_BIN_HOME}/idea${BITS}.vmoptions"
  else
    test "${OS_TYPE}" = "Darwin" && OS_SPECIFIC="mac" || OS_SPECIFIC="linux"
    if [ -r "${IDE_BIN_HOME}/${OS_SPECIFIC}/idea${BITS}.vmoptions" ]; then
      VM_OPTIONS_FILE="${IDE_BIN_HOME}/${OS_SPECIFIC}/idea${BITS}.vmoptions"
    fi
  fi
  # ... [+ <IDE_HOME>.vmoptions (Toolbox) || <config_directory>/<bin_name>.vmoptions]
  if [ -r "${IDE_HOME}.vmoptions" ]; then
    USER_VM_OPTIONS_FILE="${IDE_HOME}.vmoptions"
  elif [ -r "${CONFIG_HOME}/JetBrains/IdeaIC2021.3/idea${BITS}.vmoptions" ]; then
    USER_VM_OPTIONS_FILE="${CONFIG_HOME}/JetBrains/IdeaIC2021.3/idea${BITS}.vmoptions"
  fi
fi

VM_OPTIONS=""
USER_GC=""
if [ -n "$USER_VM_OPTIONS_FILE" ]; then
  egrep -q -e "-XX:\+.*GC" "$USER_VM_OPTIONS_FILE" && USER_GC="yes"
fi
if [ -n "$VM_OPTIONS_FILE" -o -n "$USER_VM_OPTIONS_FILE" ]; then
  if [ -z "$USER_GC" -o -z "$VM_OPTIONS_FILE" ]; then
    VM_OPTIONS=$(cat "$VM_OPTIONS_FILE" "$USER_VM_OPTIONS_FILE" 2> /dev/null | egrep -v -e "^#.*")
  else
    VM_OPTIONS=$({ egrep -v -e "-XX:\+Use.*GC" "$VM_OPTIONS_FILE"; cat "$USER_VM_OPTIONS_FILE"; } 2> /dev/null | egrep -v -e "^#.*")
  fi
else
  message "Cannot find a VM options file"
fi

CLASSPATH="$IDE_HOME/lib/util.jar"
CLASSPATH="$CLASSPATH:$IDE_HOME/lib/bootstrap.jar"
CLASSPATH="$CLASSPATH:$JDK/lib/tools.jar"
# shellcheck disable=SC2154
if [ -n "$IDEA_CLASSPATH" ]; then
  CLASSPATH="$CLASSPATH:$IDEA_CLASSPATH"
fi
CLASSPATH="$CLASSPATH:$IDE_HOME/projector-server/lib/*"

# ---------------------------------------------------------------------
# Run the IDE.
# ---------------------------------------------------------------------
IFS="$(printf '\n\t')"
# shellcheck disable=SC2086
"$JAVA_BIN" \
  -classpath "$CLASSPATH" --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.desktop/java.awt.dnd.peer=ALL-UNNAMED \
  ${VM_OPTIONS} \
  "-XX:ErrorFile=$HOME/java_error_in_idea_%p.log" \
  "-XX:HeapDumpPath=$HOME/java_error_in_idea_.hprof" \
  "-Djb.vmOptionsFile=${USER_VM_OPTIONS_FILE:-${VM_OPTIONS_FILE}}" \
  ${IDE_PROPERTIES_PROPERTY} \
  -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.vendor.name=JetBrains -Didea.paths.selector=IdeaIC2021.3 -Didea.platform.prefix=Idea -Didea.jre.check=true -Dsplash=true \
  -Dorg.jetbrains.projector.server.classToLaunch=com.intellij.idea.Main org.jetbrains.projector.server.ProjectorLauncher \
  "$@"
