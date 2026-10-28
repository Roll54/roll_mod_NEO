/// через те який Техніціан даун мені тепер треба закидувати абсолютно все в один клятий файл щоб його гавнокод міг правильно все обробити

    let mi = (id) => `modern_industrialization:${id}`;






//--------------------------------------------------------------------МАТЕРІАЛИ!!!!----------------------------------------------------------------------------------------------------
MIMaterialEvents.addMaterials(event => {
  event.createMaterial("Redstone Alloy", "redstone_alloy", 0xdb0007, builder => {
    builder
      .hardness("soft")
      .materialSet("shiny")
      .addParts("dust", "wire", "plate", "fine_wire", "rod", "bolt", "ingot")
      .defaultRecipes()
  });

  event.createMaterial("Phosphorus", "phosphorus", 0x421408, builder => {
    builder
      .hardness("soft")
      .materialSet("stone")
      .addParts("dust")
  });

  event.createMaterial("Chrome-Vanadium Steel", "chrome_vanadium_steel", 0x7c85b8, builder => {
    builder
      .hardness("hard")
      .materialSet("shiny")
      .addParts("dust", "hot_ingot", "ingot", "plate", "large_plate",  "curved_plate",
                "rod", "bolt", "ring", "gear", "blade", "rotor")
      .barrel("Chrome-Vanadium Steel Barrel", "chrome_vanadium_steel_barrel", 16384)
      .block('copper')
      .defaultRecipes()
  });

  event.createMaterial("Titanium 6/4", "titanium_64", 0x5b6e91, builder => {
    builder
      .hardness("very_hard")
      .materialSet("shiny")
      .addParts("dust", "hot_ingot", "ingot", "plate", "large_plate",  "curved_plate")
      .pipeCasing(12.0)
      .specialCasing('Titanium 6/4 Machine Casing', 'titanium_64_machine_casing', 12.0)
      .defaultRecipes()
  });

  event.createMaterial("Niobium", "niobium", 0x6a8a76, builder => {
    builder
      .hardness("average")
      .materialSet("dull")
      .addParts("dust")
  });

  event.createMaterial("Niobium–Titanium Alloy", "niobium_titanium_alloy", 0x6e5d8d, builder => {
    builder
      .hardness("soft")
      .materialSet("stone")
      .addParts("dust", "hot_ingot", "ingot", "plate", "rod", "wire", "fine_wire")
      .defaultRecipes()
  });

  event.createMaterial("Osmium", "osmium", 0x85b5c7, builder => {
    builder
      .hardness("very_hard")
      .materialSet("shiny")
      .addParts("dust")
  });

  event.createMaterial("Osmiridium", "osmiridium", 0xa5b5c7, builder => {
    builder
      .hardness("very_hard")
      .materialSet("shiny")
      .addParts("dust", "ingot", "hot_ingot", "plate", "large_plate", "curved_plate", "bolt", "rod", "ring", "gear")
      .tank("Osmiridium tank", "osmiridium_tank", 65536)
      .pipeCasing(12.0)
      .specialCasing('Osmiridium strong casing', 'osmirium_casing', 12.0)
      .defaultRecipes()
  });

  event.createMaterial("Tantalum", "tantalum", 0x625F5C, builder => {
    builder
      .hardness("very_hard")
      .materialSet("stone")
      .addParts("tiny_dust", "dust", "ingot", "plate", "wire", "fine_wire")
      .ore({
        "generate": false,
        "ore_set": "redstone",
      })
      .rawMetal("iron", false)
      .defaultRecipes()
      
  });

  event.createMaterial("Rhenium", "rhenium", 0x3e3540, builder => {
    builder
      .hardness("hard")
      .materialSet("metallic")
      .addParts("tiny_dust", "dust", "ingot", "plate", "wire", "fine_wire", "large_plate", "curved_plate")
      .ore({
        "generate": false,
        "ore_set": "redstone",
      })
      .rawMetal("iron", false)
      .defaultRecipes()
      
  });


// Космос
  event.createMaterial("Black Steel Ingot", "black_steel", 0x3d3d3d, builder => {
    builder
      .hardness("average")
      .materialSet("metallic")
      .addParts("dust", "ingot")
      .defaultRecipes()
  });

  event.createMaterial("Space Alloy", "cosmic_alloy_one", 0x4a84c7, builder => {
    builder
      .hardness("average")
      .materialSet("metallic")
      .addParts("ingot", "plate", "large_plate", "curved_plate")
      .pipeCasing(8.0)
      .defaultRecipes()
      .block('iron')
  });

  event.createMaterial("Palladium", "palladium", 0x367896, builder => {
    builder
      .hardness("average")
      .materialSet("shiny")
      .addParts("dust", "ingot", "plate", "wire", "fine_wire", "rod")
      .defaultRecipes()
      .block('iron')
  });

  event.createMaterial("Ruthenium", "ruthenium", 0x161f24, builder => {
    builder
      .hardness("hard")
      .materialSet("shiny")
      .addParts("dust", "hot_ingot","ingot", "plate", "wire", "fine_wire")
      .defaultRecipes()
      .block('iron')
  });

event.createMaterial("rhodium plated palladium", "rhodium_plated_palladium", 0xd194c3,
builder => {
builder
.hardness("very_hard")
.materialSet("shiny")
.addParts("dust", "ingot", "bolt", "plate", "rod", "ring", "curved_plate", "large_plate", "hot_ingot")
.defaultRecipes()
.barrel("rhodium plated palladium barrel", "rhodium_plated_palladium_barrel", 32768)
.block('iron')
});

event.createMaterial("Rhodium", "rhodium", 0xbf0d25,
builder => {
builder
.hardness("hard")
.materialSet("shiny")
.addParts("dust", "ingot", "plate", "large_plate", "curved_plate", "hot_ingot", "wire", "fine_wire")
.defaultRecipes()
.block('iron')
});

event.createMaterial("Block blencium", "block_blencium", 0x4c76d9,
builder => {
builder
.hardness("very_hard")
.materialSet("shiny")
.addParts("ingot", "plate", "wire")
.defaultRecipes()
.cable("superconductor")
.block('iron')
});

event.createMaterial("Sunnarium", "sunnarium", 0xf6fa00,
builder => {
builder
.hardness("soft")
.materialSet("shiny")
.addParts("dust", "ingot", "plate", "bolt")
.defaultRecipes()
.cable("hv")
.block('iron')
});


event.createMaterial("Cosmic Alloy MK2", "cosmic_alloy_two", 0xd6710d,
builder => {
builder
.hardness("very_hard")
.materialSet("metallic")
.addParts("ingot", "plate", "gear", "large_plate", "curved_plate", "bolt", "rod", "ring")
.specialCasing('Space Casing MK2', 'space_casing_t2', 12.0)
.defaultRecipes()
.block('iron')
.pipeCasing(8)

});

event.createMaterial("Cosmic Alloy MK3", "cosmic_alloy_three", 0x612f48,
  builder => {
  builder
  .hardness("very_hard")
  .materialSet("metallic")
  .addParts("ingot", "plate", "gear", "large_plate", "curved_plate", "bolt", "rod", "ring")
  .specialCasing('Space Casing MK3', 'space_casing_t3', 12.0)
  .block('iron')
  .pipeCasing(10)
  .defaultRecipes()
  
});

event.createMaterial("Cosmic Alloy MK4", "cosmic_alloy_four", 0xd42c4b,
  builder => {
  builder
  .hardness("very_hard")
  .materialSet("shiny")
  .addParts("ingot", "plate", "gear", "large_plate", "curved_plate", "bolt", "rod", "ring")
  .specialCasing('Space Casing MK4', 'space_casing_t4', 12.0)
  .block('iron')
  .pipeCasing(12)
  .defaultRecipes()
  
});



event.createMaterial('Polytetrafluoroethylene', 'polytetrafluoroethylene', 0x382f82,
builder => {
builder.addParts('plate', 'large_plate', 'gear', 'curved_plate', 'rod', 'bolt', 'ring', 'ingot')
.hardness("average")
.materialSet('shiny')
.specialCasing('Chemically Inert PTFE Casing', 'chemically_inert_ptfe_casing', 8.0)
.pipeCasing(8.0)
.block('iron')
.defaultRecipes()
});

event.createMaterial("Incoloy", "incoloy", 0x3461a3, // english name, internal name, and material color in hex
builder => {
builder
.hardness("very_hard") // hardness controls the speed of some recipes, can be "soft", "average", "hard", "very_hard" (default is "average")
.materialSet("shiny") // controls the texture set used by the material, can be "metallic", "shiny", "stone", "dull" (default is "metallic")
.addParts("ingot", "dust", "rod", "gear", "blade", "rotor", "plate", "bolt", "large_plate", "hot_ingot", "wire", "curved_plate", "ring", "fine_wire",)
//.defaultRecipes() // add the default recipes for the material (crafting, smelting and machines)
.machineCasing(8.0) // same as above but for machine casings but the custom name and path are optional
.pipeCasing(8.0)
.specialCasing('Incoloy casing', 'incoloy_casing', 16.0)
.block('iron')
.defaultRecipes()
});

event.createMaterial("Tungstensteel", "tunstensteel", 0x5c5c6d, // english name, internal name, and material color in hex
builder => {
builder
.hardness("very_hard") // hardness controls the speed of some recipes, can be "soft", "average", "hard", "very_hard" (default is "average")
.materialSet("metallic") // controls the texture set used by the material, can be "metallic", "shiny", "stone", "dull" (default is "metallic")
.addParts("ingot", "dust", "rod", "gear", "blade", "rotor", "plate", "bolt", "large_plate", "hot_ingot", "wire", "curved_plate", "ring", "fine_wire",)
//.defaultRecipes() // add the default recipes for the material (crafting, smelting and machines)
.machineCasing(8.0) // same as above but for machine casings but the custom name and path are optional
.pipeCasing(8.0)
.specialCasing('Tunstensteel Heat Proof Casing', 'tungsten_steel_heat_proof_casing', 16.0)
.block('iron')
.defaultRecipes()
});


event.createMaterial("Yeast", "yeast", 0x9e8a52, builder => {
  builder
    .hardness("soft")
    .materialSet("dull")
    .addParts("dust", "tiny_dust")
    .defaultRecipes()
});

event.createMaterial("Calcium sulfate", "calcium_sulfate", 0xb5ac8b, builder => {
  builder
    .hardness("soft")
    .materialSet("shiny")
    .addParts("dust", "tiny_dust")
    .defaultRecipes()
});

event.createMaterial("Iron(II) sulfate", "iron_two_sulfate", 0xccbd9d, builder => {
  builder
    .hardness("soft")
    .materialSet("shiny")
    .addParts("dust", "tiny_dust")
    .defaultRecipes()
});

event.createMaterial("Copper(II) sulfate", "copper_two_sulfate", 0xfac34d, builder => {
  builder
    .hardness("soft")
    .materialSet("shiny")
    .addParts("dust", "tiny_dust")
    .defaultRecipes()
});

event.createMaterial("Tantalum-Rhenium", "tantalum_rhenium", 0xe07b90,
  builder => {
  builder
  .hardness("very_hard")
  .materialSet("metallic")
  .addParts("ingot", "plate", "gear", "large_plate", "curved_plate", "bolt", "rod", "ring", "wire", "fine_wire", "hot_ingot", "dust")
  .defaultRecipes()
  
});


event.createMaterial("Enderium", "enderium", 0x0e6363, // english name, internal name, and material color in hex
builder => {
builder
.hardness("very_hard") // hardness controls the speed of some recipes, can be "soft", "average", "hard", "very_hard" (default is "average")
.materialSet("metallic") // controls the texture set used by the material, can be "metallic", "shiny", "stone", "dull" (default is "metallic")
.addParts("ingot", "dust", "rod", "gear", "blade", "rotor", "plate", "bolt", "large_plate", "hot_ingot", "wire", "curved_plate", "ring", "fine_wire",)
.specialCasing('Enderium Casing', 'enderium_casing', 8.0)
.defaultRecipes()
});

event.createMaterial("Netherite", "netherite", 0x3c2a23, // назва, внутрішнє ім'я, колір
builder => {
    builder
        .hardness("very_hard") // твердість матеріалу
        .materialSet("metallic") // набір текстур
        .addParts(
            "plate",         // пластина
            "large_plate",   // велика пластина
            "rod",           // палка
            "ring",          // кільце
            "bolt",          // болт
            "gear"           // шестерня
        )
    .addExternalPart("ingot", "minecraft:netherite_ingot")
       .addExternalPart("dust", "extended_industrialization:netherite_dust")
        .defaultRecipes(); // стандартні рецепти
});



event.createMaterial("Magnesium", "magnesium", 0x9e6959, builder => {
  builder
    .hardness("soft")
    .materialSet("dull")
    .addParts("dust", "tiny_dust")
    .defaultRecipes()
})

event.createMaterial("Magnalium", "magnalium", 0x8E9D7A,
  builder => {
  builder
  .hardness("average")
  .materialSet("metallic")
  .addParts("ingot", "plate", "gear", "large_plate", "curved_plate", "bolt", "rod", "ring", "wire", "fine_wire", "dust", "rotor")
  .block('copper')
  .defaultRecipes()
  })

  event.createMaterial("Bismuth Bronze", "bismuth_bronze", 0x9b6c3a, builder => {
  builder
    .hardness("soft")
    .materialSet("shiny")
    .addParts("dust", "ingot")
    .defaultRecipes()
});

event.createMaterial("Black Bronze", "black_bronze", 0x5e0f5a, builder => {
  builder
    .hardness("average")
    .materialSet("metallic")
    .addParts("dust", "ingot")
    .defaultRecipes()
});

//позорний метал.
event.createMaterial("Zinc", "zinc", 0xb7b7b7, builder => {
  builder
    .hardness("soft")
    .materialSet("metallic")
    .addParts("dust", "ingot", "plate")
    .defaultRecipes()
});

});



