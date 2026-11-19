#version 150

// The halftone wipe roll_mod plays when a loading screen ends (LoadingWipe). A bold band sweeps from
// the upper left to the lower right; its edges are halftone dots on an even 45-degree grid, tapering
// away from the band. Ahead of it is what was on screen, behind it the interstitial: black with the
// logo in the middle. "Reveal" mirrors it: the interstitial ahead, the next screen behind.
//
// All lengths are framebuffer pixels, y down from the top of the window.

uniform sampler2D Sampler0;      // the logo

uniform vec4 ColorModulator;
uniform vec2 ScreenSize;         // framebuffer size
uniform float Front;             // where the band's front edge is, along the sweep
uniform float Band;              // solid band width
uniform float Halo;              // width of each halftone edge
uniform float Spacing;           // dot grid spacing
uniform float Reveal;            // 0: covering, 1: revealing
uniform vec4 BandColor;
uniform vec4 LogoRect;           // x, y, width, height

out vec4 fragColor;

const float INV_SQRT2 = 0.70710678;

// How large the band's dots are at distance u ahead of its front: full at the band, none Halo ahead.
float bandSize(float u) {
    return clamp(1.0 - u / Halo, 0.0, 1.0);
}

// How large the interstitial's dots are: none at the band's back edge, full Halo behind it.
float interstitialSize(float u) {
    return clamp((-Band - u) / Halo, 0.0, 1.0);
}

// Coverage of the halftone layer whose dot size at a distance u is given by `which`, at the point st
// (sweep / across coordinates). Each dot is sized by its own centre, so every dot is a true circle;
// the neighbouring cells are checked too, as a full-size dot reaches past its own cell.
float halftone(vec2 st, float sign, int which) {
    vec2 cell = floor(st / Spacing);
    float maxRadius = Spacing * 0.78; // past half the cell diagonal: full-size dots close up solid
    float coverage = 0.0;
    for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
            vec2 centre = (cell + vec2(dx, dy) + 0.5) * Spacing;
            float u = sign * (centre.x - Front);
            float size = which == 0 ? bandSize(u) : interstitialSize(u);
            float radius = size * maxRadius;
            if (radius <= 0.0) continue;
            float d = distance(st, centre);
            // A one-pixel soft edge; a dot under a pixel across fades out instead of leaving a speck.
            float c = clamp(radius - d + 0.5, 0.0, 1.0) * clamp(radius * 2.0, 0.0, 1.0);
            coverage = max(coverage, c);
        }
    }
    return coverage;
}

void main() {
    vec2 p = vec2(gl_FragCoord.x, ScreenSize.y - gl_FragCoord.y);
    vec2 st = vec2(p.x + p.y, p.x - p.y) * INV_SQRT2;
    float sign = Reveal > 0.5 ? -1.0 : 1.0;

    float band = halftone(st, sign, 0);
    float inter = halftone(st, sign, 1);

    // The interstitial: black, with the logo over it.
    vec3 interColour = vec3(0.0);
    vec2 uv = (p - LogoRect.xy) / LogoRect.zw;
    if (uv.x >= 0.0 && uv.y >= 0.0 && uv.x <= 1.0 && uv.y <= 1.0) {
        vec4 logo = texture(Sampler0, uv);
        interColour = mix(interColour, logo.rgb, logo.a);
    }

    // Band over the old screen, interstitial over the band.
    float bandAlpha = band * (1.0 - inter);
    float alpha = bandAlpha + inter;
    if (alpha <= 0.0) discard;
    vec3 colour = (BandColor.rgb * bandAlpha + interColour * inter) / alpha;
    fragColor = vec4(colour, alpha) * ColorModulator;
}
