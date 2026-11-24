#!/usr/bin/env bash
# Deletes the scattered strings_*.xml files that were merged into strings.xml
# in values/, values-en/, values-ja/ and values-zh-rCN/. Run from the repo root:  bash cleanup_scattered_strings.sh
set -e
RES="app/src/main/res"
for d in values values-en values-ja values-zh-rCN; do
  for f in "$RES/$d"/strings_*.xml; do
    [ -e "$f" ] && rm -v "$f"
  done
done
echo "Done. You can delete this script now."
