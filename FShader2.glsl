#version 400 core

uniform sampler2D text;
uniform bool useText;
out vec4 colour;
in vec3 fColor;
in vec3 fPos;

void main()
{
  vec2 uv = (fPos.xy * 0.5) + vec2(0.5, 0.5);

  colour = useText ? vec4(texture(text, uv).rgb*fColor, texture(text, uv).a) : vec4(fColor, 1);
}