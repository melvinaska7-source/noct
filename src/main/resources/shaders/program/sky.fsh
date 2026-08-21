#version 150

uniform float GameTime;
uniform float AnimTime;
uniform vec4  FogColor;
uniform float FogStart;
uniform float FogEnd;

uniform vec3  SkyZenith;
uniform vec3  SkyHorizon;
uniform vec3  NebColor1;
uniform vec3  NebColor2;
uniform float NebIntensity;
uniform vec3  StarColor;
uniform float SkyMode;
uniform float StarDensity;
uniform float NebulaStrength;
uniform mat4 InvProjMat;
uniform vec4 CameraRotation;

in  vec2 vUv;
out vec4 fragColor;

vec3 rotateByQuat(vec3 v, vec4 q) {
    return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
}

vec3 getSkyDir(vec2 uv) {
    vec2 clip = uv * 2.0 - 1.0;
    vec4 rayView = InvProjMat * vec4(clip, -1.0, 1.0);
    vec3 dir = normalize(vec3(rayView.xy, -1.0));
    return normalize(rotateByQuat(dir, CameraRotation));
}

float hash(vec2 p) {
    uvec2 q = uvec2(ivec2(p)) * uvec2(1597334673u, 3812015801u);
    return float((q.x ^ q.y) * 1597334673u) * (1.0 / 4294967296.0);
}

float hash3(vec3 p) {
    uvec3 q = uvec3(ivec3(p)) * uvec3(1597334673u, 3812015801u, 2798796415u);
    return float((q.x ^ q.y ^ q.z) * 1597334673u) * (1.0 / 4294967296.0);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float c000 = hash3(i);
    float c100 = hash3(i + vec3(1, 0, 0));
    float c010 = hash3(i + vec3(0, 1, 0));
    float c110 = hash3(i + vec3(1, 1, 0));
    float c001 = hash3(i + vec3(0, 0, 1));
    float c101 = hash3(i + vec3(1, 0, 1));
    float c011 = hash3(i + vec3(0, 1, 1));
    float c111 = hash3(i + vec3(1, 1, 1));

    vec2 x0 = mix(vec2(c000, c001), vec2(c100, c101), u.x);
    vec2 x1 = mix(vec2(c010, c011), vec2(c110, c111), u.x);
    vec2 y = mix(x0, x1, u.y);
    return mix(y.x, y.y, u.z);
}

float fbm(vec3 p) {
    const mat3 rot = mat3(0.8, -0.6, 0.0, 0.6, 0.8, 0.0, 0.0, 0.0, 1.0);
    const vec3 off = vec3(1.7, 9.2, 5.3);

    float v = 0.5000 * vnoise(p); p = rot * p * 2.1 + off;
    v += 0.2500 * vnoise(p); p = rot * p * 2.1 + off;
    v += 0.1250 * vnoise(p); p = rot * p * 2.1 + off;
    v += 0.0625 * vnoise(p);
    return v * (1.0 / 0.9375);
}

float starLayer(vec3 dir, float scale, float threshold, float seed) {
    vec2 uv = vec2(atan(dir.z, dir.x) * (1.0 / 6.2832) + 0.5, dir.y * 0.5 + 0.5) * scale;
    vec2 id = floor(uv);
    float h = hash(id + seed);
    if (h < threshold) return 0.0;

    vec2 gv = fract(uv) - 0.5;
    vec2 pos = (vec2(hash(id + (seed + 13.7)), hash(id + (seed + 71.3))) - 0.5) * 0.7;
    return smoothstep(0.052, 0.0, length(gv - pos)) * (0.72 + 0.28 * sin(AnimTime * 18.0 + h * 400.0));
}

vec3 starColor(vec3 dir, float aboveH) {
    float s1 = starLayer(dir, 150.0, 0.964, 0.0);
    float s2 = starLayer(dir, 92.0, 0.977, 17.3);
    float s3 = starLayer(dir, 48.0, 0.988, 43.7);
    float s4 = starLayer(dir, 24.0, 0.993, 91.1);

    float hv = hash(floor(vec2(atan(dir.z, dir.x), dir.y) * 35.0));
    vec3 cSmall = mix(vec3(0.62, 0.72, 1.0), vec3(0.95, 0.88, 1.0), hv);
    vec3 stars = cSmall * (s1 * 0.55 + s2 * 0.85)
               + StarColor * (s3 * 1.35 + s4 * 2.05)
               + StarColor * StarColor * ((s3 * s3) * 0.6 + (s4 * s4) * 1.45) * 0.55;
    return stars * aboveH;
}


