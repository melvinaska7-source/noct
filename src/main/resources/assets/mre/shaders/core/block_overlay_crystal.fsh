#version 150

uniform float Time;

in vec4 FragColor;
in vec2 vUV;

out vec4 OutColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

float noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p);
    f = f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}

float fbm(vec2 p) {
    float v=0.0,a=0.5;
    for(int i=0;i<4;i++){v+=a*noise(p);p*=2.0;a*=0.5;}
    return v;
}

void main() {
    vec2 pos = vUV * 3.0;
    float t = Time * 0.8;

    vec2 uv1 = pos + vec2(t*0.3, t*0.2);
    vec2 uv2 = pos.yx - vec2(t*0.2, t*0.25);

    float pattern = pow(fbm(uv1*2.0)*0.4 + fbm(uv2*1.8)*0.35 + fbm((uv1+uv2)*1.5)*0.25, 1.8);
    float faceted = floor(pattern * 8.0) / 8.0;
    float shimmer = sin(Time*3.0 + pos.x*10.0 + pos.y*8.0)*0.5+0.5;

    vec3 finalColor = mix(FragColor.rgb*0.6, FragColor.rgb*1.4, faceted) + FragColor.rgb*shimmer*0.2;
    float alpha = FragColor.a * (0.6 + faceted*0.8);

    if (alpha <= 0.0) discard;
    OutColor = vec4(finalColor, alpha);
}
