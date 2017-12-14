import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL11;

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public class OpenGL {
	public static void main(String[] args) throws Exception {
		// open a window
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
		long hWindow = GLFW.glfwCreateWindow(720, 480, "ComGr", 0, 0);
		GLFW.glfwSetWindowSizeCallback(hWindow, (window, width, height) -> {
			glViewport(0, 0, width, height);
		});
		GLFW.glfwMakeContextCurrent(hWindow);
		GLFW.glfwSwapInterval(1);
		createCapabilities();

		// set up opengl
		glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
		// GL11.glClearDepth(1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		// GL11.glDepthFunc(GL11.GL_LESS);
		// GL11.glDisable(GL11.GL_CULL_FACE);

		// load, compile and link shaders
		// see https://www.khronos.org/opengl/wiki/Vertex_Shader
		String VertexShaderSource = readShader("./VShader.glsl");
		int hVertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(hVertexShader, VertexShaderSource);
		glCompileShader(hVertexShader);
		if (glGetShaderi(hVertexShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hVertexShader));

		// see https://www.khronos.org/opengl/wiki/Fragment_Shader
		String FragmentShaderSource = readShader("./FShader.glsl");
		int hFragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(hFragmentShader, FragmentShaderSource);
		glCompileShader(hFragmentShader);
		if (glGetShaderi(hFragmentShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hFragmentShader));

		// link shaders to a program
		int hProgram = glCreateProgram();
		glAttachShader(hProgram, hFragmentShader);
		glAttachShader(hProgram, hVertexShader);
		glLinkProgram(hProgram);
		if (glGetProgrami(hProgram, GL_LINK_STATUS) != GL_TRUE)
			throw new Exception(glGetProgramInfoLog(hProgram));

		// upload model vertices to a vbo
		float[] triangles = setupVertices();
		int vboTriangleVertices = setupVBOBuffer(GL_ARRAY_BUFFER, triangles);

		// upload color buffer to a vbo
		float[] triangleColorVertices = setupVerticesColor();
		int vboTriangleColorVertices = setupVBOBuffer(GL_ARRAY_BUFFER, triangleColorVertices);

		// upload model indices to a vbo
		int[] triangleIndices = setupVerticesIndices();
		int vboTriangleIndices = setupVBOBuffer(GL_ELEMENT_ARRAY_BUFFER, triangleIndices);

		// set up a vao
		int vaoTriangle = glGenVertexArrays();
		glBindVertexArray(vaoTriangle);

		glEnableVertexAttribArray(glGetAttribLocation(hProgram, "pos"));
		glBindBuffer(GL_ARRAY_BUFFER, vboTriangleVertices);
		glVertexAttribPointer(glGetAttribLocation(hProgram, "pos"), 3, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);

		glEnableVertexAttribArray(glGetAttribLocation(hProgram, "vColor"));
		glBindBuffer(GL_ARRAY_BUFFER, vboTriangleColorVertices);
		glVertexAttribPointer(glGetAttribLocation(hProgram, "vColor"), 3, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboTriangleIndices);

		// check for errors during all previous calls
		int error = glGetError();
		if (error != GL_NO_ERROR)
			throw new Exception(Integer.toString(error));

		// render loop
		int angle = 0;
		while (!GLFW.glfwWindowShouldClose(hWindow)) {
			// switch to our shader
			glUseProgram(hProgram);


			// projection
			Mat4 proj = Mat4.perspective(-1, 1, -1, 1, 1, 10);
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "proj"), false, proj.toArray());

			// clear screen and z-buffer
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Define current mv matrix
			Mat4 camera = Mat4.lookAt(new Vec3(1, 1, 1), new Vec3(0, 0, -5), new Vec3(0, 1, 0));

			draw(hProgram, new Vec3(0, 0, -5), angle, new Vec3(1, 1, 0), camera, vaoTriangle, vboTriangleIndices,
					triangleIndices.length);

			draw(hProgram, new Vec3(-2, -2, -8), angle+70, new Vec3(1, 0, 1), camera, vaoTriangle, vboTriangleIndices,
					triangleIndices.length);

			draw(hProgram, new Vec3(2, 2, -3), angle+50, new Vec3(0, 1, 1), camera, vaoTriangle, vboTriangleIndices,
					triangleIndices.length);
			
			// display
			GLFW.glfwSwapBuffers(hWindow);
			GLFW.glfwPollEvents();

			error = glGetError();
			if (error != GL_NO_ERROR)
				throw new Exception(Integer.toString(error));

			// Modify rotation
			angle += 5;
		}

		GLFW.glfwDestroyWindow(hWindow);
		GLFW.glfwTerminate();
	}

	private static void draw(int hProgram, Vec3 translate, int angle, Vec3 rotAxis, Mat4 camera, int vao, int indices,
			int noIndices) {
		Mat4 rot = Mat4.rotate(angle, rotAxis);
		Mat4 trans = Mat4.translate(translate);
		Mat4 mv = Mat4.multiply(camera, trans, rot);
		glUniformMatrix4fv(glGetUniformLocation(hProgram, "mv"), false, mv.toArray());

		// render our model
		glBindVertexArray(vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indices);
		glDrawElements(GL_TRIANGLES, noIndices, GL_UNSIGNED_INT, 0);
	}
	
	private static String readShader(String path) {
		File file = new File(path);
		URI uri = file.toURI();
		byte[] bytes = null;
		try {
			bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(uri));
		} catch(IOException e) {}

		return new String(bytes);
	}

	private static int setupVBOBuffer(int buffertype, float[] buffer) {
		int buf = glGenBuffers();
		glBindBuffer(buffertype, buf);
		glBufferData(buffertype, buffer, GL_STATIC_DRAW);
		return buf;
	}

	private static int setupVBOBuffer(int buffertype, int[] buffer) {
		int buf = glGenBuffers();
		glBindBuffer(buffertype, buf);
		glBufferData(buffertype, buffer, GL_STATIC_DRAW);
		return buf;
	}

	private static int[] setupVerticesIndices() {
		return new int[] { 
				0, 1, 2, 
				0, 2, 3, 
				7, 6, 5, 
				7, 5, 4, 
				0, 3, 7, 
				0, 7, 4, 
				2, 1, 5, 
				2, 5, 6, 
				3, 2, 6,
				3, 6, 7, 
				1, 0, 4, 
				1, 4, 5 
		};
	}

	public static float[] setupVertices() {
		return new float[] { 
				-1, -1, -1, 
				1, -1, -1, 
				1, 1, -1, 
				-1, 1, -1, 
				-1, -1, 1, 
				1, -1, 1, 
				1, 1, 1, 
				-1, 1, 1 
		};
	}

	public static float[] setupVerticesColor() {
		return new float[] { 
				0, 0, 0, 
				1, 0, 0, 
				0, 1, 0, 
				0, 0, 1, 
				1, 1, 0, 
				0, 1, 1, 
				1, 0, 1, 
				1, 1, 1 
		};
	}
}
