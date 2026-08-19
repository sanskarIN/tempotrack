# Security Model

This document expands the public reporting policy in [`../SECURITY.md`](../SECURITY.md) into an engineering threat/boundary reference. TempoTrack is a local-first stopwatch, so its security model is primarily about local data integrity, safe file portability, malformed input handling, platform URI/destination boundaries, build supply chain, and release credentials.

## Security goals

TempoTrack aims to:

- keep private runtime state inside application-controlled storage unless the user explicitly exports/shares it or the OS performs configured backup/device transfer;
- avoid network/account/analytics dependencies in core product behavior;
- prevent malformed local/imported data from producing unsafe timing/history state;
- avoid silent destructive migration of unknown/corrupt durable history;
- prevent path traversal or broad file-provider exposure during export/share;
- avoid spreadsheet formula execution from exported text fields where practical;
- keep signing credentials out of source control and ordinary build logs;
- preserve cancellation so cancelled asynchronous work is not misrepresented as a successful/ordinary failure path;
- keep CI/release workflows least-privileged.

## Non-goals

TempoTrack does not attempt to:

- encrypt all application-private data at rest by itself;
- protect against a fully compromised/rooted/jailbroken host OS;
- provide authenticated cloud sync;
- provide tamper-evident exported JSON backups;
- guarantee privacy after the user exports/shares a file to another application/service;
- replace OS/device security controls.

Encrypted backup is an optional future feature, not a current security claim.

## Trust boundaries

### Shared domain/data code

Trusted to enforce:

- stopwatch invariants;
- persistence schema interpretation;
- bounds/validation;
- typed import/export/share errors;
- serialization safety behavior.

It does not have direct access to arbitrary platform filesystem/network APIs.

### Platform storage adapters

Trusted to place private logical stores in platform-controlled locations and perform durable writes without broad exposure.

### Export/share adapters

Cross the application-private boundary. They receive already-generated logical content and either:

- write to a user/system-selected durable destination; or
- stage a temporary file and delegate destination choice to native share UI.

### Imported JSON

Untrusted input. It is size-bounded, decoded, count-bounded, duplicate-checked, and semantically validated before any replacement write.

### Build dependencies/actions

External supply-chain inputs. Versions/actions are explicit, dependency review/CodeQL/Dependabot are configured, and Gradle distribution metadata is pinned.

### Signing credentials

Highly sensitive build-time inputs. They are not runtime product requirements and must remain outside Git history.

## Local history integrity

Saved history is durable user data. The repository follows a fail-closed policy:

- oversized stored history → corruption failure;
- unsupported future schema → corruption failure;
- duplicate IDs → corruption failure;
- invalid session/lap relationship → corruption failure;
- malformed JSON → corruption failure.

The repository does **not** silently drop only invalid records and rewrite the remainder. That would turn corruption into unacknowledged data loss.

Legacy history is migrated only after successful decode and full validation.

## Preferences and transient state

Preferences are reconstructable. Corrupt/oversized/unsupported preference data safely falls back to defaults.

Active stopwatch persistence is transient. Invalid/oversized/unsupported checkpoints are discarded rather than used to compute an unsafe elapsed duration.

This different treatment is intentional: durable history has higher data-loss sensitivity than configuration/transient state.

## Timing integrity

Wall clock is never the source of live elapsed duration.

Why this matters:

- users/NTP can change wall time;
- timezone/DST changes do not represent elapsed duration;
- wall clock can move backward.

Live duration uses platform monotonic clocks.

For persisted running recovery, wall time is only a consistency signal paired with system uptime on Android/iOS. If references are unsafe, recovery pauses at the last known elapsed value.

Desktop never compares `System.nanoTime()` across JVM processes.

See [`state-and-recovery.md`](state-and-recovery.md).

## Imported JSON threat handling

Restore input is untrusted and may be intentionally huge/malformed.

Controls:

- maximum input characters before decode;
- maximum session count;
- maximum laps per session through shared validation;
- bounded ID/name lengths;
- nonnegative timestamp/duration checks;
- sequential lap indices;
- cumulative/split consistency;
- duplicate ID rejection;
- no partial replacement before full validation.

User-facing errors use stable categories rather than raw parser exception text or echoing supplied JSON.

## CSV formula injection

CSV consumers such as spreadsheet software may interpret values beginning with:

```text
= + - @
```

as formulas.

TempoTrack checks trimmed text fields and prefixes such values with an apostrophe before standard CSV quoting.

This is defense-in-depth for spreadsheet opening. It is not a universal guarantee about every CSV consumer.

## Filename/path handling

`ExportFileName.sanitize`:

- strips path-like/unsafe characters into `_`;
- trims leading/trailing dots/underscores;
- caps length;
- provides a safe fallback.

Platform code never treats a user-suggested filename as an arbitrary absolute path.

### Android pre-10 export

Destination directory is fixed to app-specific external Documents/TempoTrack. Filename collision resolution reserves a new file rather than overwriting an existing backup.

### Desktop export

The user explicitly selects the target path through native chooser. The suggested filename is sanitized, but chooser selection is the destination authority.

### iOS export

Temporary source file is staged inside an app temporary operation directory. The native document picker controls final destination.

## Android FileProvider boundary

Sharing uses a non-exported `FileProvider` with temporary URI grants.

Controls:

