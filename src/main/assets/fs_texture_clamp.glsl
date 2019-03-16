precision mediump float;

varying vec2 vTexCoord;
uniform sampler2D uSampler;

void main() {
    if (vTexCoord.s < 0.0 || vTexCoord.s > 1.0 || vTexCoord.t < 0.0 || vTexCoord.t > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = texture2D(uSampler, vTexCoord);
    }
}
