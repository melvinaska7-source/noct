#version 150

uniform sampler2D Sampler0; // blurred mask
uniform sampler2D Sampler1; // sharp mask
uniform vec3 color;
uniform vec3 color2;
uniform float exposure;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    // Use only alpha channel of bloom — ignore RGB to avoid white spread
    float bloom = texture(Sampler0, uv).a;
    float mask  = texture(Sampler1, uv).a;
    // Glow only outside the hand shape
    float outer = max(bloom - mask, 0.0);
    vec3 grad = mix(color, color2, uv.y);
    float intensity = clamp(outer * exposure, 0.0, 1.0);
    if (intensity <= 0.001) discard;
    OutColor = vec4(grad, intensity);
}
