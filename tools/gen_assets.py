#!/usr/bin/env python3
"""
CraftWorld 3D asset generator.

Generates ALL game art and audio procedurally so every asset is original and
bundled locally (100% offline):

  app/src/main/assets/textures/atlas.png   256-tile atlas of 16x16 pixel art
  app/src/main/assets/textures/tiles.json  tile name -> atlas index
  app/src/main/assets/sounds/*.wav         synthesized sound effects + music
  app/src/main/res/mipmap-*/ic_launcher.png launcher icons

Deterministic: fixed RNG seeds -> stable output.
Run from the repo root:  python3 tools/gen_assets.py
"""
import json
import math
import os
import random
import struct
import wave

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app/src/main/assets")
TEX_DIR = os.path.join(ASSETS, "textures")
SND_DIR = os.path.join(ASSETS, "sounds")
RES_DIR = os.path.join(ROOT, "app/src/main/res")

T = 16          # tile size in px
GRID = 16       # 16x16 tiles -> 256 tiles, 256x256 px atlas

rng = random.Random(20240001)


# ---------------------------------------------------------------- helpers

def clamp(v, a, b):
    return max(a, min(b, v))


def shade(color, factor):
    return tuple(clamp(int(c * factor), 0, 255) for c in color[:3]) + ((color[3],) if len(color) == 4 else ())


def speckle(img, base, variation=0.12, seed=1, alpha=255):
    """Fill a 16x16 tile with per-pixel brightness-varied base color."""
    r = random.Random(seed)
    px = img.load()
    for y in range(T):
        for x in range(T):
            f = 1.0 + r.uniform(-variation, variation)
            c = shade(base, f)
            px[x, y] = (c[0], c[1], c[2], alpha)
    return img


def new_tile():
    return Image.new("RGBA", (T, T), (0, 0, 0, 0))


def hline(img, y, color, x0=0, x1=T - 1):
    d = ImageDraw.Draw(img)
    d.line([(x0, y), (x1, y)], fill=color)


def vline(img, x, color, y0=0, y1=T - 1):
    d = ImageDraw.Draw(img)
    d.line([(x, y0), (x, y1)], fill=color)


