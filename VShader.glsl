#version 400 core

uniform mat4 mv;
uniform mat4 mvNorm;
uniform mat4 proj;
in vec3 pos;
in vec3 vColor;
out vec3 fColor;
out vec3 fPos;
out vec3 fMvPos;
out vec3 fNorm;
out vec3 lightPos;

void main()
{
  fPos = pos;
  vec4 mvpos = mv*vec4(pos, 1);

  fMvPos = mvpos.xyz;
  gl_Position = proj*mvpos;
  fColor = vColor;
  fNorm = normalize((mvNorm*vec4(0, 0, pos.z, 0)).xyz);

  lightPos = vec3(5, 5, 5);
}