vec3 baseSky(vec3 dir, float up, float dn) {
    vec3 sky = mix(SkyHorizon, SkyZenith, pow(up, 0.62));
    sky = mix(sky, SkyZenith * 0.34, pow(dn, 1.45));
    sky = mix(sky, SkyHorizon, pow(1.0 - up, 3.0) * clamp(1.0 - dn * 4.0, 0.0, 1.0));
    return sky;
}

float energyField(vec3 p, float t) {
    vec3 q = p;
    q += 0.18 * sin(q.yzx * 2.6 + vec3(t * 0.85, -t * 1.18, t * 0.72));
    q += 0.10 * sin(q.zxy * 4.0 + vec3(-t * 1.55, t * 0.92, t * 1.28));
    q += 0.05 * sin(q.xyz * 7.0 + vec3(t * 2.05, t * 1.68, -t * 1.86));

    float a = sin(q.x * 6.2 + sin(q.y * 3.2 + t * 1.65) * 1.05 + t * 0.62);
    float b = sin(q.y * 7.4 + sin(q.z * 3.6 - t * 1.90) * 0.95 - t * 0.48);
    float c = sin(q.z * 5.6 + sin(q.x * 3.8 + t * 2.25) * 1.05 + t * 0.36);
    float web = abs(a + b + c) * 0.3333;

    float fineLines = smoothstep(0.125, 0.018, web);
    float glow = smoothstep(0.42, 0.050, web);
    float travelling = 0.45 + 0.55 * sin(t * 7.0 + q.x * 9.0 - q.y * 5.0 + q.z * 6.0);
    float spark = pow(0.5 + 0.5 * sin(t * 10.0 + q.x * 8.0 + q.y * 5.0 - q.z * 6.0), 5.0);
    return fineLines * (0.35 + travelling * 0.75 + spark * 0.45) * 0.70 + glow * 0.15;
}

vec3 energySky(vec3 dir, float up, float aboveH) {
    float t = AnimTime;
    vec3 p = normalize(dir) * 2.85;

    float cover = 0.48 + 0.52 * smoothstep(-0.74, 0.48, dir.y);
    float fieldA = energyField(p + vec3(0.0, 1.1, 2.7), t);
    float fieldB = energyField(p * 1.65 + vec3(4.2, -1.6, 0.8), t * 1.15 + 3.0);
    float mist = fbm(p * 1.35 + vec3(sin(t * 0.70), cos(t * 0.56), sin(t * 0.44)) * 0.18);

    vec3 deep = SkyZenith * 0.3;
    vec3 mid  = NebColor1 * 0.6;
    vec3 cyan = NebColor2;
    vec3 pale = StarColor;

    vec3 sky = mix(deep, mid, 0.28 + up * 0.42 + mist * 0.25);
    sky += cyan * fieldA * cover * 0.46;
    sky += pale * pow(fieldA, 2.0) * cover * 0.18;
    sky += NebColor1 * 0.7 * fieldB * cover * 0.22;

    float waveGlow = 0.5 + 0.5 * sin(dir.x * 12.0 + dir.y * 15.0 + dir.z * 9.0 + t * 7.0);
    float slowPulse = 0.82 + 0.18 * sin(t * 2.2);
    sky += cyan * waveGlow * mist * cover * 0.16;
    sky *= slowPulse;
    sky += NebColor2 * 0.5 * pow(1.0 - abs(dir.y), 3.0) * 0.12;
    return sky;
}

vec3 nebulaSky(vec3 dir, float up, float aboveH) {
    vec3 sky = mix(vec3(0.05, 0.08, 0.18), baseSky(dir, up, clamp(-dir.y, 0.0, 1.0)), 0.75);

    float t = AnimTime;
    float fixedA = fbm(dir * 2.15 + vec3(0.1, 3.2, 1.7));
    float fixedB = fbm(dir * 4.85 + vec3(1.3, 2.7, 5.1));
    float fixedC = fbm(dir * 8.00 + vec3(4.1, -2.0, 1.2));
    float flowA = 0.5 + 0.5 * sin(dir.x * 7.0 + dir.y * 5.2 - dir.z * 3.6 + fixedA * 5.2 + t * 2.8);
    float flowB = 0.5 + 0.5 * sin(dir.z * 8.2 - dir.y * 6.6 + dir.x * 2.8 + fixedB * 4.0 - t * 3.4);

    float neb1 = smoothstep(0.22, 0.78, fixedA * 0.70 + flowA * 0.30);
    float neb2 = smoothstep(0.30, 0.82, fixedB * 0.68 + flowB * 0.32);
    float velvet = smoothstep(0.36, 0.84, fixedC * (0.70 + 0.30 * sin(t * 2.2 + fixedA * 6.0)));
    float wave = 0.5 + 0.5 * sin(dir.x * 12.0 + dir.y * 11.0 + dir.z * 4.5 + fixedA * 7.0 + t * 4.6);
    float wave2 = 0.5 + 0.5 * sin(dir.z * 13.0 + dir.y * 6.0 - dir.x * 5.0 + fixedC * 9.0 - t * 5.2);
    float pulse = 0.55 + 0.45 * sin(t * 3.3 + fixedB * 5.0);

    sky += NebColor1 * (neb1 * aboveH * (NebIntensity * 1.25) * (0.40 + wave * 0.90));
    sky += NebColor2 * (neb2 * aboveH * (NebIntensity * 0.96) * (0.58 + wave2 * 0.70));
    sky += mix(NebColor1, StarColor, 0.45) * (velvet * aboveH * 0.62 * pulse);
    return sky;
}

