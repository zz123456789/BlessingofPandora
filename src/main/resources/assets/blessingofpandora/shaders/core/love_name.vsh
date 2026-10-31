#version 150
// This Source Code Form is subject to MPL-2.0. See META-INF/third-party/tlt-love-name/LICENSE.
// Adapted from The Long Travail diary_font, commit f295c4ead89c0a99ef2f7ed9f56e9cbba42bfb34.
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float OriginX;
out vec4 vertexColor;
out vec2 texCoord0;
out float textX;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
    textX = Position.x - OriginX;
}