def blob(img, color, count, seed, size=2):
    r = random.Random(seed)
    px = img.load()
    for _ in range(count):
        cx, cy = r.randint(1, T - 2), r.randint(1, T - 2)
        for dy in range(-size // 2, size // 2 + 1):
            for dx in range(-size // 2, size // 2 + 1):
                if r.random() < 0.75:
                    x, y = clamp(cx + dx, 0, T - 1), clamp(cy + dy, 0, T - 1)
                    px[x, y] = color if len(color) == 4 else color + (255,)
    return img


# Material palettes
STONE = (127, 127, 127)
DIRT_C = (134, 96, 67)
GRASS_C = (95, 159, 53)
SAND_C = (219, 207, 163)
WOODC = (156, 127, 78)
DARKWOOD = (104, 83, 50)

ORE_COLORS = {
    "coal": (40, 40, 40), "iron": (216, 175, 147), "gold": (252, 222, 112),
    "copper": (205, 116, 73), "redstone": (255, 40, 40), "diamond": (93, 236, 245),
    "emerald": (23, 221, 98), "aluminum": (200, 206, 212), "crystal": (190, 120, 255),
}

TOOL_COLORS = {
    "wooden": (156, 127, 78), "stone": (130, 130, 130), "iron": (216, 216, 216),
    "gold": (252, 222, 112), "diamond": (93, 236, 245),
}


# ---------------------------------------------------------------- tiles

def tile_simple(base, var=0.12, seed=1):
    return speckle(new_tile(), base, var, seed)


def tile_grass_top():
    return tile_simple(GRASS_C, 0.14, 2)


def tile_grass_side():
    img = tile_simple(DIRT_C, 0.12, 3)
    r = random.Random(4)
    px = img.load()
    for x in range(T):
        depth = 3 + (1 if r.random() < 0.5 else 0)
        for y in range(depth):
            px[x, y] = shade(GRASS_C, 1 + r.uniform(-0.12, 0.12)) + (255,)
    return img


def tile_log(bark, seed):
    img = tile_simple(bark, 0.08, seed)
    r = random.Random(seed + 1)
    for x in range(0, T, 3):
        c = shade(bark, 0.72)
        vline(img, clamp(x + r.randint(0, 1), 0, 15), c + (255,))
    return img


def tile_log_top():
    img = tile_simple(WOODC, 0.06, 7)
    d = ImageDraw.Draw(img)
    for radius in (6, 4, 2):
        d.ellipse([7 - radius, 7 - radius, 8 + radius, 8 + radius], outline=shade(DARKWOOD, 1.0) + (255,))
    return img


def tile_planks(base, seed):
    img = tile_simple(base, 0.07, seed)
    dark = shade(base, 0.6) + (255,)
    for y in (3, 7, 11, 15):
        hline(img, y, dark)
    vline(img, 7, dark, 0, 3)
    vline(img, 3, dark, 4, 7)
    vline(img, 11, dark, 8, 11)
    vline(img, 5, dark, 12, 15)
    return img


def tile_leaves(base, seed):
    img = new_tile()
    r = random.Random(seed)
    px = img.load()
    for y in range(T):
        for x in range(T):
            if r.random() < 0.88:
                px[x, y] = shade(base, 1 + r.uniform(-0.25, 0.18)) + (255,)
    return img


def tile_ore(ore):
    img = tile_simple(STONE, 0.10, hash(ore) & 0xFFFF)
    blob(img, ORE_COLORS[ore] + (255,), 5, hash(ore) >> 3, 2)
    return img


def tile_metal_block(color, seed):
    img = tile_simple(color, 0.05, seed)
    light = shade(color, 1.25) + (255,)
    dark = shade(color, 0.7) + (255,)
    hline(img, 0, light); vline(img, 0, light)
    hline(img, 15, dark); vline(img, 15, dark)
    return img


def tile_bricks(base, mortar, seed):
    img = tile_simple(base, 0.08, seed)
    m = mortar + (255,)
    for y in (0, 4, 8, 12):
        hline(img, y, m)
    for row, y0 in enumerate((1, 5, 9, 13)):
        off = 0 if row % 2 == 0 else 4
        for x in (off, off + 8):
            vline(img, x % 16, m, y0, y0 + 2)
    return img


def tile_glass():
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(200, 230, 240, 255))
    d.line([(3, 5), (5, 3)], fill=(230, 245, 250, 200))
    d.line([(3, 9), (9, 3)], fill=(230, 245, 250, 140))
    px = img.load()
    for y in range(1, 15):
        for x in range(1, 15):
            if px[x, y][3] == 0:
                px[x, y] = (220, 240, 250, 40)
    return img


def tile_water():
    img = new_tile()
    r = random.Random(11)
    px = img.load()
    for y in range(T):
        for x in range(T):
            f = 1 + 0.15 * math.sin((x + y * 2) * 0.8) + r.uniform(-0.05, 0.05)
            c = shade((47, 92, 203), f)
            px[x, y] = (c[0], c[1], c[2], 170)
    return img


def tile_lava():
    img = new_tile()
    r = random.Random(12)
    px = img.load()
    for y in range(T):
        for x in range(T):
            f = 0.8 + 0.5 * abs(math.sin((x * 1.3 - y) * 0.6)) + r.uniform(-0.1, 0.1)
            px[x, y] = (clamp(int(207 * f), 120, 255), clamp(int(90 * f), 30, 220), 20, 255)
    return img


def tile_cross_plant(color, seed, stalks=5):
    img = new_tile()
    r = random.Random(seed)
    px = img.load()
    for i in range(stalks):
        x = 2 + r.randint(0, 11)
        h = r.randint(6, 12)
        for y in range(15, 15 - h, -1):
            xx = clamp(x + r.randint(-1, 1) if y < 10 else x, 0, 15)
            px[xx, y] = shade(color, 1 + r.uniform(-0.2, 0.2)) + (255,)
    return img


def tile_flower(petal):
    img = tile_cross_plant((70, 130, 44), 21, 2)
    d = ImageDraw.Draw(img)
    d.rectangle([6, 3, 9, 6], fill=petal + (255,))
    d.rectangle([7, 4, 8, 5], fill=(250, 240, 90, 255))
    return img


def tile_torch(head):
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.rectangle([7, 6, 8, 15], fill=DARKWOOD + (255,))
    d.rectangle([6, 3, 9, 6], fill=head + (255,))
    d.rectangle([7, 2, 8, 4], fill=(255, 240, 160, 255))
    return img


def tile_door():
    img = tile_planks(WOODC, 30)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=shade(DARKWOOD, 0.8) + (255,))
    d.rectangle([3, 2, 12, 6], outline=DARKWOOD + (255,))
    d.rectangle([3, 9, 12, 13], outline=DARKWOOD + (255,))
    d.rectangle([12, 7, 13, 8], fill=(60, 60, 60, 255))
    return img


def tile_ladder():
    img = new_tile()
    d = ImageDraw.Draw(img)
    for x in (2, 12):
        d.rectangle([x, 0, x + 1, 15], fill=WOODC + (255,))
    for y in (2, 7, 12):
        d.rectangle([2, y, 13, y + 1], fill=shade(WOODC, 1.15) + (255,))
    return img


def tile_rail(powered):
    img = new_tile()
    d = ImageDraw.Draw(img)
    tie = shade(DARKWOOD, 1.0) + (255,)
    for y in (2, 6, 10, 14):
        d.rectangle([1, y, 14, y], fill=tie)
    rail_c = (200, 60, 30, 255) if powered else (160, 160, 170, 255)
    for x in (3, 12):
        d.rectangle([x, 0, x, 15], fill=rail_c)
    if powered:
        d.rectangle([6, 7, 9, 8], fill=(255, 210, 70, 255))
    return img


def tile_wire():
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 7, 13, 8], fill=(200, 30, 30, 255))
    d.rectangle([7, 2, 8, 13], fill=(160, 20, 20, 255))
    return img


def tile_lever(on):
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.rectangle([5, 11, 10, 14], fill=STONE + (255,))
    if on:
        d.line([(8, 11), (12, 4)], fill=DARKWOOD + (255,), width=2)
        d.rectangle([11, 2, 13, 4], fill=(220, 60, 60, 255))
    else:
        d.line([(8, 11), (4, 4)], fill=DARKWOOD + (255,), width=2)
        d.rectangle([3, 2, 5, 4], fill=(120, 120, 120, 255))
    return img


def tile_crafting_top():
    img = tile_planks(WOODC, 31)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], outline=(80, 60, 35, 255))
    d.line([(8, 2), (8, 13)], fill=(80, 60, 35, 255))
    d.line([(2, 8), (13, 8)], fill=(80, 60, 35, 255))
    return img


