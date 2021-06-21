/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Valery Volgutov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
#include <glib.h>
#include <string.h>
#include <EGL/egl.h> // requires ndk r5 or newer
#include <GLES2/gl2.h>
#include <math.h>
#include <android/native_window.h>

#include "frameflipper.h"
#include "debug.h"


// #define GL_CHECK(code) { code; LOGD("glcode:%s (%x)", #code, glGetError()); }
#define GL_CHECK(code) { code; }
#define GLSL(shader) #shader

namespace mat4 {
    enum {
        xx = 0,     xy = 1,     xz = 2,     xw = 3,
        yx = 4,     yy = 5,     yz = 6,     yw = 7,
        zx = 8,     zy = 9,     zz = 10,    zw = 11,
        tx = 12,    ty = 13,    tz = 14,    tw = 15
    };

    void axisangle2quat(float angle, float ax, float ay, float az,
                         float* qx, float* qy, float* qz, float* qw)
    {
        float inv_len = 1.0f/(float)sqrtf(ax * ax + ay * ay + az * az);
        ax *= inv_len;
        ay *= inv_len;
        az *= inv_len;
        float aa = (angle) * float(0.00872664626f);
        float aaf = aa;
        float sin_a((float)sinf(aaf));
        *qw = float(cosf(aaf));
        *qx = sin_a * ax;
        *qy = sin_a * ay;
        *qz = sin_a * az;
    }

    void mat_rotatequat(float qx, float qy, float qz, float qw, float* m)
    {
        float _wx, _wy, _wz, _xx, _yy, _yz, _xy, _xz, _zz, _x2, _y2, _z2;
        float len = qx * qx + qy * qy + qz * qz + qw * qw;
        if( len != 1 ) {
            float inv_len = float(1)/(float)sqrtf(len);
            qx = qx * inv_len;
            qy = qy * inv_len;
            qz = qz * inv_len;
            qw = qw * inv_len;
        }
        _x2 = qx + qx;
        _y2 = qy + qy;
        _z2 = qz + qz;

        _xx = qx * _x2;
        _xy = qx * _y2;
        _xz = qx * _z2;

        _yy = qy * _y2;
        _yz = qy * _z2;
        _zz = qz * _z2;

        _wx = qw * _x2;
        _wy = qw * _y2;
        _wz = qw * _z2;

        m[xx] = 1 - (_yy + _zz);
        m[yx] = _xy - _wz;
        m[zx] = _xz + _wy;
        m[tx] = 0;

        m[xy] = _xy + _wz;
        m[yy] = 1 - (_xx + _zz);
        m[zy] = _yz - _wx;
        m[ty] = 0;

        m[xz] = _xz - _wy;
        m[yz] = _yz + _wx;
        m[zz] = 1 - (_xx + _yy);
        m[tz] = 0;

        m[xw] = 0;
        m[yw] = 0;
        m[zw] = 0;
        m[tw] = 1;
    }

    void identity(float* m)
    {
        m[xx] = 1.0f; m[xy] = 0.0f; m[xz] = 0.0f; m[xw] = 0.0f;
        m[yx] = 0.0f; m[yy] = 1.0f; m[yz] = 0.0f; m[yw] = 0.0f;
        m[zx] = 0.0f; m[zy] = 0.0f; m[zz] = 1.0f; m[zw] = 0.0f;
        m[tx] = 0.0f; m[ty] = 0.0f; m[tz] = 0.0f; m[tw] = 1.0f;
    }

