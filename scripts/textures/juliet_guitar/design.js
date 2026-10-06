// Código de la guitarra del diseño "Guitarra Voxel · Juliet" de Claude Design, copiado tal cual (de "const PAL" hasta
// antes del renderizado). Imprime en JSON cada caja del modelo: node design.js > design.json
const PAL=[0xf7f7f9,0x3a3a42,0xb9bac2,0xd6d7dd,0xc3cbea,0xb1101c,0xecc4c8,0x5a3ea8,0x6d50c4,0x4b3196,0x35206e,0x3f7d2c,0x8cb43c,0x6a3fa3,0xdfe1ea,0xe4e5ea,0xcdced6];
let seed=7;const rnd=()=>(seed=(seed*16807)%2147483647)/2147483647;
function hw(r){
  const k=[[0,3],[2,8],[5,11],[7,11.5],[11,11.5],[16,9.5],[19,9.5],[24,12.6],[29,15],[38,15],[44,12.4],[48,10.5],[55,9.5]];
  for(let i=1;i<k.length;i++)if(r<=k[i][0]){const a=k[i-1],b=k[i];return a[1]+(b[1]-a[1])*(r-a[0])/(b[0]-a[0]);}
  return 9.5;
}
function paint(x,r){
  const xc=x+0.5,ax=Math.abs(xc),h=hw(r);
  if(r===13&&(x===-9||x===-4))return 1;
  if(r===14&&x>=-8&&x<=-5)return 1;
  if(r===15&&(x===-8||x===-6))return 1;
  if(xc>0&&r<=5+xc*1.1+3*(Math.floor(xc)%2))return 1;
  if(xc<0&&r<=3+ax*0.9+3*(Math.floor(ax)%2))return 3;
  if((x===-2||x===-1)&&r>=31&&r<=38)return 5;
  if(x===2&&r>=31&&r<=36)return 5;
  if(x===-5&&r>=30&&r<=34)return 5;
  if(r===39&&x>=-3&&x<=0)return 5;
  if(x===4&&r>=31&&r<=35)return 5;
  if(x===-8&&r>=28&&r<=34)return 5;
  if(x===-9&&r===35)return 5;
  {const by=r+0.5-24.5,bd=Math.sqrt(xc*xc+by*by);
   if(bd>7.5&&bd<12.5&&(((x*73856093)^(r*19349663))>>>0)%12===1)return 5;}
  if((x===-9||x===-7||x===6)&&r>=34&&r<=45)return 4;
  if(r>21&&xc>h-3)return 2;
  if(r>21&&xc<-h+2)return 3;
  const dy=r+0.5-24.5,d=Math.sqrt(xc*xc+dy*dy);
  if(d>8&&d<10.5&&(((x*73856093)^(r*19349663))>>>0)%3===0)return 6;
  return 0;
}
function gen(){
  // coordenadas: y hacia abajo (0 = punta de la pala), z = 3 es la tapa
  const M=new Map(),X=[];
  const set=(x,y,z,c)=>M.set(x+','+y+','+z,{x,y,z,c,sx:1,sy:1,sz:1});
  const has=(x,y,z)=>M.has(x+','+y+','+z);
  const inBody=(x,r)=>{const xc=x+0.5,yb=50+5*(1-Math.abs((((xc+2.5)%5)+5)%5-2.5)/2.5);
    return r>=0&&r<=55&&Math.abs(xc)<=hw(r)&&(r<=48||r<=yb);};
  const hole=(x,r)=>{const fx=x+0.5,fy=r-24+0.5,d=Math.sqrt(fx*fx+fy*fy),th=Math.atan2(fy,fx);return {d,th,in:d<=7+0.8*Math.cos(8*th)};};
  // cuerpo con puntas de mechón y boca rebajada
  for(let r=0;r<=55;r++)for(let x=-16;x<=15;x++){
    if(!inBody(x,r))continue;
    const edge=!(inBody(x-1,r)&&inBody(x+1,r)&&inBody(x,r-1)&&inBody(x,r+1));
    for(let z=0;z<=3;z++){
      let c=z===3?paint(x,r):(z===0?16:15);
      if(z<3&&edge&&paint(x,r)===1)c=1;
      set(x,53+r,z,c);
    }
  }
  // pala: flequillo dentado, gotas azules, mini flor, hoja y cintas
  const tops={'-4':3,'-3':-1,'-2':1,'-1':-4,'0':0,'1':-5,'2':-1,'3':2};
  for(let x=-4;x<=3;x++)for(let y=tops[x];y<=13;y++){
    const fr=y<4+((x+8)%2)*2;
    set(x,y,2,fr?1:16);
    set(x,y,3,fr?1:(((x===-3&&y>=7&&y<=12)||(x===2&&y>=9&&y<=13))?4:0));
  }
  [6,9,12].forEach(ty=>{X.push({x:-5.2,y:ty,z:2.5,c:1,sx:1.4,sy:1,sz:1});X.push({x:4.2,y:ty,z:2.5,c:1,sx:1.4,sy:1,sz:1});
    X.push({x:-6,y:ty,z:2.5,c:13,sx:0.7,sy:1.5,sz:1.5});X.push({x:5,y:ty,z:2.5,c:13,sx:0.7,sy:1.5,sz:1.5});});
  for(let y=7;y<=10;y++)for(let x=-2;x<=1;x++){
    const cor=(x===-2||x===1)&&(y===7||y===10);
    if(!cor)set(x,y,4,7);
    if(x>=-1&&x<=0&&y>=8&&y<=9)set(x,y,5,8);
  }
  X.push({x:-0.5,y:8.5,z:6,c:10,sx:0.8,sy:0.8,sz:0.6});
  [[2,6],[3,6],[2,5],[3,5],[3,4],[-3,11],[-4,11],[-3,12]].forEach(a=>set(a[0],a[1],4,(a[0]+a[1])%3===0?12:11));
  for(let y=11;y<=22;y++){X.push({x:1+0.5*Math.sin(y/1.6),y,z:4.65,c:13,sx:0.45,sy:1,sz:0.3});}
  for(let y=11;y<=19;y++){X.push({x:-2+0.5*Math.sin(y/1.3+2),y,z:4.65,c:13,sx:0.45,sy:1,sz:0.3});}
  // mástil y diapasón
  for(let y=14;y<=61;y++)for(let x=-2;x<=1;x++){
    if(y<53){set(x,y,3,15);set(x,y,2,16);if(x===-1||x===0)set(x,y,1,16);}
    set(x,y,4,y===14?0:((y===16||y===18)?13:((y-14)%4===3?14:1)));
    if(y===16||y===18){if(y<53&&(x===-2||x===1)){set(x<0?-3:2,y,3,13);set(x<0?-3:2,y,2,13);}}
  }
  // flores pequeñas incrustadas en el diapasón
  [[26,7],[38,8],[50,7]].forEach(a=>{set(-1,a[0],4,a[1]);set(0,a[0],4,a[1]);set(-1,a[0]+1,4,a[1]);set(0,a[0]+1,4,a[1]);});
  // puente
  for(let x=-5;x<=4;x++){set(x,93,4,1);set(x,94,4,1);}
  // hoja
  const leaf={'-12':[9,10],'-11':[8,11],'-10':[8,13],'-9':[7,14],'-8':[7,15],'-7':[6,14],'-6':[6,13],'-5':[7,12],'-4':[8,11],'-3':[9,10]};
  const putLeaf=(fx,fy,ox,oy)=>Object.keys(leaf).forEach(k=>{const dy=parseInt(k,10),a=leaf[k];
    for(let lx=a[0];lx<=a[1];lx++){
      const x=(fx?-lx-1:lx)+ox,y=77+(fy?-dy:dy)+oy;
      set(x,y,4,lx===3-dy?12:11);
      if(!has(x,y,3))set(x,y,3,11);
    }});
  putLeaf(0,0,0,0);putLeaf(1,0,3,6);
  // sangre en relieve: baja de la flor y se queda en la tapa
  for(let r=33;r<=38;r++)X.push({x:-1.5,y:53+r,z:3.65,c:5,sx:1.6,sy:1,sz:0.3});
  X.push({x:-1.5,y:92,z:3.7,c:5,sx:2.6,sy:1,sz:0.4});
  for(let r=33;r<=36;r++)X.push({x:2,y:53+r,z:3.65,c:5,sx:0.7,sy:1,sz:0.3});
  X.push({x:2,y:90.2,z:3.7,c:5,sx:1,sy:1,sz:0.4});
  for(let r=28;r<=34;r++)X.push({x:-8,y:53+r,z:3.65,c:5,sx:0.8,sy:1,sz:0.3});
  for(let y=11;y<=13;y++)X.push({x:0,y,z:3.65,c:5,sx:0.7,sy:1,sz:0.3});
  // flor en relieve: cúpula de pétalos que sobresale de la tapa
  for(let r=13;r<=35;r++)for(let x=-11;x<=10;x++){
    const h=hole(x,r),d=h.d,th=h.th;
    if(d<=7.4+0.9*Math.cos(8*th))set(x,53+r,4,9);
    if(d<=6.2+0.8*Math.cos(8*th+1.2))set(x,53+r,5,7);
    if(d<=4.8+0.7*Math.cos(8*th+2.4))set(x,53+r,6,8);
    if(d<=3.3+0.5*Math.cos(6*th))set(x,53+r,7,7);
    if(d<=2+0.3*Math.cos(6*th+1))set(x,53+r,8,9);
    if(d<=1)set(x,53+r,9,10);
  }
  // cintas
  for(let r=30;r<=46;r++){const x=7+Math.round(Math.sin(r/3));if(has(x,53+r,3))X.push({x,y:53+r,z:3.65,c:13,sx:1,sy:1,sz:0.3});}
  for(let r=29;r<=42;r++){const x=10+Math.round(Math.sin(r/2.5+1));if(has(x,53+r,3))X.push({x,y:53+r,z:3.65,c:13,sx:1,sy:1,sz:0.3});}
  // cuerdas
  for(let i=0;i<6;i++){
    const x=-0.5+(i-2.5)*0.6,y0=26,y1=93.5;
    X.push({x,y:(y0+y1)/2,z:4.85,c:14,sx:0.12,sy:y1-y0,sz:0.12,str:1});
  }
  // mástil acortado: se quitan 12 filas y la pala baja
  const V=[],CUT=12;
  const keep=v=>{if(v.str){V.push(v);return;}
    if(v.y>=30&&v.y<30+CUT)return;
    if(v.y<30)v.y+=CUT;
    V.push(v);};
  M.forEach(keep);X.forEach(keep);
  return V;
}


const V=gen();console.log(JSON.stringify(V.map(v=>({x:v.x,y:v.y,z:v.z,c:v.c,sx:v.sx,sy:v.sy,sz:v.sz,str:v.str?1:0,hex:PAL[v.c]}))));
