#version 150

uniform sampler2D Sampler0; // trail buffer
uniform sampler2D Sampler1; // mask buffer
uniform vec3 color;
uniform vec3 color2;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 trail = texture(Sampler0, TexCoord);

    // Don't draw trail on top of the hand itself
    float handMask = texture(Sampler1, TexCoord).r;
    if (handMask > 0.5) discard;

    if (trail.a < 0.005) discard;

    // Tint trail with gradient from color settings
    vec3 tinted = mix(trail.rgb, mix(color, color2, TexCoord.y), 0.25);
    OutColor = vec4(tinted, trail.a);
}
