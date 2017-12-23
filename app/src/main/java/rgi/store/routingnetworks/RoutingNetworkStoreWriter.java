/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package rgi.store.routingnetworks;

import java.lang.reflect.Type;
import java.util.List;

import rgi.common.Pair;
import rgi.common.coordinate.CoordinateReferenceSystem;

/**
 * * Interface for all routing network store writers
 *
 * @author Luke Lambert
 */
@FunctionalInterface
public interface RoutingNetworkStoreWriter {
    /**
     * Writes the network to the underlying store
     *
     * @param nodes                     Collection of network nodes
     * @param edges                     Collection of network edges
     * @param nodeDimensionality        Indication of whether or not the
     *                                  network contains elevation data
     * @param nodeAttributeDescriptions Collection of name / type pairs
     *                                  describing the nodes
     * @param edgeAttributeDescriptions Collection of name / type pairs
     *                                  describing the edges
     * @param coordinateReferenceSystem Coordinate reference system of the data
     * @throws RoutingNetworkStoreException Throws if there is an issue storing
     *                                      the network
     */
    void write(final List<Node> nodes,
               final List<Edge> edges,
               final NodeDimensionality nodeDimensionality,
               final List<Pair<String, Type>> nodeAttributeDescriptions,
               final List<Pair<String, Type>> edgeAttributeDescriptions,
               final CoordinateReferenceSystem coordinateReferenceSystem) throws RoutingNetworkStoreException;
}
