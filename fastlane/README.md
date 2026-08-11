fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android production

```sh
[bundle exec] fastlane android production
```

Cut or re-cut a production release. GitHub Actions (build.yml) does the actual build/sign/publish once the tag lands - this lane just prepares and pushes it.
  fastlane android production type:patch|minor|major   Bump the version and cut a new release
  fastlane android production                          Re-cut the current version (no version bump),
                                                        e.g. to retry after a failed CI run

### android local_release

```sh
[bundle exec] fastlane android local_release
```

Emergency fallback: build, sign, and publish the current latest tag's release locally, for use only when GitHub Actions is unavailable. Does not bump the version, write a changelog entry, or generate a codename - run `fastlane android production` first to prepare those.

### android update_newpipe

```sh
[bundle exec] fastlane android update_newpipe
```

Check for a newer NewPipeExtractor release and bump the version catalog if one exists

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
