#!/usr/bin/env bash
set -euo pipefail

gradle clean build lint assembleDebug
