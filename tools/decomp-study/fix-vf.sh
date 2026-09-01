#!/bin/bash
# Vineflower 编译修复清单(仅作用于 /tmp/decomp-study/dec-vf 副本)
set -e
cd /tmp/decomp-study
python3 scripts/_vf_fixes.py
python3 scripts/_vf_unfold.py
echo "fix-vf.sh done"
