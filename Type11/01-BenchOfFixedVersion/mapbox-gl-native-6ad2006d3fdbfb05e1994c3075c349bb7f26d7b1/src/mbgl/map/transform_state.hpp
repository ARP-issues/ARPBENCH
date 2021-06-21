#ifndef MBGL_MAP_TRANSFORM_STATE
#define MBGL_MAP_TRANSFORM_STATE

#include <mbgl/util/mat4.hpp>
#include <mbgl/util/geo.hpp>
#include <mbgl/util/constants.hpp>
#include <mbgl/util/vec.hpp>

#include <cstdint>
#include <array>
#include <limits>

namespace mbgl {

class TileID;
struct box;

class TransformState {
    friend class Transform;

public:
    // Matrix
    void matrixFor(mat4& matrix, const TileID& id, const int8_t z) const;
    box cornersToBox(uint32_t z) const;

    // Dimensions
    bool hasSize() const;
    uint16_t getWidth() const;
    uint16_t getHeight() const;

    float worldSize() const;
    float lngX(float lon) const;
    float latY(float lat) const;
    std::array<float, 2> locationCoordinate(float lon, float lat) const;
    void getLonLat(double &lon, double &lat) const;

    // Position
    const LatLng getLatLng() const;

    // Zoom
    float getNormalizedZoom() const;
    double getZoom() const;
    int32_t getIntegerZoom() const;
    double getZoomFraction() const;
    double getScale() const;
    double getMinZoom() const;
    double getMaxZoom() const;

    // Rotation
    float getAngle() const;

    // Projection
    const vec2<double> pixelForLatLng(const LatLng latLng) const;
    const LatLng latLngForPixel(const vec2<double> pixel) const;

    // Changing
    bool isChanging() const;

private:
    void constrain(double& scale, double& y) const;

    // Limit the amount of zooming possible on the map.
    double min_scale = std::pow(2, 0);
    double max_scale = std::pow(2, 18);

    double pixel_x() const;
    double pixel_y() const;

    // logical dimensions
    uint16_t width = 0, height = 0;

private:
    // animation state
    bool rotating = false;
    bool scaling = false;
    bool panning = false;
    bool gestureInProgress = false;

    // map position
    double x = 0, y = 0;
    double angle = 0;
    double scale = 1;

    // cache values for spherical mercator math
    double Bc = (scale * util::tileSize) / 360;
    double Cc = (scale * util::tileSize) / util::M2PI;
};

}

#endif
