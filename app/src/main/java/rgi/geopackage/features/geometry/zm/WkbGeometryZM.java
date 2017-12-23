package rgi.geopackage.features.geometry.zm;

import rgi.geopackage.features.geometry.Geometry;

/**
 * @author Luke Lambert
 */
public abstract class WkbGeometryZM extends Geometry {
    @Override
    public final boolean hasZ() {
        return true;
    }

    @Override
    public final boolean hasM() {
        return true;
    }

    /**
     * Creates an envelope with x, y, z, and m components
     *
     * @return an envelope with x, y, z, and m components
     */
    public abstract EnvelopeZM createEnvelopeZM();

    /**
     * Base type value for all geometries that extend this type
     */
    public static final long GeometryTypeDimensionalityBase = 3000;
}
