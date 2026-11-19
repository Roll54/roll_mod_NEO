#version 150

// The loading wipe is one quad over the whole window, given straight in clip space (-1..1): the
// overlays it is drawn over leave their own projections set, so none is used. All of its drawing is in
// loading_wipe.fsh.
in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
