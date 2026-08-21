#version 150

uniform sampler2D Sampler0;  // bloomed/blurred mask
uniform sampler2D Sampler1;  // original mask
uniform vec3 glowColor1;     // first glow color
uniform vec3 glowColor2;     // second glow color  
uniform float exposure;      // glow brightness

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    
    // Sample bloom intensity and original mask
    float bloomIntensity = texture(Sampler0, uv).a;
    float originalMask = texture(Sampler1, uv).a;
    
    // Render glow ONLY outside the original mask (outline effect)
    // This creates an outline around each pixel of the hand/item
    float glowMask = bloomIntensity * (1.0 - originalMask);
    
    if (glowMask < 0.01) discard;
    
    // Gradient between two colors based on UV.y
    vec3 glowColor = mix(glowColor1, glowColor2, uv.y);
    
    // Apply exposure/brightness
    vec3 finalGlow = glowColor * glowMask * exposure;
    
    OutColor = vec4(finalGlow, glowMask);
}
