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
  gl_Position = proj*mv*vec4(pos, 1);
  fColor = vColor;
}
