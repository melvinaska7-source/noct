#version 150

uniform sampler2D Sampler0; // PrevTrailSampler
uniform sampler2D Sampler1; // SceneSampler  
uniform sampler2D Sampler2; // MaskSampler

uniform vec4 resolution;   // (width, height, time, intensity)
uniform vec4 glowColor;    // (r, g, b, unused)
uniform vec4 settings;     // (unused, speed, trailLength, softness)
uniform vec4 settings2;    // (blurRadius, smoke, activity, unused)
uniform vec4 reserved;     // (slash, slashDirection, cameraShift.x, cameraShift.y)

in vec2 TexCoord;
out vec4 OutColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.0;
        amplitude *= 0.5;
    }
    return value;
}

vec3 vividColor(vec3 color) {
    float peak = max(max(color.r, color.g), color.b);
    if (peak < 0.001) return vec3(0.0); // Полностью чёрный → чёрный, не glowColor
    vec3 vivid = color / max(peak, 0.20);
    return clamp(mix(color, vivid, 0.36) * 1.05, 0.0, 1.0);
}

float sampleMask(vec2 uv) {
    return texture(Sampler2, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

void addSource(vec2 sourceUv, float weight, float useItemColor, inout vec3 color, inout float alpha) {
    float mask = sampleMask(sourceUv);
    if (mask <= 0.001) return;
    
    vec3 finalColor;
    if (useItemColor > 0.5) {
        // Use item texture color
        vec3 sceneColor = texture(Sampler1, clamp(sourceUv, vec2(0.0), vec2(1.0))).rgb;
        finalColor = vividColor(sceneColor);
    } else {
        // Use theme color (glowColor)
        finalColor = glowColor.rgb;
    }
    
    float sourceWeight = mask * weight;
    color += finalColor * sourceWeight;
    alpha += sourceWeight;
}

void main() {
    vec2 texelSize = 1.0 / resolution.xy;
    float time = resolution.z;
    float intensity = clamp(resolution.w, 0.0, 1.5);
    float useItemCol = settings.x; // 0.0 = theme color, 1.0 = item color
    float speed = clamp(settings.y, 0.35, 2.4);
    float trailLength = clamp(settings.z, 0.1, 1.0);
    float softness = clamp(settings.w, 0.55, 2.0);
    float blurRadius = clamp(settings2.x, 0.45, 2.5);
    float smoke = clamp(settings2.y, 0.0, 0.8);
    float activity = clamp(settings2.z, 0.0, 1.0);
    float slash = clamp(reserved.x, 0.0, 1.0);
    float slashDirection = reserved.y < 0.0 ? -1.0 : 1.0;
    vec2 cameraShift = clamp(reserved.zw, vec2(-0.012), vec2(0.012));

    float slashReturn = smoothstep(0.20, 0.78, 1.0 - slash);
    vec2 slashAxis = normalize(mix(vec2(0.88 * slashDirection, -0.48),
                                   vec2(-0.58 * slashDirection, 0.24),
                                   slashReturn));
    vec2 slashNormal = vec2(-slashAxis.y, slashAxis.x);

    float n = fbm(TexCoord * vec2(34.0, 29.0) + vec2(time * 0.13 * speed, -time * 0.10 * speed));
    vec2 curl = vec2(
        fbm(TexCoord * 28.0 + vec2(time * 0.20 * speed, 3.1)),
        fbm(TexCoord * 31.0 + vec2(8.4, -time * 0.17 * speed))
    ) - 0.5;

    // Subtle horizontal sway animation (reduced amplitude)
    float swayAmount = sin(time * 0.8) * 0.003; // Slower and smaller drift
    vec2 swayOffset = vec2(swayAmount, 0.0);

    // Fetch previous trail with camera shift + curl + sway
    vec2 prevUv = TexCoord + cameraShift + swayOffset + curl * texelSize * (1.9 + blurRadius * 1.35);
    prevUv += slashAxis * texelSize * slash * (7.0 + blurRadius * 7.0);
    vec4 prev = texture(Sampler0, clamp(prevUv, vec2(0.0), vec2(1.0)));

    float fade = mix(0.82, 0.89, min(softness, 1.6) / 1.6); // Medium fade for smooth longer trail
    fade = mix(fade - activity * 0.015, 0.92, slash * 0.35);
    prev.rgb *= fade;
    prev.a *= fade;
    if (prev.a < 0.004) { // Lower threshold for smoother fade
        prev = vec4(0.0);
    }

    vec3 sourceColor = vec3(0.0);
    float sourceAlpha = 0.0;

    float spread = 2.2 + blurRadius * 2.85 + trailLength * 7.5;
    for (int i = 0; i < 18; i++) {
        float fi = float(i);
        float angle = fi * 2.399963 + time * (0.18 + speed * 0.09) + n * 2.6;
        vec2 dir = vec2(cos(angle), sin(angle));
        float dist = 1.1 + fi * spread / 7.2;
        vec2 sourceUv = TexCoord - dir * texelSize * dist - curl * texelSize * (2.0 + fi * 0.26);
        float weight = (18.0 - fi) / 18.0;
        addSource(sourceUv, weight, useItemCol, sourceColor, sourceAlpha);
    }

    if (slash > 0.001) {
        float slashLength = 7.5 + blurRadius * 6.0 + trailLength * 12.0;
        for (int i = 0; i < 26; i++) {
            float t = float(i) / 25.0;
            float arc = sin(t * 3.1415927);
            float curve = t - 0.45;
            vec2 slashOffset = slashAxis * texelSize * ((t - 0.18) * slashLength * 8.0);
            slashOffset += slashNormal * texelSize * ((curve * curve * 30.0 - 4.5) * arc);
            slashOffset += curl * texelSize * (1.4 + t * 2.0);
            vec2 sourceUv = TexCoord - slashOffset * (0.72 + slash * 0.42);
            float weight = slash * arc * (1.25 - t * 0.55) * (0.85 + activity * 0.45);
            addSource(sourceUv, weight, useItemCol, sourceColor, sourceAlpha);
        }
    }

    float body = sampleMask(TexCoord);
    float outside = 1.0 - body * 0.97;
    float wisps = mix(0.45, 1.16, fbm(TexCoord * 104.0 + vec2(-time * 0.34 * speed, time * 0.23 * speed)));

    float newAlpha = smoothstep(0.015, 0.25, sourceAlpha / 5.0); // Smoother gradient
    newAlpha *= outside * wisps;
    newAlpha *= (0.28 + intensity * 0.25 + smoke * 0.30 + activity * 0.10 + slash * 0.18); // Softer, shadow-like

    vec3 newColor = sourceAlpha > 0.001 ? sourceColor / sourceAlpha : glowColor.rgb;
    vec3 slashColor = clamp(mix(newColor, glowColor.rgb, 0.24) * (1.0 + slash * 0.35), 0.0, 1.0);
    newColor = mix(newColor, slashColor, slash * 0.55);

    // Trail only (smooth and blurred, shadow-like)
    vec3 outColor = mix(prev.rgb, newColor, clamp(newAlpha * (2.4 + slash * 1.2), 0.0, 0.80));
    float outAlpha = clamp(prev.a + newAlpha * (1.0 - prev.a), 0.0, 0.65 + slash * 0.12);
    
    // Fill current model position with solid color ONLY when useItemColor is disabled
    if (body > 0.01 && useItemCol < 0.5) {
        outColor = glowColor.rgb;
        outAlpha = 0.35; // Reduced opacity for subtle fill
    }
    
    if (outAlpha < 0.005) {
        outColor = vec3(0.0);
        outAlpha = 0.0;
    }

    OutColor = vec4(outColor, outAlpha);
}
