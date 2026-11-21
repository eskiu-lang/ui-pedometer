# Changelog

All notable changes to this package are documented in this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.4] - 2026-11-21

### Added

- `pedometer_supported()`: whether the device counts steps, before any hook is used. True on
  an iPhone with step counting and an Android phone with the step counter sensor; false on a
  simulator, the web and the desktop.
- CI builds the example on the native desktop hosts (`tools/ci_desktop.sh`).
- A platform table in the README.

### Fixed

- Native desktop builds (macOS, Linux, Windows) of an app using the package link: the package
  has native code for them, which answers `StepsUnavailable` to every use, since computers
  have no step counter. Before, the link failed on the missing externs.
