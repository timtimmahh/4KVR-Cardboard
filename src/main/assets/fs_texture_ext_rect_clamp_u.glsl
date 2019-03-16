#extension GL_OES_EGL_image_external : require

precision mediump float;

varying vec2 vTexCoord;

uniform vec4 uClampRect;
uniform samplerExternalOES uSampler;

void main() {
    if (vTexCoord.s < uClampRect.s || vTexCoord.s > uClampRect.p
    || vTexCoord.t < uClampRect.t || vTexCoord.t > uClampRect.q) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = texture2D(uSampler, vTexCoord);
    }
}
