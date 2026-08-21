#version 150

uniform sampler2D Sampler0; // afterBuffer (color)
uniform sampler2D Sampler1; // maskBuffer (alpha)

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec3 rgb = texture(Sampler0, TexCoord).rgb;
    float a = texture(Sampler1, TexCoord).a;
    if (a < 0.01) discard;
    OutColor = vec4(rgb, a);
}