//--------------------------------------------------------------------МОДИФІКАЦІЇ!!!!----------------------------------------------------------------------------------------------------


MIMachineEvents.addEbfTiers(event => {
  event.add('modern_industrialization:tungsten_coil', 512, 'Tungsten')
})
MIMaterialEvents.modifyMaterial("electrum", event => {
  event.builder
    .addParts('rod')
})

MIMaterialEvents.modifyMaterial("gold", event => {
  event.builder
    .addParts('wire', 'fine_wire')
})

MIMaterialEvents.modifyMaterial("annealed_copper", event => {
  event.builder
    .addParts('fine_wire')
})

MIMaterialEvents.modifyMaterial("tungsten", event => {
  event.builder
    .addParts("wire", "coil")
    .cable("lv")
})

MIMaterialEvents.modifyMaterial("copper", event => {
  event.builder
    .addParts("large_plate")
})

MIMaterialEvents.modifyMaterial("tin", event => {
  event.builder
    .addParts("large_plate")
})

MIMaterialEvents.modifyMaterial("bronze", event => {
  event.builder
    .addParts("large_plate")
})


//--------------------------------------------------------------------РІДИНИ!!!!----------------------------------------------------------------------------------------------------



MIRegistrationEvents.registerFluids(event => {

  // Базове
  event.register('Carbon Monoxide', 'carbon_monoxide',
    0x8c8c8c, 'steam', true, 'medium');
  event.register('Carbon Dioxide', 'carbon_dioxide',
    0xe8e8e8, 'steam', true, 'high');
  event.register('Ammonia', 'ammonia',
    0xe4ffff, 'lava', true, 'low');
  event.register('Hypochlorous Acid', 'hypochlorous_acid',
    0xc6df55, 'water', false, 'medium');
  event.register('Nitric Acid', 'nitric_acid',
    0x1b1994, 'water', false, 'low');
  event.register('Nitrogen Dioxide', 'nitrogen_dioxide',
    0xa03c1e, 'steam', true, 'medium');
  event.register('Nitric Oxide', 'nitric_oxide',
    0x7f7f7f, 'steam', true, 'low');
  event.register('Aqua Regia', 'aqua_regia',
    0xdbab3b, 'water', false, 'low');

  // Полімери
  event.register('Acrylonitril', 'acrylonitrile',
    0xf4e1a1, 'plasma', false, 'high');
  event.register('Epoxy', 'epoxy',
    0xffffff, 'plasma', false, 'high');
  event.register('PolyTetraFluoroEthylene', 'polytetrafluoroethylene',
    0x382f82, 'water', false, 'high');
  event.register('TetraFluoroEthylene', 'tetrafluoroethylene',
    0x435d9c, 'water', false, 'high');

  // Кислоти
  event.register('Iron(III) Chloride', 'iron_chloride',
    0x2e1500, 'lava', false, 'high');
  event.register('Sodium Persulfate', 'sodium_persulfate',
    0xa0d0a0, 'water', false, 'medium');
  event.register('Hydrofluoric Acid', 'hydrofluoric_acid',
    0x4d8aff, 'water', false, 'low');

  // Додаткові
  event.register('Molten Energium', 'molten_energium',
    0xde3c31, 'water', false, 'medium');
  event.register('Molten Glowstone', 'molten_glowstone',
    0xcfb034, 'lava', false, 'high');
  event.register('Hydrazine', 'hydrazine',
    0x91008d, 'water', false, 'medium');
  event.register('Hydrogen Peroxide', 'hydrogen_peroxide',
    0x71c8c9, 'water', false, 'medium');
  event.register('Perrhenic Acid', 'perrhenic_acid',
    0x4c4a4f, 'water', false, 'medium');
  event.register('Ethanol-containing Liquid', 'ethanol_containing_liquid',
    0x5bb099, 'plasma', false, 'high');
  event.register('Fermentation Solution', 'fermentation_solution',
    0x112940, 'water', false, 'medium');
  event.register('Difluorodichloroethylene', 'difluorodichloroethylene',
    0x31ad03, 'plasma', false, 'medium');
  event.register('Dichloroethylene', 'dichloroethylene',
    0x78999e, 'plasma', true, 'medium');
  event.register('Sulfur Dioxide', 'sulfur_dioxide',
    0x7d6725, 'steam', true, 'low');
    event.register('Glue', 'glue',
    0xe6d08f, 'water', false, 'medium');

  // Космос
  event.register('Rocket Propellant', 'rocket_propellant',
    0xcb34f1, 'water', false, 'low');
  event.register('Dimethylhydrazine', 'dimethylhydrazine',
    0x62529e, 'water', false, 'low');
  event.register('Dinitrogen Tetroxide', 'dinitrogen_tetroxide',
    0x65d7e6, 'water', false, 'medium');
  event.register('Dimethylamine', 'dimethylamine',
    0xc16000, 'plasma', true, 'low');
  event.register('Monochloramine', 'monochloramine',
    0x910d1f, 'plasma', true, 'low');
  event.register('Methanol', 'methanol',
    0x9cffff, 'water', false, 'low');

  // Планетні рідини
  event.register('Moon Air', 'moon_air',
    0xd95102, 'water', true, 'low');
  event.register('Liquid Moon Air', 'liquid_moon_air',
    0xd95102, 'lava', false, 'medium');
  event.register('Sunnarium Nitride', 'sunnarium_nitride',
    0x00fab3, 'water', false, 'medium');
  event.register('Xenon', 'xenon',
    0xc9eaff, 'water', true, 'low');

  // Елементи
  event.register('Molten Gold', 'molten_gold',
    0xd9ad2b, 'lava', false, 'high');
  event.register('Molten Tungsten', 'molten_tungsten',
    0x461e4f, 'lava', false, 'high');
  event.register('Molten Rhenium', 'molten_rhenium',
    0x3e3540, 'lava', false, 'high');
  event.register('Rhenium Plasma', 'rhenium_plasma',
    0x3e3540, 'plasma', true, 'high');
  event.register('Mercury', 'mercury',
    0xd9d0d4, 'lava', false, 'high');
  event.register('Fluorine', 'fluorine',
    0xc8b550, 'steam', true, 'high');

  // Біологія / хімія (із KubeJS-списку)
  event.register('Blood', 'blood',
    0xc11326, 'plasma', false, 'high');
  event.register('Acidic Iridium Solution', 'acidic_iridium_solution',
    0xa7ad84, 'water', false, 'low');
  event.register('Diluted Agar Solution', 'diluted_agar_solution',
    0x6c8a5e, 'plasma', false, 'low');
  event.register('Raw Nutrient Solution', 'raw_nutrient_solution',
    0xc79554, 'water', false, 'low');
  event.register('Distilled Nutrient Solution', 'distilled_nutrient_solution',
    0xba9e79, 'water', false, 'high');
  event.register('Distilled Water', 'distilled_water',
    0x40738f, 'plasma', false, 'medium');
  event.register('Saline Solution', 'saline_solution',
    0x085e8c, 'water', false, 'low');
  event.register('Benzenesulfonic Acid', 'benzenesulfonic_acid',
    0xa68367, 'water', false, 'low');
  event.register('Catechol', 'catechol',
    0x520203, 'water', false, 'low');
  event.register('Pyridoxine', 'pyridoxine',
    0x6cd15c, 'water', false, 'high');
  event.register('Adrenaline', 'adrenaline',
    0xcf3881, 'water', false, 'high');

  // KubeJS -> MI: базові флюїди (перенесені)
  event.register('Chloroform', 'chloroform',
    0xa6a6a6, 'water', false, 'medium');
  event.register('Salt Water', 'salt_water',
    0x1e18ba, 'water', false, 'low');
  event.register('Titanium Tetrachloride', 'titanium_tetrachloride',
    0x5c110a, 'water', false, 'low');
  event.register('Magnesium Chloride', 'magnesium_chloride',
    0x6b216b, 'water', false, 'low');

  // KubeJS -> MI: космос/високотех
  event.register('Diluted Hydrochloric Acid', 'diluted_hydrochloric_acid',
    0x3e804e, 'water', false, 'low');
  event.register('Vanadium Acid', 'vanadium_acid',
    0x8a6b16, 'water', false, 'low');
  event.register('Radon', 'radon',
    0xa629cc, 'water', true, 'low');
  event.register('Sulfuric Copper Solution', 'sulfuric_copper_solution',
    0x32c7b1, 'water', false, 'low');
  event.register('Rhodium Sulfate', 'rhodium_sulfate',
    0x8c0683, 'water', false, 'low');
  event.register('Sulfuric Nickel Solution', 'sulfuric_nickel_solution',
    0x5b6611, 'water', false, 'low');
  event.register('Acidic Osmium Solution', 'acidic_osmium_solution',
    0x03fbff, 'water', false, 'low');
  event.register('Osmium Tetroxide', 'osmium_tetroxide',
    0x4377ab, 'water', false, 'low');

      // Uranium processing gases
  event.register('Uranium Hexafluoride', 'uranium_hexafluoride',
    0x7fd0d8, 'steam', true, 'medium');
  event.register('Enriched Uranium Hexafluoride', 'enriched_uranium_hexafluoride',
    0x9af1ff, 'steam', true, 'medium');
  event.register('Depleted Uranium Hexafluoride', 'depleted_uranium_hexafluoride',
    0x5aa8b0, 'steam', true, 'medium');

      // Epoxy chemistry
event.register('Cumene', 'cumene',
  0xc99b52, 'water', false, 'medium');
event.register('Acetone', 'acetone',
  0xa0e1d2, 'water', false, 'low');
event.register('Acetic Acid', 'acetic_acid',
  0xded4b9, 'water', false, 'medium');
event.register('Propane', 'propane',
  0xf0c56b, 'steam', true, 'low');
event.register('Phenol', 'phenol',
  0xb38d72, 'water', false, 'medium');
event.register('Bisphenol A', 'bisphenol_a',
  0xe2b6a5, 'water', false, 'high');
event.register('Epichlorohydrin', 'epichlorohydrin',
  0x6fbcc3, 'water', false, 'medium');

event.register('Molten Lithium', 'molten_lithium',
  0xd8d8e8, 'lava', false, 'medium');
});

