    void multiply(float* l, float const *r)
    {
        float t[16];

        t[xx] = l[xx]*r[xx]+l[yx]*r[xy]+l[zx]*r[xz]+l[tx]*r[xw];
        t[yx] = l[xx]*r[yx]+l[yx]*r[yy]+l[zx]*r[yz]+l[tx]*r[yw];
        t[zx] = l[xx]*r[zx]+l[yx]*r[zy]+l[zx]*r[zz]+l[tx]*r[zw];
        t[tx] = l[xx]*r[tx]+l[yx]*r[ty]+l[zx]*r[tz]+l[tx]*r[tw];

        t[xy] = l[xy]*r[xx]+l[yy]*r[xy]+l[zy]*r[xz]+l[ty]*r[xw];
        t[yy] = l[xy]*r[yx]+l[yy]*r[yy]+l[zy]*r[yz]+l[ty]*r[yw];
        t[zy] = l[xy]*r[zx]+l[yy]*r[zy]+l[zy]*r[zz]+l[ty]*r[zw];
        t[ty] = l[xy]*r[tx]+l[yy]*r[ty]+l[zy]*r[tz]+l[ty]*r[tw];

        t[xz] = l[xz]*r[xx]+l[yz]*r[xy]+l[zz]*r[xz]+l[tz]*r[xw];
        t[yz] = l[xz]*r[yx]+l[yz]*r[yy]+l[zz]*r[yz]+l[tz]*r[yw];
        t[zz] = l[xz]*r[zx]+l[yz]*r[zy]+l[zz]*r[zz]+l[tz]*r[zw];
        t[tz] = l[xz]*r[tx]+l[yz]*r[ty]+l[zz]*r[tz]+l[tz]*r[tw];

        t[xw] = l[xw]*r[xx]+l[yw]*r[xy]+l[zw]*r[xz]+l[tw]*r[xw];
        t[yw] = l[xw]*r[yx]+l[yw]*r[yy]+l[zw]*r[yz]+l[tw]*r[yw];
        t[zw] = l[xw]*r[zx]+l[yw]*r[zy]+l[zw]*r[zz]+l[tw]*r[zw];
        t[tw] = l[xw]*r[tx]+l[yw]*r[ty]+l[zw]*r[tz]+l[tw]*r[tw];

        memcpy(l, t, sizeof(float) * 16);
    }

    void translate(float *m, float x, float y, float z)
    {
        m[tx] += m[xx]*x+m[yx]*y+m[zx]*z;
        m[ty] += m[xy]*x+m[yy]*y+m[zy]*z;
        m[tz] += m[xz]*x+m[yz]*y+m[zz]*z;
        m[tw] += m[xw]*x+m[yw]*y+m[zw]*z;
    }

    void scale(float *m, float sx, float sy, float sz)
    {
        m[xx] *= sx; m[yx] *= sy; m[zx] *= sz;
        m[xy] *= sx; m[yy] *= sy; m[zy] *= sz;
        m[xz] *= sx; m[yz] *= sy; m[zz] *= sz;
        m[xw] *= sx; m[yw] *= sy; m[zw] *= sz;
    }

    void rotate(float *m, float a, float ax, float ay, float az)
    {
        float t[16];
        float qx, qy,qz,qw;
        axisangle2quat(a, ax, ay, az, &qx, &qy, &qz, &qw);
        mat_rotatequat(qx, qy, qz, qw, t);
        multiply(m, t);
    }