vec3 cosmicVeilSky(vec3 dir, float up, float aboveH) {
    float t = AnimTime;
    vec2 uv = vec2(atan(dir.z, dir.x) / 6.2832 + 0.5, dir.y * 0.5 + 0.5);

    vec3 sky = mix(SkyZenith * 0.5, SkyHorizon * 0.4, pow(up, 0.7));

    float neb = fbm(dir * 2.4 + vec3(1.3, 0.7, 2.1));
    float nebFlow = fbm(dir * 3.8 + vec3(sin(t * 0.15) * 0.3, cos(t * 0.12) * 0.2, t * 0.08));
    float nebMask = smoothstep(0.28, 0.72, neb * 0.65 + nebFlow * 0.35);

    vec3 nebColor = mix(NebColor1, NebColor2, nebFlow);
    sky += nebColor * nebMask * aboveH * 0.55;

    float edgeNoise = fbm(dir * 5.2 + vec3(3.1, -1.4, 0.9));
    float edgeMask = smoothstep(0.55, 0.85, edgeNoise) * smoothstep(0.15, 0.50, neb);
    sky += StarColor * edgeMask * aboveH * 0.30;

    float veilY = dir.y - 0.25;
    float ribbon = exp(-veilY * veilY * 18.0);
    float veilWave = 0.5 + 0.5 * sin(dir.x * 6.0 + dir.z * 4.0 + t * 0.8 + fbm(dir * 3.0 + vec3(t * 0.2, 0.0, 0.0)) * 4.0);
    float veilIntensity = ribbon * veilWave * 0.6;
    vec3 veilColor = mix(StarColor, NebColor1, veilWave);
    sky += veilColor * veilIntensity * aboveH;

    vec3 moonDir = normalize(vec3(0.35, 0.55, -0.75));
    float moonDist = acos(clamp(dot(dir, moonDir), -1.0, 1.0));
    float moonRadius = 0.035;
    float moonMask = smoothstep(moonRadius, moonRadius - 0.003, moonDist);

    vec3 lightDir = normalize(vec3(-0.6, 0.3, 0.4));
    float terminator = dot(dir, lightDir);
    float crescentMask = smoothstep(-0.02, 0.12, terminator);
    float litSurface = moonMask * crescentMask;

    float darkSide = moonMask * (1.0 - crescentMask);
    vec3 moonColor = vec3(0.92, 0.90, 0.82) * litSurface
                   + vec3(0.06, 0.08, 0.14) * darkSide;
    sky += moonColor;

    float moonGlow = smoothstep(0.08, 0.0, moonDist - moonRadius) * 0.15;
    sky += vec3(0.7, 0.7, 0.85) * moonGlow;

    vec3 starDir = normalize(vec3(-0.55, 0.30, 0.60));
    float starDist = acos(clamp(dot(dir, starDir), -1.0, 1.0));
    float starPoint = smoothstep(0.006, 0.0, starDist);

    vec3 tangent1 = normalize(cross(starDir, vec3(0.0, 1.0, 0.0)));
    vec3 tangent2 = normalize(cross(starDir, tangent1));
    float spike1 = abs(dot(dir - starDir * dot(dir, starDir), tangent1));
    float spike2 = abs(dot(dir - starDir * dot(dir, starDir), tangent2));
    float spikeFalloff = exp(-starDist * 60.0);
    float spikes = (smoothstep(0.004, 0.0, spike1) + smoothstep(0.004, 0.0, spike2)) * spikeFalloff * 0.7;

    vec3 planetColor = vec3(1.0, 0.95, 0.80) * (starPoint + spikes);
    planetColor += vec3(0.8, 0.75, 0.55) * smoothstep(0.03, 0.0, starDist) * 0.3;
    sky += planetColor;

    sky += starColor(dir, aboveH);

    float dust1 = fbm(dir * 6.5 + vec3(7.2, -3.1, 1.8));
    float dust2 = fbm(dir * 10.0 + vec3(-2.4, 5.5, 3.3));
    float dustStreak = smoothstep(0.52, 0.72, dust1) * smoothstep(0.48, 0.68, dust2);
    vec3 dustColor = mix(vec3(0.15, 0.10, 0.22), vec3(0.22, 0.18, 0.30), dust1);
    sky += dustColor * dustStreak * aboveH * 0.20;

    return sky;
}

