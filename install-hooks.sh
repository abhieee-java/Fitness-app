#!/bin/bash
# Script to install git pre-commit hooks.
set -e

HOOK_DIR=".git/hooks"
PRE_COMMIT_SRC=".githooks/pre-commit"
PRE_COMMIT_DEST="$HOOK_DIR/pre-commit"

if [ -d ".git" ]; then
    echo "📦 Found .git directory, installing pre-commit hook..."
    mkdir -p "$HOOK_DIR"
    cp "$PRE_COMMIT_SRC" "$PRE_COMMIT_DEST"
    chmod +x "$PRE_COMMIT_DEST"
    echo "✅ Successfully installed pre-commit hook to $PRE_COMMIT_DEST"
else
    echo "⚠️ .git directory not found. Please run this script in the root of your Git repository."
fi
