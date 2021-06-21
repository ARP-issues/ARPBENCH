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
#include <utility>
#include <thread>
#include <mutex>
#include <chrono>

#include <stdint.h>
#include <memory.h>
#include <unistd.h>
#include <glib.h>

#include "frameflipper.h"
#include "debug.h"

static int pow2roundup(int x)
{
    if (x < 0)
        return 0;
    --x;
    x |= x >> 1;
    x |= x >> 2;
    x |= x >> 4;
    x |= x >> 8;
    x |= x >> 16;
    return (x + 1);
}

static bool ispot(int x)
{
    return x != 1 && (x & (x - 1));
}

static int formatBpp(FrameFlipper::FrameFormat format)
{
    switch (format) {
        case FrameFlipper::RGB24:
            return 3;
        case FrameFlipper::RGBA32:
            return 4;
        case FrameFlipper::RGB565:
        case FrameFlipper::RGBA4444:
        case FrameFlipper::RGBA5551:
            return 2;
    }
    LOGE("FrameFlipper::unknown frame format: %d", format);
    return 0;
}

struct FrameFlipper::FrameFlipperPriv
{
    enum RenderThreadMessage {
        MSG_NONE = 0,
        MSG_WINDOW_SET,
        MSG_FRAME_SET,
        MSG_UPDATE,
        MSG_RENDER_LOOP_EXIT
    };

    std::thread m_thread;
    std::mutex m_mutex;

    unsigned int m_statusInitialized: 1;
    unsigned int m_statusUpdate : 1;
    unsigned int m_statusSetFrame : 1;
    unsigned int m_statusExit: 1;
    unsigned int m_statusRunning: 1;
    std::shared_ptr<RenderTarget> m_target;

    size_t m_width;
    size_t m_height;
    size_t m_frameWidth;
    size_t m_frameHeight;
    size_t m_stride;
    FrameFlipper::frame_data_t m_frameData;
    FrameFlipper::frame_buffer_t m_frameBuffer;
    FrameFlipper::FrameFormat m_format;

    float m_scale;
    float m_scaleX;
    float m_scaleY;
    Orientation m_orientation;
    FillMode m_fillMode;
    float m_color[4];

    FrameFlipperPriv(std::shared_ptr<RenderTarget> target)
        : m_statusInitialized(false)
        , m_statusUpdate(false)
        , m_statusSetFrame(false)
        , m_statusExit(false)
        , m_statusRunning(false)
        , m_target(target)
        , m_width(0)
        , m_height(0)
        , m_frameWidth(0)
        , m_frameHeight(0)
        , m_stride(0)
        , m_frameData(NULL)
        , m_frameBuffer(NULL)
        , m_format(FrameFlipper::FORMAT_UNKNOWN)
        , m_scale(1.0)
        , m_scaleX(0.5)
        , m_scaleY(0.5)
        , m_orientation(ROTATION_0)
        , m_fillMode(PRESERVE_ASPECT_FIT)
    {
        m_color[0] = 0;
        m_color[1] = 0;
        m_color[2] = 0;
        m_color[3] = 0;

        m_thread = std::thread(renderCallback, this);
        m_thread.detach();
    }

    ~FrameFlipperPriv() {
        m_mutex.lock();
        m_statusExit = true;
        m_mutex.unlock();

        if (m_frameBuffer) {
            gst_buffer_unref(m_frameBuffer);
            m_frameBuffer = NULL;
        }

        if (m_frameData) {
            g_free(m_frameData);
            m_frameData = NULL;
        }
    }

    void start() {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_statusRunning = true;
    }

    void stop() {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_statusRunning = false;
    }

    void setFrame(frame_buffer_t data,
                  size_t width, size_t height,
                  size_t stride,
                  FrameFormat format)
    {
        if (!m_statusInitialized)
            return;

        // we must to create additional buffer if target doesn't support npot
        // textures
        if (!m_target->hasNPOT()) {
            GstMapInfo map;
            if (gst_buffer_map(data, &map, GST_MAP_READ)) {
                setFrame(map.data, width, height, stride, format);
                gst_buffer_unmap(data, &map);
            }
            gst_buffer_unref(data);
            return;
        }

        std::lock_guard<std::mutex> lock(m_mutex);

        m_width = width;
        m_height = height;
        m_format = format;
        m_stride = stride;
        m_frameWidth = width;
        m_frameHeight = height;

        if (m_frameBuffer) {
            gst_buffer_unref(m_frameBuffer);
        }
        m_frameBuffer = data;

        m_statusSetFrame = true;
        m_statusUpdate = true;
    }

    void setFrame(frame_data_t data,
                  size_t width, size_t height,
                  size_t stride,
                  FrameFormat format)
    {

        if (!m_statusInitialized)
            return;

        //FIXME: I420 does not supported yet.
        if (format == FrameFlipper::I420)
            return;

        int bpp = formatBpp(format);
        bool npot = m_target->hasNPOT();


        std::lock_guard<std::mutex> lock(m_mutex);

        if (m_width != width || m_height != height || m_format != format) {
            // need to recreate buffer (size was changed)
            g_free(m_frameData);
            m_frameData = NULL;
        }

        m_width = width;
        m_height = height;
        m_format = format;

        if (!m_frameData) {
            // check power of 2 for width & height and fix sizes.
            // (OpenGLES 1.x supports only power of 2 sizes)
            m_frameWidth = width;
            if (ispot(width) && !npot)
                m_frameWidth = pow2roundup(width);

            m_frameHeight = height;
            if (ispot(height) && !npot)
                m_frameHeight = pow2roundup(height);

            m_frameData = (FrameFlipper::frame_data_t)g_malloc(m_frameWidth * m_frameHeight * bpp);
        }

        if (m_frameWidth > width || m_frameHeight > height || (width * bpp != stride)) {
            for (size_t i = 0; i < height; ++i)
                memcpy(m_frameData + (i * m_frameWidth * bpp), data + (i * stride), width * bpp);
        } else {
            memcpy(m_frameData, data, width * height * bpp);
        }

        m_statusSetFrame = true;
        m_statusUpdate = true;
    }

