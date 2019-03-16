attribute vec4 aPosition;
attribute vec2 aTexCoord;
uniform mat4 uMVPMatrix;
uniform vec4 uTexRect;
varying vec2 vTexCoord;
varying vec4 vTexRect;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vec2 texCoord;
    texCoord.s = aTexCoord.s  * uTexRect.p + uTexRect.s;
    texCoord.t = aTexCoord.t * uTexRect.q + uTexRect.t;
    vTexRect = uTexRect;
    vTexCoord = texCoord;
}
