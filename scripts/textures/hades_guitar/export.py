import hades, math
CH={hades.C['k']:'k',hades.C['d']:'d',hades.C['g']:'g',hades.C['l']:'l',hades.C['w']:'w',hades.C['s']:'s',hades.C['n']:'n',hades.C['f']:'f'}
grid={}; extra=[]; thin={}
for v in hades.V:
    if v['rot']: continue
    unit = v['sx']==1 and v['sy']==1 and v['sz']==1 and float(v['x']).is_integer() and float(v['y']).is_integer() and float(v['z']).is_integer()
    if unit: grid[(int(v['x']),int(v['y']),int(v['z']))]=CH[v['c']]
    elif v['sx']==1 and v['sy']==1 and float(v['x']).is_integer() and float(v['y']).is_integer():
        thin.setdefault((v['z'],v['sz']),{})[(int(v['x']),int(v['y']))]=CH[v['c']]
    else: extra.append(v)
MAX=31
# 1) a lo largo de z
segs={}
cols={}
for (x,y,z),c in grid.items(): cols.setdefault((x,y),{})[z]=c
zr=[]
for (x,y),zs in cols.items():
    zl=sorted(zs); i=0
    while i<len(zl):
        z0=zl[i]; c=zs[z0]; j=i
        while j+1<len(zl) and zl[j+1]==zl[j]+1 and zs[zl[j+1]]==c: j+=1
        zr.append((x,y,z0,zl[j]+1,c)); i=j+1
# 2) a lo largo de x
byrow={}
for x,y,z0,z1,c in zr: byrow.setdefault((y,z0,z1,c),[]).append(x)
xr=[]
for (y,z0,z1,c),xs in byrow.items():
    xs.sort(); i=0
    while i<len(xs):
        j=i
        while j+1<len(xs) and xs[j+1]==xs[j]+1 and (xs[j+1]-xs[i]+1)+(z1-z0)<=MAX: j+=1
        xr.append((xs[i],xs[j]+1,y,z0,z1,c)); i=j+1
# 3) a lo largo de y
bycol={}
for x0,x1,y,z0,z1,c in xr: bycol.setdefault((x0,x1,z0,z1,c),[]).append(y)
boxes=[]
for (x0,x1,z0,z1,c),ys in bycol.items():
    ys.sort(); i=0
    while i<len(ys):
        j=i
        while j+1<len(ys) and ys[j+1]==ys[j]+1 and (ys[j+1]-ys[i]+1)+(z1-z0)<=MAX: j+=1
        y0,y1=ys[i],ys[j]+1
        # vox: y hacia abajo; el vóxel (x,y) ocupa x-0.5..x+0.5, y-0.5..y+0.5
        boxes.append((c,x0-0.5,-(y1-0.5),z0,x1-x0,y1-y0,z1-z0)); i=j+1
# Capas finas (golpeador, vetas, pastillas): mismas pasadas en x y en y dentro de cada capa.
for (z,sz),cells in thin.items():
    rows={}
    for (x,y),c in cells.items(): rows.setdefault((y,c),[]).append(x)
    runs=[]
    for (y,c),xs in rows.items():
        xs.sort(); i=0
        while i<len(xs):
            j=i
            while j+1<len(xs) and xs[j+1]==xs[j]+1 and xs[j+1]-xs[i]+1<MAX: j+=1
            runs.append((xs[i],xs[j]+1,y,c)); i=j+1
    col={}
    for x0,x1,y,c in runs: col.setdefault((x0,x1,c),[]).append(y)
    for (x0,x1,c),ys in col.items():
        ys.sort(); i=0
        while i<len(ys):
            j=i
            while j+1<len(ys) and ys[j+1]==ys[j]+1 and ys[j+1]-ys[i]+1<MAX: j+=1
            boxes.append((c,x0-0.5,-(ys[j]+0.5),z+0.5-sz/2,x1-x0,ys[j]-ys[i]+1,sz)); i=j+1
for v in extra:
    c=CH[v['c']]; sx,sy,sz=v['sx'],v['sy'],v['sz']
    X0=v['x']-sx/2; Ytop=-(v['y']+sy/2); Z0=v['z']+0.5-sz/2
    n=max(1,math.ceil((sy+sz)/MAX))
    for k in range(n):
        boxes.append((c,X0,Ytop+sy*k/n,Z0,sx,sy/n,sz))
with open('hades.vox','w') as f:
    f.write('# Hades: cajas "color x y z ancho alto fondo" en píxeles del modelo (y hacia abajo, la cara hacia +z)\n')
    for b in boxes: f.write('%s %.3g %.3g %.3g %.3g %.3g %.3g\n'%b)
print(len(grid),len(extra),len(boxes))
ys=[b[2] for b in boxes]; print('y range',min(ys),max(b[2]+b[5] for b in boxes))
xs=[b[1] for b in boxes]; print('x range',min(xs),max(b[1]+b[4] for b in boxes))
