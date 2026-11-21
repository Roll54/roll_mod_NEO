## Datagen
- Run datagen: `./gradlew :client:runData` (outputs to common/src/generated/resources)
- If there is no item texture and datagen fails do not create or copy .png files
- Exctraction of PNGs from bbmodel, geo, json models are permited, but folder where this files will be located ALWAYS should be asked.
- When creating LuckPerms meta write it and explain what it does into docs/luckperms-keys.txt
- Never create recipes except example ones, like bedrock -> dirt for testing reasons
- If mechanic, Item, Block, practicly anything. goes to deprication it should not be deleted if not stated otherwise
## Hub UI (LdLib2)
- Text keeps at least 3px from any box edge: buttons, fields, drop-downs, switches, list and window frames. Labels over or under a control get a 3px margin; scrollers inside themed frames pad their viewport by 7px (up to 4px frame + 3px). Check every new or changed screen against this before calling it done.
- On the AE2 and Ore themes a button's face is drawn 1px above and below its box (`LdLibBevelButtonMixin`). LdLib containers clip their children by default, so a button on a container's edge loses its border: give the container `setOverflowVisible(true)` (keep clipping only for collapsed, height-0 rows) or keep the button 1-2px off that edge.
- Pixel-art icons are drawn at their own size or a whole multiple of it, on whole pixels (`HubIcons.snapped`); a non-integer scale or a half-pixel position doubles or drops pixels.
