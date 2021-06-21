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

#ifndef RENDERER_H
#define RENDERER_H

#include <memory>
#include <gst/gst.h>

// Device orientation
enum Orientation {
    // Portrait 0
    ROTATION_0 = 0,
    // Landscape 90
    ROTATION_90,
    // Portrait 180
    ROTATION_180,
    // Landscape 270
    ROTATION_270,
};

// Fill mode (default preserve aspect fit)
enum FillMode {
    STRETCH = 0,
    PRESERVE_ASPECT_FIT,
    PRESERVE_ASPECT_CROP,
};

class RenderTarget;

class FrameFlipper
{
public:
    typedef unsigned char* frame_data_t;
    typedef GstBuffer* frame_buffer_t;

    // Frame formats
    enum FrameFormat {
        RGB24 = 0,
        RGBA32,
        RGB565,
        RGBA4444,
        RGBA5551,
        I420,
        FORMAT_UNKNOWN,
    };

public:
    FrameFlipper(std::shared_ptr<RenderTarget> target);
    ~FrameFlipper();

    void start();
    void stop();

    // Set new frame
    void setFrame(frame_data_t data,
                  size_t width, size_t height,
                  size_t stride,
                  FrameFormat format);
    void setFrame(frame_buffer_t bufffer,
                  size_t width, size_t height,
                  size_t stride,
                  FrameFormat format);

    // Set & get orientation
    Orientation orientation() const;
    void setOrientation(Orientation orientation);

    // Set & get fillMode
    FillMode fillMode() const;
    void setFillMode(FillMode mode);

    // Set & get scale transformation (scale point in screen coords)
    size_t scaleX() const;
    size_t scaleY() const;
    float scaleFactor() const;
    void setScale(size_t x, size_t y, float scale);

    // Set & get background color
    void color(float *r, float *g, float *b, float *a);
    void setColor(float r, float g, float b, float a);

private:
    struct FrameFlipperPriv;
    FrameFlipperPriv *m_impl;
};

class RenderTarget
{
public:
    enum Format {
        RGB24 = 0,
        RGB565,
    };
    virtual ~RenderTarget() {}

    virtual bool hasNPOT() const = 0;
    virtual bool initialize() = 0;
    virtual void destroy() = 0;
    virtual void ogl2scr(float x, float y, size_t *sx, size_t *sy) = 0;
    virtual void scr2ogl(size_t sx, size_t sy, float *x, float *y) = 0;
    virtual void drawFrame(float color[4],
                           Orientation orientation,
                           FillMode fillMode,
                           float scaleX, float scaleY, float scaleFactor,
                           size_t width, size_t height,
                           size_t crWidth, size_t crHeight) = 0;
    virtual void setFrame(FrameFlipper::frame_data_t data,
                          size_t width, size_t height,
                          FrameFlipper::FrameFormat format) = 0;;

    static RenderTarget *create(void* window,
                                RenderTarget::Format format);
};

#endif // RENDERER_H
