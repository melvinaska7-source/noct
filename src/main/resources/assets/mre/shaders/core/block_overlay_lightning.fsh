#version 150

uniform float Time;

in vec4 FragColor;
in vec2 vUV;

out vec4 OutColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1,0)), f.x),
        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x),
        f.y
    );
}

// Fractal noise for jagged bolt shape
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.0 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

// Single lightning bolt along axis, returns intensity
float bolt(vec2 uv, float t, float seed) {
    // displace the bolt path with fbm
    float disp = fbm(vec2(uv.y * 3.0 + seed, t * 2.0 + seed)) - 0.5;
    float disp2 = fbm(vec2(uv.y * 6.0 + seed + 5.3, t * 3.0 + seed)) - 0.5;

    // bolt center x with fractal displacement
    float boltX = 0.5 + disp * 0.35 + disp2 * 0.15;

    float dist = abs(uv.x - boltX);

    // core bright line
    float core = smoothstep(0.018, 0.0, dist);
    // outer glow
    float glow = smoothstep(0.12, 0.0, dist) * 0.4;

    // flicker: bolt appears/disappears rapidly
    float flicker = step(0.3, fract(hash(vec2(seed, floor(t * 8.0))) + t * 0.5));
    // secondary branch
    float branchDisp = fbm(vec2(uv.y * 5.0 + seed + 2.1, t * 2.5)) - 0.5;
    float branchX = boltX + branchDisp * 0.2;
    float branchDist = abs(uv.x - branchX);
    float branch = smoothstep(0.008, 0.0, branchDist) * step(0.4, uv.y) * step(uv.y, 0.85);

    return (core + glow + branch * 0.6) * flicker;
}

void main() {
    float t = Time;

    // run 3 bolts with different seeds and orientations
    float b1 = bolt(vUV, t, 0.0);
    float b2 = bolt(vec2(1.0 - vUV.x, vUV.y), t + 0.3, 3.7);
    float b3 = bolt(vec2(vUV.y, vUV.x), t + 0.7, 7.1);

    float intensity = clamp(b1 + b2 * 0.7 + b3 * 0.5, 0.0, 1.0);

    // color: white core fading to theme color
    vec3 boltColor = mix(FragColor.rgb, vec3(1.0), pow(intensity, 0.4));
    // add slight blue-white tint on bright spots
    boltColor = mix(boltColor, vec3(0.8, 0.9, 1.0), pow(intensity, 2.0) * 0.5);

    float alpha = FragColor.a * intensity * 2.5;
    alpha = clamp(alpha, 0.0, 1.0);

    if (alpha < 0.01) discard;
    OutColor = vec4(boltColor, alpha);
}
