# Privacy

TempoTrack is designed to work without an account or hosted backend.

## Data stored locally

The app can store:

- saved stopwatch sessions and lap data;
- app preferences;
- a temporary active-stopwatch checkpoint;
- files created by explicit export actions;
- temporary cache/directory files created only when you explicitly choose a platform export or share action.

## Data transmission

The application code does not include analytics, advertising, telemetry, authentication, or cloud-sync services. TempoTrack does not automatically transmit session data.

Export and share operations happen only after an explicit user action. Android sharing prepares a temporary app-cache file and delegates the destination choice to the operating-system share sheet using a read-only content URI grant. iOS export/share operations prepare a sanitized UTF-8 file in a unique application temporary directory and present native system destination UI. TempoTrack does not choose or contact a recipient itself. Once you choose another app, service, or document destination, that destination's privacy terms and storage behavior apply.

On iOS, direct export uses the system document picker and sharing uses the system activity sheet. The temporary staging directory is removed when the native operation completes, is cancelled/dismissed, or fails to present.

## Android backup

Saved sessions and preferences are eligible for Android platform backup/device transfer. The transient active-stopwatch checkpoint, generated export directory and temporary share-cache files are excluded from cloud backup.

## Temporary export/share data

Android share files live in the application cache area. iOS export/share staging files live in unique operation directories below the application temporary directory. These locations are managed by the operating system and are not treated as durable backups. A durable copy exists only when you explicitly export/save/share it to a destination that retains the file.

## Deleting data

Saved sessions can be deleted in History. Application-private data can also be removed by clearing application data or uninstalling the app. Temporary export/share data may be reclaimed by the operating system and TempoTrack removes iOS operation directories after native export/share completion paths.

## Contact

Privacy questions: **supportramsandesh@gmail.com**
