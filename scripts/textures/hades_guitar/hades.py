"""Puerto a Python del diseño 'Guitarra Voxel Invernal' (variante Llama) de Claude Design."""
import math
C=dict(k=0x111114,d=0x3a3a42,g=0x8a8a94,l=0xc9c9cf,w=0xf2f2f0,r=0xf2f2f0,s=0xb8b8c0,n=0x24242a,f=0x18181c)
O=dict(n='Llama',bands=[C['w'],C['l'],C['g'],C['d'],C['k']],bind=C['k'],guard=[C['k'],C['w']],tong='curl',tc=[C['w'],C['l'],C['g'],C['d']],br=1,fr=0,cres=1,head=C['w'])
V=[]
seed=[7]
def rnd():
    seed[0]=(seed[0]*16807)%2147483647
    return seed[0]/2147483647
def add(x,y,z,c,sx=1,sy=1,sz=1,rot=0):
    V.append(dict(x=x,y=y,z=z,c=c,sx=sx,sy=sy,sz=sz,rot=rot))
def body(x,y):
    xs=x-(y-22)*0.16
    e1=(xs/17)**2+((y-15)/15)**2<=1
    e2=((xs+1)/14)**2+((y-32)/13)**2<=1
    if not(e1 or e2): return False
    if (x-12)**2+(y-47)**2<7.5**2: return False
    if (x+10)**2+(y-49)**2<6**2: return False
    if (xs-20)**2+(y-25)**2<5**2: return False
    if (xs+20)**2+(y-26)**2<5**2: return False
    cr=(x+9)**2+(y-9)**2<16 and (x+7.4)**2+(y-10.6)**2>=12
    if O['cres'] and cr: return False
    return True
h1=lambda x:8+5*math.sin(x*0.45+1)+3*math.sin(x*1.1+2)
def flame(x,y):
    a=h1(x);b=a+5+3*math.sin(x*0.7);c=b+6+2*math.sin(x*0.9+4);d=c+7+3*math.sin(x*0.5+1.5)
    B=O['bands']
    return B[0] if y<a else B[1] if y<b else B[2] if y<c else B[3] if y<d else B[4]
def inn(a,b): return body(a,b) and body(a-1,b) and body(a+1,b) and body(a,b-1) and body(a,b+1)
for y in range(-1,48):
    for x in range(-22,25):
        if not body(x,y): continue
        inner=body(x-1,y) and body(x+1,y) and body(x,y-1) and body(x,y+1)
        col=flame(x,y)
        ring=inner and not(inn(x-1,y) and inn(x+1,y) and inn(x,y-1) and inn(x,y+1))
        xg=x-(y-22)*0.16; inG=((xg+2)/9)**2+((y-23)/12)**2<=1.15 and xg<7
        if inner and not ring and not inG and y<h1(x)-1 and not(5<=y<=8 and abs(x)<=5): add(x,y,3.6,C['w'],1,1,0.25)
        for z in range(-5,4):
            if (z==-5 or z==3) and not inner: continue
            if z==-5 and ring: continue
            add(x,y,z,O['bind'] if (not inner or (ring and z==3)) else col)
for y in range(10,37):
    for x in range(-14,11):
        xs=x-(y-22)*0.16
        if ((xs+2)/9)**2+((y-23)/12)**2<=1 and xs<6 and body(x,y) and body(x-1,y) and body(x+1,y):
            edge=((xs+2)/9)**2+((y-23)/12)**2>0.8 or xs>=5
            add(x,y,3.65,O['guard'][0] if edge else O['guard'][1],1,1,0.3)
def slab(y0,y1,z,c,poles):
    for y in range(y0,y1+1):
        for x in range(-4,5):
            add(x,y,z,C['s'] if (poles and y==((y0+y1)>>1) and x%2!=0 and abs(x)<4) else c,1,1,0.8)
slab(19,21,4.1,C['k'],True);slab(31,33,4.1,C['k'],True);slab(12,13,4.2,C['s'],False);slab(6,7,4.1,C['s'],False)
add(10,9,4.2,C['s'],1.6,1.6,1.2);add(13,14,4.2,C['d'],1.6,1.6,1.2);add(-9,31,4.2,C['s'],0.6,0.6,1.6)
L=84;frets=[]
for n in range(1,23):
    y=round(96-L*(1-2**(-n/12)))
    if y>41: frets.append(y)
dots={}
for n in [3,5,7,9,12,15]:
    if n>len(frets): continue
    a=96 if n==1 else frets[n-2]; b=frets[n-1]
    dots[round((a+b)/2)]=2 if n==12 else 1
for y in range(40,97):
    for x in range(-3,4):
        if y>47:
            add(x,y,2,C['n'])
            if abs(x)<3: add(x,y,1,C['n'])
            if abs(x)<3: add(x,y,0,C['n'])
            if abs(x)<2: add(x,y,-1,C['n'])
        add(x,y,3,C['n'])
        c=C['f']
        if y==96: c=C['w']
        elif y in frets: c=C['l']
        elif dots.get(y)==1 and x==0: c=C['w']
        elif dots.get(y)==2 and abs(x)==2: c=C['w']
        add(x,y,4,c)
