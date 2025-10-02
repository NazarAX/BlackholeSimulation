package com.nazarii;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class BlackholeSimulator {

    private long window;
    private int program;
    private int vao;
    private int uTime, uRes, uMouse;

    public static void main(String[] args) {
        new BlackholeSimulator().run();
    }

    void run() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        int width = 1280, height = 720;
        window = glfwCreateWindow(width, height, "LWJGL Black Hole", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();


        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        program = createProgram(VS_SRC, FS_SRC);
        glUseProgram(program);

        uTime = glGetUniformLocation(program, "uTime");
        uRes  = glGetUniformLocation(program, "uRes");
        uMouse= glGetUniformLocation(program, "uMouse");

        long start = System.nanoTime();

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            int[] w = new int[1], h = new int[1];
            glfwGetFramebufferSize(window, w, h);
            glViewport(0, 0, w[0], h[0]);

            double[] mx = new double[1], my = new double[1];
            glfwGetCursorPos(window, mx, my);

            float time = (float)((System.nanoTime() - start) * 1e-9);

            glClear(GL_COLOR_BUFFER_BIT);

            glUseProgram(program);
            glUniform1f(uTime, time);
            glUniform2f(uRes, (float)w[0], (float)h[0]);
            glUniform2f(uMouse, (float)mx[0], (float)my[0]);

            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);

            glfwSwapBuffers(window);
        }

        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback.createPrint(System.err).free();
    }

    private static int createProgram(String vs, String fs) {
        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs);
        glCompileShader(v);
        checkShader(v, "VERTEX");

        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs);
        glCompileShader(f);
        checkShader(f, "FRAGMENT");

        int p = glCreateProgram();
        glAttachShader(p, v);
        glAttachShader(p, f);
        glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL33.GL_FALSE) {
            throw new RuntimeException("Program link error: " + glGetProgramInfoLog(p));
        }
        glDeleteShader(v);
        glDeleteShader(f);
        return p;
    }

    private static void checkShader(int shader, String stage) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            throw new RuntimeException(stage + " SHADER COMPILE ERROR:\n" + log);
        }
    }

    private static final String VS_SRC =
            "#version 330 core\n" +
                    "out vec2 vUV;\n" +
                    "void main(){\n" +
                    "    // Correct fullscreen triangle covering the entire viewport\n" +
                    "    vec2 pos;\n" +
                    "    if (gl_VertexID == 0) pos = vec2(-1.0, -1.0);\n" +
                    "    else if (gl_VertexID == 1) pos = vec2( 3.0, -1.0);\n" +
                    "    else pos = vec2(-1.0,  3.0);\n" +
                    "\n" +
                    "    vUV = pos * 0.5 + 0.5; // maps to ~[0,1] over the screen\n" +
                    "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
                    "}\n";

    private static final String FS_SRC =
            "#version 330 core\n" +
                    "out vec4 FragColor;\n" +
                    "in vec2 vUV;\n" +
                    "\n" +
                    "uniform float uTime;\n" +
                    "uniform vec2  uRes;\n" +
                    "uniform vec2  uMouse;\n" +
                    "\n" +
                    "float hash(vec2 p){\n" +
                    "    p = fract(p * vec2(123.34, 456.21));\n" +
                    "    p += dot(p, p + 45.32);\n" +
                    "    return fract(p.x * p.y);\n" +
                    "}\n" +
                    "\n" +
                    "float noise(vec2 p){\n" +
                    "    vec2 i = floor(p);\n" +
                    "    vec2 f = fract(p);\n" +
                    "    float a = hash(i);\n" +
                    "    float b = hash(i + vec2(1.0, 0.0));\n" +
                    "    float c = hash(i + vec2(0.0, 1.0));\n" +
                    "    float d = hash(i + vec2(1.0, 1.0));\n" +
                    "    vec2 u = f*f*(3.0 - 2.0*f);\n" +
                    "    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);\n" +
                    "}\n" +
                    "\n" +
                    "vec3 stars(vec2 uv){\n" +
                    "    vec2 g = uv * 800.0;\n" +
                    "    float n = noise(g);\n" +
                    "    float s = smoothstep(0.992, 1.0, n); // slightly denser\n" +
                    "    s *= 0.6 + 0.4*sin(20.0*n + uTime*3.0);\n" +
                    "    return vec3(s) * 1.2;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 heat(float t){\n" +
                    "    t = clamp(t, 0.0, 1.0);\n" +
                    "    vec3 c;\n" +
                    "    if (t < 0.5) {\n" +
                    "        float k = t/0.5;\n" +
                    "        c = mix(vec3(0.03, 0.0, 0.0), vec3(0.85, 0.25, 0.02), k);\n" +
                    "    } else {\n" +
                    "        float k = (t-0.5)/0.5;\n" +
                    "        c = mix(vec3(0.85, 0.25, 0.02), vec3(1.05, 0.98, 0.82), k);\n" +
                    "    }\n" +
                    "    return c;\n" +
                    "}\n" +
                    "\n" +
                    "vec2 center(){\n" +
                    "    vec2 m = uMouse / uRes;\n" +
                    "    return vec2(0.5) + (m - 0.5)*0.2;\n" +
                    "}\n" +
                    "const float eventRadius = 0.12;\n" +
                    "const float photonRing  = 0.18;\n" +
                    "const float diskInner   = 0.16;\n" +
                    "const float diskOuter   = 0.45;\n" +
                    "\n" +
                    "vec2 lensWarp(vec2 uv, vec2 c){\n" +
                    "    vec2 d = uv - c;\n" +
                    "    float r = length(d);\n" +
                    "    float eps = 1e-4;\n" +
                    "    float strength = 0.08;\n" +
                    "    float bend = strength / max(r, eps);\n" +
                    "    bend += 0.06 * exp(-pow((r - photonRing), 2.0) / (2.0*0.03*0.03));\n" +
                    "    bend = clamp(bend, 0.0, 0.22);\n" +
                    "    return uv - normalize(d) * bend;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 accretion(vec2 uv, vec2 c){\n" +
                    "    vec2 d = uv - c;\n" +
                    "    float r = length(d);\n" +
                    "    float angle = atan(d.y, d.x);\n" +
                    "    float ring = smoothstep(diskOuter, diskOuter-0.01, r) * smoothstep(diskInner, diskInner+0.01, r);\n" +
                    "    if (ring < 1e-4) return vec3(0.0);\n" +
                    "    float spin = angle - uTime * 0.7;\n" +
                    "    float lanes = 0.6 + 0.4*noise(vec2(spin*6.0, r*40.0 + uTime*2.0));\n" +
                    "    float heatT = lanes;\n" +
                    "    float doppler = 0.5 + 0.5*cos(angle - 1.57);\n" +
                    "    doppler = pow(doppler, 1.5);\n" +
                    "    float fade = smoothstep(eventRadius+0.02, eventRadius+0.10, r);\n" +
                    "    vec3 col = heat(heatT) * doppler * fade * 1.4;\n" +
                    "    return col * ring;\n" +
                    "}\n" +
                    "\n" +
                    "void main(){\n" +
                    "    vec2 uv = vUV;\n" +
                    "    vec2 c  = center();\n" +
                    "\n" +
                    "    vec2 warped = lensWarp(uv, c);\n" +
                    "    vec3 col = stars(warped);\n" +
                    "\n" +
                    "    float r = distance(uv, c);\n" +
                    "\n" +
                    "    col += accretion(warped, c);\n" +
                    "\n" +
            "    float ringGlow = exp(-pow((r - photonRing), 2.0) / (2.0*0.008*0.008)) * 0.7;\n" +
            "    col += vec3(1.1, 0.95, 0.8) * ringGlow;\n" +
            "\n" +
            "    float vign = smoothstep(1.30, 0.90, length(uv - 0.5));\n" +
            "    col *= vign;\n" +
            "\n" +
            "    float hole = smoothstep(eventRadius, eventRadius - 0.0005, r);\n" +
            "    col = mix(vec3(0.0), col, hole);\n" +
            "\n" +
            "    float exposure = 1.1;\n" +
            "    col = vec3(1.0) - exp(-col * exposure);\n" +
            "    col = col / (1.0 + col);\n" +
            "\n" +
            "    FragColor = vec4(col, 1.0);\n" +
            "}\n";

}
