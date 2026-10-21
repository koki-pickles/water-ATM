# Water ATM: notes for Claude

- **Keep `ROADMAP.md` current.** When a roadmap item is finished, tick it (`- [x]`) and update anything it
  affects (repo layout, test counts, phase "✅ code done" labels) **in the same commit** as the code.
  A hook (`.claude/hooks/roadmap-check.py`) blocks code commits that leave ROADMAP.md out; if nothing on
  the roadmap was finished, prefix the commit with `ROADMAP_CHECKED=1`.
- **Small commits**, one per layer/feature, with a bullet list of what's done in each message.
- Backend is **hexagonal** (see `docs/SETUP.md`); `HexagonalArchitectureTest` enforces the rules.
- Theme is **monochrome blue** only (palette in ROADMAP.md §2).
