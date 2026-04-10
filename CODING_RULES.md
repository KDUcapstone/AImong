# Coding Rules

This repository follows the development rules below when working with Codex or other AI coding agents.

## Pre-Work

1. Step 0 rule
   - Before any structural refactor on a file larger than 300 LOC, first remove dead props, unused exports, unused imports, and debug logs.
   - Commit this cleanup separately before starting the actual refactor.

2. Phased execution
   - Never attempt a multi-file refactor in a single response.
   - Break work into explicit phases.
   - Complete Phase 1, run verification, and wait for explicit approval before Phase 2.
   - Each phase must touch no more than 5 files.

## Code Quality

3. Senior dev override
   - Do not stop at the smallest possible patch if the surrounding architecture is flawed.
   - If state is duplicated, patterns are inconsistent, or the structure would be rejected in a strong code review, propose and implement structural fixes.

4. Forced verification
   - Do not report a task as complete until the code has been verified.
   - Run `npx tsc --noEmit` or the project's equivalent type-check command.
   - Run `npx eslint . --quiet` if ESLint is configured.
   - Fix all resulting errors before reporting success.
   - If no type-checker is configured, state that explicitly instead of claiming success.

## Context Management

5. Sub-agent swarming
   - For tasks touching more than 5 independent files, launch parallel sub-agents.
   - Assign roughly 5 to 8 files per agent when possible.

6. Context decay awareness
   - After 10 or more messages in a conversation, re-read any file before editing it.
   - Do not rely on memory of earlier file contents.

7. File read budget
   - Cap each file read at 2,000 lines.
   - For files over 500 LOC, read them in sequential chunks with explicit offsets and limits.

8. Tool result blindness
   - Treat suspiciously short results from broad searches or commands as possible truncation.
   - Re-run with narrower scope when needed.
   - State when truncation is suspected.

## Edit Safety

9. Edit integrity
   - Before every file edit, re-read the target file.
   - After editing, read it again to confirm the change applied correctly.
   - Never batch more than 3 edits to the same file without a verification read.

10. No semantic search assumption
   - When renaming or changing any function, type, or variable, search separately for:
     - Direct calls and references
     - Type-level references such as interfaces and generics
     - String literals containing the name
     - Dynamic imports and `require()` calls
     - Re-exports and barrel file entries
     - Test files and mocks
   - Do not assume a single grep catches everything.

## Usage Notes

- These rules are intended as repository workflow guidance.
- If a higher-priority system or tool constraint conflicts with a rule, follow the higher-priority constraint and note the conflict explicitly.
