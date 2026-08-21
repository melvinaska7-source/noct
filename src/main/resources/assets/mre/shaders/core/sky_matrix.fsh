#version 150

uniform float iTime;
uniform vec3 uColor;
uniform int uShowStars;
uniform float uIntensity;

in vec3 vPos;
in vec4 FragColor;

out vec4 OutColor;

float hash21(vec2 n) { return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453); }

void main() {
    vec3 rd = normalize(vPos);
    vec3 rdSky = vec3(rd.x, abs(rd.y), rd.z);
    
    float theta = atan(rdSky.z, rdSky.x);
    float phi = asin(rdSky.y);
    vec2 uv = vec2(theta * 3.0, phi * 5.0);
    
    float t = iTime * 2.0;
    vec3 col = vec3(0.0);
    
    // Падающие символы
    for (float i = 0.0; i < 20.0; i++) {
        float x = floor(uv.x + i * 0.5);
        float speed = 0.5 + hash21(vec2(x, i)) * 1.5;
        float y = fract(uv.y - t * speed + hash21(vec2(x, i)) * 10.0);
        
        float trail = smoothstep(0.0, 0.1, y) * smoothstep(1.0, 0.3, y);
        float brightness = hash21(vec2(x, floor(y * 20.0 + t)));
        
        float dist = abs(fract(uv.x + i * 0.5) - 0.5) * 2.0;
        trail *= smoothstep(1.0, 0.0, dist);
        
        col += uColor * trail * brightness * uIntensity;
    }
    
    // Яркие головы символов
    for (float i = 0.0; i < 15.0; i++) {
        float x = floor(uv.x + i * 0.7);
        float speed = 0.5 + hash21(vec2(x + 100.0, i)) * 1.5;
        float y = fract(uv.y - t * speed + hash21(vec2(x + 100.0, i)) * 10.0);
        
        float head = smoothstep(0.05, 0.0, abs(y - 0.95));
        float dist = abs(fract(uv.x + i * 0.7) - 0.5) * 2.0;
        head *= smoothstep(1.0, 0.0, dist);
        
        col += vec3(1.0) * head * 2.0;
    }
    
    col *= 1.5;
    OutColor = vec4(col, 1.0);
}
