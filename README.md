Demo of Minecraft plugin with dance and music:

https://github.com/nukmuk/sheepy/assets/10235825/b6bb5278-4c10-4f04-b4aa-6e9159921d18

Web visualizer: https://github.com/nukmuk/sheepy-web-viewer

[![image](https://github.com/user-attachments/assets/08f5e060-dbe3-4492-b59c-ac0f5919db25)](https://sheepy-web-viewer.vercel.app/)


How the example dance video was created:

1. Export alembic from MMD using [MMDBridge](https://github.com/uimac/mmdbridge)
2. Import into Houdini, scatter points on the surface of the mesh, change FPS to 20 since Minecraft runs at 20 ticks/second
3. Use the script in my Houdini Digital Asset to export the points to .shny file
4. Play animation in-game

# My .shny file format for storing the animations

Every "frame" starts with an int16 which defines how many particles the frame has. After that comes the particles which are 10 bytes each. After the last particle in the frame the next frame starts with an int16 again. The file uses little endian byte order.

Particle format: 3\*2 bytes (position) float16, 1 byte (blue) uint8, 1 byte (green) uint8, 1 byte (red) uint8, 1 byte (pscale) uint8
total 10bytes per particle

### Simple example animation with 3 frames and 2 particles per frame:

```hex
02 00 00 3C 00 40 00 42 FF 00 FF FF 00 40 00 40 00 42 FF 00 FF FF 02 00 00 40 00 40 00 42 FF 00 FF FF 00 42 00 40 00 42 FF 00 FF FF 02 00 00 42 00 40 00 42 FF 00 FF FF 00 44 00 40 00 42 FF 00 FF FF
```

Explained:

```hex
02 00 (first frame length: 2 particles, uint16)

00 3C (x, float16)
00 40 (y, float16)
00 42 (z, float16)
FF 00 FF (color: purple, format is BGR, uint8)
FF (pscale: 255 uint8, can be interpreted as alpha channel of the color so the color would be stored as int32)

00 40 (start of the second particle)
00 40
00 42
FF 00 FF
FF


02 00 (second frame length with 2 particles too)

00 40
00 40
00 42
FF 00 FF
FF

00 42
00 40
00 42
FF 00 FF
FF


02 00 (3rd frame)

00 42
00 40
00 42
FF 00 FF
FF

00 44
00 40
00 42
FF 00 FF
FF
```

### More detailed steps on how the example was made

1. Import .abc file, set "Load As" to "Unpack Alembic Delayed Load Primitives". Set Project Animation FPS to 20.
2. Copy the alembic node as many times as the object has materials. For example, if model has 33 materials, make 33 alembic nodes.
3. For each node under "Selection" tab, for "Object Path" choose a single material. (or multiple materials if you know they are using the same textures)
4. Connect each alembic node to an attribexpression node. Set "Attribute" to "Texture (uv)", "Constant Value" to 1 1 1, "VEXpression" to "self % value". This clamps/wraps the UVs to 0-1.
5. Connect those to attribfrommap nodes and put the correct texture in the "Texture Map" field. You can find the right texture for each material by opening "alembic_file.mtl" exported from MMDBridge in Notepad.
6. Connect all of those to a merge node.
7. Connect to scatter node to create the particles. "Force Total Count" is how many particles are generated every frame/tick. In my testing 500-2000 gives ok performance with 1 player on the server. Set "Global Seed" to "$F" to randomize the particle positions every frame.
8. Set "pscale" with attribcreate node to change the size of the particles. I used about 0.015.
9. Connect to my exporter node, set file path, start and end frames correctly and export.

You can also use my example.hip file for guidance

### Inspired by https://www.youtube.com/watch?v=iwhotujrJqE