//--------------------------------------------------------------------МОДЕЛІ ДЛЯ МУЛЬТИБЛОКІВ!!!!----------------------------------------------------------------------------------------------------
/*
MIMachineEvents.registerCasings(event => {
    event.registerBlockImitation("space_research_station_controller_casing", 'modern_industrialization:titanium_64_machine_casing');
    event.registerBlockImitation("space_casing_t2", 'modern_industrialization:space_casing_t2');
    event.registerBlockImitation("inert_casing", 'modern_industrialization:chemically_inert_ptfe_casing');
    event.registerBlockImitation("turbine_casing", 'modern_industrialization:incoloy_casing');
    event.registerBlockImitation("enderium_casing", 'modern_industrialization:enderium_casing');
})
*/

//--------------------------------------------------------------------ОДНОБЛОКИ!!!!----------------------------------------------------------------------------------------------------
let THERMAL_CENTREFUGE;

MIMachineEvents.registerRecipeTypes(e => {
  THERMAL_CENTREFUGE = e.register('thermal_centrefuge')
        .withItemInputs()
        .withItemOutputs();
});

MIMachineEvents.registerMachines(event => {
  event.craftingSingleBlock(
      /* GENERAL PARAMETERS FIRST */
      // English name, internal name, recipe type (see above), list of tiers (can be bronze/steel/electric)
      "Thermal Centrefuge", "thermal_centrefuge", THERMAL_CENTREFUGE, "electric",
      /* GUI CONFIGURATION */
      // Background height (or -1 for default value), progress bar, efficiency bar, energy bar
      186,
       event.progressBar(70, 45, "centrifuge"),
       event.efficiencyBar(48, 86), 
       event.energyBar(14, 44),
      /* SLOT CONFIGURATION */
      // Number of slots: item inputs, item outputs, fluid inputs, fluid outputs
      1, 3, 0, 0,
      // Capacity for fluid slots (unused here)
      48,
      // Slot positions: items and fluids.
      items => items.addSlots(42, 47, 1, 1).addSlots(100, 47, 3, 1),
      fluids => {},
      /* MODEL CONFIGURATION */
      // front overlay?, top overlay?, side overlay?
      false, false, true,
  );
})

