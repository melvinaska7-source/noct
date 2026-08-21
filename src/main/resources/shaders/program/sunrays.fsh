#version 150

uniform float u_Time;
uniform vec2 u_Resolution;

in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;
    
    // Лучи идут сверху вниз
    float rayCount = 12.0;
    float angle = atan(uv.x - 0.5, uv.y) + u_Time * 0.1;
    float rayPattern = sin(angle * rayCount) * 0.5 + 0.5;
    
    // Затухание от верха к низу
    float fade = smoothstep(0.0, 0.4, uv.y) * smoothstep(1.0, 0.6, uv.y);
    
    // Мягкое свечение
    float glow = rayPattern * fade * 0.2;
    
    // Золотистый цвет лучей
    vec3 rayColor = vec3(1.0, 0.9, 0.7);
    
    fragColor = vec4(rayColor * glow, glow) * vertexColor;
}
