#!/usr/bin/env python3
"""PreToolUse hook: before a `git commit` that includes code, make sure ROADMAP.md was updated.

Blocks the commit (with a reminder for Claude) when backend/ or mobile/ changes are committed
without ROADMAP.md. Bypass when nothing on the roadmap was finished: prefix `ROADMAP_CHECKED=1`.
"""
import json
import os
import subprocess
import sys

data = json.load(sys.stdin)
command = data.get("tool_input", {}).get("command", "")

if "git commit" not in command or "ROADMAP_CHECKED=1" in command or "ROADMAP.md" in command:
    sys.exit(0)

repo = os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or "."
staged = subprocess.run(["git", "-C", repo, "diff", "--cached", "--name-only"],
                        capture_output=True, text=True).stdout.split()

if "ROADMAP.md" in staged:
    sys.exit(0)

code_paths = ("backend/", "mobile/")
touches_code = any(f.startswith(code_paths) for f in staged) or any(p in command for p in code_paths) \
    or "git add -A" in command or "git add ." in command or "commit -a" in command

if touches_code:
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": (
            "Roadmap check: this commit includes code but not ROADMAP.md. "
            "Tick finished items in ROADMAP.md (phase checklists, repo layout, test counts) and include it "
            "in this commit. If nothing on the roadmap was finished, re-run the same command prefixed with "
            "ROADMAP_CHECKED=1."
        ),
    }}))
sys.exit(0)