let LV_ORE_WASHER;

MIMachineEvents.registerRecipeTypes(e => {
  LV_ORE_WASHER = e.register('lv_ore_washer')
        .withItemInputs()
        .withFluidInputs()
        .withItemOutputs();
});

MIMachineEvents.registerMachines(event => {
  event.craftingSingleBlock(
      /* GENERAL PARAMETERS FIRST */
      // English name, internal name, recipe type (see above), list of tiers (can be bronze/steel/electric)
      "Ore Washer", "lv_ore_washer", LV_ORE_WASHER, ["electric"],
      /* GUI CONFIGURATION */
      // Background height (or -1 for default value), progress bar, efficiency bar, energy bar
      186, event.progressBar(90, 40, "macerate"), event.efficiencyBar(48, 86), event.energyBar(14, 44),
      /* SLOT CONFIGURATION */
      // Number of slots: item inputs, item outputs, fluid inputs, fluid outputs
      3, 4, 1, 0,
      // Capacity for fluid slots (unused here)
      64,
      // Slot positions: items and fluids.
      items => items.addSlots(32, 35, 3, 1).addSlots(120, 35, 2, 2),
      fluids => fluids.addSlots(32,53, 1, 1),
      /* MODEL CONFIGURATION */
      // front overlay?, top overlay?, side overlay?
      false, false, true,
  );
})



let LASER_ENGRAVER;

MIMachineEvents.registerRecipeTypes(e => {
  LASER_ENGRAVER = e.register('laser_engraver')
        .withItemInputs()
        .withItemOutputs();
});

