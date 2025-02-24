# Falling Leaves

This Fabric Minecraft mod adds a neat little particle effect to leaf blocks.  
Users can configure which types of leaf blocks will drop leaves and the frequency that these leaves are dropped at.  
  
Requires [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config).

![](https://i.imgur.com/Y6zGq33.gif)

## Troubleshooting aka "Why are no leaves falling?"

Some trees do not drop leaves by default, these are: conifer trees (like spruce and pine), large leaved trees (like jungle trees and palms) and shrubs.

You can change this as you like by adjusting "Conifer Leaf Spawn Rate" in the Mod Menu settings and adjusting the spawn rates for specific trees under "Leaf Settings".

A complete list of all conifer trees is found [here](https://github.com/RandomMcSomethin/fallingleaves/blob/1.18/src/main/java/randommcsomethin/fallingleaves/config/ConfigDefaults.java#L9-L23) and a list of all trees with adjusted spawn rates is found [here](https://github.com/RandomMcSomethin/fallingleaves/blob/1.18/src/main/java/randommcsomethin/fallingleaves/config/ConfigDefaults.java#L33-L53).

## FAQ

- Is this compatible with trees from other mods? What about Resource Packs?
  - Falling Leaves _should_ be 100% compatible with any modded trees and any resource pack changing leaves!

- Does this need to be installed on the server for multiplayer?
  - Nope! This mod is 100% client-side.

- Lol fabric suxxx gief forge plx.
  - Cheaterpaul [ported this mod to Forge](https://www.curseforge.com/minecraft/mc-mods/falling-leaves-forge "Forge port"), note that this port is inofficial and might not always be up-to-date, it also does configuration differently.

- I enjoy old things. Can you backport this to Minecraft 1.7?
  - No. We will only focus on supporting the most recently released version of Minecraft.

- I found a problem / have an idea! Do I just post a comment on your CurseForge page?
  - You **could**, but we'd prefer it if you posted an issue on [our GitHub repository's issue tracker](https://github.com/RandomMcSomethin/fallingleaves/issues).  
    It makes it far easier for us to follow up when developing new versions of this mod.

- Can I include this mod in my modpack?
  - Absolutely! Just remember to put a link to our CurseForge page somewhere in your modpack's documentation.

- What do you get if you jumble the letters in "Falling Leaves Mod"?
  - You get "Five Golden Llamas". We're still not sure if this means anything in particular...

## Custom Textures

![](https://i.imgur.com/jIup4qv.png)  
(Just a bad example of a block-specific custom texture)

It has always been possible to add or change the falling leaf textures through the use of resource packs (by overwriting the mod's particle definitions and/or textures).   
Since 1.17.0 it is now also possible use specific textures for specific blocks.  
There is an [example resource pack](CustomFallingLeavesTexturesExample.zip) that uses two custom leaf textures for Oak Leaves. It should be pretty self-explanatory, but here's instructions just in case:  

Add your textures under `assets/fallingleaves/textures/particle/` while making sure to use unique names (or even better: subfolders), e.g.:
```
assets/fallingleaves/textures/particle/example/texture_1.png
assets/fallingleaves/textures/particle/example/texture_2.png
```

Add a particle definition for the block you want, let's say the block has the id `golden_llamas:leaf_block`:

`assets/fallingleaves/particles/block/golden_llamas/leaf_block.json`:

```JSON
{
  "textures": [
    "fallingleaves:example/texture_1",
    "fallingleaves:example/texture_2"
  ]
}
```

## Leaf Spawners

When your trees look like this:  
![](https://i.imgur.com/UGiuOj4.png)  

You can make them drop leaves by adding the relevant block ids under "Additional block states that drop leaves" in the experimental settings tab.

## Thanks and Credits

All good developers learn from reading code other people have written, and it's only fair to credit those who have inspired us. For that reason, we give thanks to:

- [user11681](https://github.com/user11681/java), whose work in their `limitless` mod was very helpful when developing our own configuration screen.
- [TehNut](https://github.com/TehNut), whose work in `HWYLA` was very useful in figuring out how to get a mod's name from a block it adds.
