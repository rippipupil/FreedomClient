"""Fondo de la tarjeta de Hades: noche gris con vegetación densa en capas (arbustos, hojas, helechos, lianas)."""
from PIL import Image
import math, random
W,H=320,64
rnd=random.Random(13)
img=Image.new('RGBA',(W,H)); px=img.load()
for y in range(H):
    band=round(y/(H-1)*6)/6
    c=int(22+band*18)
    for x in range(W): px[x,y]=(c,c,c+5,255)
def put(x,y,col):
    x=round(x); y=round(y)
    if 0<=x<W and 0<=y<H: px[x,y]=col
def blob(cx,cy,rx,ry,col):
    for y in range(int(cy-ry),int(cy+ry)+1):
        for x in range(int(cx-rx),int(cx+rx)+1):
            if ((x-cx)/rx)**2+((y-cy)/ry)**2<=1: put(x,y,col)
def leaf(x,y,ang,ln,wd,col,vein=None):
    for i in range(ln):
        t=i/ln; half=wd*math.sin(math.pi*t)
        cx=x+math.cos(ang)*i; cy=y+math.sin(ang)*i
        for k in range(-int(half),int(half)+1):
            put(cx-math.sin(ang)*k,cy+math.cos(ang)*k,col)
        if vein: put(cx,cy,vein)
def blade(x,base,height,lean,col,w=1,tip=None):
    for i in range(height):
        t=i/height; xx=x+lean*t*t*height*0.35
        for k in range(w if t<0.6 else 1): put(xx+k,base-i,col)
    if tip: put(x+lean*height*0.35,base-height,tip)
def fern(x,base,height,col,side=1):
    for i in range(height):
        t=i/height; xx=x+side*t*t*height*0.45; yy=base-i
        put(xx,yy,col)
        if i%2==0 and i>2:
            ln=max(1,int((1-t)*6))
            for k in range(1,ln+1):
                put(xx-k,yy+k*0.5,col); put(xx+k,yy+k*0.5,col)
FAR=(74,74,82,255); FAR2=(62,62,70,255); MID=(44,44,50,255); MID2=(52,52,58,255); NEAR=(12,12,15,255); RIM=(160,160,168,255)
# Lejos: copas de arbustos redondos y helechos altos
for i in range(26): blob(rnd.randrange(W),H-8-rnd.randint(4,14),rnd.randint(7,14),rnd.randint(5,10),FAR2 if i%2 else FAR)
for i in range(16): fern(rnd.randrange(W),H-1,rnd.randint(22,34),FAR,rnd.choice((-1,1)))
for i in range(60): blade(rnd.randrange(W),H-1,rnd.randint(16,30),rnd.uniform(-1,1),FAR)
# Medio: hojas grandes y hierba
for i in range(22):
    x=rnd.randrange(W); ang=-math.pi/2+rnd.uniform(-1.0,1.0)
    leaf(x,H-2,ang,rnd.randint(14,24),rnd.uniform(3,5),MID2,MID)
for i in range(70): blade(rnd.randrange(W),H-1,rnd.randint(10,20),rnd.uniform(-1.3,1.3),MID,2)
for i in range(14): fern(rnd.randrange(W),H-1,rnd.randint(14,22),MID,rnd.choice((-1,1)))
# Lianas que cuelgan de arriba, con hojitas
for i in range(12):
    x=rnd.randrange(W); ln=rnd.randint(10,26)
    for j in range(ln):
        X=x+math.sin(j*0.45+i)*1.5; put(X,j,MID)
        if j%4==2: leaf(X,j,0.6 if j%8==2 else 2.5,4,1.4,MID2)
# Cerca: casi negro con brillo gris en el borde de arriba
for i in range(18):
    x=rnd.randrange(W); ang=-math.pi/2+rnd.uniform(-1.1,1.1)
    leaf(x,H-1,ang,rnd.randint(9,15),rnd.uniform(2.5,3.5),NEAR)
for i in range(60): blade(rnd.randrange(W),H-1,rnd.randint(6,13),rnd.uniform(-1.5,1.5),NEAR,2,RIM if i%3==0 else None)
for x in range(W):
    for y in range(H-3,H): px[x,y]=NEAR
# Brillo de borde: un píxel claro encima de las hojas cercanas cuando hay cielo detrás
snap=img.copy().load()
for y in range(1,H):
    for x in range(W):
        if snap[x,y]==NEAR and snap[x,y-1]!=NEAR and rnd.random()<0.35: put(x,y,(70,70,78,255))
# Esporas brillantes
for i in range(36): put(rnd.randrange(W),rnd.randrange(6,H-10),(210,210,216,255) if i%3==0 else (130,130,138,255))
img.save('hades_card.png')
img.resize((W*3,H*3),Image.NEAREST).save('hades_card_big.png')
