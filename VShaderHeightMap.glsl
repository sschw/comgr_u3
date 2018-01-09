#version 400 core

uniform mat4 mv;
uniform mat4 proj;
uniform sampler2D text;
in vec3 pos;
in vec3 vColor;
out vec3 fColor;
out vec3 fMvPos;
out vec3 fNorm;
out vec3 lightPos;

void main()
{
  vec2 uv = (pos.xz * 0.5) + vec2(0.5, 0.5);
  fMvPos = (mv*vec4(pos, 1)).xyz;
  gl_Position = proj*mv*vec4(pos.x, texture(text, uv).x, pos.z, 1);
  fColor = vColor;
  fNorm = normalize(
		  cross(
				  vec3(0.2, 0, texture(text, uv+vec2(0.1, 0)).x - texture(text, uv-vec2(0.1, 0)).x),
				  vec3(0, 0.2, texture(text, uv+vec2(0, 0.1)).x - texture(text, uv-vec2(0, 0.1)).x)
		  )
  );

  lightPos = vec3(1, 1, 1);
}
