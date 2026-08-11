# En-able

🎵 Play songs from the cloud for free - stream or download on the go. Feel free to PR.

En-able is a fork of [Able Music Player](https://github.com/uditkarode/AbleMusicPlayer) by Udit Karode.

# Downloads
A CI will build an APK for `arm64-v8a` and `armeabi-v7a` architectures and create a release.

If you're not sure which APK you should install, refer https://www.howtogeek.com/339665/how-to-find-your-android-devices-info-for-correct-apk-downloads/

# Releasing

Releases are cut with [Fastlane](https://fastlane.tools) and built/published by GitHub Actions
(`.github/workflows/build.yml`):

```sh
# Bump the version (major.minor.patch), write a changelog entry, pick a codename,
# and push the tag - this triggers the GitHub Actions build+release.
bundle exec fastlane android production type:patch   # or type:minor / type:major

# Re-cut the current version without bumping it (e.g. to retry after a failed
# GitHub Actions run) - force-moves the existing tag and re-triggers the build.
bundle exec fastlane android production
```

See [CHANGELOG.md](CHANGELOG.md) for release notes and `fastlane/codenames.txt`
for the codename history. `fastlane android local_release` is an emergency
fallback that builds, signs, and publishes the current tag locally if GitHub
Actions is ever unavailable.
