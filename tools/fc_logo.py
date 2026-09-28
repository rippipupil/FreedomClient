"""Logo de FreedomClient: las iniciales FC hechas con trazos de circuito y nodos redondos, con un halo encima,
y el emblema sin letras que se usa dentro del cliente.

El FC lo usa make_launcher_icons.py (iconos y logo de la app); el emblema, make_badge.py (insignia del nombre) y,
copiado a mano, PixelSky.java (menús del cliente). Cada fila es un string: '#' trazo, 'o' borde de un nodo, 'O' centro de un nodo.
"""

LOGO = [
    "########...#########",
    "#######...##########",
    "##.......##.........",
    "##.......##.........",
    "##...oo..##.........",
    "####oOOo.##.........",
    "####oOOo.##.........",
    "##...oo..##.........",
    "##.......##.........",
    "##.......##......oo.",
    "##.......#######oOOo",
    "##........######oOOo",
    ".................oo.",
]

# Emblema sin letras para dentro del cliente: tres barras, una diagonal y dos nodos, como el icono de Neon.
EMBLEM = [
    "..oo..................",
    ".oOOo#################",
    ".oOOo################.",
    "..oo...........####...",
    "..............####....",
    "....#########.####....",
    "...#########.####.....",
    "............####......",
    "...........####.......",
    "..........####....oo..",
    "..#############..oOOo.",
    "..############...oOOo.",
    "..................oo..",
]

# Paletas por tema: degradado de los trazos por fila (de arriba abajo), nodos (borde, centro) y halo (claro, oscuro).
PALETTES = {
    "angel": {
        "rows": [(255, 255, 255), (250, 246, 236), (245, 241, 232), (247, 234, 208), (247, 226, 190), (246, 214, 150),
                 (245, 205, 120), (242, 196, 90), (236, 182, 70), (232, 169, 58), (222, 120, 60), (215, 38, 61), (215, 38, 61)],
        "node": ((215, 38, 61), (255, 120, 120)),
        "halo": ((242, 201, 76), (201, 143, 30)),
        "outline": (26, 5, 8),
        "shadow": (18, 4, 10),
    },
    "neon": {
        "rows": [(234, 250, 255), (200, 244, 255), (160, 234, 255), (110, 220, 255), (63, 215, 255), (60, 190, 255),
                 (58, 160, 255), (52, 130, 255), (46, 100, 255), (42, 80, 255), (70, 70, 240), (107, 91, 255), (107, 91, 255)],
        "node": ((255, 216, 74), (255, 245, 170)),
        "halo": ((168, 240, 90), (63, 215, 255)),
        "outline": (6, 8, 30),
        "shadow": (4, 6, 22),
    },
}


def cells(rows=None):
    """Diccionario (x, y) -> tipo ('#', 'o' u 'O') de los píxeles del logo (o de las filas que se pasen)."""
    rows = rows or LOGO
    return {(x, y): c for y, row in enumerate(rows) for x, c in enumerate(row) if c != "."}


def color(kind, y, palette):
    p = PALETTES[palette]
    if kind == "o":
        return p["node"][0]
    if kind == "O":
        return p["node"][1]
    return p["rows"][min(y, len(p["rows"]) - 1)]
