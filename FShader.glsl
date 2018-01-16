#version 400 core

uniform sampler2D text;
out vec4 colour;
in vec3 fColor;
in vec3 fPos;
in vec3 fMvPos;
in vec3 fNorm;

in vec3 lightPos;

void main()
{
  vec2 uv = (fPos.xy * 0.5) + vec2(0.5, 0.5);
  
  vec4 tex = texture(text, uv);
  vec3 toLight = normalize(lightPos - fMvPos);
  float beta = max(0, dot(toLight, fNorm));
  vec3 diffuse = beta * fColor * tex.rgb;
  vec3 r = 2 * beta * fNorm - toLight;
  vec3 specular = vec3(pow(max(0, -dot(r, normalize(fMvPos))), 20));

  colour = vec4(diffuse + specular, tex.a);
}