#version 150

uniform sampler2D Sampler0;     // current frame mask
uniform sampler2D Sampler1;     // blurred glow texture
uniform sampler2D Sampler2;     // after buffer (with item rendered)
uniform vec3 color;
uniform vec3 color2;
uniform vec2 texelSize;
uniform float trailLength;
uniform float trailIntensity;
uniform float glowSize;
uniform float time;
uniform float useItemColor;
uniform vec2 cameraMotion;

in vec2 TexCoord;
out vec4 OutColor;

float sampleMask(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 0.0;
    return texture(Sampler0, uv).a;
}

vec3 extractItemColor(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return vec3(1.0);
    vec4 itemPixel = texture(Sampler2, uv);
    vec3 c = itemPixel.rgb;
    
    float m = max(c.r, max(c.g, c.b));
    if (m > 0.001) {
        c = c / m * 1.2;
    }
    
    if (length(c) < 0.1) return vec3(1.0);
    
    return clamp(c, 0.0, 1.0);
}

void main() {
    vec2 uv = TexCoord;
    float center = sampleMask(uv);
    
    float trailMask = 0.0;
    vec3 trailColor = vec3(0.0);
    int steps = 10;
    
    // Simple smooth animated direction
    float angle = time * 1.2;
    vec2 direction = vec2(cos(angle), sin(angle));
    
    for (int i = 1; i <= steps; i++) {
        float t = float(i) / float(steps);
        
        // Simple offset
        vec2 offset = direction * texelSize * trailLength * t;
        
        float ghostMask = sampleMask(uv - offset);
        vec3 itemColor = extractItemColor(uv - offset);
        
        // Simple fade
        float fade = 1.0 - t * 0.6;
        
        trailMask += ghostMask * fade;
        trailColor += itemColor * ghostMask * fade;
    }
    
    trailMask /= float(steps);
    trailColor /= float(steps);
    
    // Only show trail where there's no current hand/item
    trailMask *= (1.0 - center);
    
    // Get blurred glow
    float bloom = texture(Sampler1, uv).a;
    bloom *= (1.0 - center);
    
    // Color gradient
    vec3 grad = mix(color, color2, uv.y);
    
    // Choose between item color and gradient
    vec3 finalTrailColor = mix(grad, trailColor, useItemColor);
    
    // Combine trail with glow
    float trailAlpha = trailMask * trailIntensity * 2.0;
    float glowAlpha = bloom * trailIntensity * 1.2;
    
    float totalAlpha = clamp(trailAlpha + glowAlpha, 0.0, 1.0);
    
    vec3 finalColor = finalTrailColor * (1.0 + glowAlpha * 1.5);
    
    if (totalAlpha <= 0.001) discard;
    
    OutColor = vec4(finalColor, totalAlpha);
}
