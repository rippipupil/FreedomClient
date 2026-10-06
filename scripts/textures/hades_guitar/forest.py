"""Tarjeta de Hades: bosque muerto en tres capas (fondo, niebla que se mueve, árboles cercanos). 320x64, en bucle horizontal."""
from PIL import Image
import math, random
W,H=320,64
def save(img,name): img.save(name)
# ---------- Fondo: cielo gris con árboles muertos lejanos ----------
far=Image.new('RGBA',(W,H)); px=far.load()
for y in range(H):
    band=round(y/(H-1)*7)/7
    c=int(30+band*22)
    for x in range(W): px[x,y]=(c,c,c+6,255)
rnd=random.Random(5)
def put(img,x,y,col):
    p=img.load(); x=int(round(x))%W; y=int(round(y))
    if 0<=y<H: p[x,y]=col
def tree(img,x,base,height,col,thick,rnd,lean=0.0):
    # tronco que se va estrechando y ramas secas que se abren
    def branch(x,y,ang,ln,th,depth):
        steps=int(ln)
        for i in range(steps):
            x+=math.cos(ang); y+=math.sin(ang)
            ang+=rnd.uniform(-0.18,0.18)
            for k in range(max(1,int(th))): put(img,x+k,y,col)
            if depth>0 and rnd.random()<0.12 and i>2:
                branch(x,y,ang+rnd.choice((-1,1))*rnd.uniform(0.5,1.0),ln*0.45,max(1,th-1),depth-1)
        if depth>0:
            branch(x,y,ang-rnd.uniform(0.3,0.7),ln*0.6,max(1,th-1),depth-1)
            branch(x,y,ang+rnd.uniform(0.3,0.7),ln*0.55,max(1,th-1),depth-1)
    branch(x,base,-math.pi/2+lean,height*0.55,thick,3)
for i in range(16):
    tree(far,rnd.randrange(W),H-6,rnd.randint(30,44),(64,64,72,255),2,rnd,rnd.uniform(-0.15,0.15))
for x in range(W):
    for y in range(H-7,H): put(far,x,y,(52,52,58,255))
save(far,'hades_card_far.png')
# ---------- Niebla: banda suave en bucle (se desplaza despacio) ----------
fog=Image.new('RGBA',(W,H)); p=fog.load()
for x in range(W):
    t=x/W*2*math.pi
    top=34+4*math.sin(t*2+1)+3*math.sin(t*5)+2*math.sin(t*9+2)
    for y in range(H):
        d=y-top
        if d<0: a=max(0.0,1+d/10)*0.55
        else: a=0.55-0.15*min(1,d/20)
        a=round(a*6)/6  # escalones pixel
        if a>0: p[x,y]=(200,200,208,int(a*120))
save(fog,'hades_card_fog.png')
# ---------- Cerca: árboles muertos negros y el suelo ----------
near=Image.new('RGBA',(W,H))
rnd=random.Random(11)
for i in range(7):
    tree(near,rnd.randrange(W),H-2,rnd.randint(46,64),(14,14,17,255),3,rnd,rnd.uniform(-0.2,0.2))
# raíces y suelo ondulado
for x in range(W):
    g=H-4+round(1.5*math.sin(x/W*2*math.pi*3)+math.sin(x/W*2*math.pi*7))
    for y in range(g,H): put(near,x,y,(14,14,17,255))
# hierbas secas
for i in range(90):
    x=rnd.randrange(W); h=rnd.randint(2,6); lean=rnd.uniform(-1,1)
    for k in range(h): put(near,x+lean*k*0.4,H-4-k,(22,22,26,255))
save(near,'hades_card_near.png')
# vista previa compuesta
comp=far.copy(); comp.alpha_composite(fog); comp.alpha_composite(near)
comp.resize((W*3,H*3),Image.NEAREST).save('forest_preview.png')
