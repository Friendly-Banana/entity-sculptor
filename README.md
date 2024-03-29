# Entity Sculptor

Ever wanted to capture the moment your dog sits down and looks at you?
Now you can. Use this mod and build a statue.
No matter where you take your dog, the blocks stay where they are.

## Installation

1. Install Fabric
2. Put [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
   and [this mod](https://www.curseforge.com/minecraft/mc-mods/entity-sculptor) in the mods folder (both server and client)
3. Optional: Install WorldEdit

## Usage

### Statue Command

`/statue <entity> [position] [scale]`

Example: dog/wolf statue above you

`/statue @e[limit=1,type=minecraft:wolf] ~ ~2 ~`

### Match Color Command

`/matchcolor r g b [a|directions | limit]`

Example: best fitting block for yellow (255, 255, 0)

`/matchcolor 255 255 0 all 1`

## Images

<table>
<img alt="an axolotl" src="images/axolotl.png" width="50%"/>
<img alt="statue of an axolotl" src="images/axolotl_statue.png" width="50%"/>
</table>

![living dog looking at you](images/cute_dog.png)
![dog statue built out of blocks](images/dog_statue.png)
![player in front of dog statue](images/player_with_dog_statue.png)

## Technical

This mod renders the entity with a custom VertexConsumer, collecting all vertices. Then we interpolate between the vertices, take the
pixel from the entity's texture and map it to a block state. All those block states are then sent to the server together with their
position.
