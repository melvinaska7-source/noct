#version 150

uniform sampler2D Sampler0;     // mask texture
uniform sampler2D Sampler1;     // bloom texture (for auto color)
uniform vec2 texelSize;
uniform float width;
uniform float alpha;
uniform float rainbowTime;
uniform float rainbowSpeed;
uniform float rainbowScale;
uniform float screenH;
uniform float saturation;
uniform float colorMode;        // 0=solid, 1=rainbow, 2=auto
uniform vec3 solidColor;

in vec2 TexCoord;
out vec4 OutColor;

vec3 hue2rgb(float h) {
    vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
    return clamp(p - 1.0, 0.0, 1.0);
}

void main() {
    vec2 uv = TexCoord;
    vec4 c = texture(Sampler0, uv);
    if (c.a > 0.01) discard;
    
    float maxA = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            if (x == 0 && y == 0) continue;
            vec2 d = vec2(float(x), float(y)) * texelSize * width;
            maxA = max(maxA, texture(Sampler0, uv + d).a);
        }
    }
    if (maxA < 0.01) discard;
    
    vec3 col;
    if (colorMode > 1.5) {
        vec4 b = texture(Sampler1, uv);
        vec3 cb = b.rgb / max(b.a, 0.001);
        float m = max(cb.r, max(cb.g, cb.b));
        if (m > 0.001) cb /= m;
        float l = dot(cb, vec3(0.299, 0.587, 0.114));
        col = clamp(mix(vec3(l), cb, saturation), 0.0, 1.0);
    } else if (colorMode > 0.5) {
        float yN = gl_FragCoord.y / max(screenH, 1.0);
        float h = fract(yN * rainbowScale + rainbowTime * rainbowSpeed);
        col = hue2rgb(h);
    } else {
        col = solidColor;
    }
    
    OutColor = vec4(col, maxA * alpha);
}
