#version 150

uniform sampler2D Sampler0;
uniform float u_Saturation;

in vec4 FragColor;
in vec2 TexCoord;

out vec4 OutColor;

void main() {
    vec4 color = texture(Sampler0, TexCoord);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 result = mix(vec3(gray), color.rgb, u_Saturation);
    OutColor = vec4(result, color.a);
}