MIMachineEvents.registerMachines(event => {
  event.craftingSingleBlock(
      /* GENERAL PARAMETERS FIRST */
      // English name, internal name, recipe type (see above), list of tiers (can be bronze/steel/electric)
      "Laser Engraver", "laser_engraver", LASER_ENGRAVER, ["electric"],
      /* GUI CONFIGURATION */
      // Background height (or -1 for default value), progress bar, efficiency bar, energy bar
      186, event.progressBar(105, 45, "circuit"), event.efficiencyBar(48, 86), event.energyBar(14, 44),
      /* SLOT CONFIGURATION */
      // Number of slots: item inputs, item outputs, fluid inputs, fluid outputs
      3, 1, 0, 0,
      // Capacity for fluid slots (unused here)
      48,
      // Slot positions: items and fluids.
      items => items.addSlots(42, 47, 3, 1).addSlots(139, 47, 1, 1),
      fluids => {},
      /* MODEL CONFIGURATION */
      // front overlay?, top overlay?, side overlay?
      true, false, false,
  );
})



let CIRCUIT_ASSEMBLER;

MIMachineEvents.registerRecipeTypes(e => {
  CIRCUIT_ASSEMBLER = e.register('circuit_assembler')
        .withItemInputs()
        .withFluidInputs()
        .withItemOutputs();
});

MIMachineEvents.registerMachines(event => {
  event.craftingSingleBlock(
      /* GENERAL PARAMETERS FIRST */
      // English name, internal name, recipe type (see above), list of tiers (can be bronze/steel/electric)
      "Circuit Assembler", "circuit_assembler", CIRCUIT_ASSEMBLER, ["electric"],
      /* GUI CONFIGURATION */
      // Background height (or -1 for default value), progress bar, efficiency bar, energy bar
      186, event.progressBar(105, 45, "circuit"), event.efficiencyBar(48, 86), event.energyBar(14, 44),
      /* SLOT CONFIGURATION */
      // Number of slots: item inputs, item outputs, fluid inputs, fluid outputs
      6, 3, 2, 0,
      // Capacity for fluid slots (unused here)
      48,
      // Slot positions: items and fluids.
      items => items.addSlots(42, 27, 3, 2).addSlots(139, 27, 1, 3),
      fluids => fluids.addSlots(42,64, 2, 1),
      /* MODEL CONFIGURATION */
      // front overlay?, top overlay?, side overlay?
      true, true, false,
  );
})


//--------------------------------------------------------------------МУЛЬТИБЛОКИ!!!!----------------------------------------------------------------------------------------------------



let srs;

MIMachineEvents.registerRecipeTypes(event => {
  srs = event.register('srs')
    .withItemInputs()
    .withFluidInputs()
    .withItemOutputs()
});

MIMachineEvents.registerMachines(event => {

  const machineBase = event.memberOfBlock('modern_industrialization:titanium_64_machine_casing');
  const machineInside = event.memberOfBlock('modern_industrialization:tungsten_coil');
  const machineCenter = event.memberOfBlock('roll_mod:lapotronic_laser_block');
  const machineProof = event.memberOfBlock('ae2:quartz_vibrant_glass');
  const machineFrame = event.memberOfBlock('modern_industrialization:titanium_64_machine_casing_pipe');
  const machineHatches = event.hatchOf('item_input', 'item_output', 'energy_input', 'fluid_input', 'fluid_output');

  const SRS_SHAPE = event.layeredShape('space_research_station_controller_casing', [
    ['               ','               ','               ','               ','               ','               ','               ','     CCCCC     ','      C C      ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ','   CCC P CCC   ','               ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ','  CC   P   CC  ','               ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ',' CC    P    CC ','               ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ',' C     P     C ','       P       ','               ','               '],
    ['               ','               ','               ','               ','     C   C     ','     C   C     ','     CC CC     ','CC    PPP    CC','      PPP      ','      CCC      ','               '],
    ['               ','               ','      CVC      ','      CVC      ','      VVV      ','      VVV      ','     CCCCC     ','C    PRRRP    C','C    PRRRP    C','     CCCCC     ','      ccc      '],
    ['       L       ','       L       ','      VLV      ','      VLV      ','      VLV      ','      VLV      ','      CLC      ','CPPPPPRLRPPPPPC','    PPRRRPP    ','     CC CC     ','      c#c      '],
    ['               ','               ','      CVC      ','      CVC      ','      VVV      ','      VVV      ','     CCCCC     ','C    PRRRP    C','C    PRRRP    C','     CCCCC     ','      ccc      '],
    ['               ','               ','               ','               ','     C   C     ','     C   C     ','     CC CC     ','CC    PPP    CC','      PPP      ','      CCC      ','               '],
    ['               ','               ','               ','               ','               ','               ','               ',' C     P     C ','       P       ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ',' CC    P    CC ','               ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ','  CC   P   CC  ','               ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ','   CCC P CCC   ','               ','               ','               '],
    ['               ','               ','               ','               ','               ','               ','               ','     CCCCC     ','      C C      ','               ','               ']
  ])
  .key('C', machineBase, event.noHatch())
  .key('c', machineBase, machineHatches)
  .key('R', machineInside, event.noHatch())
  .key('L', machineCenter, event.noHatch())
  .key('V', machineProof, event.noHatch())
  .key('P', machineFrame, event.noHatch())
  .build();
  
  event.simpleElectricCraftingMultiBlock(
    /* GENERAL PARAMETERS */
    // English name, internal name, recipe type, multiblock shape
    'Space Research Station', 'space_research_station', srs, SRS_SHAPE,

    /* REI DISPLAY CONFIGURATION */
    // Progress bar
    event.progressBar(109, 52, 'circuit'),
    // Item inputs, Item outputs, Fluid inputs, Fluid outputs
    itemInputs => itemInputs.addSlots(56, 35, 3, 2), 
    itemOutputs => itemOutputs.addSlots(130, 53, 3, 1),
    fluidInputs => fluidInputs.addSlots(56, 71, 2, 1),
    fluidOutputs => {},

    /* MODEL CONFIGUATION */
    // Casing of the controller, Overlay folder
    'space_research_station_controller_casing', 'srs',
    // Front overlay?, Top overlay?, Side overlay?
    false, true, false,
  );
});


let rocket_assembler;

MIMachineEvents.registerRecipeTypes(event => {
  rocket_assembler = event.register('rocket_assembler')
    .withItemInputs()
    .withItemOutputs()
    .withFluidInputs()
});

