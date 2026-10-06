"""Rótulo 'HADES' de la tarjeta: letras pixel góticas finas, blancas con sombra gris, una gota que cae y brillo frío."""
from PIL import Image
L={
'H':["##...##",".#...#.",".#...#.",".#####.",".#...#.",".#...#.",".#...#.","##...##",".#....."],
'A':["...#...","..#.#..",".#...#.",".#...#.",".#####.",".#...#.",".#...#.","##...##","......."],
'D':["#####..",".#...#.",".#....#",".#....#",".#....#",".#....#",".#...#.","#####..","......."],
'E':["######.",".#...#.",".#.....",".####..",".#.....",".#.....",".#...#.","######.","...#..."],
'S':[".####.#","#....##","#......",".####..",".....#.","......#","##....#","#.####.","......."],
}
word="HADES"; gap=2
W=len(word)*7+(len(word)-1)*gap+2; H=9+2
img=Image.new('RGBA',(W,H)); p=img.load()
WHITE=(242,242,240,255); LIGHT=(201,201,207,255); SHADOW=(26,26,30,230); GLOW=(242,242,240,60)
cells=set()
for i,ch in enumerate(word):
    ox=i*(7+gap)
    for y,row in enumerate(L[ch]):
        for x,c in enumerate(row):
            if c=='#': cells.add((ox+x,y))
# brillo suave alrededor
for (x,y) in cells:
    for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
        X,Y=x+dx,y+dy
        if 0<=X<W and 0<=Y<H and (X,Y) not in cells and p[X,Y][3]==0: p[X,Y]=GLOW
# sombra abajo a la derecha
for (x,y) in cells:
    X,Y=x+1,y+1
    if (X,Y) not in cells and X<W and Y<H: p[X,Y]=SHADOW
# letras: blancas arriba, gris claro en la mitad de abajo (degradado frío)
for (x,y) in cells: p[x,y]=WHITE if y<5 else LIGHT
img.save('hades_title.png'); print(img.size)
img.resize((W*8,H*8),Image.NEAREST).save('title_big.png')
