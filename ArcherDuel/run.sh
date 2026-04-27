#!/bin/bash
# ArcherDuel — Build & Run Script (Linux / macOS)
# Requires Java JDK 11+ installed (javac + java on PATH)

echo "=============================="
echo "  ArcherDuel — Build & Run"
echo "=============================="

# Check javac is available
if ! command -v javac &> /dev/null; then
    echo "ERROR: javac not found. Please install a Java JDK (11+)."
    echo "  Ubuntu/Debian: sudo apt-get install default-jdk"
    echo "  macOS:         brew install openjdk"
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -1)"

# Create output dir
mkdir -p bin

# Compile all sources
echo ""
echo "Compiling..."
javac -d bin src/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Compilation successful!"
echo ""
echo "Starting game..."
java -cp bin Main
