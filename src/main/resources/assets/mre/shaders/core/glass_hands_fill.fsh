#version 150

uniform sampler2D Sampler0;     // original texture (afterBuffer)
uniform sampler2D Sampler1;     // mask texture
uniform vec3 fillColor;
uniform float fillAlpha;
uniform float keepShading;
uniform float shadingStrength;
uniform float rainbow;
uniform float rainbowTime;
uniform float rainbowSpeed;
uniform float rainbowScale;
uniform float screenH;

in vec2 TexCoord;
out vec4 OutColor;

vec3 hue2rgb(float h) {
    vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
    return clamp(p - 1.0, 0.0, 1.0);
}

void main() {
    vec2 uv = TexCoord;
    vec4 s = texture(Sampler0, uv);
    float mask = texture(Sampler1, uv).a;
    
    if (mask < 0.01) discard;
    
    vec3 f;
    if (rainbow > 0.5) {
        float yN = gl_FragCoord.y / max(screenH, 1.0);
        float h = fract(yN * rainbowScale + rainbowTime * rainbowSpeed);
        f = hue2rgb(h);
    } else {
        f = fillColor;
    }
    
    if (keepShading > 0.5) {
        f *= mix(1.0, dot(s.rgb, vec3(0.299, 0.587, 0.114)), shadingStrength);
    }
    
    OutColor = vec4(mix(s.rgb, f, fillAlpha), s.a);
}