MIMachineEvents.registerMachines(event => {

  const cleanStainlessSteelMachineCasing = event.memberOfBlock('modern_industrialization:steel_machine_casing');
  const tintedGlassBlock = event.memberOfBlock('modern_industrialization:stainless_steel_machine_casing_pipe');
  const crusherHatch = event.hatchOf('item_input', 'item_output', 'energy_input', 'fluid_input', 'fluid_output');

  const rocket_assembler_shape = event.layeredShape('solid_titanium_machine_casing', [
    [ ' ccc ', '  C  ', '  C  ', '  C  ', '     ' ], 
    [ 'ccccc', ' G G ', ' C C ', ' C C ', ' C C ' ],
    [ 'ccccc', 'C   C', 'C   C', 'C   C', '     ' ],
    [ 'ccccc', ' G G ', ' C C ', ' C C ', ' C C ' ],
    [ ' c#c ', '  C  ', '  C  ', '  C  ', '     ' ]
  ])
  .key('c', cleanStainlessSteelMachineCasing, crusherHatch)
  .key('G', cleanStainlessSteelMachineCasing, event.noHatch())
  .key('C', tintedGlassBlock, event.noHatch())
  .build();

  event.simpleElectricCraftingMultiBlock(
    // General parameters
    'Rocket Assembler',          // English name
    'rocket_assembler',          // internal name
    rocket_assembler,            // recipe type
    rocket_assembler_shape,        // multiblock shape

    // REI Display configuration
    event.progressBar(77, 33, 'triple_arrow'),
    // REI Item inputs, item outputs, fluid inputs, fluid outputs
    itemInputs => itemInputs.addSlots(35, 35, 2, 4), 
    itemOutputs => itemOutputs.addSlot(102, 35,),
    fluidInputs => fluidInputs.addSlots(35, 109, 2, 1),
    fluidOutputs => {},

    /* Model configuration */
    'solid_titanium_machine_casing',            // casing of the controller
    'rocket_assembler',      // overlay folder
    true,               // front overlay
    false,              // top overlay
    false,              // side overlay
  );
});


let PYROLYSE_OVEN;

MIMachineEvents.registerRecipeTypes(e => {
    PYROLYSE_OVEN = e.register('pyrolyse_oven')
        .withItemInputs()
        .withItemOutputs()
        .withFluidInputs()
        .withFluidOutputs();
});

MIMachineEvents.registerMachines(e => {
    // -- MOD NAMESPACE UTILITY FUNCTIONS -- // 
    let mi = (id) => `modern_industrialization:${id}`;
    
    const heatproofCasing = e.memberOfBlock(mi('heatproof_machine_casing'));
    const cupronickelCoil = e.memberOfBlock(mi('cupronickel_coil'));
    const pyrolyseHatch = e.hatchOf('item_input', 'item_output', 'fluid_input', 'fluid_output', 'energy_input');

    const pyrolyseShape = e.layeredShape('heatproof_machine_casing', [
        //y=0      1      2
        [ 'HHH', 'HHH', 'HHH' ],
        [ 'CCC', 'C C', 'CCC' ],
        [ 'CCC', 'C C', 'CCC' ],
        [ 'HHH', 'H#H', 'HHH' ]
    ])
        .key('H', heatproofCasing, pyrolyseHatch)
        .key('C', cupronickelCoil, e.noHatch())
        .build();

    e.simpleElectricCraftingMultiBlock(
        // General parameters
        'Pyrolyse Oven', // English name
        'pyrolyse_oven', // internal name
        PYROLYSE_OVEN, // recipe type
        pyrolyseShape, // multiblock shape

        // REI Display configuration
        e.progressBar(77, 33, 'triple_arrow'),
        // REI item inputs, item outputs, fluid inputs, fluid outputs
        itemInputs => itemInputs.addSlots(56, 35, 1, 2), 
        itemOutputs => itemOutputs.addSlots(102, 35, 1, 2),
        fluidInputs => fluidInputs.addSlots(36, 35, 1, 2), 
        fluidOutputs => fluidOutputs.addSlots(122, 35, 1, 2),

        /* MODEL CONFIGUATION */
        'heatproof_machine_casing', // casing of the controller
        'pyrolyse_oven', // overlay folder
        true, // front overlay
        false, // top overlay
        false, // side overlay
    );
});


let photolithographer;

MIMachineEvents.registerRecipeTypes(event => {
  photolithographer = event.register('photolithographer')
  .withItemInputs()
  .withItemOutputs()
  .withFluidInputs()
});

MIMachineEvents.registerMachines(event => {

  const cleanStainlessSteelMachineCasing = event.memberOfBlock('modern_industrialization:solid_titanium_machine_casing');
  const tintedGlassBlock = event.memberOfBlock('modern_industrialization:titanium_machine_casing_pipe');
  const kanthalCoil = event.memberOfBlock('ae2:quartz_vibrant_glass');
  const laser_block = event.memberOfBlock('roll_mod:lapotronic_laser_block');
  const crusherHatch = event.hatchOf('item_input', 'item_output', 'energy_input', 'fluid_input', 'fluid_output');

  const photolithographer_shape = event.layeredShape('solid_titanium_machine_casing', [
    [ 'TTT', 'TTT', 'TTT' ],
    [ 'TPT', 'TPT', 'TPT' ],
    [ 'TPT', 'PAP', 'TPT' ],
    [ 'TPT', 'GAG', 'TPT' ],
    [ 'TPT', 'PAP', 'TPT' ],
    [ 'TPT', 'TPT', 'TPT' ],
    [ 'TTT', 'T#T', 'TTT' ]
  ])
  .key('T', cleanStainlessSteelMachineCasing, crusherHatch)
  .key('P', tintedGlassBlock, event.noHatch())
  .key('G', kanthalCoil, event.noHatch())
  .key('A', laser_block, event.noHatch())
  .build();

  event.simpleElectricCraftingMultiBlock(
    // General parameters
    'Photolithographer',          // English name
    'photolithographer',          // internal name
    photolithographer,            // recipe type
    photolithographer_shape,        // multiblock shape

    // REI Display configuration
    event.progressBar(78, 23, 'wiremill'),
    
    // REI Item inputs, item outputs, fluid inputs, fluid outputs
    //я досі в душі не єбу як це блять працює, але працює 
    itemInputs => itemInputs.addSlots(62,5,3,1).addSlots(20,10,2,3),
    itemOutputs => itemOutputs.addSlot(120,23),
    fluidInputs => fluidInputs.addSlots(0, 10,1,3),
    fluidOutputs => {},
    /* Model configuration */
    'solid_titanium_machine_casing',            // casing of the controller
    'assembler',      // overlay folder
    true,               // front overlay
    false,              // top overlay
    false,              // side overlay
  );
});
//--------------------------------------------------------------------ГЕНЕРАТОРИ!!!!----------------------------------------------------------------------------------------------------

