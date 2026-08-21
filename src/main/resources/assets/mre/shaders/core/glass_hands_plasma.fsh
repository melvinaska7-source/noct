﻿#version 150

uniform sampler2D Sampler0;
uniform float iTime;
uniform vec3 uColor;
uniform float plasmaScale;
uniform float uShowStars;

in vec2 TexCoord;
out vec4 OutColor;

float hash21(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

vec3 stars(vec2 uv) {
    if (uShowStars < 0.5) return vec3(0.0);
    vec3 c = vec3(0.0);
    float res = 300.0;
    for (int i = 0; i < 2; i++) {
        vec2 q = fract(uv * res) - 0.5;
        vec2 id = floor(uv * res);
        float rn = hash21(id);
        float c2 = 1.0 - smoothstep(0.0, 0.6, length(q));
        c2 *= step(rn, 0.005 + float(i) * 0.002);
        c += c2 * (mix(vec3(1.0, 0.49, 0.1), vec3(0.75, 0.9, 1.0), hash21(id + 100.0)) * 0.1 + 0.9);
        res *= 1.3;
    }
    return c * c * 0.5;
}

void main() {
    vec2 uv = TexCoord;
    vec4 mask = texture(Sampler0, uv);
    if (mask.a < 0.01) discard;
    vec2 p = uv * plasmaScale;
    float plasma = 0.0;
    plasma += sin(p.x * 10.0 + iTime);
    plasma += sin(p.y * 10.0 + iTime * 1.3);
    plasma += sin((p.x + p.y) * 5.0 + iTime * 0.7);
    plasma += sin(length(p * 5.0) + iTime * 1.5);
    plasma *= 0.25;
    vec3 col1 = uColor;
    vec3 col2 = vec3(uColor.y, uColor.z, uColor.x);
    vec3 col = mix(col1, col2, plasma * 0.5 + 0.5);
    col += stars(uv);
    col *= 1.2;
    OutColor = vec4(col, mask.a);
}
