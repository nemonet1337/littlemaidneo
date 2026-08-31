# Minecraft NeoForge Coding Standards

## Core Principles
- Prefer the **latest stable NeoForge** and corresponding Minecraft version APIs and systems.
- Always favor official NeoForge / Minecraft standard features, events, registries, and data-driven systems over custom reinvention or outdated patterns.
- When multiple approaches exist, choose the one that aligns with current NeoForge best practices and the latest documentation.

## Version & API Preferences
- Target the newest stable Minecraft + NeoForge combination unless the project explicitly requires an older version.
- Prefer NeoForge-specific APIs and events over legacy Forge-only patterns.
- Use the modern registry system, deferred registers, and data generation (Datagen) pipelines.
- Prefer data-driven content (recipes, loot tables, advancements, tags, models, blockstates) over hard-coded Java implementations whenever feasible.
- Use the latest event bus patterns and lifecycle events provided by NeoForge.

## Implementation Guidelines
- Follow NeoForge’s recommended project structure and packaging conventions.
- Prefer composition and existing Minecraft/NeoForge systems (capabilities, attachments, components where applicable) over large inheritance hierarchies.
- Keep side-specific code clearly separated (`@OnlyIn` / DistExecutor or equivalent modern approaches).
- Avoid deprecated methods and classes. When encountering them, migrate to the current replacement.
- Prefer configuration via the modern config system rather than ad-hoc solutions.

## Dependencies & Compatibility
- When adding libraries or other mods as dependencies, prefer versions that are actively maintained for the current NeoForge target.
- Minimize reflection and access transformers; use them only when no public API alternative exists.
- Document any version-specific workarounds clearly.

## Documentation Reference
- Prefer the official NeoForge documentation and the latest Minecraft source mappings when resolving API questions.
- When uncertain about the correct modern approach, prioritize solutions that match current NeoForge examples and recommended patterns over older Forge tutorials.