vec3 deepSpaceSky(vec3 dir, float up, float aboveH) {
    float moveSpeed = NebIntensity;
    float t = AnimTime * moveSpeed;

    vec3 sky = vec3(0.005, 0.005, 0.012);

    vec3 bandNormal = normalize(vec3(0.45, 0.72, 0.25));
    float bandDist = abs(dot(dir, bandNormal));
    float band = exp(-bandDist * bandDist * 7.0);
    float bandWide = exp(-bandDist * bandDist * 2.5);

    sky += vec3(0.06, 0.055, 0.08) * band * 0.6 * NebulaStrength;

    vec3 nebP = dir * 2.8 + vec3(sin(t * 0.3) * 0.15, cos(t * 0.25) * 0.1, sin(t * 0.2) * 0.12);

    float neb1 = fbm(nebP + vec3(3.1, 7.2, -1.5));
    float nebMask1 = smoothstep(0.30, 0.70, neb1) * band;
    sky += NebColor1 * nebMask1 * 0.7 * NebulaStrength;

    float neb2 = fbm(nebP * 1.5 + vec3(-5.0 + t * 0.1, 2.3, 8.1));
    float nebMask2 = smoothstep(0.35, 0.75, neb2) * bandWide;
    sky += NebColor2 * nebMask2 * 0.5 * NebulaStrength;

    vec3 upperP = dir * 2.0 + vec3(t * 0.08, -t * 0.06, t * 0.05);

    float neb3 = fbm(upperP + vec3(11.0, -3.0, 5.0));
    float upperMask = smoothstep(0.3, 0.7, up);
    float nebMask3 = smoothstep(0.40, 0.78, neb3) * upperMask;
    sky += mix(NebColor1, NebColor2, neb3) * nebMask3 * 0.45 * NebulaStrength;

    float neb4 = fbm(upperP * 2.2 + vec3(-7.0, 4.0 + t * 0.12, -2.0));
    float nebMask4 = smoothstep(0.50, 0.82, neb4) * upperMask;
    sky += StarColor * nebMask4 * 0.3 * NebulaStrength;

    float neb5 = fbm(dir * 1.5 + vec3(20.0, -10.0 + t * 0.07, 15.0));
    float nebMask5 = smoothstep(0.55, 0.85, neb5);
    sky += NebColor1 * 0.5 * nebMask5 * 0.2 * NebulaStrength;

    float dust = fbm(nebP * 2.0 + vec3(1.0, -7.0, 3.0));
    sky *= 1.0 - smoothstep(0.35, 0.70, dust * band) * 0.5 * NebulaStrength;

    float densityThresh = 0.96 - StarDensity * 0.03;
    float s1 = starLayer(dir, 180.0, densityThresh, 0.0);
    float s2 = starLayer(dir, 100.0, densityThresh + 0.01, 17.3);
    float s3 = starLayer(dir, 55.0, densityThresh + 0.015, 43.7);
    float s4 = starLayer(dir, 30.0, densityThresh + 0.02, 91.1);
    float s5 = starLayer(dir, 250.0, densityThresh - 0.005, 150.0);

    float hv = hash(floor(vec2(atan(dir.z, dir.x), dir.y) * 40.0));
    vec3 coolStar = mix(vec3(0.7, 0.8, 1.0), vec3(1.0, 0.95, 0.85), hv);
    vec3 warmStar = vec3(1.0, 0.7, 0.4);

    vec3 stars = coolStar * (s1 * 0.3 + s2 * 0.5 + s5 * 0.2)
               + StarColor * (s3 * 1.0 + s4 * 1.5)
               + warmStar * s4 * 0.3 * step(0.8, hv);

    stars *= 1.0 + band * 1.8 * StarDensity;
    sky += stars * (0.5 + StarDensity * 0.8);

    vec3 gDir1 = normalize(vec3(-0.5, 0.5, 0.7));
    float gDist1 = acos(clamp(dot(dir, gDir1), -1.0, 1.0));
    sky += vec3(0.7, 0.65, 0.85) * (exp(-gDist1 * gDist1 * 600.0) + exp(-gDist1 * gDist1 * 60.0) * 0.3) * 0.8;

    vec3 gDir2 = normalize(vec3(0.65, -0.25, -0.55));
    float gDist2 = acos(clamp(dot(dir, gDir2), -1.0, 1.0));
    sky += vec3(0.85, 0.75, 0.55) * (exp(-gDist2 * gDist2 * 1500.0) + exp(-gDist2 * gDist2 * 150.0) * 0.25) * 0.6;

    return sky;
}

