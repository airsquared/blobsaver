#!/usr/bin/env bash

if [ $# -ne 1 ]; then
    >&2 echo "Must provide exactly one argument for version"
    exit 1
fi

brew tap homebrew/cask && brew bump-cask-pr blobsaver --no-audit --no-style --version "$1"
brew untap homebrew/cask
