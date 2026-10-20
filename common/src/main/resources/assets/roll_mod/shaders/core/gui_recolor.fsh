#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

// Vanilla's GUI art is drawn from a few greys. A grey pixel is mapped through the theme's five tones,
// placed at the greys vanilla uses: black outline 0%, dark bevel #555555 33%, slot #8B8B8B 55%,
// panel #C6C6C6 78%, light bevel white 100%. Greys in between are blended. Anything with colour
// (icons, arrows, fire) is left exactly as drawn. Set by roll_mod's GuiRecolor.
uniform vec4 RecolorOutline;
uniform vec4 RecolorShadow;
uniform vec4 RecolorWell;
uniform vec4 RecolorSurface;
uniform vec4 RecolorHighlight;

vec3 recolor(vec3 rgb) {
    float hi = max(rgb.r, max(rgb.g, rgb.b));
    float lo = min(rgb.r, min(rgb.g, rgb.b));
    if (hi - lo > 0.04) {
        return rgb;
    }
    float g = hi;
    if (g < 0.333) return mix(RecolorOutline.rgb, RecolorShadow.rgb, g / 0.333);
    if (g < 0.545) return mix(RecolorShadow.rgb, RecolorWell.rgb, (g - 0.333) / 0.212);
    if (g < 0.776) return mix(RecolorWell.rgb, RecolorSurface.rgb, (g - 0.545) / 0.231);
    return mix(RecolorSurface.rgb, RecolorHighlight.rgb, (g - 0.776) / 0.224);
}

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = vec4(recolor(color.rgb), color.a) * ColorModulator;
}
