#version 150

uniform sampler2D Sampler0;
uniform vec2 texelSize;
uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float speed;
uniform float scale;
uniform float density;
uniform float outline;
uniform float glow;
uniform float fill;
uniform float alpha;

in vec2 TexCoord;
out vec4 OutColor;

// Hash function
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Smooth noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

// Fractal Brownian Motion
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    
    for (int i = 0; i < 6; i++) {
        value += amplitude * noise(p);
        p *= 2.07;
        amplitude *= 0.5;
    }
    
    return value;
}

void main() {
    vec2 uv = TexCoord;
    vec4 baseMask = texture(Sampler0, uv);
    
    // Расширяем маску вверх для создания дыма
    float extendedMask = baseMask.a;
    
    // Проверяем пиксели ниже
    const int samples = 20;
    for (int i = 1; i <= samples; i++) {
        float dy = float(i) * 0.015; // Высота дыма
        vec2 checkUV = uv + vec2(0.0, dy);
        
        if (checkUV.y <= 1.0) {
            vec4 check = texture(Sampler0, checkUV);
            if (check.a > 0.1) {
                float fade = 1.0 - (dy / (float(samples) * 0.015));
                fade = pow(fade, 2.0); // Быстрее затухает
                extendedMask = max(extendedMask, check.a * fade);
            }
        }
    }
    
    if (extendedMask < 0.001) {
        discard;
    }
    
    // Дымный эффект
    vec2 noiseUV = uv * scale;
    float t = time * speed;
    
    // Движение вверх и волны
    noiseUV.y += t * 0.3;
    noiseUV.x += sin(noiseUV.y * 2.0 + t) * 0.15;
    
    // Многослойный шум
    float smoke = fbm(noiseUV);
    float smoke2 = fbm(noiseUV * 1.3 + vec2(t * 0.2, -t * 0.15));
    
    float smokePattern = mix(smoke, smoke2, 0.5);
    smokePattern = pow(smokePattern, 1.5 - density * 0.5);
    
    // Края
    float edge = noise(uv * 25.0 + t * 0.5);
    edge = edge * 0.3 + 0.7;
    
    // Финальная альфа
    float smokeAlpha = extendedMask * smokePattern * edge * density;
    
    // Свечение на краях
    float glowEdge = smoothstep(0.0, outline * 0.1, extendedMask);
    glowEdge -= smoothstep(outline * 0.1, outline * 0.25, extendedMask);
    glowEdge = max(0.0, glowEdge) * glow * 1.5;
    
    // Цвет с градиентом
    vec3 smokeColor = mix(color, color2, uv.y * 0.6 + smokePattern * 0.4);
    smokeColor *= (0.6 + smokePattern * 0.4);
    
    // Осветляем верх
    float heightFade = smoothstep(0.0, 1.0, uv.y);
    smokeColor = mix(smokeColor, vec3(1.0), heightFade * smokePattern * 0.2);
    
    // Комбинируем
    float finalAlpha = clamp(smokeAlpha * fill + glowEdge, 0.0, 1.0) * alpha;
    
    // Пульсация
    finalAlpha *= (sin(t * 1.5) * 0.1 + 0.9);
    
    if (finalAlpha < 0.005) {
        discard;
    }
    
    OutColor = vec4(smokeColor, finalAlpha);
}
