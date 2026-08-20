#version 150

uniform sampler2D Sampler0;
uniform vec2 TextureSize;
uniform float Range;
uniform float EdgeStrength;
uniform float Thickness;
uniform vec4 Color;
uniform int Outline;
uniform float OutlineThickness;
uniform vec4 OutlineColor;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord);

    float dx = dFdx(texCoord.x) * TextureSize.x;
    float dy = dFdy(texCoord.y) * TextureSize.y;
    float d = dx * dx + dy * dy;
    float toPixels = d > 0.000001 ? Range * inversesqrt(d) : Range * 4.0;

    float sigDist = median(texColor.r, texColor.g, texColor.b) - 0.5 + Thickness;

    float edge = EdgeStrength > 0.0 ? EdgeStrength : 0.5;
    float alpha = smoothstep(-edge, edge, sigDist * toPixels);

    vec4 effectiveColor = Color * vertexColor;

    if (Outline == 1) {
        float outlineAlpha = smoothstep(-edge, edge, (sigDist + OutlineThickness) * toPixels) - alpha;
        float finalAlpha = alpha * effectiveColor.a + outlineAlpha * effectiveColor.a;
        fragColor = vec4(mix(OutlineColor.rgb, effectiveColor.rgb, alpha), finalAlpha);
        return;
    }

    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(effectiveColor.rgb, effectiveColor.a * alpha);
}