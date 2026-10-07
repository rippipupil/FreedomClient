"""Partitura de las teclas para "Flow" (Creo): saca del audio cuándo suena cada nota y cuál es, para que el teclado
ilumine la tecla que toca mientras suena la canción.

    python3 timeline.py flow.wav flow_keys.txt   (wav mono a 22050 Hz)

Detecta los golpes (subidas bruscas del espectro), y en cada uno mira qué nota (de las 12) suena más fuerte en los
100 ms siguientes y en qué octava suena con más fuerza: eso da una de las 44 teclas del teclado (de do3 a sol6; lo que
cae fuera se lleva a la octava más cercana que sí está). Sale una línea por nota: "milisegundo tecla fuerza(0-100)".
"""
import sys
import numpy as np

src, dst = sys.argv[1], sys.argv[2]
x = np.fromfile(src, dtype=np.int16).astype(np.float32) / 32768.0
sr = 22050
hop, win = 256, 2048
n = (len(x) - win) // hop
frames = np.lib.stride_tricks.sliding_window_view(x, win)[::hop][:n] * np.hanning(win)
mag = np.abs(np.fft.rfft(frames, axis=1))
freqs = np.fft.rfftfreq(win, 1 / sr)
fps = sr / hop

# Golpes: flujo espectral (lo que sube de un fotograma al siguiente), con umbral que se adapta a la zona.
logm = np.log1p(mag * 20)
flux = np.maximum(0, np.diff(logm, axis=0)).sum(1)
flux = np.concatenate([[0], flux])
k = int(fps * 0.5)
local = np.convolve(flux, np.ones(2 * k + 1) / (2 * k + 1), mode='same')
peaks = []
last = -1e9
for i in range(2, len(flux) - 2):
    if flux[i] > local[i] * 1.35 + 0.5 and flux[i] == flux[i - 2:i + 3].max() and i - last > fps * 0.09:
        peaks.append(i)
        last = i

# Nota de cada golpe: energía por nota (12) en 80-2000 Hz durante los siguientes 100 ms.
band = (freqs > 80) & (freqs < 2000)
midi = 69 + 12 * np.log2(freqs[band] / 440.0)
pc = np.mod(np.round(midi), 12).astype(int)
notes = np.round(midi).astype(int)
LOW, KEYS = 48, 44  # do3 y las 44 teclas
fmax = np.percentile(flux[peaks], 90) / 0.6 if peaks else 1.0
lines = []
for i in peaks:
    seg = mag[i:i + int(fps * 0.1) + 1][:, band].sum(0)
    chroma = np.bincount(pc, weights=seg, minlength=12)
    note = int(np.argmax(chroma))
    sel = pc == note
    # La octava de esa nota con más energía.
    octs = np.bincount(notes[sel] // 12, weights=seg[sel])
    key = int(np.argmax(octs)) * 12 + note - LOW
    while key < 0: key += 12
    while key >= KEYS: key -= 12
    strength = int(round(100 * min(1.0, flux[i] / (fmax * 0.6))))
    lines.append((int(round(i / fps * 1000)), key, strength))
with open(dst, 'w') as f:
    f.write('# Flow (Creo): "milisegundo tecla fuerza", tecla 0-43 = de do3 a sol6. Hecho con timeline.py\n')
    for t, key, s in lines:
        f.write(f'{t} {key} {s}\n')
print(len(lines), 'notes; first', lines[:8])
print('per second', len(lines) / (len(x) / sr))
