#version 150
// This Source Code Form is subject to MPL-2.0. See META-INF/third-party/tlt-love-name/LICENSE.
// Adapted from The Long Travail diary_font, commit f295c4ead89c0a99ef2f7ed9f56e9cbba42bfb34.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Seconds;
uniform float Halo;
uniform float AtlasMode;
in vec4 vertexColor;
in vec2 texCoord0;
in float textX;
out vec4 fragColor;

const vec3 roseDark = vec3(159.0, 77.0, 112.0) / 255.0;
const vec3 roseMain = vec3(227.0, 154.0, 181.0) / 255.0;
const vec3 roseHighlight = vec3(255.0, 240.0, 245.0) / 255.0;

// Same flowing veins, grain, four-second travel and glints as the reference name material.
vec3 flowingRose() {
    float angle = fract(Seconds / 4.0) * 6.2831853;
    vec2 p = vec2(textX, gl_FragCoord.y * 0.5);
    float grain = 0.5 + 0.5 * sin(p.x * 1.7 + sin(p.y * 0.9) * 1.8);
    float bend = sin(p.y * 0.73 + p.x * 0.21) * 0.65;
    float distance = abs(sin((p.x * 0.24 - angle + bend) * 0.5));
    float vein = 1.0 - smoothstep(0.02, 0.22, distance);
    float glow = 1.0 - smoothstep(0.16, 0.62, distance);
    vec3 color = mix(roseDark, roseMain, 0.35 + grain * 0.65);
    color = mix(color, roseMain, glow * 0.65);
    color = mix(color, roseHighlight, vein * 0.94);
    vec2 cell = floor(p / 3.5);
    vec2 local = fract(p / 3.5) - 0.5;
    float seed = fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
    float point = step(0.83, seed) * (1.0 - smoothstep(0.04, 0.30, length(local)));
    float pulse = 0.5 + 0.5 * sin(angle + seed * 6.2831853);
    return mix(color, roseHighlight, point * pulse * glow * 0.80);
}

void main() {
    vec4 glyph = texture(Sampler0, texCoord0);
    float coverage = AtlasMode > 0.5 && AtlasMode < 1.5 ? glyph.r : glyph.a;
    if (AtlasMode > 1.5) {
        float distance = glyph.a - 127.0 / 255.0 + 0.04;
        coverage = clamp(distance / max(fwidth(distance), 0.0001) + 0.5, 0.0, 1.0);
    }
    float alpha = coverage * vertexColor.a * ColorModulator.a;
    if (alpha < 0.01) discard;
    float brightness = max(vertexColor.r, max(vertexColor.g, vertexColor.b));
    float shadow = brightness < 0.35 ? 0.25 : 1.0;
    vec3 color;
    if (Halo > 0.5) {
        float ripple = 0.5 + 0.5 * sin(textX * 0.48 - Seconds * 1.5707963);
        float swell = 0.5 + 0.5 * sin(Seconds * 1.0471976);
        alpha *= 0.55 + 0.30 * ripple + 0.15 * swell;
        color = mix(roseMain, roseHighlight, ripple * 0.6);
    } else {
        color = flowingRose();
    }
    fragColor = vec4(color * shadow * ColorModulator.rgb, alpha);
}

