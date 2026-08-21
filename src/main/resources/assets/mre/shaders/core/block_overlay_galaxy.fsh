#version 150

uniform float Time;

in vec4 FragColor;
in vec2 vUV;

out vec4 OutColor;

float random(vec2 st) { return fract(sin(dot(st,vec2(12.9898,78.233)))*43758.5453123); }

float noise(vec2 st) {
    vec2 i=floor(st);vec2 f=fract(st);
    float a=random(i),b=random(i+vec2(1,0)),c=random(i+vec2(0,1)),d=random(i+vec2(1,1));
    vec2 u=f*f*(3.0-2.0*f);
    return mix(a,b,u.x)+(c-a)*u.y*(1.0-u.x)+(d-b)*u.x*u.y;
}

vec3 nebula(vec2 uv, float t) {
    float n1=noise(uv*3.0+vec2(t*0.1,t*0.15));
    float n2=noise(uv*5.0-vec2(t*0.08,t*0.12));
    float n3=noise(uv*8.0+vec2(t*0.05,-t*0.1));
    float combined=pow(n1*0.5+n2*0.3+n3*0.2,2.0);
    vec3 col=mix(FragColor.rgb,mix(FragColor.rgb,vec3(0.5,0.2,0.8),0.6),n1);
    col=mix(col,mix(FragColor.rgb,vec3(0.2,0.6,1.0),0.5),n2*0.5);
    return col*combined;
}

void main() {
    vec2 pos = vUV * 4.0;
    float t = Time * 0.3;

    vec2 uv1 = pos + vec2(t*0.02, t*0.03);
    vec2 uv2 = pos.yx - vec2(t*0.025, t*0.02);

    vec3 nebColor = (nebula(uv1,t)+nebula(uv2,t+10.0))*0.5;

    float star1=step(0.995,random(floor(uv1*50.0)));
    float star2=step(0.993,random(floor(uv2*50.0)));
    float tw1=sin(Time*3.0+random(floor(uv1*50.0))*6.28)*0.5+0.5;
    float tw2=sin(Time*2.5+random(floor(uv2*50.0))*6.28)*0.5+0.5;
    vec3 stars=vec3(1.0,0.95,0.9)*(star1*tw1+star2*tw2*0.7);

    float spiral=atan(uv1.y-0.5,uv1.x-0.5)+t;
    float spiralPat=pow(sin(spiral*3.0+length(uv1-0.5)*10.0)*0.5+0.5,3.0);

    vec3 finalColor=nebColor+stars+FragColor.rgb*spiralPat*0.3;
    float glow=length(nebColor)*0.5+spiralPat*0.3;
    float alpha=FragColor.a*(0.5+glow*0.7)*(sin(t*1.5)*0.1+0.9);

    if(alpha<=0.0) discard;
    OutColor=vec4(finalColor*(1.0+glow*0.4),alpha);
}
