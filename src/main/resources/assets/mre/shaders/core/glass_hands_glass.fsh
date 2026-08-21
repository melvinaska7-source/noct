#version 150

uniform sampler2D Sampler0;  // blurred background
uniform sampler2D Sampler1;  // hands texture (after render)
uniform sampler2D Sampler2;  // hand mask
uniform float mixFactor;     // 0 = full blur (glass), 1 = original hands

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    float mask = texture(Sampler2, uv).a;
    
    if (mask < 0.01) discard;
    
    // Sample blurred background with flipped Y coordinate (for reflection effect)
    vec3 blurred = texture(Sampler0, vec2(uv.x, 1.0 - uv.y)).rgb;
    
    // Sample hands texture normally
    vec3 handsColor = texture(Sampler1, uv).rgb;
    
    // Mix between blurred reflection (glass effect) and original hands color
    vec3 finalColor = mix(blurred, handsColor, mixFactor);
    
    OutColor = vec4(finalColor, mask);
}
