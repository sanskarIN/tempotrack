# Accessibility

TempoTrack's UI aims for WCAG-oriented practices even though Android/Desktop are native applications.

## Implemented

- Large-control setting for primary stopwatch actions.
- Text labels in addition to status meaning; fastest/slowest are not encoded by color alone.
- Compose semantic description for elapsed time.
- Material components with keyboard/focus semantics.
- Touch-friendly primary controls.
- Light/dark/system theme support.
- Reduced-motion preference is stored for features that introduce motion.

## Manual review checklist

- [ ] Navigate every control with keyboard on Desktop.
- [ ] Verify visible focus state.
- [ ] Run Android TalkBack through start/pause/resume/lap/save/history.
- [ ] Test Android font scale at 200%.
- [ ] Confirm controls do not overlap at narrow widths.
- [ ] Check light and dark contrast.
- [ ] Check fastest/slowest labels remain understandable without color.
- [ ] Verify mini stopwatch can be closed without trapping focus.

Accessibility regressions should be treated as functional bugs.