def tile_furnace_front(lit):
    img = tile_simple(STONE, 0.08, 33)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 8, 12, 14], fill=(35, 35, 35, 255))
    if lit:
        d.rectangle([4, 10, 11, 13], fill=(255, 140, 20, 255))
        d.rectangle([6, 9, 9, 10], fill=(255, 220, 90, 255))
    return img


def tile_chest_front():
    img = tile_simple((170, 120, 56), 0.08, 34)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(90, 60, 25, 255))
    d.line([(0, 6), (15, 6)], fill=(90, 60, 25, 255))
    d.rectangle([7, 5, 8, 8], fill=(60, 60, 60, 255))
    return img


def tile_piston_top():
    img = tile_planks(WOODC, 36)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(90, 90, 90, 255))
    return img


def tile_runway_top():
    img = tile_simple((70, 70, 75), 0.06, 38)
    d = ImageDraw.Draw(img)
    d.rectangle([6, 2, 9, 13], fill=(240, 230, 90, 255))
    return img


def tile_portal():
    img = new_tile()
    r = random.Random(40)
    px = img.load()
    for y in range(T):
        for x in range(T):
            f = 0.7 + 0.5 * abs(math.sin((x + y) * 0.7)) + r.uniform(-0.1, 0.1)
            px[x, y] = (clamp(int(140 * f), 60, 220), 30, clamp(int(220 * f), 120, 255), 200)
    return img


def tile_glowstone():
    img = tile_simple((225, 180, 90), 0.15, 41)
    blob(img, (255, 235, 160, 255), 6, 42, 2)
    return img


def tile_farmland():
    img = tile_simple(shade(DIRT_C, 0.8), 0.1, 43)
    for x in (2, 6, 10, 14):
        vline(img, x, shade(DIRT_C, 0.55) + (255,))
    return img


def tile_tnt_side():
    img = tile_simple((190, 60, 50), 0.08, 44)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 6, 15, 9], fill=(230, 230, 230, 255))
    d.text((4, 5), "T", fill=(30, 30, 30, 255))
    return img


def tile_bookshelf():
    img = tile_planks(WOODC, 45)
    d = ImageDraw.Draw(img)
    r = random.Random(46)
    for shelf_y in (2, 9):
        x = 2
        while x < 13:
            w = r.randint(1, 2)
            color = r.choice([(170, 50, 50), (50, 90, 170), (60, 140, 70), (200, 170, 60)])
            d.rectangle([x, shelf_y, x + w - 1, shelf_y + 4], fill=color + (255,))
            x += w + 1
    return img


def tile_cactus_side():
    img = tile_simple((58, 128, 40), 0.1, 47)
    for x in (1, 14):
        vline(img, x, (40, 90, 28, 255))
    r = random.Random(48)
    px = img.load()
    for _ in range(8):
        px[r.randint(2, 13), r.randint(1, 14)] = (220, 230, 180, 255)
    return img


def tile_mob(name):
    """Simple mob skin tile: body color + face pattern."""
    colors = {
        "pig": (238, 160, 160), "cow": (120, 85, 60), "sheep": (228, 228, 228),
        "chicken": (240, 238, 225), "zombie": (90, 140, 80), "skeleton": (210, 210, 200),
        "creeper": (80, 180, 80), "spider": (50, 42, 38), "villager": (190, 140, 100),
        "fiend": (120, 40, 40),
    }
    base = colors[name]
    img = tile_simple(base, 0.10, hash(name) & 0xFFF)
    d = ImageDraw.Draw(img)
    eye = (20, 20, 20, 255)
    if name == "creeper":
        d.rectangle([3, 4, 6, 7], fill=eye)
        d.rectangle([9, 4, 12, 7], fill=eye)
        d.rectangle([6, 8, 9, 12], fill=eye)
    elif name == "spider":
        for x in (3, 6, 9, 12):
            d.rectangle([x, 5, x, 6], fill=(200, 30, 30, 255))
    else:
        d.rectangle([4, 5, 5, 7], fill=eye)
        d.rectangle([10, 5, 11, 7], fill=eye)
        if name == "pig":
            d.rectangle([6, 9, 9, 11], fill=(250, 120, 120, 255))
        if name == "zombie" or name == "skeleton":
            d.rectangle([6, 10, 9, 11], fill=(30, 30, 30, 255))
        if name == "villager":
            d.rectangle([7, 8, 8, 12], fill=(160, 110, 80, 255))
    return img


def tile_vehicle(color):
    img = tile_metal_block(color, hash(color) & 0xFFF)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 3, 13, 7], fill=(150, 210, 240, 255))
    return img


def tile_sun():
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=(255, 235, 120, 255))
    d.rectangle([4, 4, 11, 11], fill=(255, 250, 200, 255))
    return img


def tile_moon():
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=(226, 229, 240, 255))
    d.rectangle([4, 4, 7, 7], fill=(190, 195, 215, 255))
    d.rectangle([9, 8, 12, 11], fill=(200, 205, 222, 255))
    return img


# ---------------------------------------------------------------- items

def item_tool(kind, material):
    img = new_tile()
    d = ImageDraw.Draw(img)
    mat = TOOL_COLORS[material] + (255,)
    stick = DARKWOOD + (255,)
    d.line([(4, 11), (11, 4)], fill=stick, width=2)   # handle
    if kind == "pickaxe":
        d.arc([2, 1, 14, 13], 200, 340, fill=mat, width=2)
        d.line([(3, 4), (12, 2)], fill=mat, width=2)
        d.line([(12, 2), (14, 8)], fill=mat, width=2)
    elif kind == "axe":
        d.rectangle([8, 1, 13, 6], fill=mat)
        d.rectangle([7, 2, 9, 7], fill=mat)
    elif kind == "shovel":
        d.rectangle([10, 1, 13, 5], fill=mat)
        d.rectangle([11, 0, 12, 2], fill=mat)
    elif kind == "sword":
        d.line([(5, 10), (13, 2)], fill=mat, width=3)
        d.line([(4, 9), (7, 12)], fill=(120, 90, 50, 255), width=2)
    elif kind == "hoe":
        d.rectangle([8, 1, 13, 3], fill=mat)
        d.rectangle([8, 2, 9, 5], fill=mat)
    return img


