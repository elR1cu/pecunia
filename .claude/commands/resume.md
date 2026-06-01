# Resume Session

The user is starting a new Claude Code session on the Pecunia project. Help them resume work efficiently by reconstructing context from recent activity.

## Steps

1. Read `JOURNAL.md` to understand the most recent high-level state.
2. Locate the most recent session recap:
    - List `docs/session-recaps/` to find the latest monthly folder.
    - Within that folder, find the file with the highest session number.
3. Read that recap to understand the detailed state of recent work.
4. Read `docs/roadmap.md` to identify the current block and phase.
5. Run `git status` to check the current branch and any uncommitted changes.
6. Run `git log --oneline -5` to see the recent commits.

## Output format

After gathering the context, provide this concise summary:

```markdown
# Session Resume

## Where we left off
{Concise 2-3 sentence summary based on the most recent recap and JOURNAL entry.}

## Current state
- Branch: {current git branch}
- Last commit: {message of last commit}
- Uncommitted changes: {yes / no — and brief description if yes}
- Block / phase: {from roadmap.md}

## In progress
{Items from "Open questions / TODOs" of the last recap. Skip if none.}

## Suggested next steps
{From "Suggested next steps" of the last recap, refined with your own judgment based on the roadmap.}

What would you like to focus on today?
```

## Tone

- Concise. The user wants to start working, not read a long preamble.
- Do not pad with explanations of what you did to gather context.
- If something is unclear or missing (e.g., no recaps yet), say so briefly and proceed with what is available.