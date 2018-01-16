#version 400 core

uniform mat4 mv;
uniform mat4 proj;
in vec3 pos;
in vec3 vColor;
out vec3 fColor;
out vec3 fPos;

void main()
{
  fPos = pos;
  vec4 mvpos = mv*vec4(pos, 1);

  gl_Position = proj*mvpos;
  fColor = vColor;
}
