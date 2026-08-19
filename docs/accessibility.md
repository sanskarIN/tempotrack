# Accessibility

TempoTrack's UI aims for WCAG-oriented practices even though Android/Desktop are native applications.

## Implemented

- Large-control setting for primary stopwatch actions.
- Text labels in addition to status meaning; fastest/slowest are not encoded by color alone.
- Compose semantic description for elapsed time.
- Material components with keyboard/focus semantics.
- Touch-friendly primary controls and shared minimum-size guidance.
- Light/dark/system theme support.
- Reduced-motion preference; the current product avoids decorative animations by default.
- Adaptive bottom-navigation/navigation-rail layout so larger screens do not simply stretch the compact UI.
- Desktop keyboard shortcuts for core stopwatch actions.
- In-app Desktop shortcut help so keyboard controls are discoverable.
- Externalized visible UI strings, allowing accessibility copy to be localized with the rest of the interface.

## Keyboard controls

Desktop currently supports:

- Space — start, pause, or resume;
- L — record a lap while the stopwatch is running;
- R — reset the stopwatch.

Shortcut actions persist the same active-checkpoint state used by button interactions.

## Manual review checklist

- [ ] Navigate every control with keyboard on Desktop.
- [ ] Verify visible focus state.
- [ ] Verify the shortcut help dialog can be opened, read, and dismissed from keyboard-only input.
- [ ] Run Android TalkBack through onboarding/start/pause/resume/lap/save/history/rename/restore/settings.
- [ ] Test Android font scale at 200%.
- [ ] Test large controls on a compact phone-sized window.
- [ ] Confirm controls do not overlap at narrow widths.
- [ ] Confirm the navigation rail appears and remains usable on tablet/large-window widths.
- [ ] Check light and dark contrast.
- [ ] Check fastest/slowest labels remain understandable without color.
- [ ] Verify destructive session deletion has a usable undo path.
- [ ] Verify mini stopwatch can be closed without trapping focus.
- [ ] Review long translated strings for clipping or inaccessible truncation.

## Reduced motion

The setting is persisted now even though TempoTrack currently uses minimal motion. New animated features should consult this preference or avoid nonessential motion entirely.

Accessibility regressions should be treated as functional bugs.
