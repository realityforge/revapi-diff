# revapi-diff: Report differences between Java APIs

[![Build Status](https://secure.travis-ci.org/realityforge/revapi-diff.svg?branch=master)](http://travis-ci.org/realityforge/revapi-diff)
[<img src="https://img.shields.io/maven-central/v/org.realityforge.revapi.diff/revapi-diff.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.revapi-diff%22%20a%3A%22revapi.diff%22)

## What is Revapi Diff?

This is a simple, self-contained, command line application that generates a json report
of API differences between between libraries. The tools is powered by the magnificent
[Revapi](https://revapi.org) but has been wrapped to make it easier to consume by other
tool chains.

### Getting Started

The tool is released to Maven Central and can be downloaded using normal dependency download mechanisms.
The Maven dependency is:

```xml
<dependency>
  <groupId>org.realityforge.revapi.diff</groupId>
  <artifactId>revapi-diff</artifactId>
  <version>0.00</version>
  <classification>all</classification>
</dependency>
```

Alternatively it can be directly downloaded from [http://central.maven.org/maven2/org/realityforge/revapi/diff/revapi-diff/0.00/revapi-diff-0.00-all.jar](http://central.maven.org/maven2/org/realityforge/revapi/diff/revapi-diff/0.00/revapi-diff-0.00-all.jar)
as the package has been built with all dependencies included. It is an executable jar file and can be run
by passing it as an argument to the `java -jar revapi-diff-0.00-all.jar ...`.  To get a list of options that
can be passed to the program run `java -jar revapi-diff-0.00-all.jar --help`.

Although in most cases it is sufficient to pass the old api and the new api as paths as well as option for
location of output file. The new api is the jar that you are comparing against the old api.

For example:

```bash
java -jar revapi-diff-0.00-all.jar \
  --old-api ~/.m2/repository/org/realityforge/arez/arez-core/0.117/arez-core-0.117.jar \
  --new-api ~/.m2/repository/org/realityforge/arez/arez-core/0.127/arez-core-0.127.jar \
  --output-file report.json
```

You can also specify an explicit label for the api jars as they appear in the reports by prefixing
the paths with the label.

For example:

```bash
java -jar revapi-diff-0.00-all.jar \
  --old-api org.realityforge.arez:arez-core:jar:0.117::~/.m2/repository/org/realityforge/arez/arez-core/0.117/arez-core-0.117.jar \
  --new-api org.realityforge.arez:arez-core:jar:0.127::~/.m2/repository/org/realityforge/arez/arez-core/0.127/arez-core-0.127.jar \
  --output-file report.json
```

The format of the output report is a direct translation of the internal data based on the output from the
[Online API Diff](https://diff.revapi.org/) tool provided by the [Revapi](https://revapi.org/) project.
The format should be largely self-explanatory despite but there is no documentation available for it at this
stage. It is in json format and consists of an array of difference objects.

i.e.

```json
[
  {
    "name": "method removed",
    "code": "java.method.removed",
    "description": "Method was removed.",
    "newElement": null,
    "newElementModule": null,
    "oldElement": "method boolean arez.Arez::areEnvironmentsEnabled()",
    "oldElementModule": "org.realityforge.arez:arez-core:jar:0.117",
    "classification": { "BINARY": "BREAKING", "SOURCE": "BREAKING" },
    "attachments": {
      "classSimpleName": "Arez",
      "package": "arez",
      "classQualifiedName": "arez.Arez",
      "oldArchive": "org.realityforge.arez:arez-core:jar:0.117",
      "elementKind": "method",
      "methodName": "areEnvironmentsEnabled",
      "exampleUseChainInOldApi": "class arez.Arez is part of the API"
    }
  }
]
```

# Contributing

The project was released as open source so others could benefit from the project. We are thankful for any
contributions from the community. A [Code of Conduct](CODE_OF_CONDUCT.md) has been put in place and
a [Contributing](CONTRIBUTING.md) document is under development.

# License

The project is licensed under [Apache License, Version 2.0](LICENSE).
