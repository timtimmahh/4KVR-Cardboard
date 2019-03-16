#extension GL_OES_EGL_image_external : require

precision mediump float;

varying vec2 vTexCoord;
varying vec4 vTexRect;

uniform samplerExternalOES uSampler;

void main() {
    if (vTexCoord.s < vTexRect.s || vTexCoord.s > (vTexRect.s + vTexRect.p)
    || vTexCoord.t < vTexRect.t || vTexCoord.t > (vTexRect.t + vTexRect.q)) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = texture2D(uSampler, vTexCoord);
    }
}