def item_ingot(color):
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.polygon([(2, 10), (6, 6), (13, 6), (9, 10)], fill=color + (255,))
    d.polygon([(2, 10), (9, 10), (9, 13), (2, 13)], fill=shade(color, 0.8) + (255,))
    d.polygon([(9, 10), (13, 6), (13, 9), (9, 13)], fill=shade(color, 0.6) + (255,))
    return img


def item_gem(color):
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.polygon([(8, 2), (13, 7), (8, 14), (3, 7)], fill=color + (255,))
    d.polygon([(8, 2), (10, 7), (8, 10), (6, 7)], fill=shade(color, 1.35) + (255,))
    return img


def item_food(color, bone=False):
    img = new_tile()
    d = ImageDraw.Draw(img)
    d.ellipse([3, 4, 12, 12], fill=color + (255,))
    if bone:
        d.rectangle([11, 2, 13, 7], fill=(235, 230, 215, 255))
    return img


def item_simple(draw_fn):
    img = new_tile()
    draw_fn(ImageDraw.Draw(img))
    return img


def build_item_tiles():
    tiles = {}
    tiles["item_stick"] = item_simple(lambda d: d.line([(4, 12), (12, 4)], fill=DARKWOOD + (255,), width=2))
    tiles["item_coal"] = item_simple(lambda d: d.ellipse([4, 4, 11, 11], fill=(45, 45, 45, 255)))
    tiles["item_iron_ingot"] = item_ingot((216, 216, 216))
    tiles["item_gold_ingot"] = item_ingot((252, 222, 112))
    tiles["item_copper_ingot"] = item_ingot((205, 116, 73))
    tiles["item_aluminum_ingot"] = item_ingot((200, 206, 212))
    tiles["item_redstone"] = item_simple(lambda d: [
        d.ellipse([5, 5, 10, 10], fill=(220, 40, 40, 255)),
        d.point([(3, 8), (12, 6), (8, 12)], fill=(220, 40, 40, 255))])
    tiles["item_diamond"] = item_gem((93, 236, 245))
    tiles["item_emerald"] = item_gem((23, 221, 98))
    tiles["item_nether_crystal"] = item_gem((190, 120, 255))
    tiles["item_rubber"] = item_simple(lambda d: d.ellipse([3, 3, 12, 12], outline=(40, 40, 40, 255), width=3))
    tiles["item_wheat"] = tile_cross_plant((210, 180, 80), 50, 4)
    tiles["item_seeds"] = item_simple(lambda d: d.point(
        [(4, 6), (7, 4), (10, 7), (6, 10), (11, 11), (8, 8), (5, 12)], fill=(90, 160, 60, 255)))
    tiles["item_bread"] = item_simple(lambda d: [
        d.ellipse([2, 5, 13, 11], fill=(190, 140, 70, 255)),
        d.line([(5, 6), (7, 8)], fill=(150, 105, 50, 255)),
        d.line([(8, 6), (10, 8)], fill=(150, 105, 50, 255))])
    tiles["item_porkchop_raw"] = item_food((240, 150, 160))
    tiles["item_porkchop_cooked"] = item_food((190, 120, 80))
    tiles["item_beef_raw"] = item_food((200, 60, 60))
    tiles["item_beef_cooked"] = item_food((140, 80, 50))
    tiles["item_mutton_raw"] = item_food((225, 110, 110), True)
    tiles["item_mutton_cooked"] = item_food((165, 100, 60), True)
    tiles["item_chicken_raw"] = item_food((235, 200, 190), True)
    tiles["item_chicken_cooked"] = item_food((200, 140, 70), True)
    tiles["item_feather"] = item_simple(lambda d: [
        d.polygon([(4, 12), (8, 3), (11, 6), (6, 13)], fill=(240, 240, 240, 255)),
        d.line([(4, 12), (10, 5)], fill=(180, 180, 180, 255))])
    tiles["item_leather"] = item_simple(lambda d: d.polygon(
        [(3, 4), (12, 3), (13, 11), (5, 13)], fill=(170, 110, 60, 255)))
    tiles["item_string"] = item_simple(lambda d: d.arc([2, 2, 13, 13], 0, 300, fill=(235, 235, 235, 255), width=2))
    tiles["item_bone"] = item_simple(lambda d: [
        d.line([(4, 11), (11, 4)], fill=(235, 230, 215, 255), width=2),
        d.ellipse([2, 9, 6, 13], fill=(235, 230, 215, 255)),
        d.ellipse([9, 2, 13, 6], fill=(235, 230, 215, 255))])
    tiles["item_arrow"] = item_simple(lambda d: [
        d.line([(3, 12), (11, 4)], fill=DARKWOOD + (255,), width=1),
        d.polygon([(10, 2), (13, 5), (10, 5)], fill=(160, 160, 170, 255)),
        d.line([(3, 12), (5, 12)], fill=(240, 240, 240, 255)),
        d.line([(3, 12), (3, 10)], fill=(240, 240, 240, 255))])
    tiles["item_gunpowder"] = item_simple(lambda d: d.point(
        [(x, y) for x in range(4, 12) for y in range(4, 12) if (x * 7 + y * 3) % 4 == 0], fill=(70, 70, 70, 255)))
    tiles["item_rotten_flesh"] = item_food((130, 160, 90))
    tiles["item_engine"] = item_simple(lambda d: [
        d.rectangle([3, 5, 12, 12], fill=(120, 120, 128, 255)),
        d.rectangle([5, 3, 7, 5], fill=(90, 90, 95, 255)),
        d.rectangle([9, 3, 11, 5], fill=(90, 90, 95, 255)),
        d.rectangle([5, 7, 10, 10], fill=(200, 60, 40, 255))])
    tiles["item_wheel"] = item_simple(lambda d: [
        d.ellipse([2, 2, 13, 13], fill=(35, 35, 35, 255)),
        d.ellipse([6, 6, 9, 9], fill=(180, 180, 190, 255))])
    tiles["item_parachute"] = item_simple(lambda d: [
        d.arc([2, 2, 13, 12], 180, 360, fill=(220, 80, 60, 255), width=3),
        d.line([(3, 7), (8, 14)], fill=(200, 200, 200, 255)),
        d.line([(12, 7), (8, 14)], fill=(200, 200, 200, 255))])
    tiles["item_boat"] = item_simple(lambda d: [
        d.polygon([(2, 8), (13, 8), (11, 12), (4, 12)], fill=WOODC + (255,)),
        d.line([(2, 8), (13, 8)], fill=DARKWOOD + (255,))])
    tiles["item_minecart"] = item_simple(lambda d: [
        d.polygon([(2, 5), (13, 5), (12, 11), (3, 11)], fill=(120, 120, 128, 255)),
        d.ellipse([3, 11, 6, 14], fill=(40, 40, 40, 255)),
        d.ellipse([9, 11, 12, 14], fill=(40, 40, 40, 255))])

    def car(body):
        return item_simple(lambda d: [
            d.rectangle([1, 6, 14, 11], fill=body + (255,)),
            d.rectangle([4, 3, 11, 6], fill=shade(body, 1.2) + (255,)),
            d.rectangle([5, 4, 10, 6], fill=(150, 210, 240, 255)),
            d.ellipse([2, 10, 5, 13], fill=(30, 30, 30, 255)),
            d.ellipse([10, 10, 13, 13], fill=(30, 30, 30, 255))])

    tiles["item_car_sedan"] = car((200, 60, 50))
    tiles["item_car_suv"] = car((60, 110, 180))
    tiles["item_car_truck"] = car((120, 120, 128))
    tiles["item_car_sports"] = car((240, 180, 40))
    tiles["item_car_jeep"] = car((90, 130, 70))

    def plane(body):
        return item_simple(lambda d: [
            d.rectangle([2, 7, 13, 9], fill=body + (255,)),
            d.polygon([(6, 3), (9, 3), (8, 7), (7, 7)], fill=shade(body, 0.85) + (255,)),
            d.polygon([(6, 13), (9, 13), (8, 9), (7, 9)], fill=shade(body, 0.85) + (255,)),
            d.rectangle([12, 6, 13, 7], fill=shade(body, 1.2) + (255,))])

    tiles["item_plane_small"] = plane((230, 230, 235))
    tiles["item_plane_jet"] = plane((140, 150, 160))
    tiles["item_biplane"] = plane((190, 60, 50))
    tiles["item_helicopter"] = item_simple(lambda d: [
        d.rectangle([1, 4, 14, 4], fill=(90, 90, 95, 255)),
        d.ellipse([4, 6, 12, 12], fill=(60, 110, 180, 255)),
        d.rectangle([2, 8, 5, 9], fill=(60, 110, 180, 255)),
        d.rectangle([5, 12, 11, 13], fill=(60, 60, 65, 255))])

    for material in TOOL_COLORS:
        for kind in ("pickaxe", "axe", "shovel", "sword", "hoe"):
            tiles[f"item_{material}_{kind}"] = item_tool(kind, material)
    return tiles


