# bloom

a loving reimaginging of [Brian Eno's "bloom" app](https://generativemusic.com/bloom.html), adapted for norns and SuperCollider.

![Untitled](https://github.com/schollz/bloom/assets/6550035/a4e5e034-d2e8-4c8f-818e-63a1bcd03a80)

this script aims to the spirit of the original “bloom” app. the major functioning is the same - clicking on the grid / screen will create circles that grow and disappear. sequences can be created which eventually fade away. the scales are implemented the same: "ambrette", "benzoin", "bergamot", "labdanum", "neroli", "orris", "tolu", "vetiver", and "ylang" are all implemented (`PARAMS > scale`). There is also a “evolve when idle” function (`PARAMS > evolve`), similar to the app and toggles for automatic generation (`PARAMS > generate`) and randomization (`PARAMS > randomize`). 

what has changed is the sound design - its difficult to obtain 100% the same sounds of the app so I did my best with my synthesis knowledge to create a similar sound. the sounds I have are a bell-like feedback sine sound which can be mixed with a kalimba sample (swapping between acoustic and synthetic) (`PARAMS > blend`). the drone notes are taken from the scale, as in the app, but the switching between drones is not precisely the same.

another change is the introduction of "lanes" (`PARAMS > lanes`). this essentially splits the grid into multiple lanes where each lane is a separate sequencer.

## requirements

- norns or supercollider
- grid optional

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

## Install

you can install through maiden:

```
;install https://github.com/schollz/bloom
```
