# bloom

a loving reimaginging of [the "bloom" app](https://generativemusic.com/bloom.html), adapted for norns and SuperCollider.

![291158687-a4e5e034-d2e8-4c8f-818e-63a1bcd03a80(1)](https://github.com/schollz/bloom/assets/6550035/80491d3f-ab93-4419-8cb1-708088e3d5d5)


this script aims to replicate “bloom” app by Brian Eno and Peter Chilvers that was released in 2008. the major functionality persists: clicking on the grid / screen will create circles and sounds that grow and disappear. multiple touches will create sequences that also eventually fade away. the sounds belong to scales with alien names like "ambrette", "benzoin", "bergamot", "labdanum", "neroli", "orris", "tolu", "vetiver", and "ylang" - all which were painstakingly captured from the original app (`PARAMS > scale`). also, like the app, the norns script has an “evolve when idle” function (`PARAMS > evolve`), and toggles for automatic generation (`PARAMS > generate`) and randomization (`PARAMS > randomize`). 

there are major differences between the "bloom" app and the "bloom" norns script. its difficult to obtain 100% the same sounds of the app so I did my best with my synthesis knowledge to create a similar sounds with SuperCollider. the sounds I have are a bell-like feedback sine sound which can be mixed with a kalimba sample (swapping between acoustic and synthetic) (`PARAMS > blend`). the drone notes are taken from the scale, as in the app, but the switching between drones is not precisely the same.

another change is the introduction of "lanes" (`PARAMS > lanes`). this essentially splits the grid into multiple lanes where each lane is a separate sequencer. by default there is one lane, but you can have one lane for each row on your grid.

## requirements

- norns (version 231114+) or supercollider
- grid optional
- crow optional
- midi optional

## documentation

to get started - just click `K3` to generate a random pattern. on the grid you can select specific notes.

- E1: change scale
- E2: change delay
- E3: change blend
- K2: clear pattern
- K3: generate pattern 

in the parameters there are also options for outputs (midi, crow), sound design parameters, and parameters for generating/randomizing/evolving as per the original app.

### supercollider

without a norns, you can also run bloom with SuperCollider + a grid. just open `ignore/runWithGrid.scd` and follow the instructions and run.


## thanks

massive thanks to @instantjuggler for supporting me in creating this and constantly inspiring!

also thanks to @tlubke and @catfact for the overhaul of the screen. this is my first script utilizing the `refresh()` which was fantastic for implementing the screen redraw without a clock.


## install

you can install through maiden:

```
;install https://github.com/schollz/bloom
```
