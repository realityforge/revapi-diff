# Change Log

### [v0.06](https://github.com/realityforge/revapi-diff/tree/v0.06) (2019-04-24)
[Full Changelog](https://github.com/realityforge/revapi-diff/compare/v0.05...v0.06)

* Remove recording of the archive for each diff in both the attachments and the main packet.
* Rename the name field from difference as it is never presented to the user and can be derived
  from the code if ever required.
* Remove examples from attachments as this tool only includes api elements and thus examples
  are completely derivable.

### [v0.05](https://github.com/realityforge/revapi-diff/tree/v0.05) (2019-02-25)
[Full Changelog](https://github.com/realityforge/revapi-diff/compare/v0.04...v0.05)

* Change the formatting of json output to use 2-space layout and to strip the leading newline.
  The goal is to make the json output easier to visually inspect.

### [v0.04](https://github.com/realityforge/revapi-diff/tree/v0.04) (2019-02-13)
[Full Changelog](https://github.com/realityforge/revapi-diff/compare/v0.03...v0.04)

* Remove ignore for scala annotation from the default configuration.

### [v0.03](https://github.com/realityforge/revapi-diff/tree/v0.03) (2019-02-11)
[Full Changelog](https://github.com/realityforge/revapi-diff/compare/v0.02...v0.03)

* Add an option `--expect-no-differences` that will generate a non-zero exit code when differences are
  detected otherwise the tool has been updated to return `0` on success.
* Ensure that the order of the differences in the report are stable and the order of the keys within a
  difference are stable. The purpose is to minimize churn when the differences are committed to version
  control systems.
* Add a trailing new line to report to avoid warnings from version control tools.

### [v0.02](https://github.com/realityforge/revapi-diff/tree/v0.02) (2019-02-09)
[Full Changelog](https://github.com/realityforge/revapi-diff/compare/v0.01...v0.02)

* Fix bug where a `NullPointerException` was triggered when a output file was specified without a directory.

### [v0.01](https://github.com/realityforge/revapi-diff/tree/v0.01) (2019-02-09)
[Full Changelog](https://github.com/realityforge/revapi-diff/compare/97c8a479bf22e718a2cf74522e7bb65806c4f382...v0.01)

 ‎🎉	Initial super-alpha release ‎🎉.
