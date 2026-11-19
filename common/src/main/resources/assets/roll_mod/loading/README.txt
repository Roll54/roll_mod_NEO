Loading-screen slides (the player bases shown while the game loads)
====================================================================

This folder: common/src/main/resources/assets/roll_mod/loading/
One JSON file per slide, with its image next to it in this folder (any file name, ending in .json):

    {
      "image": "spawn_megabase.png",
      "date": "Season 3",
      "description": "Spawn Megabase",
      "authors": "Roll54, Lorp_ooo"
    }

  image        file name of the picture in this folder. PNG or JPG only (no WebP).
  date         which ModernTech season the build is from.
  description  the building's name. Shown bold, bottom left.
  authors      who built it. Shown under the name, after the season.

Keys are not case-sensitive ("Date", "AuthorS" work too). Text: Latin letters only, the
early-window font has no Cyrillic.

The image is drawn to fill a 16:9 screen; anything wider or taller is cropped evenly from both
sides. 1920x1080 is ideal; it is rendered at 1708x960 (earlyWindowFBScale = 2), so smaller images
get soft, and below about 1280x720 visibly blurry. Files are shipped as they are, so keep an eye on
size: a 1080p screenshot is 2-5 MB as PNG, a few hundred KB as JPG.

Order is shuffled every launch; each slide stays 5 s, then crossfades into the next.

These files are NOT part of roll_mod's jar: :earlywindow builds them into
roll_mod-earlywindow-<version>.jar (see earlywindow/build.gradle). That jar goes in the pack's mods/
on its own, and the pack's config/fml.toml needs:
    earlyWindowProvider = "roll_mod"
    earlyWindowFBScale = 2
In dev, runClient rebuilds it and copies it into client/run/mods by itself.
