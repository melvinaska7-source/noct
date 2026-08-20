#version 150

in vec2 FragCoord;
in vec4 FragColor;

out vec4 fragColor;

uniform float time;

void main() {
    vec2 uv = (FragCoord - vec2(0.5)) * 2.0;

    float dist = length(uv);

    float horizonRadius = 0.35;

    float blackHole = smoothstep(horizonRadius - 0.02, horizonRadius + 0.01, dist);

    float angle = time * 2.5;
    angle += 1.3 / (dist + 0.05);

    mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    vec2 rotatedUv = rot * uv;

    float spiral = abs(sin(rotatedUv.x * 14.0 + rotatedUv.y * 4.0) * 0.5 + 0.5);

    float disk = 0.0;
    if (dist > horizonRadius) {
        float falloff = pow(horizonRadius / dist, 2.2);
        disk = falloff * (0.55 + 0.45 * spiral);
    }

    vec3 diskColor = vec3(1.0, 0.4, 0.08) * (1.0 - smoothstep(0.4, 0.95, dist)) +
                     vec3(0.85, 0.08, 0.6) * smoothstep(0.25, 0.95, dist);

    float innerGlow = smoothstep(horizonRadius + 0.18, horizonRadius, dist) * 0.95;
    vec3 glowColor = vec3(1.0, 0.65, 0.15);

    vec3 diskRGB = (diskColor * disk + glowColor * innerGlow);

    vec3 finalDiskRGB = diskRGB * FragColor.rgb;

    vec3 color = finalDiskRGB * blackHole;

    float alpha = 0.0;
    if (dist <= horizonRadius) {
        alpha = 1.0;
    } else {
        alpha = smoothstep(1.0, horizonRadius, dist) * (disk + innerGlow * 0.5);
    }

    alpha = clamp(alpha, 0.0, 1.0) * FragColor.a;

    fragColor = vec4(color, alpha);
}