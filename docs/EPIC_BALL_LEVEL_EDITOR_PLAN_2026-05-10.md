# Epic Ball Adventure Level Editor Plan - 2026-05-10

## Current Direction

The level editor is now usable for creating and playtesting basic levels, but it should be treated as an early production tool. The next work should focus on making it safe, predictable, and fast enough for real level design.

## Recommended Next Steps

### 1. Stabilize the Level Data Model

- Add load/import support for existing `.tmx` levels so the editor can modify real shipped levels, not only newly created ones.
- Add save/play validation:
  - missing Start object,
  - missing Exit object,
  - bad numeric values,
  - invalid or incomplete pulley pairs,
  - moving platforms with too few path points,
  - objects outside world bounds.
- Add backup-before-save or autosave so a bad save cannot destroy the only copy of a level.
- Add undo/redo for object creation, deletion, movement, resizing, point dragging, and property edits.

### 2. Replace Raw Text Fields With Typed Object Controls

Some properties are currently edited as plain text even when the game expects a specific kind of value. This makes it easy to type values that look valid but are not meaningful.

Examples:

- `Loop` should be a checkbox, not a text field.
- `Speed`, `Weight`, `Torque`, and `PowerX/PowerY` should be numeric fields with min/max limits where sensible.
- `Contact` should be a checkbox or select control, not a free text field expecting `oui`.
- Pulley `Groupe` should be a controlled group selector or generated pairing tool.
- Swing angle fields should be numeric degree controls.

The goal is to make each property editor match the data type and gameplay meaning of that property. This prevents invalid TMX data and makes the editor easier to understand.

### 3. Improve the Playtest/Edit Loop

- Preserve editor camera position, zoom, and selected object when returning from playtest.
- Return to the editor automatically when a playtest level is completed or fails validation.
- Show clear editor-side errors when playtest map creation fails.
- Keep `Esc` as the playtest escape route back to editor.

### 4. Improve Platform Path Editing

- Show direction arrows on moving platform paths.
- Mark the first and last path points clearly.
- Keep the platform body footprint visible only at the first path point to avoid visual clutter.
- Add point insertion/deletion controls for platform paths.
- Consider making `Loop` visually obvious by drawing a faint return segment from the last point to the first only when loop is enabled.

### 5. Improve Object Editing Ergonomics

- Add copy/paste and duplicate shortcuts.
- Add multi-select for moving related objects together.
- Add delete-key support for selected points and selected objects.
- Add object ordering/layer visibility if overlapping objects become hard to select.
- Add snap/free-position controls for polygon points, not only whole objects.

### 6. Runtime Cleanup Related to Editor-Created Levels

- Parse boolean properties explicitly in runtime code instead of using property presence checks.
- Remove remaining debug prints or route them through `DebugConfig`.
- Reduce temporary `Vector2` allocations in moving platform movement code.
- Add focused regression tests for:
  - moving platform ping-pong and loop behavior,
  - pulley pair creation,
  - swing behavior,
  - editor TMX serialization for each object type.

## Priority Recommendation

The best next milestone is:

1. Load existing `.tmx` levels.
2. Validate before save/play.
3. Add undo/redo.
4. Convert remaining free-text properties into typed controls.

After that, the editor becomes much safer for building actual production levels.
