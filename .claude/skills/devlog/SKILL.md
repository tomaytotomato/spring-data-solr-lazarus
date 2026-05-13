---
name: devlog
description: >
  Maintains a DEVLOG.md development journal at the repo root. Use this skill whenever a work session
  ends, significant changes are committed, or the user says "update the devlog", "log this", "devlog",
  "what did we do", "write up the session", or "end of session". Also trigger proactively when you
  notice substantial work has been done (multiple commits, new features, bug fixes, refactors) and
  no devlog update has been made yet — nudge the user with "Shall I update the DEVLOG?" Even if the
  user doesn't mention the devlog explicitly, if a meaningful chunk of work just wrapped up, this
  skill applies.
author: bruce
license: UNLICENSED
keywords: [devlog, log, journal, session, notes, decisions, gotchas, documentation]
---

# DEVLOG Skill

The DEVLOG.md is a living engineering journal — not a changelog, not a commit log, but a narrative
record of *what happened, what was learned, and what surprised you*. It captures the context that
git history alone cannot: why an approach was chosen, what broke along the way, and what a future
reader (including future-you) needs to know.

## When to Update

- End of a work session (one or more meaningful commits)
- After resolving a tricky bug or making a non-obvious architectural decision
- When the user explicitly asks
- Proactively, if significant uncommitted work has accumulated

## Gathering Context

Before writing the entry, collect the raw material:

1. **Git log since the last DEVLOG entry** — scan the DEVLOG.md for the most recent commit SHA
   mentioned, then run `git log --oneline <last-sha>..HEAD`. If no SHA is found, use the log from
   today.
2. **What changed** — `git diff --stat <last-sha>..HEAD` (or today's diff) to understand scope.
3. **Test counts** — run `mvn test -q 2>&1 | tail -5` or equivalent to capture current test state.
   If a build would be too slow, check the most recent build output or ask the user.
4. **The existing DEVLOG.md** — read the file to understand the current structure, voice, and where
   the new entry slots in.

## Entry Format

Follow the established structure exactly. The DEVLOG.md reads top-down chronologically — newest day
at the bottom of the day entries (but above any "What's Next" section).

```markdown
## YYYY-MM-DD — Day N: Brief Title

### Session N: Descriptive Session Title (HH:MM–HH:MM)

**Commit:** `abc1234` (or **Commits:** `abc1234` → `def5678` for ranges)

One or two paragraphs of narrative: what was done, why, and how. Written in past tense, first person
plural ("we") or passive voice. Be specific about class names, patterns, and decisions — this is an
engineering journal, not a status report.

- Bullet points for discrete items when a list reads better than prose
- New features, refactors, or structural changes get their own bullets

**Gotchas discovered:**
- Concrete technical surprises (API differences, dependency quirks, runtime behaviour)
- Include enough detail that someone hitting the same issue would recognise it immediately

**Tests:** X total (Y unit + Z integration), 0 failures.
```

### Key Principles

- **Day number is sequential** — count from the first entry in the file
- **Session numbers reset per day** — Session 1, Session 2, etc.
- **Time ranges are approximate** — use the user's local time, HH:MM format
- **Commit references are mandatory** — always include at least one SHA
- **Gotchas are the most valuable part** — if nothing surprised you, say so, but that's rare
- **Test counts anchor confidence** — always report the current state, even if tests didn't change
- **Don't duplicate the commit message** — the devlog adds *why* and *context*, the commit says *what*
- **Architecture diagrams are welcome** — use ASCII or fenced code blocks (see the PartTree flow
  example in the reference project)

### The "What's Next" Section

If the DEVLOG.md has a "What's Next" checklist at the bottom, review it:
- Check off items that were completed in this session
- Add new items that emerged
- Reorder by priority if the landscape shifted

If there's no such section and the project has clear next steps, suggest adding one.

## Creating a New DEVLOG.md

If the repo has no DEVLOG.md, create one with this skeleton:

```markdown
# [Project Name] — Dev Log

[One-sentence project description.]

## YYYY-MM-DD — Day 1: [Title]

### Session 1: [Title] (HH:MM–HH:MM)

**Commit:** `sha`

[Entry content...]
```

Ask the user for the project description if it's not obvious from the README or CLAUDE.md.

## Tone

Write like an engineer talking to a future engineer who will inherit this codebase. Technical
precision matters. Jargon is fine when it's the right word. Brevity is preferred over formality,
but don't sacrifice clarity for it. The voice should match the existing entries — read them first
and mirror the style.
