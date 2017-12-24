#version 400 core

uniform mat4 mv;
uniform mat4 proj;
uniform sampler2D text;
in vec3 pos;
in vec3 vColor;
out vec3 fColor;
out vec3 fPos;
out vec3 fNorm;

void main()
{
  vec2 uv = (pos.xz * 0.5) + vec2(0.5, 0.5);
  fPos = (mv*vec4(pos, 1)).xyz;
  gl_Position = proj*mv*vec4(pos.x, texture(text, uv).x, pos.z, 1);
  fColor = vColor;
}
