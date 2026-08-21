#version 150

uniform vec2 Size;
uniform vec2 Location;
uniform vec4 Radius;
uniform float Time;
uniform float BeamWidth;
uniform float Brightness;
uniform float EdgeK;

out vec4 fragColor;

const float SWEEP = 2.2;
const float PAUSE = 1.1;
const float EDGEW = 2.6;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    float full = SWEEP + PAUSE;
    float t = mod(Time, full);
    if (t > SWEEP) {
        fragColor = vec4(0.0);
        return;
    }
    float progress = t / SWEEP;

    float phase = mod(floor(Time / full), 4.0);

    vec2 uv = (gl_FragCoord.xy - Location) / Size;

    float fx = (phase == 1.0 || phase == 3.0) ? (1.0 - uv.x) : uv.x;
    float fy = (phase == 2.0 || phase == 3.0) ? (1.0 - uv.y) : uv.y;
    float proj = (fx + fy) * 0.5;

    float center = (1.0 + BeamWidth) - progress * (1.0 + 2.0 * BeamWidth);
    float tBeam = (proj - center) / BeamWidth;

    float profile = 0.0;
    if (abs(tBeam) < 1.0) {
        float c = cos(tBeam * 1.5707963);
        profile = c * c;
    }
    if (profile <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 center_pos = Size * 0.5;
    vec2 half_size = Size * 0.5;
    float dist = roundedBoxSDF(gl_FragCoord.xy - Location - center_pos, half_size, Radius);
    float inside = 1.0 - smoothstep(-1.0, 1.0, dist);

    float a = Brightness * profile * inside;
    float edge = exp(-(dist * dist) / (EDGEW * EDGEW));
    a = a + Brightness * EdgeK * profile * edge;
    a = min(a, 1.0);
    fragColor = vec4(1.0, 1.0, 1.0, a);
}
