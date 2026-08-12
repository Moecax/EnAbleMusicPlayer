# Roadmap

Planned work for En-able. Not in priority order.

## Download to disk

`DownloadService`'s download body is currently commented out (WIP). Finish
wiring it up so users can save streamed songs to local storage instead of
only caching/streaming, including progress reporting back to `MusicClient`s
and writing metadata/album art via `jaudiotagger-android`.

## Fix casting

Harden the unsafe casts around `MusicService` and friends. There are a
couple of `ClassCastException` catches already in `MusicService.kt` masking
underlying type-safety issues — audit call sites that cast intent extras,
`MediaPlayer` state, and `MusicClient` implementations, and replace unsafe
casts with safe casts (`as?`) or better typing where possible.

## Analytics

Add lightweight, privacy-respecting usage analytics (e.g. crash reporting
and basic feature-usage counts) to help prioritize future work. Needs a
decision on provider and an opt-out/consent flow before landing.

## Listening habits ("Top music")

Track on-device listening history — song, artist, timestamp, and duration
listened per play — in local storage, no backend or account sync. Surface
it as queries over selectable periods (week/month/year/all-time) so users
can see their top songs/artists/albums, similar to a personal Spotify
Wrapped. UI placement (new screen vs. existing tab) is an implementation
detail to decide when this is picked up; the roadmap scope here is just
the tracking + query layer.

## Lyrics during playback

Show lyrics on the `Player` screen while a song plays, synced to playback
position where available. Fetch from [LRCLIB](https://lrclib.net) (free,
no API key, provides time-synced lyrics) keyed on title/artist/duration,
and cache the result to disk (alongside `albumArtDir`-style local storage)
so lyrics are available offline after the first fetch. Needs graceful
handling for songs LRCLIB has no match for (hide the lyrics panel rather
than error).

## Bulk import YouTube Music playlists

Let users import an entire YouTube Music playlist in one action instead of
adding songs individually. Given a playlist URL, use
`YouTube.getPlaylistExtractor(link)` (same NewPipeExtractor path already
used for playlist enumeration) to page through `initialPage.items`, map
each entry to a `Song`, and write them into a new or existing local
`Playlist`. Needs progress feedback for large playlists and sane handling
of unavailable/region-locked entries (skip with a summary rather than
aborting the whole import).

## Backup/restore: export & import songlist and listening habits

Let users back up their library and [[Listening habits ("Top music")]]
history to a single external file (e.g. JSON, written via SAF so it can be
saved anywhere/shared) and restore it later, on the same device or a new
one. Export should cover the local playlists (`playlistFolder` JSON),
song/queue metadata, and the listening-history data once that tracking
layer exists. Import should validate the file, merge or replace existing
data (needs a decision on merge-vs-overwrite UX), and re-link songs by
`youtubeLink`/`filePath` where local files or cached streams aren't
present on the new device.

## Fix `fastlane android release` asset upload

`upload_release_assets` in `fastlane/Fastfile` shells out to `curl` to push
APKs to the GitHub release (worked around fastlane's `upload_assets`
truncating binaries in text mode on Windows/Ruby). That `sh(...)` call
reliably fails with exit status 3, even though running the identical `curl`
command directly in a shell succeeds — seen on both the v0.1.6 and v0.1.8
releases, worked around each time by re-running the upload manually.
Track down why fastlane's `sh` wrapper breaks the command (quoting/escaping
under Ruby's `sh`, environment differences, etc.) so `fastlane android
release` can publish unattended.