- provider `exported=false`;
- `grantUriPermissions=true`;
- configured path restricted to `cache/shared-exports` rather than all files/cache;
- each share receives a unique staged file;
- URI is `content://`, not a raw filesystem path;
- read permission is temporary and explicitly granted;
- URI is attached in both `ClipData` and `EXTRA_STREAM` for robust permission propagation.

Do not broaden `file_paths.xml` to `<cache-path path="."/>` or a filesystem root as a convenience.

## Android share staging lifetime

Preparation/presentation failure deletes the staged file where it is known that no recipient needs it.

If coroutine cancellation races chooser launch, the file is intentionally retained in OS-managed application cache because a recipient may already have received the granted URI. Premature deletion could cause broken/inconsistent sharing.

A later share creates a different file so retaining the earlier one cannot cause content overwrite behind an already granted URI.

## Android MediaStore export integrity

Android 10+ export:

- inserts a pending MediaStore item;
- writes content;
- requires successful finalization update;
- deletes the item when write/finalization fails.

This avoids returning success for an unfinished/pending record.

## iOS staging and native UI

Each iOS export/share creates a unique directory under the app temporary directory.

Controls:

- sanitized filename;
- UTF-8 write;
- unique operation directory prevents cross-operation overwrite;
- native document picker/activity sheet chooses destination/recipient;
- staging cleanup on completion/cancel/failure;
- document exporter serializes picker operations;
- activity service prevents overlapping active controller;
- popover anchoring is configured where UIKit requires it.

Actual UIKit lifecycle behavior requires simulator/device verification.

## Private-file atomicity

Android/Desktop private stores use:

1. sibling temporary file;
2. full UTF-8 write;
3. atomic replacement when filesystem supports it;
4. fallback only for explicit `AtomicMoveNotSupportedException`.

This reduces risk that process interruption leaves a partially replaced primary store.

It does not provide journaling or cryptographic integrity; malformed result still fails validation/decoding on next read.

## Concurrency controls

Repositories serialize writes with coroutine `Mutex`.

Why:

- simultaneous read-modify-write operations can otherwise lose updates;
- platform atomic file replacement does not make higher-level repository updates transactional with one another.

UI also prevents duplicate/conflicting submissions so users do not queue multiple destructive/expensive operations.

## Coroutine cancellation

Platform and shared helpers explicitly rethrow `CancellationException`.

Security/reliability rationale:

- cancellation is a control-flow signal, not an ordinary storage error;
- swallowing it can keep work alive after UI/lifecycle cancellation;
- converting it into a failure may trigger incorrect rollback/message behavior.

Cleanup that must finish after cancellation uses non-cancellable context where required (notably iOS document staging cleanup).

## Backup/privacy boundary

Android backup/device transfer is configured through explicit XML rules. Review those rules with `PRIVACY.md` whenever local paths change.

Transient share cache and unsafe stale active state should not be treated as durable user backup content.

## Release signing

Android local signing configuration requires all of:

```text
TEMPOTRACK_KEYSTORE_PATH
TEMPOTRACK_KEYSTORE_PASSWORD
TEMPOTRACK_KEY_ALIAS
TEMPOTRACK_KEY_PASSWORD
```

GitHub release workflow additionally expects protected base64 keystore secret.

Controls:

- no real secret values in source;
- partial config fails;
- keystore path must exist;
- CI decodes keystore into runner temp;
- restrictive file mode applied;
- password/alias secrets scoped to build step;
- build jobs use read-only repository permission;
- only publish job receives `contents: write`.

## Secret scanning

The repository workflow checks common committed-secret patterns on pushes/PRs with full Git history checkout.

This is a defense layer, not proof of absence. Maintainers must still review:

- Gradle properties;
- YAML workflows;
- source/config files;
- issue/PR attachments;
- generated logs/artifacts.

## Dependency/supply-chain controls

- centralized version catalog;
- pinned Gradle distribution checksum;
- exact Gradle fallback version;
- Dependabot update PRs;
- dependency-review workflow;
- CodeQL Java/Kotlin analysis;
- CI build/test/lint matrix;
- semantic release-tag validation;
- release artifact SHA-256 checksums.

The missing standard `gradle-wrapper.jar` is a known bootstrap limitation. It should be generated from a trusted Gradle 9.5.0 installation rather than fabricated.

## Logging/error disclosure

Product UI should not display:

- raw imported JSON;
- local private filesystem contents;
- signing/build secrets;
- raw exceptions that may include path/system details.

Current import/export/share UI maps failures to localized user-safe messages.

Engineering logs/tests may use internal error detail when necessary, but never include real credentials/user data in repository fixtures.

## Security review checklist for changes

Ask:

- Does new data leave private storage?
- Is a new platform URI/path broader than necessary?
- Can untrusted input cause unbounded allocation/loops/storage?
- Is validation done before mutation/migration?
- Can two writes race and lose data?
- Can cancellation leave unsafe partial state?
- Does a new export field need CSV formula protection?
- Does a new schema default make old/future data ambiguous?
- Does a workflow need write permission or can it remain read-only?
- Could a secret be printed or committed?
- Does backup scope change?
- Does privacy documentation need updating?
- Is the behavior actually verified on the required platform?

## Vulnerability reporting

Do not publish undisclosed vulnerabilities as public issues. Follow [`../SECURITY.md`](../SECURITY.md) for the current private reporting contacts and requested report contents.
