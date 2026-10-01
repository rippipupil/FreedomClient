import sys
from PIL import Image
import neon

EDGE = (12, 26, 36, 255)

def texture(f):
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    for y in range(0, 136):
        for x in range(0, 176): img.putpixel((x, y), EDGE)
    for y in range(0, 176):
        for x in range(176, 368):
            if y < 16 and not (192 <= x < 352): continue
            img.putpixel((x, y), EDGE)
    cape = neon.frame(f).convert('RGBA')
    img.paste(cape, (8, 8))
    img.paste(cape, (96, 8))
    wing = neon.frame(f, 160).convert('RGBA')
    img.paste(wing, (192, 16))
    img.paste(wing.transpose(Image.FLIP_LEFT_RIGHT), (288, 16))
    return img

for f in range(neon.N):
    texture(f).save(f'{sys.argv[1]}/cape_neon_card_{f}.png', optimize=True)
