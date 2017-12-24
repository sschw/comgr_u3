import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL11;

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;

import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class HeightMap {
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

		// set up texture
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		
		// set up opengl
		glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
		// GL11.glClearDepth(1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		// GL11.glDepthFunc(GL11.GL_LESS);
		// GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL_FRAMEBUFFER_SRGB);
		

		// load, compile and link shaders
		// see https://www.khronos.org/opengl/wiki/Vertex_Shader
		String VertexShaderSource = readShader("./VShaderHeightMap.glsl");
		int hVertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(hVertexShader, VertexShaderSource);
		glCompileShader(hVertexShader);
		if (glGetShaderi(hVertexShader, GL_COMPILE_STATUS) != GL_TRUE)
			throw new Exception(glGetShaderInfoLog(hVertexShader));

		// see https://www.khronos.org/opengl/wiki/Fragment_Shader
		String FragmentShaderSource = readShader("./FShaderHeightMap.glsl");
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

		// Load texture object
		BufferedImage texImage = loadImage("./heightmap.bmp");
		ByteBuffer texture = getRGBFromImage(texImage); 
		int texStorageTexture = glGenTextures();
		
		// parameterize the textures
		glBindTexture(GL_TEXTURE_2D, texStorageTexture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8, texImage.getWidth(), texImage.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, texture);
		int textureUnit = 0;
		
		// check for errors during all previous calls
		int error = glGetError();
		if (error != GL_NO_ERROR)
			throw new Exception(Integer.toString(error));

		// render loop
		while (!GLFW.glfwWindowShouldClose(hWindow)) {
			// switch to our shader
			glUseProgram(hProgram);

			// load texture
			glActiveTexture(GL_TEXTURE0 + textureUnit);
			glBindTexture(GL_TEXTURE_2D, texStorageTexture);
			glUniform1i(glGetUniformLocation(hProgram, "text"), textureUnit);

			// projection
			Mat4 proj = Mat4.perspective(-1, 1, -1, 1, 1, 10);

			// Define current camera matrix
			Mat4 camera = Mat4.lookAt(new Vec3(1, 1, 1), new Vec3(0, 0, -5), new Vec3(0, 1, 0));
			
			glUniformMatrix4fv(glGetUniformLocation(hProgram, "proj"), false, Mat4.multiply(proj, camera).toArray());

			// clear screen and z-buffer
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			draw(hProgram, new Vec3(0, 0, -5), 0, new Vec3(1, 1, 0), 5.0f, vaoTriangle, vboTriangleIndices,
					triangleIndices.length);
			
			// display
			GLFW.glfwSwapBuffers(hWindow);
			GLFW.glfwPollEvents();

			error = glGetError();
			if (error != GL_NO_ERROR)
				throw new Exception(Integer.toString(error));
		}

		GLFW.glfwDestroyWindow(hWindow);
		GLFW.glfwTerminate();
	}

	private static void draw(int hProgram, Vec3 translate, int angle, Vec3 rotAxis, float s, int vao, int indices,
			int noIndices) {
		Mat4 scale = Mat4.scale(s, s, s);
		Mat4 rot = Mat4.rotate(angle, rotAxis);
		Mat4 trans = Mat4.translate(translate);
		Mat4 mv = Mat4.multiply(trans, rot, scale);
		glUniformMatrix4fv(glGetUniformLocation(hProgram, "mv"), false, mv.toArray());

		// render our model
		glBindVertexArray(vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indices);
		glDrawElements(GL_TRIANGLE_STRIP, noIndices, GL_UNSIGNED_INT, 0);
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
	
	private static BufferedImage loadImage(String path) {
		BufferedImage m = null;
		try {
			m = ImageIO.read(Files.newInputStream(Paths.get(path)));
		} catch (IOException e) {
			System.out.println("Fix your f*cking image path.");
		}
		return m;
	}
	
	private static ByteBuffer getRGBFromImage(BufferedImage i) {
		ByteBuffer b = ByteBuffer.allocateDirect(i.getWidth()*i.getHeight()*3);
		for(int y = 0; y < i.getHeight(); y++) {
			for(int x = 0; x < i.getWidth(); x++) {
				int rgb = i.getRGB(x, y);
				b.put((byte) ((rgb >> 16) & 0xFF));
				b.put((byte) ((rgb >> 8) & 0xFF));
				b.put((byte) (rgb & 0xFF));
			}
		}
		b.flip();
		return b;
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
		// 0, 100, 1, 101, 2, 102, 202, 101, 201, 100, 200, 300, 201, 301, 202, 302
		int[] index = new int[19800];
		int k = 0;
		boolean forward = true;
		for(int i = 0; i < 99; i++) {
			for(int j = 0; j < 100; j++) {
				for(int l = 0; l < 2; l++)
					if(forward)
						index[k++] = i*100+j+100*l;
					else
						index[k++] = i*100+(99-j)+100*l;
			}
			forward = !forward;
		}
		return index;
	}

	public static float[] setupVertices() {
		float[] field = new float[30000];
		for(int i = 0; i < 100; i++) {
			for(int j = 0; j < 100; j++) {
				field[i*300+j*3] = -1+(0.02f*j);
				field[i*300+j*3+1] = 0;
				field[i*300+j*3+2] = -1+(0.02f*i);
			}
		}

		return field;
	}

	public static float[] setupNormals() {
		return new float[] { 
				0, 0, 1,
				0, 1, 0,
				1, 0, 0,
				0, 0, -1,
				0, -1, 0,
				-1, 0, 0 
		};
	}

	public static float[] setupVerticesColor() {
		float[] c = new float[30000];
		for(int x = 0; x < 100; x++)
			for(int y = 0; y < 100; y++) {
				c[3*x+300*y] = x/100.0f;
				c[1+3*x+300*y] = y/100.0f;
				// blue = 0
			}
		return c;
	}
}