    void invert(float *m)
    {
        float a9 = m[zx] * m[ty] - m[zy] * m[tx];
        float aa = m[zx] * m[tz] - m[zz] * m[tx];
        float a8 = m[zx] * m[tw] - m[zw] * m[tx];

        float a2 = m[zy] * m[tz] - m[zz] * m[ty];
        float a5 = m[zy] * m[tw] - m[zw] * m[ty];
        float a3 = m[zz] * m[tw] - m[zw] * m[tz];

        float t1 = m[yy] * a3 - m[yz] * a5 + m[yw] * a2;
        float t2 = m[yx] * a3 - m[yz] * a8 + m[yw] * aa;
        float t3 = m[yx] * a5 - m[yy] * a8 + m[yw] * a9;
        float t4 = m[yx] * a2 - m[yy] * aa + m[yz] * a9;

        float det=m[xx] * t1 - m[xy] * t2 + m[xz] * t3 - m[xw] * t4;
        if( det == 0 ) {
            return;
        }

        det = 1.0f / det;

        float a0 = m[yx] * m[zy] - m[yy] * m[zx];
        float af = m[yx] * m[ty] - m[yy] * m[tx];
        float ac = m[yy] * m[zz] - m[yz] * m[zy];
        float a6 = m[yy] * m[tz] - m[yz] * m[ty];

        float ab = m[yz] * m[zw] - m[yw] * m[zz];
        float a7 = m[yz] * m[tw] - m[yw] * m[tz];
        float ae = m[yx] * m[zw] - m[yw] * m[zx];
        float ad = m[yx] * m[tw] - m[yw] * m[tx];

        float b0 = m[yy] * m[zw] - m[yw] * m[zy];
        float b1 = m[yx] * m[tz] - m[yz] * m[tx];
        float b2 = m[yx] * m[zz] - m[yz] * m[zx];
        float b3 = m[yy] * m[tw] - m[yw] * m[ty];

        float t[16];

        t[xx] =  det * t1;
        t[xy] = -det * (m[xy] * a3 - m[xz] * a5 + m[xw] * a2);
        t[xz] =  det * (m[xy] * a7 - m[xz] * b3 + m[xw] * a6);
        t[xw] = -det * (m[xy] * ab - m[xz] * b0 + m[xw] * ac);

        t[yx] = -det * t2;
        t[yy] =  det * (m[xx] * a3 - m[xz] * a8 + m[xw] * aa);
        t[yz] = -det * (m[xx] * a7 - m[xz] * ad + m[xw] * b1);
        t[yw] =  det * (m[xx] * ab - m[xz] * ae + m[xw] * b2);

        t[zx] =  det * t3;
        t[zy] = -det * (m[xx] * a5 - m[xy] * a8 + m[xw] * a9);
        t[zz] =  det * (m[xx] * b3 - m[xy] * ad + m[xw] * af);
        t[zw] = -det * (m[xx] * b0 - m[xy] * ae + m[xw] * a0);

        t[tx] = -det * t4;
        t[ty] =  det * (m[xx] * a2 - m[xy] * aa + m[xz] * a9);
        t[tz] = -det * (m[xx] * a6 - m[xy] * b1 + m[xz] * af);
        t[tw] =  det * (m[xx] * ac - m[xy] * b2 + m[xz] * a0);

        memcpy(m, t, sizeof(float) * 16);
    }

    void transform(float *m, float *_x, float *_y, float *_z, float *_w)
    {
        float x(*_x);
        float y(*_y);
        float z(*_z);
        float w(*_w);
        *_x = (x*(m[xx])+y*(m[yx])+z*(m[zx])+w*(m[tx]));
        *_y = (x*(m[xy])+y*(m[yy])+z*(m[zy])+w*(m[ty]));
        *_z = (x*(m[xz])+y*(m[yz])+z*(m[zz])+w*(m[tz]));
        *_w = (x*(m[xw])+y*(m[yw])+z*(m[zw])+w*(m[tw]));
    }
}

class AndroidRenderTarget
    : public RenderTarget
{
public:
    AndroidRenderTarget(ANativeWindow* window,
                        RenderTarget::Format format);
    virtual ~AndroidRenderTarget();

private:
    virtual bool hasNPOT() const;
    virtual bool initialize();
    virtual void destroy();
    virtual void ogl2scr(float x, float y, size_t *sx, size_t *sy);
    virtual void scr2ogl(size_t sx, size_t sy, float *x, float *y);
    virtual void drawFrame(float color[4],
                           Orientation orientation,
                           FillMode fillMode,
                           float scaleX, float scaleY, float scaleFactor,
                           size_t width, size_t height,
                           size_t crWidth, size_t crHeight);
    virtual void setFrame(FrameFlipper::frame_data_t data,
                          size_t width, size_t height,
                          FrameFlipper::FrameFormat format);

private:
    // android window, supported by NDK r5 and newer
    ANativeWindow* m_window;

    EGLDisplay m_display;
    EGLSurface m_surface;
    EGLContext m_context;
    size_t m_windowWidth;
    size_t m_windowHeight;
    RenderTarget::Format m_windowFormat;

    GLuint m_frame[3];
    GLuint m_vsh;
    GLuint m_fsh;
    GLuint m_program;
    GLint a_position;
    GLint a_texture_coordinates;
    GLint u_texture_unit;

    //I420
    GLuint m_programI420;
    GLuint m_fshI420;
    GLint y_tex;
    GLint u_tex;
    GLint v_tex;
    GLint yuv2rgb_matrix;

    bool m_hasNPOT;
};

