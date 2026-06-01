# Session Recap

Generate a structured summary of the current Claude Code session and save it as a new file in `docs/session-recaps/`.

## Steps

1. Determine today's date in `YYYY-MM-DD` format and the appropriate monthly folder (`YYYY-MM`).
2. Check if `docs/session-recaps/YYYY-MM/` exists. If not, create it.
3. List existing files in `docs/session-recaps/YYYY-MM/` to determine the next session number for the day. The number `NN` is zero-padded to 2 digits and increments within the month (not within the day).
4. Create the file `docs/session-recaps/YYYY-MM/YYYY-MM-DD-session-NN.md` with the recap content using the template below.
5. Show the user the full path of the created recap file.
6. Suggest a corresponding short entry for `JOURNAL.md` (see below). Do NOT modify `JOURNAL.md` automatically; let the user copy the suggested entry manually.

## Recap template

```markdown
# Session Recap — {YYYY-MM-DD} — Session {NN}

## Objective
{One sentence describing what we set out to do at the start of the session. If the objective evolved during the session, mention both the initial and actual focus.}

## What was done
- {Concrete deliverables and tasks completed. Be specific: file paths, configurations created, commands run.}

## Key decisions made
- {Architectural or technical choices made during the session, each with a 1-line rationale. Write "No significant architectural decisions in this session." if applicable.}

## Concepts explained / learned
- {Concepts discussed in depth during the session, each with a 1-2 line summary. Skip this section entirely if nothing notable was explained or learned.}

## Issues encountered & resolved
- {Problems hit during the session and how they were resolved. Skip if no significant issues.}

## Open questions / TODOs
- {Things left unfinished, doubts raised, items to revisit. Be honest — this is for the user's future reference.}

## Files modified or created
- `path/to/file` — brief description of change

## Suggested next steps
- {2-4 concrete next actions, in priority order, based on what was started but not completed and the project roadmap.}

## Session metadata
- Duration: ~{estimated duration based on conversation length}
- Model(s) used: {models used during the session}
- Block / phase: {current block and phase from `docs/roadmap.md`, if identifiable}
```

## Tone and quality

- Factual and concrete, not promotional.
- Use actual file paths, not generic references.
- Be honest about issues and gaps in understanding.
- Professional tone (these recaps will eventually be in a public repository).
- Concise but complete: avoid padding to look impressive.

## Suggested JOURNAL.md entry

After saving the recap file, display this suggested entry for the user to copy into `JOURNAL.md` (at the top of the `## Entries` section, most recent first):

```markdown
### {YYYY-MM-DD} — Session {NN}

**Block / Task**: {current block / phase}

**Done**:
- {3-5 most important bullets from the recap}

**Next**:
- {2-3 most important next steps}

See [detailed recap](docs/session-recaps/{YYYY-MM}/{YYYY-MM-DD}-session-{NN}.md).
```

Do not modify JOURNAL.md yourself. Display the suggested entry as text and let the user paste it manually.