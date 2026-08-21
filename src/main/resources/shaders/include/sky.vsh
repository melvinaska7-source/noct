#version 150

in vec3 Position;

out vec2 vUv;

void main() {
    // Position is in NDC (-1 to 1), output directly to clip space
    gl_Position = vec4(Position.xy, -1.0, 1.0);
    vUv = Position.xy * 0.5 + 0.5;
}
