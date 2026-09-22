## Datagen
- Run datagen: `./gradlew :client:runData` (outputs to common/src/generated/resources)
- If there is no item texture and datagen fails do not create or copy .png files
- Exctraction of PNGs from bbmodel, geo, json models are permited, but folder where this files will be located ALWAYS should be asked.
- When creating LuckPerms meta write it and explain what it does into docs/luckperms-keys.txt
- Never create recipes exceot example ones, like bedrock -> dirt for testing reasons