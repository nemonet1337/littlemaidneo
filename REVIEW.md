# REVIEW.md

## What matters in this repository
- Protected core B is immutable: `LMMLResources/` path, `.cfg` + `.ogg` voice packs, `LMSounds` strings, `TextureIndexes` / `TextureColors`, built-in `biped_*` part names, and `LMSoundPayload` packet shape.
- Do not revive LMM/MMM `.class` model-pack loading.
- Dependency direction is `mods -> modelloader -> common` only. modelloader/common must not import `LittleMaidEntity` or `NetworkHandler`.
- New Mixins must be registered in `apps/mods/src/main/resources/littlemaidneo.mixins.json`.
- Entity NBT / world-save compatibility (souls, contract, inventory, `HeadCosmetic`) is high-risk.
- Do not add affection (好感度); it is frozen until a spec exists. Prefer small, explicit fixes over refactors of `LittleMaidEntity`.

## Severity calibration
- Critical: world-save corruption, load-time Mixin crash, client/server packet incompatibility.
- Warning: unregistered Mixin, module-boundary violation, missing GameTest for a new job or contract path, untested edge cases.
- Do not flag Gradle wrapper noise or generated `apps/mods/src/generated/resources/` formatting.

## Verification expectations
- Behavior changes to contract, jobs, or AI need GameTests under namespace `littlemaidneo`.
- `./gradlew compileJava` (or `build`) should stay green. DataGen output is git-tracked.
- Networking payload changes need a paired client/server look, not a one-sided packet edit.
