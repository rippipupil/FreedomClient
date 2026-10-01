import sys
from PIL import Image
import scene

N = 16
EDGE = {'color': (16, 10, 32), 'mono': (4, 4, 6)}

def texture(frame, style):
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    edge = EDGE[style] + (255,)
    for y in range(0, 136):
        for x in range(0, 176): img.putpixel((x, y), edge)
    for y in range(0, 176):
        for x in range(176, 368):
            if y < 16 and not (192 <= x < 352): continue
            img.putpixel((x, y), edge)
    cape = scene.colorize(scene.render(frame, N), style).convert('RGBA')
    img.paste(cape, (8, 8))
    img.paste(cape, (96, 8))
    wing = scene.colorize(scene.render(frame, N, 160), style).convert('RGBA')
    img.paste(wing, (192, 16))
    img.paste(wing.transpose(Image.FLIP_LEFT_RIGHT), (288, 16))
    return img

out = sys.argv[1]
for style in ('color', 'mono'):
    for f in range(N):
        texture(f, style).save(f'{out}/cape_angel_sky_{style}_{f}.png', optimize=True)
