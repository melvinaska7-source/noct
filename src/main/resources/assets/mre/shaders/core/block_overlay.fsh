#version 150

uniform float Time;

in vec4 FragColor;
in vec2 vUV;

out vec4 OutColor;

#define MAX_ITER 5

void main() {
    float t = Time * 0.5 + 23.0;

    vec2 uv = vUV * 2.0;
    vec2 p = mod(uv * 6.28318, 6.28318) - 250.0;
    vec2 i = p;
    float c = 1.0;
    float inten = 0.005;

    for (int n = 0; n < MAX_ITER; n++) {
        float tn = t * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(tn - i.x) + sin(tn + i.y),
                     sin(tn - i.y) + cos(tn + i.x));
        c += 1.0 / length(vec2(
            p.x / (sin(i.x + tn) / inten),
            p.y / (cos(i.y + tn) / inten)
        ));
    }

    c /= float(MAX_ITER);
    c = 1.17 - pow(c, 1.4);
    float noise = pow(abs(c), 8.0);

    vec3 finalColor = mix(FragColor.rgb, vec3(1.0), noise * 0.7);
    float alpha = FragColor.a * (0.25 + noise * 1.5);

    if (alpha <= 0.0) discard;
    OutColor = vec4(finalColor, alpha);
}
