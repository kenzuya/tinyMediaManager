#!/usr/bin/env bash
#
# tinyMediaManager v4 by Manuel Laggner
# https://www.tinymediamanager.org/
# SPDX-License-Identifier: Apache-2.0
#
# legacy launch script for tinyMediaManager

# Allow the script to be called from any directory and through symlinks
TMM_DIR="$(dirname "$(test -L "${BASH_SOURCE[0]}" && \
    readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")")"

# Ma! Start the car! :)
cd "$TMM_DIR" || return 1
./tinyMediaManager "$@"