MIMachineEvents.registerMachines(event => {
 
    let mi = (id) => `modern_industrialization:${id}`;
    
    const heatproofCasing = event.memberOfBlock(mi('incoloy_casing'));
    const cupronickelCoil = event.memberOfBlock(mi('incoloy_machine_casing_pipe'));
    const pyrolyseHatch = event.hatchOf('item_input', 'item_output', 'fluid_input', 'fluid_output', 'energy_input', 'energy_output');

    const high_efficiency_turbine_Shape = event.layeredShape('turbine_casing', [
        //y=0      1      2
        [ 'HHH', 'HHH', 'HHH' ],
        [ 'CCC', 'CCC', 'CCC' ],
        [ 'CCC', 'CCC', 'CCC' ],
        [ 'HHH', 'H#H', 'HHH' ]
    ])
        .key('H', heatproofCasing, pyrolyseHatch)
        .key('C', cupronickelCoil, event.noHatch())
        .build(); 

   event.simpleGeneratorMultiBlock(
       "High efficiency steam turbine", // The english name
       "high_efficiency_steam_turbine", // The internal name
       high_efficiency_turbine_Shape, // The multiblock shape
       65536, // Maximum energy generation rate (eu/tick)
       builder => { // The builder (same as for a single block generator)
           builder
           .fluid("modern_industrialization:steam", 1)
           .fluid("modern_industrialization:heavy_water_steam", 1)
           .fluid("modern_industrialization:high_pressure_steam", 8)
           .fluid("modern_industrialization:high_pressure_heavy_water_steam", 8)
       },
           // --- Standard model configuration --- //
       "turbine_casing", // casing
       "steam_turbine", // model folder
       true, false, false // front overlay?, top overlay?, side overlay?
   );
});


//--------------------------------------------------------------------MI TWEAKS МУЛЬТИБЛОКИ!!!!----------------------------------------------------------------------------------------------------

MITweaksMachineEvents.registerBatchMultiblocks((event) =>
{
	// Create your multiblock shape ...
	// This is the same as how you do it for other multiblock machines in MI's KubeJS plugin
	const hatch = event.hatchOf("item_input", "item_output", "fluid_input", "fluid_output", "energy_input");
	const steelPlatedBricks = event.memberOfBlock("modern_industrialization:chemically_inert_ptfe_casing");
	const shape = event.layeredShape("inert_casing",
		[
			["HHH", "MMM", "HHH"],
			["HHH", "M M", "HHH"],
			["HHH", "M#M", "HHH"]
		])
		.key("H", steelPlatedBricks, hatch)
		.key("M", steelPlatedBricks, event.noHatch())
		.build();

	event.electric(
		// English name, internal name
		"Large Chemical Reactor", "large_chemical_reactor",
		// Recipe type, multiblock shape
		event.getRecipeType("modern_industrialization:chemical_reactor"), shape,
		// The workstations (in EMI/JEI/REI) to add this machine to
		// In some cases you may want to add it to multiple workstations (if there are bronze/steel tiers of this machine)
		(workstations) => workstations.add("modern_industrialization:chemical_reactor"),
		// Casing of the controller, overlay folder, front overlay?, top overlay?, side overlay?
		"inert_casing", "chemical_reactor", true, false, false,
		// Batch size, EU cost multiplier
		128, 0.5,
		// Should the machine use 128 EU/t as the max base EU (like normal multiblocks)? If false, it will use 32 EU/t (like single block electric machines). Optional, defaults to false
		false
	);
});



MITweaksMachineEvents.registerBatchMultiblocks((event) =>
{
	// Create your multiblock shape ...
	// This is the same as how you do it for other multiblock machines in MI's KubeJS plugin
	const hatch = event.hatchOf("item_input", "item_output", "fluid_input", "fluid_output", "energy_input");
	const steelPlatedBricks = event.memberOfBlock("modern_industrialization:enderium_casing");
	const HatchBlocks = event.memberOfBlock("modern_industrialization:enderium_casing");
	const PTFEBlocks = event.memberOfBlock("modern_industrialization:polytetrafluoroethylene_machine_casing_pipe");

	const shape = event.layeredShape("enderium_casing",
		[
			[" H H ", " HHH ", " H H "],
			["HhhhH", "HPPPH", "HhhhH"],
			[" hhh ", "HPPPH", " hhh "],
      ["HhhhH", "HPPPH", "HhhhH"],
      [" H H ", " H#H ", " H H "]
		])
		.key("H", steelPlatedBricks, event.noHatch())
		.key("h", HatchBlocks, hatch)
		.key("P", PTFEBlocks, event.noHatch())

		.build();

	event.electric(
		// English name, internal name
		"Maelstrom", "maelstrom",
		// Recipe type, multiblock shape
		event.getRecipeType("modern_industrialization:centrifuge"), shape,
		// The workstations (in EMI/JEI/REI) to add this machine to
		// In some cases you may want to add it to multiple workstations (if there are bronze/steel tiers of this machine)
		(workstations) => workstations.add("modern_industrialization:centrifuge"),
		// Casing of the controller, overlay folder, front overlay?, top overlay?, side overlay?
		"enderium_casing", "chemical_reactor", true, false, false,
		// Batch size, EU cost multiplier
		128, 0.75,
		// Should the machine use 128 EU/t as the max base EU (like normal multiblocks)? If false, it will use 32 EU/t (like single block electric machines). Optional, defaults to false
		true
	);
});


MITweaksMachineEvents.registerBatchMultiblocks((event) => {
    const hatch = event.hatchOf("item_input", "item_output", "energy_input");

    const enderiumCasing = event.memberOfBlock("modern_industrialization:enderium_casing");
    const titaniumCasing = event.memberOfBlock("modern_industrialization:solid_titanium_machine_casing");
    const titaniumPipe = event.memberOfBlock("modern_industrialization:titanium_machine_casing_pipe");
    const GrindingBlock = event.memberOfBlock("roll_mod:grinding_wheels");

    const shape = event.layeredShape("solid_titanium_machine_casing", [
        ["H   H","H T H","HTTTH","HTTTH","HHHHH"],
        ["     ","  P  "," TPT ","tGGGt","H   H"],
        ["     ","  P  "," TPT ","tGGGt","H   H"],
        ["     ","  P  "," TPT ","tGGGt","H   H"],
        ["H   H","H T H","HT#TH","HTTTH","HHHHH"]
    ])
        .key("H", enderiumCasing, event.noHatch())
        .key("T", titaniumCasing, event.noHatch())
        .key("t", titaniumCasing, hatch)
        .key("P", titaniumPipe, event.noHatch())
        .key("G", GrindingBlock, event.noHatch())

        .build();

    event.electric(
        "Maceration Tower", "maceration_tower",
        event.getRecipeType("modern_industrialization:macerator"), shape,
        (workstations) => workstations.add("modern_industrialization:bronze_macerator"),
        "solid_titanium_machine_casing", "macerator", true, false, false,
        128, 0.75,
        true
    );
});




// nbt: 5x5x5, контролер утоплений на 1 блок за передньою рамкою
MITweaksMachineEvents.registerBatchMultiblocks((event) => {
    // THERMAL_CENTREFUGE зареєстрований вище як withItemInputs().withItemOutputs()
    const hatch = event.hatchOf("item_input", "item_output", "energy_input");

    const enderiumCasing = event.memberOfBlock("modern_industrialization:enderium_casing");
    const kanthalCoil = event.memberOfBlock("modern_industrialization:kanthal_coil");

    const shape = event.layeredShape("enderium_casing", [
        [" H H ", " HHH ", "HH HH", " HHH ", " H H "],
        ["HHHHH", "HCCCH", "H H H", "HCCCH", "HHHHH"],
        [" HHH ", "HCCCH", " H H ", "HCCCH", " HHH "],
        ["HHHHH", "HCCCH", "H # H", "HCCCH", "HHHHH"],
        [" H H ", " HHH ", "HH HH", " HHH ", " H H "]
    ])
        .key("H", enderiumCasing, hatch)
        .key("C", kanthalCoil, event.noHatch())

        .build();

    event.electric(
        "Large Thermal Centrefuge", "large_thermal_centrefuge",
        event.getRecipeType("modern_industrialization:thermal_centrefuge"), shape,
        (workstations) => workstations.add("modern_industrialization:thermal_centrefuge"),
        "enderium_casing", "centrifuge", true, false, false,
        128, 0.75,
        true
    );
});


