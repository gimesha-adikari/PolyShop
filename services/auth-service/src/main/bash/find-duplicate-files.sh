#!/usr/bin/env bash
set -euo pipefail
git ls-files | xargs -I{} sh -c 'sha1sum "{}" | sed "s| | |g"' | awk '{print $1 " " $2}' | sort | uniq -w40 -D
