#version 150

uniform float Time;

in vec2 TexCoord;
in vec4 FragColor;

out vec4 OutColor;

void main() {
    vec2 uv = TexCoord * 2.0 - 1.0;
    float t = Time * 0.6;

    float d = length(uv);

    float w1 = sin(d * 12.0 - t * 3.0);
    float w2 = sin(uv.x * 8.0 + t * 2.0) * sin(uv.y * 8.0 - t * 1.5);
    float w3 = sin((uv.x + uv.y) * 6.0 + t * 2.5);
    float w4 = sin(d * 8.0 + uv.x * 5.0 - t * 2.0);

    float wave = (w1 + w2 + w3 + w4) * 0.25;
    wave = wave * 0.5 + 0.5;

    float noise = pow(abs(wave), 1.8);

    vec3 finalColor = mix(FragColor.rgb, vec3(1.0), noise * 0.6);
    float alpha = FragColor.a * (0.2 + noise * 1.4);

    if (alpha <= 0.0) discard;
    OutColor = vec4(finalColor, alpha);
}
