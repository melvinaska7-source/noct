#version 150

uniform sampler2D Sampler0;     // bloom texture (color+alpha, blurred)
uniform vec3 glowColor1;
uniform vec3 glowColor2;
uniform float exposure;
uniform float autoColor;
uniform float saturation;
uniform float rainbow;
uniform float rainbowTime;
uniform float rainbowSpeed;
uniform float rainbowScale;
uniform float screenH;

in vec2 TexCoord;
out vec4 OutColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 u = TexCoord;
    vec4 b = texture(Sampler0, u);
    float intensity = clamp(b.a * exposure, 0.0, 1.0);
    if (intensity <= 0.001) discard;

    vec3 col;
    if (rainbow > 0.5) {
        float yPos = u.y * screenH;
        float hue = fract(rainbowTime * rainbowSpeed * 0.15 - yPos / (screenH / rainbowScale));
        col = hsv2rgb(vec3(hue, 1.0, 1.0));
    } else if (autoColor > 0.5) {
        vec3 c = b.rgb / max(b.a, 0.001);
        float m = max(c.r, max(c.g, c.b));
        if (m > 0.001) c /= m;
        float l = dot(c, vec3(0.299, 0.587, 0.114));
        col = clamp(mix(vec3(l), c, saturation), 0.0, 1.0);
    } else {
        col = mix(glowColor1, glowColor2, u.y);
    }

    OutColor = vec4(col * intensity, intensity);
}
