
// expelimental code!


attribute vec4 aPosition;
attribute vec2 aTexCoord; // not use
uniform int uProjectionType;
uniform float uFovY;
uniform float uAspect;
uniform mat4 uMVPMatrix;
varying vec2 vTexCoord;

const float PI = 3.141592653589793;
const float _2PI = 6.283185307179586;

void main() {
    //test var
//    uProjectionType = 1;
//    uFovY = 90.0;
//    uAspect = 1.3333;

    gl_Position = uMVPMatrix * aPosition;


    // calcurate texture coords
    vec3 p = aPosition.xyz;
    normalize(p);

    vec2 texCoord;
//
//    if (uProjectionType == 0) { // equirectangular
        float theta = asin(p.y);
        float phi = atan(p.x, p.z);

        texCoord.x = phi / _2PI + 0.5;
        texCoord.y = 1.0 - (theta / PI + 0.5);


//    } else {
//        float a = atan(length(p.x, p.y), p.z); // lens circle angle
//        // float a = asin(length(p.xy); // lens circle angle
//
//        float f = cos(uFovY * 0.5);
//
//        // formula from panotools (http://wiki.panotools.org/Fisheye_Projection)
//        float r;
//        switch (uProjectionType) {
//            case 1: r = f * tan(a); break;             // rectilinear
//            case 2: r = f * a; break;                  // equidistant
//            case 3: r = f * 2.0 * tan(a * 0.5); break; // stereographic
//            case 4: r = f * sin(a); break;             // orthographic
//            case 5: r = f * 2.0 * sin(a * 0.5); break; // equisolid
//            default: r = f * tan(a);                   // default rectilinear
//        }
//
//        float b = atan(p.y, p.x); // 2d plane corresponding polar angle
//
////        texCoord.x = r * cos(b) / uAspect + 0.5;
//        texCoord.x = r * cos(b) + 0.5;
//
//        texCoord.y = r * sin(b) + 0.5;
//    }

    vTexCoord = texCoord;
//    vTexCoord = aTexCoord;
}
