# Use Cases

This directory contains business logic use cases that orchestrate domain operations.

## Structure

Each use case should:
- Have a single responsibility
- Be named with an action verb (e.g., `CompleteStepUseCase`)
- Use async/await for asynchronous operations
- Return `Result` types or throw errors
- Be injected via protocols

## Examples

- `CreateRoutineUseCase` - Create routine with steps
- `CompleteStepUseCase` - Record step completion event
- `UndoStepUseCase` - Record undo event
- `DeriveStepCompletionUseCase` - Calculate current step state from events
- `DeriveRoutineCompletionUseCase` - Check if routine is complete

