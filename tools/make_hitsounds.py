"""Genera los sonidos de HitSounds (normal y crítico de cada uno) sintetizados desde cero, en OGG Vorbis.

Uso: python3 tools/make_hitsounds.py   (requiere numpy y soundfile)
Escribe assets/freedomclient/sounds/hit/*.ogg y assets/freedomclient/sounds.json.
"""
import json
from pathlib import Path

import numpy as np
import soundfile

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freedomclient"
RATE = 44100


def t(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def env(length, attack=0.003, decay=12.0):
    """Envolvente: subida muy corta y caída exponencial."""
    x = np.arange(length) / RATE
    a = np.clip(x / attack, 0, 1)
    return a * np.exp(-x * decay)


def square(freq, seconds):
    return np.sign(np.sin(2 * np.pi * freq * t(seconds)))


def sweep(f0, f1, seconds, shape="sine"):
    x = t(seconds)
    freq = f0 * (f1 / f0) ** (x / seconds)
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    return np.sin(phase) if shape == "sine" else np.sign(np.sin(phase)) * 0.6 + np.sin(phase) * 0.4


def noise(seconds, seed=1):
    return np.random.default_rng(seed).uniform(-1, 1, int(RATE * seconds))


def lowpass(signal, amount):
    out = np.zeros_like(signal)
    acc = 0.0
    for i, v in enumerate(signal):
        acc += (v - acc) * amount
        out[i] = acc
    return out


def highpass(signal, amount):
    return signal - lowpass(signal, amount)


def pluck(freq, seconds, decay=0.996):
    """Cuerda pulsada (Karplus-Strong): suena a arpa."""
    n = int(RATE * seconds)
    period = int(RATE / freq)
    buffer = np.random.default_rng(int(freq)).uniform(-1, 1, period)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buffer[i % period]
        buffer[i % period] = decay * 0.5 * (buffer[i % period] + buffer[(i + 1) % period])
    return out


def seq(*parts, gap=0.0):
    """Encadena trozos de sonido con un hueco opcional entre ellos."""
    silence = np.zeros(int(RATE * gap))
    out = []
    for i, part in enumerate(parts):
        if i:
            out.append(silence)
        out.append(part)
    return np.concatenate(out)


def mix(*parts):
    length = max(len(p) for p in parts)
    out = np.zeros(length)
    for p in parts:
        out[:len(p)] += p
    return out


def shaped(signal, attack=0.002, decay=12.0):
    return signal * env(len(signal), attack, decay)


def bell(freq, seconds, decay=6.0):
    x = t(seconds)
    partials = [(1.0, 1.0), (2.76, 0.5), (5.40, 0.25), (8.93, 0.12)]
    s = sum(a * np.sin(2 * np.pi * freq * r * x) * np.exp(-x * decay * r ** 0.5) for r, a in partials)
    return s * np.clip(x / 0.002, 0, 1)


SOUNDS = {
    # Pop pixel: un blip cuadrado; crítico: dos blips que suben.
    "pop": lambda: shaped(square(880, 0.07) * 0.5, decay=40),
    "pop_crit": lambda: seq(shaped(square(880, 0.05) * 0.5, decay=40), shaped(square(1760, 0.09) * 0.5, decay=30), gap=0.01),
    # Campana: campana de metal; crítico: acorde más brillante.
    "bell": lambda: bell(1320, 0.45),
    "bell_crit": lambda: mix(bell(1320, 0.6, 5), bell(1980, 0.6, 5) * 0.7, bell(2640, 0.6, 5) * 0.5),
    # Láser: disparo que baja de tono; crítico: dos disparos.
    "laser": lambda: shaped(sweep(2400, 300, 0.16, "square") * 0.6, decay=14),
    "laser_crit": lambda: seq(shaped(sweep(2800, 400, 0.1, "square") * 0.6, decay=18),
                              shaped(sweep(3200, 250, 0.16, "square") * 0.6, decay=12), gap=0.02),
    # Moneda 8-bit; crítico: arpegio de subida.
    "coin": lambda: seq(shaped(square(988, 0.06) * 0.45, decay=4), shaped(square(1319, 0.22) * 0.45, decay=10)),
    "coin_crit": lambda: seq(*[shaped(square(f, 0.06) * 0.45, decay=8) for f in (1047, 1319, 1568)],
                             shaped(square(2093, 0.25) * 0.45, decay=9)),
    # Puñetazo: golpe grave con un poco de ruido; crítico: más fuerte y con un chasquido.
    "punch": lambda: mix(shaped(sweep(140, 45, 0.12), decay=22), shaped(lowpass(noise(0.08), 0.15) * 1.5, decay=45)),
    "punch_crit": lambda: mix(shaped(sweep(170, 40, 0.18) * 1.2, decay=16), shaped(lowpass(noise(0.1, 2), 0.2) * 1.6, decay=35),
                              shaped(highpass(noise(0.04, 3), 0.3) * 0.8, decay=80)),
    # Ángel: arpa; crítico: glissando de arpa hacia arriba.
    "angel": lambda: shaped(mix(pluck(1047, 0.5), pluck(1568, 0.5) * 0.6), decay=5),
    "angel_crit": lambda: mix(*[np.concatenate([np.zeros(int(RATE * 0.035 * i)), shaped(pluck(f, 0.5), decay=5) * 0.8])
                                for i, f in enumerate((1047, 1319, 1568, 2093))]),
    # Demonio: gruñido grave distorsionado; crítico: más grave y más largo.
    "devil": lambda: shaped(np.tanh(3 * mix(sweep(160, 70, 0.22, "square"), sweep(163, 72, 0.22, "square"))) * 0.5, decay=9),
    "devil_crit": lambda: shaped(np.tanh(4 * mix(sweep(140, 45, 0.32, "square"), sweep(143, 47, 0.32, "square"),
                                                 sweep(70, 30, 0.32) * 0.8)) * 0.5, decay=6),
    # Clic seco estilo osu; crítico: clic con un platillo corto.
    "click": lambda: shaped(highpass(noise(0.03, 4), 0.25) * 0.9 + np.sin(2 * np.pi * 2200 * t(0.03)) * 0.4, decay=120),
    "click_crit": lambda: mix(shaped(highpass(noise(0.03, 4), 0.25) * 0.9 + np.sin(2 * np.pi * 2200 * t(0.03)) * 0.4, decay=120),
                              np.concatenate([np.zeros(int(RATE * 0.02)), shaped(highpass(noise(0.2, 5), 0.6) * 0.5, decay=18)])),
}


def main():
    out_dir = ASSETS / "sounds" / "hit"
    out_dir.mkdir(parents=True, exist_ok=True)
    sounds_json = {}
    for name, make in SOUNDS.items():
        audio = make()
        # Pequeño fundido al final y normalizado al 80 % del máximo.
        fade = min(len(audio), int(RATE * 0.01))
        audio[-fade:] *= np.linspace(1, 0, fade)
        audio = audio / max(1e-9, np.max(np.abs(audio))) * 0.8
        soundfile.write(out_dir / f"{name}.ogg", audio.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
        sounds_json[f"hit.{name}"] = {"sounds": [f"freedomclient:hit/{name}"], "subtitle": "freedomclient.subtitle.hit"}
    (ASSETS / "sounds.json").write_text(json.dumps(sounds_json, indent="\t") + "\n")
    print(f"{len(SOUNDS)} hit sounds written to {out_dir}")


if __name__ == "__main__":
    main()
