# Changelog

All notable changes to this package are documented in this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `pedometer_supported()`: whether the device counts steps, before any hook is used. True on
  an iPhone with step counting and an Android phone with the step counter sensor; false on a
  simulator, the web and the desktop.
