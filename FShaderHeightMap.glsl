#version 400 core

out vec4 colour;
in vec3 fColor;
in vec3 fMvPos;
in vec3 fNorm;

in vec3 lightPos;

void main()
{
  vec3 toLight = normalize(lightPos - fMvPos);
  float beta = max(0, dot(toLight, fNorm));
  vec4 diffuse = beta * vec4(fColor, 1);
  vec3 r = 2 * beta * fNorm - toLight;
  vec4 specular = vec4(pow(max(0, -dot(r, normalize(fMvPos))), 20));

  colour = diffuse + specular;
}