# ---------------------------------------------------------------- atlas

def build_tiles():
    tiles = {}
    tiles["air"] = new_tile()
    tiles["grass_top"] = tile_grass_top()
    tiles["grass_side"] = tile_grass_side()
    tiles["dirt"] = tile_simple(DIRT_C, 0.12, 3)
    tiles["stone"] = tile_simple(STONE, 0.10, 5)
    cobble = tile_simple(STONE, 0.18, 6)
    d = ImageDraw.Draw(cobble)
    for (x0, y0, x1, y1) in [(0, 0, 5, 5), (6, 0, 11, 4), (12, 0, 15, 6), (0, 6, 4, 11),
                             (5, 5, 10, 10), (11, 5, 15, 11), (0, 12, 6, 15), (7, 11, 12, 15), (13, 12, 15, 15)]:
        d.rectangle([x0, y0, x1, y1], outline=shade(STONE, 0.6) + (255,))
    tiles["cobblestone"] = cobble
    tiles["sand"] = tile_simple(SAND_C, 0.08, 8)
    tiles["gravel"] = tile_simple((136, 126, 126), 0.22, 9)
    tiles["clay"] = tile_simple((159, 164, 177), 0.08, 10)
    tiles["snow"] = tile_simple((240, 246, 246), 0.04, 11)
    ice = tile_simple((145, 183, 253), 0.06, 12)
    ice.putalpha(Image.new("L", (T, T), 210))
    tiles["ice"] = ice
    tiles["water"] = tile_water()
    tiles["lava"] = tile_lava()
    tiles["bedrock"] = tile_simple((60, 60, 60), 0.35, 13)
    tiles["log_top"] = tile_log_top()
    tiles["oak_log"] = tile_log((103, 82, 49), 14)
    tiles["birch_log"] = tile_log((216, 215, 210), 15)
    tiles["spruce_log"] = tile_log((58, 37, 16), 16)
    tiles["rubber_log"] = tile_log((140, 110, 80), 17)
    d = ImageDraw.Draw(tiles["rubber_log"])
    d.line([(4, 2), (4, 13)], fill=(230, 225, 210, 255))  # sap line
    tiles["oak_planks"] = tile_planks(WOODC, 18)
    tiles["birch_planks"] = tile_planks((196, 179, 123), 19)
    tiles["spruce_planks"] = tile_planks((114, 84, 48), 20)
    tiles["oak_leaves"] = tile_leaves((60, 143, 35), 21)
    tiles["birch_leaves"] = tile_leaves((115, 163, 83), 22)
    tiles["spruce_leaves"] = tile_leaves((44, 96, 56), 23)
    tiles["rubber_leaves"] = tile_leaves((52, 130, 90), 24)
    tiles["glass"] = tile_glass()
    tiles["bricks"] = tile_bricks((150, 85, 70), (200, 190, 180), 25)
    tiles["stone_bricks"] = tile_bricks(STONE, shade(STONE, 0.6), 26)
    mossy = tile_bricks(STONE, shade(STONE, 0.6), 27)
    blob(mossy, (90, 130, 60, 255), 6, 28, 2)
    tiles["mossy_stone_bricks"] = mossy
    sandstone = tile_simple(SAND_C, 0.05, 29)
    hline(sandstone, 4, shade(SAND_C, 0.8) + (255,))
    hline(sandstone, 11, shade(SAND_C, 0.8) + (255,))
    tiles["sandstone"] = sandstone
    obsidian = tile_simple((28, 22, 40), 0.3, 30)
    blob(obsidian, (80, 60, 130, 255), 3, 31, 1)
    tiles["obsidian"] = obsidian

    for ore in ("coal", "iron", "gold", "copper", "redstone", "diamond", "emerald", "aluminum"):
        tiles[f"{ore}_ore"] = tile_ore(ore)
    nco = tile_simple((70, 35, 35), 0.15, 32)
    blob(nco, ORE_COLORS["crystal"] + (255,), 5, 33, 2)
    tiles["nether_crystal_ore"] = nco

    tiles["coal_block"] = tile_metal_block((45, 45, 45), 50)
    tiles["iron_block"] = tile_metal_block((216, 216, 216), 51)
    tiles["gold_block"] = tile_metal_block((252, 222, 112), 52)
    tiles["copper_block"] = tile_metal_block((205, 116, 73), 53)
    tiles["redstone_block"] = tile_metal_block((200, 40, 40), 54)
    tiles["diamond_block"] = tile_metal_block((93, 236, 245), 55)
    tiles["emerald_block"] = tile_metal_block((23, 221, 98), 56)
    tiles["aluminum_block"] = tile_metal_block((200, 206, 212), 57)
    tiles["rubber_block"] = tile_metal_block((50, 50, 55), 58)

    tiles["crafting_table_top"] = tile_crafting_top()
    ct_side = tile_planks(WOODC, 59)
    d = ImageDraw.Draw(ct_side)
    d.rectangle([2, 2, 7, 7], fill=(80, 60, 35, 255))
    tiles["crafting_table_side"] = ct_side
    tiles["furnace_top"] = tile_simple(shade(STONE, 0.9), 0.08, 60)
    tiles["furnace_front"] = tile_furnace_front(False)
    tiles["furnace_front_lit"] = tile_furnace_front(True)
    tiles["chest_front"] = tile_chest_front()
    chest_top = tile_simple((170, 120, 56), 0.08, 61)
    ImageDraw.Draw(chest_top).rectangle([0, 0, 15, 15], outline=(90, 60, 25, 255))
    tiles["chest_top"] = chest_top
    tiles["door"] = tile_door()
    tiles["ladder"] = tile_ladder()
    tiles["torch"] = tile_torch((255, 180, 60))
    tiles["redstone_torch"] = tile_torch((230, 50, 50))
    tiles["redstone_wire"] = tile_wire()
    tiles["lever"] = tile_lever(False)
    tiles["lever_on"] = tile_lever(True)
    button = new_tile()
    ImageDraw.Draw(button).rectangle([5, 6, 10, 9], fill=STONE + (255,))
    tiles["button"] = button
    plate = new_tile()
    ImageDraw.Draw(plate).rectangle([2, 6, 13, 9], fill=WOODC + (255,))
    tiles["pressure_plate"] = plate
    lamp = tile_simple((120, 90, 60), 0.1, 62)
    blob(lamp, (190, 150, 90, 255), 4, 63, 2)
    tiles["redstone_lamp"] = lamp
    lamp_lit = tile_simple((235, 190, 110), 0.1, 64)
    blob(lamp_lit, (255, 230, 150, 255), 5, 65, 2)
    tiles["redstone_lamp_lit"] = lamp_lit
    repeater = tile_simple(shade(STONE, 1.1), 0.05, 66)
    d = ImageDraw.Draw(repeater)
    d.rectangle([7, 3, 8, 5], fill=(230, 50, 50, 255))
    d.rectangle([7, 10, 8, 12], fill=(230, 50, 50, 255))
    tiles["repeater"] = repeater
    tiles["piston_top"] = tile_piston_top()
    piston_side = tile_simple(STONE, 0.1, 67)
    d = ImageDraw.Draw(piston_side)
    d.rectangle([0, 0, 15, 3], fill=WOODC + (255,))
    tiles["piston_side"] = piston_side
    tiles["rail"] = tile_rail(False)
    tiles["powered_rail"] = tile_rail(True)
    tiles["runway_top"] = tile_runway_top()
    tiles["runway_side"] = tile_simple((80, 80, 85), 0.06, 68)
    tiles["farmland"] = tile_farmland()
    tiles["wheat_crop"] = tile_cross_plant((205, 170, 70), 69, 6)
    tiles["tall_grass"] = tile_cross_plant((80, 140, 50), 70, 6)
    tiles["flower_yellow"] = tile_flower((240, 200, 50))
    tiles["flower_red"] = tile_flower((210, 60, 50))
    tiles["cactus_top"] = tile_simple((70, 140, 48), 0.1, 71)
    tiles["cactus_side"] = tile_cactus_side()
    tiles["dead_bush"] = tile_cross_plant((130, 95, 50), 72, 5)
    tiles["netherrack"] = tile_simple((95, 40, 40), 0.2, 73)
    tiles["ash_sand"] = tile_simple((80, 72, 70), 0.12, 74)
    tiles["nether_brick"] = tile_bricks((55, 25, 30), (30, 15, 18), 75)
    tiles["glowstone"] = tile_glowstone()
    tiles["portal"] = tile_portal()
    tiles["wool"] = tile_simple((228, 228, 228), 0.06, 76)
    tiles["tnt_side"] = tile_tnt_side()
    tnt_top = tile_simple((190, 60, 50), 0.08, 77)
    ImageDraw.Draw(tnt_top).rectangle([5, 5, 10, 10], fill=(80, 50, 40, 255))
    tiles["tnt_top"] = tnt_top
    tiles["bookshelf"] = tile_bookshelf()

    for mob in ("pig", "cow", "sheep", "chicken", "zombie", "skeleton", "creeper", "spider", "villager", "fiend"):
        tiles[f"mob_{mob}"] = tile_mob(mob)
    tiles["vehicle_red"] = tile_vehicle((200, 60, 50))
    tiles["vehicle_blue"] = tile_vehicle((60, 110, 180))
    tiles["vehicle_gray"] = tile_vehicle((120, 120, 128))
    tiles["vehicle_yellow"] = tile_vehicle((240, 180, 40))
    tiles["vehicle_green"] = tile_vehicle((90, 130, 70))
    tiles["vehicle_white"] = tile_vehicle((230, 230, 235))
    tiles["sun"] = tile_sun()
    tiles["moon"] = tile_moon()

    tiles.update(build_item_tiles())
    return tiles


