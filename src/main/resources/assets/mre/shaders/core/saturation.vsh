#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 u_Resolution;

out vec4 FragColor;
out vec2 TexCoord;

void main() {
    FragColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    TexCoord = vec2(Position.x / u_Resolution.x, 1.0 - Position.y / u_Resolution.y);
}