RenderTarget *RenderTarget::create(void* w,
                                   RenderTarget::Format format)
{
    ANativeWindow* window = (ANativeWindow*)w;
    return new AndroidRenderTarget(window, format);
}

static GLchar const *g_vertexShader = GLSL(
    attribute vec4 a_Position;
    attribute vec2 a_TextureCoordinates;

    varying vec2 v_TextureCoordinates;

    void main()
    {
        v_TextureCoordinates = a_TextureCoordinates.xy;
        gl_Position = a_Position;
    }
);

static GLchar const *g_fragmentShader = GLSL(
    precision mediump float;

    uniform sampler2D u_TextureUnit;
    varying vec2 v_TextureCoordinates;

    void main()
    {
        gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates);
        // gl_FragColor = vec4(1, 1, 0, 1);
    }
);

static GLchar const *g_fragmentShaderI420 = GLSL(
    precision mediump float;
    varying vec2 v_TextureCoordinates;

    uniform sampler2D y_tex;
    uniform sampler2D u_tex;
    uniform sampler2D v_tex;
    uniform mat3 yuv2rgb;

    void main()
    {
        float y = texture2D(y_tex, v_TextureCoordinates).x;
        float u = texture2D(u_tex, v_TextureCoordinates).r - 0.5;
        float v = texture2D(v_tex, v_TextureCoordinates).r - 0.5;
        vec3 rgb = yuv2rgb * vec3(y, u, v);
        gl_FragColor = vec4(rgb, 1);
        // gl_FragColor = vec4(1, 1, 0, 1);
    }
);

// Matrix used for the YUV to RGB conversion.
static const GLfloat kYUV2RGB[9] = {
    1.f,       1.f,    1.f,
    0.f,    -.344f, 1.772f,
    1.403f, -.714f,    0.f,
};

GLenum format2gl(FrameFlipper::FrameFormat format)
{
    switch (format) {
        case FrameFlipper::I420:
            return GL_LUMINANCE;
        case FrameFlipper::RGB565:
        case FrameFlipper::RGB24:
            return GL_RGB;
        case FrameFlipper::RGBA4444:
        case FrameFlipper::RGBA5551:
        case FrameFlipper::RGBA32:
            return GL_RGBA;
    }
    LOGE("sink::FrameFlipper::unknown frame format: %d", format);
    return GL_RGB;
}

GLenum format2type(FrameFlipper::FrameFormat format)
{
    switch (format) {
        case FrameFlipper::I420:
        case FrameFlipper::RGB24:
        case FrameFlipper::RGBA32:
            return GL_UNSIGNED_BYTE;
        case FrameFlipper::RGB565:
            return GL_UNSIGNED_SHORT_5_6_5;
        case FrameFlipper::RGBA4444:
            return GL_UNSIGNED_SHORT_4_4_4_4;
        case FrameFlipper::RGBA5551:
            return GL_UNSIGNED_SHORT_5_5_5_1;
    }
    LOGE("sink::FrameFlipper::unknown frame format: %d", format);
    return GL_UNSIGNED_BYTE;
}

