# Automatic Low Detail

Automatically turn off ground decorations while inside certain areas (like raids)

Improves floor tile visibility without having to manually toggle the Low Detail plugin each time you enter or leave an area. This is especially useful for intense PvM encounters.

This plugin was originally based on [Low Detail Chambers](https://runelite.net/plugin-hub/show/low-detail-chambers) ([github](https://github.com/JacobLindelof/runelite-plugins/tree/low-detail-chambers)), extended to support the other two raid encounters, etc. as well as being toggleable per-area.

### Supported areas

- Chambers of Xeric
- Theatre of Blood
- Tombs of Amascut
- Inferno (and TzHaar-Ket-Rak's Challenges)
- Hallowed Sepulchre

### Changelog

#### v1.1.2 (Patch release)

- Fixed Automatic Low Detail being disabled after starting a Chambers of Xeric raid (See [#4](https://github.com/bepzi/runelite-plugins/issues/4))

#### v1.1.1 (Patch release)

- Fixed CoX scouting breaking when reloading the raid from the inside stairs (See [#2](https://github.com/bepzi/runelite-plugins/issues/2))
- Changed internal logic for changing low detail mode to only be applied if absolutely necessary (See [#3](https://github.com/bepzi/runelite-plugins/issues/3))
- Minor code optimizations and improvements
- Flagged incompatibility with Low Detail Chambers

#### v1.1 (Minor release)

- Renamed plugin to "Automatic Low Detail"
- Added support for Inferno (and TzHaar-Ket-Rak's Challenges)
- Added support for Hallowed Sepulchre (See [#1](https://github.com/bepzi/runelite-plugins/issues/1))
- Fixed a bug where disabling Runelite's Low Detail plugin could incorrectly disable low detail mode while Automatic Low Detail was still active

#### v1.0 (Major release)

- Initial release
- Added support for Theatre of Blood
- Added support for Tombs of Amascut