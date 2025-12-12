### 2.0.3

port to 1.21.11

### 2.0.2

port to 1.21.9

### 2.0.1

add Argentine Spanish translation (es_ar), thanks to Texaliuz!

### 2.0.0

Now that Vanilla has their own implementation of falling leaves, we encourage everyone to try playing without this mod and see how you like it!

If you feel the need to be able to change spawn rates, dislike evergreens and conifer trees dropping regular leaves, or want any of the other features like wind back:  
Welcome back!

Note that this is a large rework of the mod, so take some time, maybe some tea, and give the changes a read.

### Changes

- There is a new general config option "Always Use 'Vanilla' Particle Implementation" (defaults to off)

This is a way to have a mostly vanilla-like experience while still being able to adjust spawn rates, use most features like on-hit and decay leaves - but having no control over the actual particles, like their size, lifetime or wind.

- Another new option "Use Vanilla Textures" (defaults to off)

Yet another way to get closer to vanilla, by letting this mod's particles, specifically the "Regular" kind (we'll get to that), use vanilla textures.  
This has none of the drawbacks of the above option. 


- Add Leaf Block setting "Particle Implementation"

This can be "Vanilla", "Regular" or "Conifer" - the latter two being the ones the mod always used.  
"Vanilla" means leaf particles are spawned via a vanilla method, which mods may use to add custom particles to their leaves as well.  
Falling Leaves tries to detect whether a mod has a custom implementation and will make use of it by default.  

For vanilla, "tinted" particles - from most trees like Oak, Birch, Spruce etc. - will by default be replaced with either the "Regular" or "Conifer" kind.  
Untinted particles - Azelia, Pale Oak and Cherry - will currently use "Vanilla" by default.

- Add "Cherry" particle implementation

We lied, there's actually another one!  
"Cherry" is very close to the vanilla implementation. It uses the vanilla textures, has similar physics, except it is affected by wind and can be applied to any block your want.  
This implementation is currently unused by default. Do give it a try!

- Leaves use the vanilla spawn chances by default

Most leaves will now spawn slightly less (1.33% -> 1%), while modded cherries will likely spawn a ton more (~1.8% -> 10%).
  
Since mods can now define their own spawn chances, nearly all the old config defaults like for autumn and cherry trees have been reset, to be revised at a later time if needed. The main exception are shrubs and big leaved trees (notably jungle trees), which still won't spawn leaves by default.  
Conifer trees also won't spawn leaves by default, as before.

- The config has a new home

Due to the fundamental changes in spawn rates and other adjustments, we couldn't keep the old config format.  
Falling Leaves will thus automatically migrate from `fallingleaves.json` to `fallingleaves2.json`, essentially copying everything over *except* for spawn rates.  
The old config will be left intact, so if you want to look at it and e.g. fish out your old spawn rate settings, nothing will be lost!

---

And finally, as there were so many large changes, be aware that issues might be waiting to pounce on you from the trees, leaves may explode, Llama's might be golden.  
Please report any issue you can find!

 \- Fourmisain