AndroidRenderTarget::AndroidRenderTarget(ANativeWindow* window,
                                         RenderTarget::Format format)
    : m_window(window)
    , m_display(EGL_NO_DISPLAY)
    , m_surface(EGL_NO_SURFACE)
    , m_context(EGL_NO_CONTEXT)
    , m_windowWidth(0)
    , m_windowHeight(0)
    , m_windowFormat(format)
    , m_vsh(0)
    , m_fsh(0)
    , m_program(0)
    , m_hasNPOT(false)
{
    m_frame[0] = m_frame[1] = m_frame[2] = 0;
}

AndroidRenderTarget::~AndroidRenderTarget()
{
    destroy();
}

bool AndroidRenderTarget::hasNPOT() const
{
    return m_hasNPOT;
}

bool AndroidRenderTarget::initialize()
{
    const EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_SURFACE_TYPE,    EGL_WINDOW_BIT,
        EGL_RED_SIZE,        (m_windowFormat == RGB24 ? 8 : 5),
        EGL_GREEN_SIZE,      (m_windowFormat == RGB24 ? 8 : 6),
        EGL_BLUE_SIZE,       (m_windowFormat == RGB24 ? 8 : 5),
        EGL_NONE
    };

    EGLConfig config;
    EGLint numConfigs;
    EGLint format;
    EGLint width;
    EGLint height;
    EGLint major, minor;

    LOGD("sink::Initializing context");

    if ((m_display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGE("sink::eglGetDisplay() returned error %x", eglGetError());
        return false;
    }
    if (!eglInitialize(m_display, &major, &minor)) {
        LOGE("sink::eglInitialize() returned error %x", eglGetError());
        return false;
    }
    LOGD("sink:: EGL %d.%d", major, minor);

    if (!eglChooseConfig(m_display, attribs, &config, 1, &numConfigs)) {
        LOGE("sink::eglChooseConfig() returned error %x", eglGetError());
        destroy();
        return false;
    }

    LOGD("sink:: numConfigs = %d", numConfigs);

    if (!eglGetConfigAttrib(m_display, config, EGL_NATIVE_VISUAL_ID, &format)) {
        LOGE("sink::eglGetConfigAttrib() returned error %x", eglGetError());
        destroy();
        return false;
    }

    LOGD("sink: native format: (%p) %d", m_window, format);

    ANativeWindow_setBuffersGeometry(m_window, 0, 0, format);

    if (!(m_surface = eglCreateWindowSurface(m_display, config, m_window, 0))) {
        LOGE("sink::eglCreateWindowSurface() returned error %x", eglGetError());
        destroy();
        return false;
    }

    int attrib_list[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    if (!(m_context = eglCreateContext(m_display, config, EGL_NO_CONTEXT, attrib_list))) {
        LOGE("sink::eglCreateContext() returned error %x", eglGetError());
        destroy();
        return false;
    }

    if (!eglMakeCurrent(m_display, m_surface, m_surface, m_context)) {
        LOGE("sink::eglMakeCurrent() returned error %x", eglGetError());
        destroy();
        return false;
    }

    if (!eglQuerySurface(m_display, m_surface, EGL_WIDTH, &width) ||
        !eglQuerySurface(m_display, m_surface, EGL_HEIGHT, &height)) {
        LOGE("sink::eglQuerySurface() returned error %x", eglGetError());
        destroy();
        return false;
    }

    m_windowWidth = width;
    m_windowHeight = height;

    GLint status;
    GLint length;

    //Prepare shaders
    GL_CHECK(m_vsh = glCreateShader(GL_VERTEX_SHADER));
    GL_CHECK(m_fsh = glCreateShader(GL_FRAGMENT_SHADER));
    GL_CHECK(m_fshI420 = glCreateShader(GL_FRAGMENT_SHADER));

    length = strlen(g_vertexShader);
    GL_CHECK(glShaderSource(m_vsh, 1, (const GLchar **)&g_vertexShader, &length));
    GL_CHECK(glCompileShader(m_vsh));
    GL_CHECK(glGetShaderiv(m_vsh, GL_COMPILE_STATUS, &status));
    // LOGD("sink::compile status: %x:%d", glGetError(), status);

    length = strlen(g_fragmentShader);
    GL_CHECK(glShaderSource(m_fsh, 1, (const GLchar **)&g_fragmentShader, &length));
    GL_CHECK(glCompileShader(m_fsh));
    GL_CHECK(glGetShaderiv(m_fsh, GL_COMPILE_STATUS, &status));
    // LOGD("sink::compile status: %x:%d", glGetError(), status);

    length = strlen(g_fragmentShaderI420);
    GL_CHECK(glShaderSource(m_fshI420, 1, (const GLchar **)&g_fragmentShaderI420, &length));
    GL_CHECK(glCompileShader(m_fshI420));
    GL_CHECK(glGetShaderiv(m_fshI420, GL_COMPILE_STATUS, &status));
    // LOGD("sink::compile status: %x:%d", glGetError(), status);

    GL_CHECK(m_program = glCreateProgram());
    GL_CHECK(glAttachShader(m_program, m_vsh));
    GL_CHECK(glAttachShader(m_program, m_fsh));
    GL_CHECK(glLinkProgram(m_program));
    GL_CHECK(glGetProgramiv(m_program, GL_LINK_STATUS, &status));
    // LOGD("sink::link status: %x:%d", glGetError(), status);

    GL_CHECK(m_programI420 = glCreateProgram());
    GL_CHECK(glAttachShader(m_programI420, m_vsh));
    GL_CHECK(glAttachShader(m_programI420, m_fshI420));
    GL_CHECK(glLinkProgram(m_programI420));
    GL_CHECK(glGetProgramiv(m_programI420, GL_LINK_STATUS, &status));
    // LOGD("sink::link status: %x:%d", glGetError(), status);

    GL_CHECK(a_position = glGetAttribLocation(m_program, "a_Position"));
    GL_CHECK(a_texture_coordinates = glGetAttribLocation(m_program, "a_TextureCoordinates"));
    GL_CHECK(u_texture_unit = glGetUniformLocation(m_program, "u_TextureUnit"));
    GL_CHECK(y_tex = glGetUniformLocation(m_programI420, "y_tex"));
    GL_CHECK(u_tex = glGetUniformLocation(m_programI420, "u_tex"));
    GL_CHECK(v_tex = glGetUniformLocation(m_programI420, "v_tex"));
    GL_CHECK(yuv2rgb_matrix = glGetUniformLocation(m_programI420, "yuv2rgb"));

    const char *extension = (char const *)glGetString(GL_EXTENSIONS);
    m_hasNPOT = !!strstr(extension, "texture_npot");
    LOGD("sink::npot texture %d", m_hasNPOT);

    LOGD("sink:: initialization done: (0x%p, 0x%p, 0x%p, %dx%d)",
            m_display, m_surface, m_context, m_windowWidth, m_windowHeight);

    return true;
}

void AndroidRenderTarget::destroy()
{
    LOGD("sink:Destroying context (%p, %p, %p)", m_display, m_surface, m_context);

    if (m_frame[0])
        glDeleteTextures(1, &m_frame[0]);
    if (m_frame[1])
        glDeleteTextures(2, &m_frame[1]);
    if (m_frame[2])
        glDeleteTextures(3, &m_frame[2]);
    if (m_program)
        glDeleteProgram(m_program);
    if (m_vsh)
        glDeleteShader(m_vsh);
    if (m_fsh)
        glDeleteShader(m_fsh);
    if (m_programI420)
        glDeleteProgram(m_programI420);
    if (m_fshI420)
        glDeleteShader(m_fshI420);

    m_frame[0] = m_frame[1] = m_frame[2] = 0;
    m_program = 0;
    m_vsh = 0;
    m_fsh = 0;
    m_fshI420 = 0;
    m_programI420 = 0;

    if (m_display != EGL_NO_DISPLAY) {
        eglMakeCurrent(m_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (m_context != EGL_NO_CONTEXT)
            eglDestroyContext(m_display, m_context);
        if (m_surface != EGL_NO_SURFACE)
            eglDestroySurface(m_display, m_surface);

        eglTerminate(m_display);
    }

    m_display = EGL_NO_DISPLAY;
    m_surface = EGL_NO_SURFACE;
    m_context = EGL_NO_CONTEXT;
}

void AndroidRenderTarget::ogl2scr(float x, float y, size_t *sx, size_t *sy)
{
    if (sx)
        *sx = (1.0 + x) * m_windowWidth;
    if (sy)
        *sy = (1.0 + y) * m_windowHeight;
}

void AndroidRenderTarget::scr2ogl(size_t sx, size_t sy, float *x, float *y)
{
    if (x)
        *x = 2 * (float)sx / (float)m_windowWidth - 1.0;
    if (y)
        *y = 2 * (float)sy / (float)m_windowHeight - 1.0;
}

void AndroidRenderTarget::drawFrame(float color[4],
                                           Orientation orientation,
                                           FillMode fillMode,
                                           float scaleX, float scaleY, float scaleFactor,
                                           size_t width, size_t height,
                                           size_t crWidth, size_t crHeight)
{
    // Calculate aspect, fillMode, rotation
    // static float orientationValues[] = {0, 90, 180, 270};
    // float rotation = orientationValues[orientation];
    //
    // LOGD("sink: drawFrame");

    float w = orientation == ROTATION_90 || orientation == ROTATION_270 ?
        m_windowHeight : m_windowWidth;
    float h = orientation == ROTATION_90 || orientation == ROTATION_270 ?
        m_windowWidth : m_windowHeight;
    float widthScale = w / (float)width;
    float heightScale = h / (float)height;
    float paintedWidth, paintedHeight;
    float aspectX, aspectY;

    switch (fillMode) {
        case STRETCH:
            aspectX = (float)width / (float)crWidth;
            aspectY = (float)height / (float)crHeight;
            break;
        case PRESERVE_ASPECT_FIT:
            if (widthScale <= heightScale) {
                paintedWidth = w;
                paintedHeight = widthScale * (float)height;
            } else if (heightScale < widthScale) {
                paintedWidth = heightScale * (float)width;
                paintedHeight = h;
            }
            break;
        case PRESERVE_ASPECT_CROP:
            if (widthScale < heightScale) {
                widthScale = heightScale;
            } else if (heightScale < widthScale) {
                heightScale = widthScale;
            }
            paintedHeight = heightScale * (float)height;
            paintedWidth = widthScale * (float)width;
            break;
    }
    aspectX = paintedWidth / w;
    aspectY = paintedHeight / h;

    GLfloat vertices[] = {
        -1.0f, -1.0f, 0.0f, 1.0f,
         1.0f, -1.0f, 0.0f, 1.0f,
        -1.0f,  1.0f, 0.0f, 1.0f,
         1.0f,  1.0f, 0.0f, 1.0f,
    };

    const GLfloat texCoords[] = {
        0.0f, (float)height / (float)crHeight,
        (float)width / (float)crWidth, (float)height / (float)crHeight,
        0.0f, 0.0f,
        (float)width / (float)crWidth, 0.0f,
    };

    float m[16];
    mat4::identity(m);
    // mat4::rotate(m, rotation, 0, 0, 1);
    mat4::scale(m, aspectX, aspectY, 1);
    mat4::translate(m, -scaleX, -scaleY, 0);
    mat4::scale(m, scaleFactor, scaleFactor, scaleFactor);
    mat4::translate(m, scaleX, scaleY, 0);

    //FIXME: Elephone P8000 has gles driver error (need to apply matrix manually)
    for (int i = 0; i < 4; ++i)
        mat4::transform(m, &vertices[i * 4 + 0], &vertices[i * 4 + 1], &vertices[i * 4 + 2], &vertices[i * 4 + 3]);

    GL_CHECK(glViewport(0, 0, w, h));
    GL_CHECK(glClearColor(color[0], color[1], color[2], color[3]));
    GL_CHECK(glClear(GL_COLOR_BUFFER_BIT));

    GL_CHECK(glVertexAttribPointer(a_position, 4, GL_FLOAT, 0, 0, vertices));
    GL_CHECK(glEnableVertexAttribArray(a_position));
    GL_CHECK(glVertexAttribPointer(a_texture_coordinates, 2, GL_FLOAT, 0, 0, texCoords));
    GL_CHECK(glEnableVertexAttribArray(a_texture_coordinates));

    GL_CHECK(glDrawArrays(GL_TRIANGLE_STRIP, 0, 4));
    // LOGD("sink::glDrawArrays: %x", glGetError());

    if (!eglSwapBuffers(m_display, m_surface)) {
        LOGE("sink::eglSwapBuffers() returned error %x(%p)", eglGetError(), m_surface);
    }
}

void AndroidRenderTarget::setFrame(FrameFlipper::frame_data_t data,
                                   size_t width, size_t height,
                                   FrameFlipper::FrameFormat format)
{
    GLenum glFormat = format2gl(format);
    GLenum glType = format2type(format);

    // LOGD("sink: android_setFrame: (%dx%d)", width, height);
    if (format == FrameFlipper::I420) {
        GL_CHECK(glUseProgram(m_programI420));
        GL_CHECK(glUniform1i(y_tex, 0));
        GL_CHECK(glUniform1i(u_tex, 1));
        GL_CHECK(glUniform1i(v_tex, 2));
        GL_CHECK(glUniformMatrix3fv(yuv2rgb_matrix, 1, GL_FALSE, kYUV2RGB));


        if (!m_frame[0]) {
            GL_CHECK(glGenTextures(3, m_frame));

            GL_CHECK(glActiveTexture(GL_TEXTURE0));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[0]));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexImage2D(GL_TEXTURE_2D, 0, glFormat,
                    width, height, 0, glFormat, glType, data));

            GL_CHECK(glActiveTexture(GL_TEXTURE0 + 1));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[1]));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexImage2D(GL_TEXTURE_2D, 0, glFormat,
                    width / 2, height / 2, 0, glFormat, glType, data + width * height));

            GL_CHECK(glActiveTexture(GL_TEXTURE0 + 2));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[2]));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexImage2D(GL_TEXTURE_2D, 0, glFormat,
                    width / 2, height / 2, 0, glFormat, glType, data + width * height + width / 2 * height / 2));
        } else {
            GL_CHECK(glActiveTexture(GL_TEXTURE0));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[0]));
            GL_CHECK(glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0,
                    width, height, glFormat, glType, data));

            GL_CHECK(glActiveTexture(GL_TEXTURE0 + 1));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[1]));
            GL_CHECK(glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0,
                    width / 2, height / 2, glFormat, glType, data + width * height));

            GL_CHECK(glActiveTexture(GL_TEXTURE0 + 2));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[2]));
            GL_CHECK(glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0,
                    width / 2, height / 2, glFormat, glType, data + width * height + width / 2 * height / 2));
        }
    }
    else {
        if (!m_frame[0]) {
            GL_CHECK(glActiveTexture(GL_TEXTURE0));
            GL_CHECK(glGenTextures(1, m_frame));
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[0]));
            GL_CHECK(glTexImage2D(GL_TEXTURE_2D, 0, glFormat,
                    width, height, 0, glFormat, glType, data));

            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE));
            GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE));
            // LOGD("sink::createTexture %dx%d %x", width, height, glGetError());
        } else {
            GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[0]));
            GL_CHECK(glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0,
                    width, height, glFormat, glType, data));
            // LOGD("sink::updateTexture %x", glGetError());
        }
        GL_CHECK(glUseProgram(m_program));
        GL_CHECK(glActiveTexture(GL_TEXTURE0));
        GL_CHECK(glBindTexture(GL_TEXTURE_2D, m_frame[0]));
        GL_CHECK(glUniform1i(u_texture_unit, 0));
    }
}
