#ifndef MediaPlayer_H
#define MediaPlayer_H

#include <gst/gst.h>
#include <memory>
#include <functional>
#include <stdexcept>
#include <string>
#include <android/native_window.h>
#include <gst/app/gstappsink.h>
#include "frameflipper.h"
#include "eventloop.h"

namespace evercam {

class MediaPlayer;

typedef struct {
    GstCaps    *caps;             /* Target frame caps */
    GstSample  *sample;           /* Sample for conversion */
    MediaPlayer *player;          /* Global data for call java stuff */
} ConvertSampleContext;

class MediaPlayer
{
public:
    typedef std::function<void ()> StreamSuccessHandler;
    typedef std::function<void ()> StreamFailedHandler;
    typedef std::function<void ()> SampleFailedHandler;
    typedef std::function<void (unsigned char *data, size_t size)> SampleReadyHandler;
    MediaPlayer(const EventLoop& loop);
    ~MediaPlayer();
    void play();
    void pause();
    void stop();
    void requestSample(const std::string &fmt);
    void setUri(const std::string& uri);
    void setTcpTimeout(int value);
    void recordVideo(const std::string& fileName) throw (std::runtime_error);
    void setVideoLoadedHandler(StreamSuccessHandler handler);
    void setVideoLoadingFailedHandler(StreamFailedHandler handler);
    void setSampleReadyHandler(SampleReadyHandler handler);
    void setSampleFailedHandler(SampleFailedHandler handler);
    void setSurface(ANativeWindow *window);
    void expose();
    void releaseSurface();
    bool isInitialized() const;
private:
    void initialize(const EventLoop& loop) throw (std::runtime_error);
    void drawFrame(GstSample* sample);
    void dropPipeline();
    static void handle_bus_error(GstBus *,  GstMessage *message, MediaPlayer *self);
    static void process_converted_sample(GstSample *sample, GError *err, ConvertSampleContext *data);
    static void *convert_thread_func(void *arg);
    static void convert_sample(ConvertSampleContext *ctx);
    static GstFlowReturn new_sample (GstAppSink *, gpointer );

    int m_tcp_timeout;
    std::shared_ptr<GstElement> msp_pipeline;
    std::shared_ptr<GstSample> msp_last_sample;
    std::shared_ptr<GstElement> msp_source;
    std::shared_ptr<GstElement> msp_sink;
    GstState m_target_state;

    SampleReadyHandler m_sample_ready_handler;
    SampleFailedHandler m_sample_failed_handler;
    StreamSuccessHandler mfn_stream_sucess_handler;
    StreamFailedHandler mfn_stream_failed_handler;
    std::unique_ptr<FrameFlipper> m_renderer;
    ANativeWindow *m_window;
    bool m_initialized;
    EventLoop m_loop;
    std::string m_uri;
};

} // evercam

#endif // MediaPlayer_H