    static void renderCallback(FrameFlipperPriv *p)
    {
        p->renderLoop();
    }

    void renderLoop() {
        LOGD("sink::renderLoop(): %p", this);

        if (m_target) {
            m_target->initialize();
            m_statusInitialized = true;
        }

        bool renderingEnabled = true;
        while (renderingEnabled) {
            if (m_statusExit) {
                renderingEnabled = false;
            }

            if (!m_statusRunning)
                continue;

            m_mutex.lock();
            bool r = false;
            auto s = std::chrono::high_resolution_clock::now();
            if (m_statusSetFrame) {
                r = true;
                if (m_target) {
                    if (m_frameBuffer) {
                        GstMapInfo map;
                        if (gst_buffer_map(m_frameBuffer, &map, GST_MAP_READ)) {
                            m_target->setFrame(map.data,
                                               m_frameWidth, m_frameHeight,
                                               m_format);
                            gst_buffer_unmap(m_frameBuffer, &map);
                        }
                        gst_buffer_unref(m_frameBuffer);
                        m_frameBuffer = NULL;
                    } else if (m_frameData) {
                        m_target->setFrame(m_frameData,
                                           m_frameWidth, m_frameHeight,
                                           m_format);
                    }
                }
                m_statusSetFrame = false;
            }

            if (m_statusUpdate) {
                r = true;
                if (m_target) {
                    m_target->drawFrame(m_color,
                                        m_orientation,
                                        m_fillMode,
                                        m_scaleX, m_scaleY, m_scale,
                                        m_width, m_height,
                                        m_frameWidth, m_frameHeight);
                }
                m_statusUpdate = false;
            }
            auto e = std::chrono::high_resolution_clock::now();
            std::chrono::duration<double, std::milli> elapsed = e - s;

            m_mutex.unlock();
            // if (r)
                // LOGD("sink::Render time: %g", elapsed.count());
            std::this_thread::yield();
        }

        LOGD("sink::Render loop exits: %p", this);
    }

    Orientation orientation() const
    {
        return m_orientation;
    }

    void setOrientation(Orientation orientation)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_orientation = orientation;
        m_statusUpdate = true;
    }

    FillMode fillMode() const
    {
        return m_fillMode;
    }

    void setFillMode(FillMode mode)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_fillMode = mode;
        m_statusUpdate = true;
    }

    size_t scaleX() const
    {
        size_t x, y;
        m_target->ogl2scr(m_scaleX, m_scaleY, &x, &y);
        return x;
    }

    size_t scaleY() const
    {
        size_t x, y;
        m_target->ogl2scr(m_scaleX, m_scaleY, &x, &y);
        return y;
    }

    float scaleFactor() const
    {
        return m_scale;
    }

    void setScale(size_t x, size_t y, float scale)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_target->scr2ogl(x, y, &m_scaleX, &m_scaleY);
        m_scale = scale;
        m_statusUpdate = true;
    }

    void color(float *r, float *g, float *b, float *a)
    {
        if (r)
            *r = m_color[0];
        if (g)
            *g = m_color[1];
        if (b)
            *b = m_color[2];
        if (a)
            *a = m_color[3];
    }

    void setColor(float r, float g, float b, float a)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_color[0] = r;
        m_color[1] = g;
        m_color[2] = b;
        m_color[3] = a;
        m_statusUpdate = true;
    }

};

FrameFlipper::FrameFlipper(std::shared_ptr<RenderTarget> target)
{
    m_impl = new FrameFlipperPriv(target);
}

FrameFlipper::~FrameFlipper()
{
    delete m_impl;
}

void FrameFlipper::start()
{
    m_impl->start();
}

void FrameFlipper::stop()
{
    m_impl->stop();
}

void FrameFlipper::setFrame(frame_data_t data,
                            size_t width, size_t height,
                            size_t stride,
                            FrameFormat format)
{
    m_impl->setFrame(data, width, height, stride, format);
}

void FrameFlipper::setFrame(frame_buffer_t buffer,
                            size_t width, size_t height,
                            size_t stride,
                            FrameFormat format)
{
    m_impl->setFrame(buffer, width, height, stride, format);
}

Orientation FrameFlipper::orientation() const
{
    return m_impl->orientation();
}

void FrameFlipper::setOrientation(Orientation orientation)
{
    m_impl->setOrientation(orientation);
}

FillMode FrameFlipper::fillMode() const
{
    return m_impl->fillMode();
}

void FrameFlipper::setFillMode(FillMode mode)
{
    m_impl->setFillMode(mode);
}

size_t FrameFlipper::scaleX() const
{
    return m_impl->scaleX();
}

size_t FrameFlipper::scaleY() const
{
    return m_impl->scaleY();
}

float FrameFlipper::scaleFactor() const
{
    return m_impl->scaleFactor();
}

void FrameFlipper::setScale(size_t x, size_t y, float scale)
{
    m_impl->setScale(x, y, scale);
}

void FrameFlipper::color(float *r, float *g, float *b, float *a)
{
    m_impl->color(r, g, b, a);
}

void FrameFlipper::setColor(float r, float g, float b, float a)
{
    m_impl->setColor(r, g, b, a);
}
