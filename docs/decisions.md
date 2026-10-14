# Architecture decisions

## P0: single application module

The initial scaffold uses one `app` module while the product surface is empty. Feature and core module boundaries from `AGENTS.md` will be introduced as code grows, without changing the package root or application ID.

## P0: build tooling

The project uses Gradle Kotlin DSL, version catalogs, Android Gradle Plugin 8.9.1, Kotlin 2.1.20, and the Compose compiler plugin. These versions are compatible with Java 17 and keep dependency versions centralized.

## P0: dependency injection staging

The Hilt navigation dependency is available in the catalog, while the Hilt Gradle plugin and compiler are introduced with generated repositories and data sources. This keeps the empty shell buildable and leaves the package and dependency choices ready for P2.
