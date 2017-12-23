package rgi.geopackage.features.geometry;

import java.nio.ByteBuffer;

import rgi.geopackage.features.WellKnownBinaryFormatException;

/**
 * @author Luke Lambert
 */
@FunctionalInterface
public interface GeometryFactory {
    Geometry create(final ByteBuffer byteBuffer) throws WellKnownBinaryFormatException;
}