def write_atlas(tiles):
    os.makedirs(TEX_DIR, exist_ok=True)
    names = list(tiles.keys())
    assert len(names) <= GRID * GRID, f"too many tiles: {len(names)}"
    atlas = Image.new("RGBA", (GRID * T, GRID * T), (0, 0, 0, 0))
    mapping = {}
    for i, name in enumerate(names):
        x, y = (i % GRID) * T, (i // GRID) * T
        atlas.paste(tiles[name], (x, y))
        mapping[name] = i
    atlas.save(os.path.join(TEX_DIR, "atlas.png"))
    with open(os.path.join(TEX_DIR, "tiles.json"), "w") as f:
        json.dump(mapping, f, indent=0, sort_keys=True)
    print(f"atlas.png: {len(names)} tiles")


# ---------------------------------------------------------------- icons

def write_icons():
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    grass_top = tile_grass_top()
    grass_side = tile_grass_side()
    for dpi, size in sizes.items():
        img = Image.new("RGBA", (size, size), (99, 155, 255, 255))
        # Isometric-ish block: top + front face.
        top = grass_top.resize((size * 3 // 4, size * 3 // 8), Image.NEAREST)
        side = grass_side.resize((size * 3 // 4, size * 3 // 8), Image.NEAREST)
        img.paste(side, (size // 8, size // 2), side)
        img.paste(top, (size // 8, size // 8), top)
        out = os.path.join(RES_DIR, f"mipmap-{dpi}")
        os.makedirs(out, exist_ok=True)
        img.save(os.path.join(out, "ic_launcher.png"))
        img.save(os.path.join(out, "ic_launcher_round.png"))
    print("launcher icons written")


# ---------------------------------------------------------------- sounds

SR = 22050


def write_wav(name, samples):
    os.makedirs(SND_DIR, exist_ok=True)
    path = os.path.join(SND_DIR, name + ".wav")
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        frames = b"".join(struct.pack("<h", clamp(int(s * 32767), -32767, 32767)) for s in samples)
        w.writeframes(frames)


def env(i, n, attack=0.02, decay=1.0):
    """Simple attack/decay envelope."""
    t = i / n
    a = min(1.0, t / max(attack, 1e-6))
    d = math.exp(-t * 5 * decay)
    return a * d


def noise_burst(dur, lowpass=0.3, decay=1.0, vol=0.8, seed=1):
    r = random.Random(seed)
    n = int(SR * dur)
    out = []
    prev = 0.0
    for i in range(n):
        white = r.uniform(-1, 1)
        prev = prev + lowpass * (white - prev)
        out.append(prev * env(i, n, 0.005, decay) * vol)
    return out


def tone(freq, dur, vol=0.5, shape="sine", decay=1.0, slide=0.0):
    n = int(SR * dur)
    out = []
    phase = 0.0
    for i in range(n):
        f = freq + slide * (i / n)
        phase += 2 * math.pi * f / SR
        if shape == "sine":
            s = math.sin(phase)
        elif shape == "square":
            s = 1 if math.sin(phase) > 0 else -1
        elif shape == "saw":
            s = 2 * ((phase / (2 * math.pi)) % 1) - 1
        else:
            s = math.sin(phase)
        out.append(s * env(i, n, 0.01, decay) * vol)
    return out


def mix(*tracks):
    n = max(len(t) for t in tracks)
    out = [0.0] * n
    for t in tracks:
        for i, s in enumerate(t):
            out[i] += s
    peak = max(1.0, max(abs(s) for s in out))
    return [s / peak * 0.9 for s in out]


def concat(*tracks):
    out = []
    for t in tracks:
        out.extend(t)
    return out


def build_sounds():
    write_wav("step_grass", noise_burst(0.12, 0.25, 3.0, 0.5, 1))
    write_wav("step_stone", noise_burst(0.09, 0.6, 4.0, 0.5, 2))
    write_wav("step_wood", mix(noise_burst(0.1, 0.4, 4.0, 0.4, 3), tone(180, 0.1, 0.25, "sine", 4)))
    write_wav("step_sand", noise_burst(0.14, 0.15, 2.5, 0.45, 4))
    write_wav("dig_stone", mix(noise_burst(0.2, 0.5, 2.5, 0.7, 5), tone(90, 0.2, 0.3, "sine", 3)))
    write_wav("dig_wood", mix(noise_burst(0.18, 0.35, 2.5, 0.6, 6), tone(140, 0.18, 0.3, "sine", 3)))
    write_wav("dig_grass", noise_burst(0.18, 0.2, 2.0, 0.55, 7))
    write_wav("dig_sand", noise_burst(0.2, 0.12, 1.8, 0.5, 8))
    write_wav("dig_glass", mix(noise_burst(0.15, 0.85, 3.5, 0.5, 9), tone(2400, 0.12, 0.3, "sine", 5)))
    write_wav("place", mix(noise_burst(0.07, 0.5, 5.0, 0.5, 10), tone(220, 0.07, 0.3, "sine", 6)))
    write_wav("splash", noise_burst(0.4, 0.18, 1.5, 0.6, 11))
    write_wav("eat", concat(noise_burst(0.08, 0.3, 4, 0.4, 12), [0.0] * 800, noise_burst(0.08, 0.3, 4, 0.4, 13)))
    write_wav("hurt", tone(220, 0.18, 0.6, "square", 3, -80))
    write_wav("mob_pig", tone(320, 0.18, 0.5, "saw", 2.4, 60))
    write_wav("mob_cow", tone(140, 0.5, 0.5, "saw", 1.5, -30))
    write_wav("mob_sheep", mix(tone(500, 0.4, 0.4, "saw", 1.6), tone(505, 0.4, 0.3, "saw", 1.6)))
    write_wav("mob_chicken", concat(tone(900, 0.08, 0.4, "square", 3), [0.0] * 600, tone(1100, 0.1, 0.4, "square", 3)))
    write_wav("mob_zombie", tone(90, 0.6, 0.6, "saw", 1.2, -20))
    write_wav("arrow", noise_burst(0.15, 0.9, 4.0, 0.4, 14))
    write_wav("creeper_fuse", noise_burst(1.4, 0.95, 0.4, 0.5, 15))
    write_wav("explosion", mix(noise_burst(0.9, 0.08, 1.2, 1.0, 16), tone(55, 0.9, 0.7, "sine", 1.4)))
    write_wav("rain", noise_burst(3.0, 0.3, 0.01, 0.30, 17))
    write_wav("thunder", mix(noise_burst(1.8, 0.06, 0.8, 0.9, 18), tone(45, 1.8, 0.5, "sine", 1.0)))
    write_wav("wind", noise_burst(3.0, 0.04, 0.01, 0.25, 19))
    write_wav("click", tone(700, 0.05, 0.4, "square", 6))
    write_wav("achievement", concat(tone(660, 0.1, 0.4, "sine", 2), tone(880, 0.2, 0.4, "sine", 2)))
    write_wav("portal", mix(tone(120, 1.2, 0.4, "sine", 1.0, 160), noise_burst(1.2, 0.1, 0.8, 0.3, 20)))
    write_wav("engine", [0.5 * math.sin(2 * math.pi * 70 * i / SR) * (1 + 0.3 * math.sin(2 * math.pi * 9 * i / SR))
                         for i in range(int(SR * 2.0))])
    write_wav("horn", mix(tone(360, 0.5, 0.5, "square", 0.8), tone(452, 0.5, 0.4, "square", 0.8)))

    # Calm ambient music loop (~12 s): slow chord arpeggio.
    notes = [262, 330, 392, 523, 392, 330, 294, 349, 440, 349, 294, 262]
    track = []
    for k, f in enumerate(notes):
        seg = mix(tone(f, 1.0, 0.18, "sine", 0.7), tone(f / 2, 1.0, 0.12, "sine", 0.6),
                  tone(f * 1.5, 1.0, 0.05, "sine", 0.8))
        track = concat(track, seg)
    write_wav("music", track)
    print(f"sounds written: {len(os.listdir(SND_DIR))}")


if __name__ == "__main__":
    tiles = build_tiles()
    write_atlas(tiles)
    write_icons()
    build_sounds()
    print("done")
