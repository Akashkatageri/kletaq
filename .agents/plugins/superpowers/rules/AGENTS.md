# Superpowers — Agentic Engineering Rules

## 1. Mandatory Skill Invocation
- **Invoke relevant skills BEFORE any response or action** — including clarifying questions, exploring the codebase, or touching files.
- Announce `Using [skill] to [purpose]` and follow the skill's instructions strictly.
- When a skill specifies a checklist or sequence, track each item until verified.

## 2. Process Skills Precede Implementation
- When multiple skills apply, process skills come first:
  - **New Features / Design**: "Let's build X" -> invoke `brainstorming` before implementation planning or code writing.
  - **Debugging / Defect Resolution**: "Fix this bug" or unexpected behavior -> invoke `systematic-debugging` first (find root cause before fixing).
  - **Planning**: Invoke `writing-plans` to create step-by-step verifiable plans.

## 3. Test-Driven Development (TDD)
- Follow strict Red-Green-Refactor cycles:
  - Write a failing test first that reproduces the problem or specifies the requirement.
  - Implement the minimal solution to make the test pass.
  - Refactor while keeping all tests green.

## 4. Subagent-Driven Development & Review
- Use `invoke_subagent` (`TypeName: "self"` for full tools, `"research"` for read-only exploration) for deep dives or independent work streams.
- Review and verify subagent outputs before committing changes.

## 5. Verification Before Completion
- Never declare a task complete without direct, empirical verification (compilation, automated tests, APK build/launch check).
- Review all touched files and diffs against requirements.

## 6. Antigravity Tool Equivalents
- **Subagents**: Use `invoke_subagent` with `TypeName: "self"` or `"research"`.
- **Task Tracking**: Maintain a markdown task checklist or artifact as the single source of truth for multi-step tasks.

