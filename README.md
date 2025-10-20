
# Rewrite Plexus Test Case

A [OpenRewrite](https://docs.openrewrite.org/) recipe collection for migrating from Plexus TestCase to modern JUnit testing patterns in Maven projects.

## Overview

This project provides OpenRewrite recipes to help modernize legacy Maven plugins and projects that use the `PlexusTestCase` class. 

## Features

The project includes the following OpenRewrite recipes:

- **ReplacePlexusTestCase**: Migrates test classes from `PlexusTestCase` to use `PlexusTest` JUnit 5 annotation 
- **ReplaceLookup**: Refactors Plexus component lookup patterns to use dependency injection
- **MigratePlexusTestCase** is a composite OpenRewrite recipe that provides a complete migration path from legacy Plexus TestCase-based tests to modern JUnit 5 with Plexus testing support (includes the above-mentioned recipe).

## What it Does

This recipe collection helps with:

- Converting legacy Plexus TestCase-based tests to JUnit 5
- Replacing manual component lookups with proper dependency injection

## Usage

This project can be used as part of an OpenRewrite migration to automatically refactor legacy Maven plugin tests. The recipes are configured through the standard OpenRewrite YAML configuration and can be applied to codebases that need to migrate away from  Plexus TestCase patterns.

```shell
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=dev.parsick.maven.rewrite.plexustestcase:rewrite-plexus-test-case:0.1.0-SNAPSHOT -Drewrite.activeRecipes=dev.parsick.maven.rewrite.plexustestcase.MigratePlexusTestCase
```

## License
See [LICENSE](LICENSE) file for details.