#version 400 core

out vec4 colour;
in vec3 fColor;

void main()
{
  colour = vec4(fColor, 1.0);
}