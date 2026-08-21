#version 150

in vec2 FragCoord; // 0..1 within the quad
in vec4 FragColor;

uniform vec2 Size;        // quad size in pixels (square)
uniform float Thickness;  // ring thickness in px (<=0 => filled circle)
uniform float StartAngle; // radians
uniform float EndAngle;   // radians

out vec4 OutColor;

const float TAU = 6.28318530718;

void main() {
    vec2 center = Size * 0.5;
    vec2 p = FragCoord * Size - center;
    float radius = min(center.x, center.y);
    float dist = length(p);

    // Внешний край со сглаживанием
    float alpha = 1.0 - smoothstep(radius - 1.0, radius, dist);

    // Внутреннее отверстие (кольцо)
    if (Thickness > 0.0) {
        float inner = radius - Thickness;
        if (inner > 0.0) {
            alpha *= smoothstep(inner - 1.0, inner, dist);
        }
    }

    // Угловая маска для дуги
    float sweep = EndAngle - StartAngle;
    if (sweep < TAU - 0.01) {
        float ang = atan(p.y, p.x);
        float rel = mod(ang - StartAngle, TAU);
        if (rel < 0.0) rel += TAU;
        // Ширину сглаживания ограничиваем половиной дуги, иначе для коротких
        // дуг обе границы перекрываются и дуга полностью пропадает.
        float aaAng = min(1.0 / max(dist, 1.0), sweep * 0.5);
        float startEdge = smoothstep(0.0, aaAng, rel);
        float endEdge = 1.0 - smoothstep(sweep - aaAng, sweep, rel);
        alpha *= startEdge * endEdge;
    }

    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);
    if (color.a <= 0.0) {
        discard;
    }
    OutColor = color;
}
