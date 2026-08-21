#version 150

uniform sampler2D Sampler0; // mask
uniform vec3 color;
uniform vec3 color2;
uniform vec2 texelSize;
uniform float time;
uniform float speed;
uniform float scale;
uniform float outline;
uniform float glow;
uniform float fill;
uniform float alpha;

in vec2 TexCoord;
out vec4 OutColor;

#define MAX_ITER 4

float sampleMask(vec2 uv) {
    return texture(Sampler0, uv).a;
}

void main() {
    vec2 uv = TexCoord;
    float center = sampleMask(uv);

    // Outline: sample neighbors
    float maxNeighbor = 0.0;
    float step = texelSize.x * outline * 2.0;
    maxNeighbor = max(maxNeighbor, sampleMask(uv + vec2(step, 0.0)));
    maxNeighbor = max(maxNeighbor, sampleMask(uv - vec2(step, 0.0)));
    maxNeighbor = max(maxNeighbor, sampleMask(uv + vec2(0.0, step)));
    maxNeighbor = max(maxNeighbor, sampleMask(uv - vec2(0.0, step)));
    float outlineMask = clamp(maxNeighbor - center, 0.0, 1.0);

    // Animated plasma on the outline
    float t = time * speed * 0.5 + 23.0;
    vec2 p = mod(uv * scale * 6.28318, 6.28318) - 250.0;
    vec2 i = p;
    float c = 1.0;
    float inten = 0.005;
    for (int n = 0; n < MAX_ITER; n++) {
        float tn = t * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(tn - i.x) + sin(tn + i.y), sin(tn - i.y) + cos(tn + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + tn) / inten), p.y / (cos(i.y + tn) / inten)));
    }
    c /= float(MAX_ITER);
    c = 1.17 - pow(c, 1.4);
    float noise = pow(abs(c), 8.0);

    vec3 grad = mix(color, color2, uv.y + noise * 0.3);

    // Combine outline + fill
    float outlineAlpha = outlineMask * clamp(glow * (0.6 + noise * 0.8), 0.0, 1.0);
    float fillAlpha    = center * fill * alpha;

    float totalAlpha = clamp(outlineAlpha + fillAlpha, 0.0, 1.0);
    if (totalAlpha <= 0.001) discard;

    OutColor = vec4(grad, totalAlpha);
}
