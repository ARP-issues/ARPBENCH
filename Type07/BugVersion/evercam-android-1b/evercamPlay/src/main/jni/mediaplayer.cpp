#include "mediaplayer.h"
#include <string>
#include <algorithm>
#include <gst/video/video.h>
#include <gst/app/gstappsink.h>
#include <gst/video/videooverlay.h>
#include <pthread.h>
#include "debug.h"
#include "eventloop.h"
#include <thread>
#include <chrono>

using namespace evercam;

MediaPlayer::MediaPlayer(const EventLoop &loop)
    : m_tcp_timeout(0)
    , m_target_state(GST_STATE_NULL)
    , m_sample_ready_handler(0)
    , m_sample_failed_handler(0)
    , m_window(0)
    , m_initialized(false)
    , m_loop(loop)
{
    LOGD("MediaPlayer::MediaPlayer()");
}

MediaPlayer::~MediaPlayer()
{
    LOGD("~MediaPlayer::MediaPlayer()");
    stop();
}

void MediaPlayer::dropPipeline()
{
    if (msp_pipeline) {
        msp_pipeline.reset();
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
}

void MediaPlayer::play()
{
    GstState currentTarget = m_target_state;
    m_target_state = GST_STATE_PLAYING;

    gst_element_set_state(msp_pipeline.get(), m_target_state);
    msp_last_sample.reset();

    if (currentTarget == GST_STATE_PAUSED)
        mfn_stream_sucess_handler();
}

void MediaPlayer::pause()
{
    m_target_state = GST_STATE_PAUSED;
    gst_element_set_state(msp_pipeline.get(), m_target_state);
}

void MediaPlayer::stop()
{
    m_target_state = GST_STATE_NULL;
    gst_element_set_state(msp_pipeline.get(), m_target_state);
}

/* Handle sample conversion */
void MediaPlayer::process_converted_sample(GstSample *sample, GError *err, ConvertSampleContext *data)
{
    gst_caps_unref(data->caps);

    if (err == NULL) {
        if (sample != NULL) {
            GstBuffer *buf = gst_sample_get_buffer(sample);
            GstMapInfo info;
            gst_buffer_map (buf, &info, GST_MAP_READ);
            data->player->m_sample_ready_handler(info.data, info.size);
            gst_buffer_unmap (buf, &info);
        }
    }
    else {
        LOGD("Conversion error %s", err->message);
        g_error_free(err);
        data->player->m_sample_failed_handler();
    }

    if (sample != NULL)
        gst_sample_unref(sample);

    //FIXME: last sample?
    if (data->player->msp_last_sample.get() != data->sample)
        gst_sample_unref(data->sample);

    gst_caps_unref(data->caps);
}

/* Sample pthread function */
void *MediaPlayer::convert_thread_func(void *arg)
{
    ConvertSampleContext *data = (ConvertSampleContext*) arg;
    GError *err = NULL;

    if (data->caps != NULL && data->sample != NULL) {
        GstSample *sample = gst_video_convert_sample(data->sample, data->caps, GST_CLOCK_TIME_NONE, &err);
        process_converted_sample(sample, err, data);
        g_free(data);
    }

    return NULL;
}

/* Asynchronous function for converting frame */
void MediaPlayer::convert_sample(ConvertSampleContext *ctx)
{
    pthread_t thread;

    if (pthread_create(&thread, NULL, convert_thread_func, ctx) != 0)
        LOGE("Strange, but can't create sample conversion thread");
}

void MediaPlayer::requestSample(const std::string &fmt)
{
    if (!msp_pipeline)
        return;

    std::string format(fmt);
    std::transform(format.begin(), format.end(), format.begin(), ::tolower);
    if (format != "png"  && format != "jpeg") {
        LOGE("Unsupported image format %s", format.c_str());
        return;
    }

    GstSample *sample = NULL;

    if (msp_last_sample)
        sample = msp_last_sample.get();

    if (sample) {
        ConvertSampleContext *ctx = reinterpret_cast<ConvertSampleContext *> (g_malloc(sizeof(ConvertSampleContext)));
        memset(ctx, 0, sizeof(ConvertSampleContext));
        gchar *img_fmt = g_strdup_printf("image/%s", format.c_str());
        LOGD("img fmt == %s", img_fmt);
        ctx->caps = gst_caps_new_simple (img_fmt, NULL);
        g_free(img_fmt);
        ctx->sample = sample;
        ctx->player = this;
        convert_sample(ctx);
    }
    else {
        LOGD("Can't get sample");
        m_sample_failed_handler();
    }
}

void MediaPlayer::setUri(const std::string& uri)
{
    LOGD("MediaPlayer: uri %s", uri.c_str());
    m_uri = uri;

    initialize(m_loop);
    if (msp_source) {
        g_object_set(msp_source.get(), "location", uri.c_str(), NULL);
    }
    gst_element_set_state (msp_pipeline.get(), GST_STATE_READY);
}

void MediaPlayer::setTcpTimeout(int value)
{
    m_tcp_timeout = value;
}

void MediaPlayer::recordVideo(const std::string& /* fileName */) throw (std::runtime_error)
{
    // TODO
}

void MediaPlayer::setVideoLoadedHandler(StreamSuccessHandler handler)
{
    mfn_stream_sucess_handler = handler;
}

void MediaPlayer::setVideoLoadingFailedHandler(StreamFailedHandler handler)
{
    mfn_stream_failed_handler = handler;
}

void MediaPlayer::setSampleReadyHandler(SampleReadyHandler handler)
{
   m_sample_ready_handler = handler;
}

void MediaPlayer::setSampleFailedHandler(SampleFailedHandler handler)
{
   m_sample_failed_handler = handler;
}

void MediaPlayer::setSurface(ANativeWindow *window)
{
    LOGD("MediaPlayer: setSurface: %p\n", window);
    if (m_window != 0) {
        ANativeWindow_release (m_window);
        m_initialized = false;
    }
    if (m_window != window) {
        delete m_renderer.release();
        // Need to wait for destroy complete
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        RenderTarget *rt = RenderTarget::create(window, RenderTarget::RGB24);
        m_renderer.reset(new FrameFlipper(std::shared_ptr<RenderTarget>(rt)));
        m_renderer->start();
        // Need to wait for construction complete
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    m_window = window;
    m_initialized = true;
}

void MediaPlayer::expose()
{
    drawFrame(msp_last_sample.get());
}

void MediaPlayer::releaseSurface()
{
    if (m_window != 0) {
        ANativeWindow_release(m_window);
        m_window = 0;
    }

    m_initialized = false;
}

bool MediaPlayer::isInitialized() const
{
    return m_initialized;
}

void
MediaPlayer::drawFrame(GstSample* sample)
{
    if (!m_renderer)
        return;

    GstBuffer *buf = gst_sample_get_buffer(sample);
    GstCaps *caps = gst_sample_get_caps(sample);

    GstVideoInfo info;
    GstVideoFormat fmt;
    gint w, h, s, par_n, par_d;
    FrameFlipper::FrameFormat format;

    if (!gst_video_info_from_caps (&info, caps)) {
        LOGD("MediaPlayer: can't get information from caps");
        return;
    }

    fmt = GST_VIDEO_INFO_FORMAT (&info);
    w = GST_VIDEO_INFO_WIDTH (&info);
    h = GST_VIDEO_INFO_HEIGHT (&info);
    s = GST_VIDEO_INFO_PLANE_STRIDE(&info, 0);
    par_n = GST_VIDEO_INFO_PAR_N (&info);
    par_d = GST_VIDEO_INFO_PAR_N (&info);

    // LOGD("MediaPlayer::drawFrame: (%d, %d)", w, h);

    switch (fmt) {
        case GST_VIDEO_FORMAT_RGB16:
            // LOGD("MediaPlayer: format RGB565");
            format = FrameFlipper::RGB565;
            break;
        case GST_VIDEO_FORMAT_RGB:
            // LOGD("MediaPlayer: format RGB24");
            format = FrameFlipper::RGB24;
            break;
        case GST_VIDEO_FORMAT_RGBA:
            // LOGD("MediaPlayer: format RGBA32");
            format = FrameFlipper::RGBA32;
            break;
        case GST_VIDEO_FORMAT_I420:
            // LOGD("MediaPlayer: format I420");
            format = FrameFlipper::I420;
            break;
        default:
            LOGD("MediaPlayer: format unknow");
            format = FrameFlipper::RGB24;
    }

    // LOGD("MediaPlayer:format             : %d", fmt);
    // LOGD("MediaPlayer:width x height     : %d x %d", w, h);
    // LOGD("MediaPlayer:stride             : %d", s);
    // LOGD("MediaPlayer:pixel-aspect-ratio : %d/%d", par_n, par_d);

    m_renderer->setFrame(gst_buffer_ref(buf), w, h, s, format);
}

GstFlowReturn
MediaPlayer::new_sample (GstAppSink * sink, gpointer data)
{
    MediaPlayer *player = (MediaPlayer*)data;
    if (!player->m_initialized) {
        player->mfn_stream_sucess_handler();
        player->m_initialized = true;
    }
    GstSample *sample = gst_app_sink_pull_sample(sink);
    player->msp_last_sample = std::shared_ptr<GstSample>(sample, gst_sample_unref);

    player->drawFrame(sample);

    return GST_FLOW_OK;
}

void MediaPlayer::initialize(const EventLoop& loop) throw (std::runtime_error)
{
    dropPipeline();

    m_initialized = false;
    GError *err = nullptr;

    gchar const *pipeline_str =
            "rtspsrc name=video_src latency=0 drop-on-latency=1 protocols=4 !"
            "rtph264depay ! h264parse ! avdec_h264 ! tee name=tee_sink "
            "tee_sink. ! queue ! appsink sync=false name=app_sink"
            ;
    GstElement *pipeline = gst_parse_launch(pipeline_str, &err);

    if (!pipeline) {
        std::string error_message = "Could not to create pipeline";

        if (err) {
            error_message = err->message;
            g_error_free(err);
        }

        throw std::runtime_error(error_message);
    }
    LOGD ("MediaPlayer: pipeline (%p)%s\n", pipeline, pipeline_str);

    GstElement *video_src = gst_bin_get_by_name(GST_BIN(pipeline), "video_src");
    msp_source = std::shared_ptr<GstElement>(video_src, gst_object_unref);

    GstElement *video_sink = gst_bin_get_by_name(GST_BIN(pipeline), "video_sink");
    GstElement *app_sink = gst_bin_get_by_name (GST_BIN(pipeline), "app_sink");

    gst_app_sink_set_emit_signals (GST_APP_SINK(app_sink), true);
    g_signal_connect (G_OBJECT (app_sink), "new-sample", G_CALLBACK(new_sample), this);

    GstBus *bus = gst_element_get_bus (pipeline);
    GSource *bus_source = gst_bus_create_watch (bus);
    g_source_set_callback (bus_source, (GSourceFunc) gst_bus_async_signal_func, NULL, NULL);
    gint res = g_source_attach (bus_source, loop.msp_main_ctx.get());
    LOGD("res %d ctx %p pipeline %p", res, loop.msp_main_ctx.get(), pipeline);
    g_source_unref (bus_source);
    g_signal_connect (G_OBJECT (bus), "message::error", (GCallback) handle_bus_error, const_cast<MediaPlayer*> (this));
    gst_object_unref (bus);


    msp_pipeline = std::shared_ptr<GstElement>(pipeline, gst_object_unref);
}

void MediaPlayer::handle_bus_error(GstBus *, GstMessage *message, MediaPlayer *self)
{
    GError *err;
    gchar *debug;

    gst_message_parse_error (message, &err, &debug);
    LOGE ("MediaPlayer::handle_bus_error: %s\n", err->message);
    g_error_free (err);
    g_free (debug);

    if (self->m_target_state == GST_STATE_PLAYING)
        self->mfn_stream_failed_handler();

    self->m_target_state == GST_STATE_NULL;
}