vec3 voidSky(vec3 dir, float up, float aboveH) {
    float t = AnimTime;
    vec3 p = normalize(dir) * 2.5;

    vec3 abyss = SkyZenith * 0.1;
    vec3 deepPurple = NebColor1 * 0.25;
    vec3 sky = mix(abyss, deepPurple, pow(up, 0.4));

    float warp1 = fbm(p * 1.8 + vec3(t * 0.08, -t * 0.06, t * 0.1));
    float warp2 = fbm(p * 2.5 + vec3(-t * 0.12, t * 0.09, -t * 0.07) + warp1 * 1.5);
    float tendrils = smoothstep(0.25, 0.65, warp2);

    vec3 tendrilColor = mix(NebColor1, NebColor2, warp1);
    sky += tendrilColor * tendrils * aboveH * 0.6;

    float pulse = 0.5 + 0.5 * sin(t * 1.8 + warp2 * 8.0);
    float pulse2 = 0.5 + 0.5 * sin(t * 2.6 + warp1 * 6.0 + 2.0);
    sky += NebColor1 * 0.4 * pulse * tendrils * 0.25;
    sky += NebColor2 * 0.5 * pulse2 * pow(tendrils, 2.0) * 0.15;

    vec3 eyeDir = normalize(vec3(0.0, 0.6, 0.0));
    float eyeDist = acos(clamp(dot(dir, eyeDir), -1.0, 1.0));
    float eyeGlow = exp(-eyeDist * eyeDist * 8.0) * 0.4;
    float eyeCore = exp(-eyeDist * eyeDist * 80.0) * 0.6;
    sky += NebColor2 * eyeGlow;
    sky += StarColor * eyeCore;

    float s1 = starLayer(dir, 120.0, 0.990, 500.0);
    float s2 = starLayer(dir, 60.0, 0.995, 510.0);
    sky += StarColor * (s1 * 0.5 + s2 * 1.0) * aboveH;

    sky += NebColor1 * 0.3 * pow(1.0 - abs(dir.y), 5.0) * 0.2;
    return sky;
}

void main() {
    vec3 dir = getSkyDir(vUv);
    float up = clamp(dir.y, 0.0, 1.0);
    float dn = clamp(-dir.y, 0.0, 1.0);
    float aboveH = smoothstep(-1.0, -0.02, dir.y);

    vec3 sky;
    if (SkyMode < 0.5) {
        sky = energySky(dir, up, aboveH);
    } else if (SkyMode < 1.5) {
        sky = nebulaSky(dir, up, aboveH);
        sky += starColor(dir, aboveH);
    } else if (SkyMode < 2.5) {
        sky = cosmicVeilSky(dir, up, aboveH);
    } else if (SkyMode < 3.5) {
        sky = deepSpaceSky(dir, up, aboveH);
    } else {
        sky = voidSky(dir, up, aboveH);
    }

    sky += SkyHorizon * (pow(1.0 - abs(dir.y), 8.0) * 0.12);

    if (dir.y < 0.0) {
        vec3 belowSky = mix(SkyHorizon, SkyZenith * 0.2, pow(dn, 0.8));
        sky = mix(sky, belowSky, smoothstep(0.0, -0.5, dir.y));
    }

    float fogRange = max(FogEnd - FogStart, 1.0);
    float fogAmt = clamp((1.0 - abs(dir.y)) * FogEnd - FogStart, 0.0, fogRange) / fogRange;
    fogAmt *= fogAmt;
    sky = mix(sky, FogColor.rgb, fogAmt * FogColor.a * 0.82);

    fragColor = vec4(clamp(sky / (sky + 0.22) * 1.42, 0.0, 1.0), 1.0);
}