def head(x,y):
    t=(y-97)/14; lo=-4; hi=4+round(math.sin(t*math.pi)*2)
    if y<97 or y>111: return False
    if y>109 and (x<lo+1 or x>hi-(y-109)): return False
    return lo<=x<=hi
for y in range(97,112):
    for x in range(-6,9):
        if not head(x,y): continue
        edge=not(head(x-1,y) and head(x+1,y) and head(x,y-1) and head(x,y+1)) and y>97
        add(x,y,2,C['k'])
        if not edge: add(x,y,1,C['k'])
        add(x,y,3,C['k'] if edge else O['head'])
for i in range(6):
    ty=99+2*i
    add(-5.2,ty,2.5,C['s'],1.4,1,1)
    add(-2,ty,3.9,C['s'],0.6,0.6,0.8)
    x=(i-2.5)*1.0;y0=7;y1=96
    add(x,(y0+y1)/2,4.85,C['l'],0.14,y1-y0,0.14)
    dx=-2-x;dy=ty-96;ln=math.hypot(dx,dy)
    add(x+dx/2,96+dy/2,4.6,C['l'],0.14,ln,0.14,-math.atan2(dx,dy))
T={}
def tongue(x,y,ang,ln,w,curl):
    for t in range(0,ln+1):
        a=ang+curl*t/ln; r=w*(1-t/ln); u=t/ln
        x+=math.cos(a); y+=math.sin(a)
        K=O['tc']; c=K[0] if u<0.3 else K[1] if u<0.55 else K[2] if u<0.8 else K[3]
        if t==ln:
            add(x+math.cos(a)*1.5,y+math.sin(a)*1.5+0.5,0,C['w'],0.6,0.6,0.6)
            add(x+math.cos(a)*2.6,y+math.sin(a)*2.6+1.6,0,C['l'],0.35,0.35,0.35)
        R=math.ceil(r)
        for dx in range(-R,R+1):
            for dy in range(-R,R+1):
                if dx*dx+dy*dy>r*r+0.3: continue
                X=round(x+dx);Y=round(y+dy)
                if body(X,Y): continue
                T[(X,Y)]=dict(X=X,Y=Y,c=c,thin=r<0.9)
for a in [[-16,12,2.3,13,2.3,-0.9],[-15,21,2.6,10,1.8,-1.0],[-12,31,2.4,11,1.8,-0.8],[-10,3,3.3,8,1.6,-1.3],[17,10,0.6,12,2.1,0.9],[18,21,0.4,9,1.6,1.0],[15,33,0.7,8,1.4,0.7]]:
    tongue(*a)
for v in T.values():
    if v['thin']: add(v['X'],v['Y'],0,v['c'])
    else:
        for z in (-2,-1,0,1): add(v['X'],v['Y'],z,v['c'])
def branch(x,y,ang,ln,depth,top=None):
    if top is None: top=depth
    steps=round(ln/0.6)
    for t in range(steps):
        ang+=(rnd()-0.5)*0.16+(math.pi/2-ang)*0.025; x+=math.cos(ang)*0.6; y+=math.sin(ang)*0.6
        u=(top-depth+t/steps)/(top+1); th=2.6*(1-u)+0.7
        add(x,y,2,C['k'] if u<0.3 else C['d'] if u<0.55 else C['g'] if u<0.8 else C['l'],th,th,th)
        if depth>0 and t==math.floor(steps*0.5): branch(x,y,ang+(-1 if rnd()<0.5 else 1)*(0.7+rnd()*0.3),ln*0.45,0,top)
    if depth>0:
        branch(x,y,ang+0.45+rnd()*0.25,ln*0.72,depth-1,top); branch(x,y,ang-0.45-rnd()*0.25,ln*0.62,depth-1,top)
    else: add(x+math.cos(ang)*0.5,y+math.sin(ang)*0.5,2,C['w'],0.9,0.9,0.9)
branch(3,110,1.2,10,3);branch(-2,110,2.0,7,2);branch(-10,42,2.3,8,3)

if __name__=='__main__':
    from PIL import Image
    print(len(V))
    # vista frontal: el vóxel con z+sz/2 mayor gana en cada celda
    S=6
    xs=[v['x'] for v in V]; ys=[v['y'] for v in V]
    minx,maxx,miny,maxy=int(min(xs))-4,int(max(xs))+4,int(min(ys))-4,int(max(ys))+4
    W=(maxx-minx+1)*S; H=(maxy-miny+1)*S
    img=Image.new('RGB',(W,H),(60,90,70)); zb=[[-99]*W for _ in range(H)]
    px=img.load()
    for v in sorted(V,key=lambda v:v['z']+v['sz']/2):
        if v['rot']: continue
        x0=(v['x']-v['sx']/2-minx)*S; x1=(v['x']+v['sx']/2-minx)*S
        y0=(maxy-(v['y']+v['sy']/2))*S; y1=(maxy-(v['y']-v['sy']/2))*S
        c=v['c']; col=(c>>16,(c>>8)&255,c&255)
        for yy in range(max(0,int(y0)),min(H,int(math.ceil(y1)))):
            for xx in range(max(0,int(x0)),min(W,int(math.ceil(x1)))):
                px[xx,yy]=col
    img.save('front.png'); print(img.size, minx,maxx,miny,maxy)