// --------------------------------------------------- LARGE ELECTROLYZER ----
// nbt: 5 шарів x 8 рядків x 7 колонок. Арка (платина + вольфрам) стоїть
// у центральному шарі, товщиною в 1 блок — так, як у структурі.
// minecraft:light з .nbt проігноровано (це не частина мультиблока).
MITweaksMachineEvents.registerBatchMultiblocks((event) => {
    const hatch = event.hatchOf("item_input", "item_output", "fluid_input", "fluid_output", "energy_input");

    const enderiumCasing = event.memberOfBlock("modern_industrialization:enderium_casing");
    const titaniumPipe = event.memberOfBlock("modern_industrialization:titanium_machine_casing_pipe");
    const platinumBlock = event.memberOfBlock("modern_industrialization:platinum_block");
    const tungstenCoil = event.memberOfBlock("modern_industrialization:tungsten_coil");

    const shape = event.layeredShape("enderium_casing", [
        [" HHHHH ", "  HHH  ", "  HHH  ", "       ", "       ", "       ", "       ", "       "],
        [" HHHHH ", " WTTTW ", " WT TW ", "       ", "       ", "       ", "       ", "       "],
        [" HHHHH ", " HPTPH ", " PTTTP ", " P   P ", " P   P ", "P     P", " W   W ", "  WWW  "],
        [" HHHHH ", " WTTTW ", " WT TW ", "       ", "       ", "       ", "       ", "       "],
        [" HHHHH ", "  H#H  ", "  HHH  ", "       ", "       ", "       ", "       ", "       "]
    ])
        .key("H", enderiumCasing, hatch)
        .key("T", titaniumPipe, event.noHatch())
        .key("P", platinumBlock, event.noHatch())
        .key("W", tungstenCoil, event.noHatch())

        .build();

    event.electric(
        "Large Electrolyzer", "large_electrolyzer",
        event.getRecipeType("modern_industrialization:electrolyzer"), shape,
        (workstations) => workstations.add("modern_industrialization:electrolyzer"),
        "enderium_casing", "electrolyzer", true, false, false,
        128, 0.75,
        true
    );
});


MITweaksMachineEvents.registerBatchMultiblocks((event) => {


    const bricks = event.memberOfBlock(mi('fire_clay_bricks'));
    const pipecasing = event.memberOfBlock(mi('stainless_steel_machine_casing_pipe'));
    const NOhatchCasing = event.memberOfBlock(mi('clean_stainless_steel_machine_casing'));

    const FluidInput = event.hatchOf('fluid_input');
    const FluidOutput = event.hatchOf('fluid_output');
    const ItemInput = event.hatchOf('item_input');
    const ItemOutput = event.hatchOf('item_output');
    const EnergyInput = event.hatchOf('energy_input');

    const ore_washerShape = event.layeredShape('clean_stainless_steel_machine_casing', [
        //y=0  \/
        [ 'BBBBBBBBB', 'CCC   CCC', 'CFC   CfC', 'CFC   CfC', 'CCC   CCC', ' C     C ', ' C     C '  ],
        [ 'BBBBBBBBB', 'CPCPPPCPC', 'IPCEEECPi', 'IPC   CPi', 'CPC   CPC', 'CPC   CPC', 'CCC   CCC'  ],
        [ 'BBBBBBBBB', 'CCCPPPCCC', 'CCC   CCC', 'CCC   CCC', 'CCC   CCC', ' C     C ', ' C     C '  ],
        [ 'BBBBBBBBB', '  CP#PC  ', '  CCCCC  ', '         ', '         ', '         ', '         '  ]
    ])
        .key('B', bricks, event.noHatch())
        .key('P', pipecasing, event.noHatch())
        .key('C', NOhatchCasing, event.noHatch())
        .key('F', NOhatchCasing, FluidInput)
        .key('I', NOhatchCasing, ItemInput)
        .key('f', NOhatchCasing, FluidOutput)
        .key('i', NOhatchCasing, ItemOutput)
        .key('E', NOhatchCasing, EnergyInput)
        .build();

    event.electric(
        'Ore Washer',
        'ore_washer',
        event.getRecipeType(mi('lv_ore_washer')), 
        ore_washerShape,
        (workstations) =>
            workstations.add(['modern_industrialization:lv_ore_washer']),

        /* MODEL CONFIGURATION */
        'clean_stainless_steel_machine_casing',
        'ore_washer',
        true,
        false,
        false,

        128, 0.75,
        true
    );
});

const CASING = 'steel';

let STEAM_ORE_WASHER;

MIMachineEvents.registerRecipeTypes(event => {
    STEAM_ORE_WASHER = event.register('steam_ore_washer')
        .withItemInputs()
        .withFluidInputs()
        .withItemOutputs();
});

MITweaksMachineEvents.registerBatchMultiblocks(event => {
    const I_Block = event.memberOfBlock('extended_industrialization:steel_plated_bricks');
    const S_Block = event.memberOfBlock('modern_industrialization:steel_machine_casing_pipe');
    const B_Block = event.memberOfBlock('modern_industrialization:steel_machine_casing');

    const hatch = event.hatchOf('item_input', 'item_output', 'fluid_input');

    const shape = event.layeredShape(CASING, [
        [
            'IIIII',
            ' iii ',
            ' iii '
        ],
        [
            'IBBBI',
            'iS Si',
            'i   i'
        ],
        [
            'IBBBI',
            'iS Si',
            'i   i'
        ],
        [
            'IBBBI',
            'iS Si',
            'i   i'
        ],
        [
            'IIIII',
            ' i#i ',
            ' iii '
        ]
    ])
        .key('I', I_Block, hatch)
        .key('i', I_Block, event.noHatch())
        .key('B', B_Block, event.noHatch())
        .key('S', S_Block, event.noHatch())
        .build();

    event.steamStandalone(
        'Steam Ore Washer', 'steam_ore_washer',
        STEAM_ORE_WASHER, shape,
        event.progressBar(84, 40, 'macerate'),
        (itemInputs) => itemInputs.addSlots(14, 35, 3, 1),
        (itemOutputs) => itemOutputs.addSlots(120, 35, 3, 1),
        (fluidInputs) => fluidInputs.addSlot(14, 53),
        (fluidOutputs) => {},
        CASING, 'steam_ore_washer', true, false, false,
        32, 1
    );
});