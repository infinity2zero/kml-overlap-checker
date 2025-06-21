package com.example.kml_overlap_checker.service;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class KMLOverlapService {

    private final GeometryFactory factory = new GeometryFactory();

    public Map<String, Map<String, String>> findGroupedOverlaps(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".kml"));
        if (files == null || files.length < 2) {
            return Collections.singletonMap("overlap", Collections.singletonMap("error", "Need at least two KML files"));
        }

        List<Pair<String, Geometry>> geometries = new ArrayList<>();
        STRtree index = new STRtree();

        for (File file : files) {
            Geometry geom = extractGeometry(file);
            if (geom != null) {
                Pair<String, Geometry> pair = Pair.of(file.getName(), geom);
                geometries.add(pair);
                index.insert(geom.getEnvelopeInternal(), pair);
            }
        }

        Map<String, Set<String>> overlapMap = new HashMap<>();

        for (Pair<String, Geometry> outer : geometries) {
            @SuppressWarnings("unchecked")
            List<Pair<String, Geometry>> candidates = index.query(outer.getRight().getEnvelopeInternal());

            for (Pair<String, Geometry> inner : candidates) {
                if (!outer.getLeft().equals(inner.getLeft())) {
                    Geometry g1 = outer.getRight();
                    Geometry g2 = inner.getRight();

                    boolean partialOverlap = g1.intersects(g2)
                            && !g1.contains(g2)
                            && !g2.contains(g1)
                            && !g1.equalsTopo(g2)
                            && !g1.touches(g2);

                    if (partialOverlap) {
                        overlapMap.computeIfAbsent(outer.getLeft(), k -> new LinkedHashSet<>()).add(inner.getLeft());
                    }
                }
            }
        }

        Map<String, String> formatted = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : overlapMap.entrySet()) {
            formatted.put(entry.getKey(), String.join(", ", entry.getValue()));
        }

        return Collections.singletonMap("overlap", formatted);
    }

   
    public List<String> detectPartialOverlaps(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".kml"));
        if (files == null) return Collections.emptyList();

        List<Pair<String, Geometry>> geometries = new ArrayList<>();
        STRtree index = new STRtree();

        for (File file : files) {
            Geometry g = extractGeometry(file);
            if (g != null) {
                Pair<String, Geometry> entry = Pair.of(file.getName(), g);
                geometries.add(entry);
                index.insert(g.getEnvelopeInternal(), entry);
            }
        }

        Set<String> results = new LinkedHashSet<>();

        for (Pair<String, Geometry> outer : geometries) {
            @SuppressWarnings("unchecked")
            List<Pair<String, Geometry>> candidates = index.query(outer.getRight().getEnvelopeInternal());
            for (Pair<String, Geometry> inner : candidates) {
                if (outer.getLeft().equals(inner.getLeft())) continue;

                Geometry g1 = outer.getRight();
                Geometry g2 = inner.getRight();

                boolean partialOverlap = g1.intersects(g2) &&
                                         !g1.contains(g2) &&
                                         !g2.contains(g1) &&
                                         !g1.equalsTopo(g2) &&
                                         !g1.touches(g2);

                if (partialOverlap) {
                    String result = "Partial overlap between: " + outer.getLeft() + " ⟷ " + inner.getLeft();
                    results.add(result);
                }
            }
        }
        return new ArrayList<>(results);
    }
   
    private Geometry extractGeometry(File file) {
        try {
            Kml kml = Kml.unmarshal(file);
            Feature feature = kml.getFeature();
            if (feature instanceof Document) {
                Document doc = (Document) feature;
                for (Feature f : doc.getFeature()) {
                    if (f instanceof Placemark) {
                        Placemark place = (Placemark) f;
                        return convertToJTS(place.getGeometry());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse KML: " + file.getName());
        }
        return null;
    }

    private Geometry convertToJTS(de.micromata.opengis.kml.v_2_2_0.Geometry kmlGeom) {
        if (kmlGeom instanceof Polygon) {
            Polygon poly = (Polygon) kmlGeom;
            List<org.locationtech.jts.geom.Coordinate> jtsCoords = new ArrayList<>();
            for (de.micromata.opengis.kml.v_2_2_0.Coordinate c : poly.getOuterBoundaryIs().getLinearRing().getCoordinates()) {
                jtsCoords.add(new org.locationtech.jts.geom.Coordinate(c.getLongitude(), c.getLatitude()));
            }
            return factory.createPolygon(jtsCoords.toArray(new org.locationtech.jts.geom.Coordinate[0]));
        } else if (kmlGeom instanceof LineString) {
            LineString line = (LineString) kmlGeom;
            List<org.locationtech.jts.geom.Coordinate> jtsCoords = new ArrayList<>();
            for (de.micromata.opengis.kml.v_2_2_0.Coordinate c : line.getCoordinates()) {
                jtsCoords.add(new org.locationtech.jts.geom.Coordinate(c.getLongitude(), c.getLatitude()));
            }
            return factory.createLineString(jtsCoords.toArray(new org.locationtech.jts.geom.Coordinate[0]));
        }
        return null;
    }
}
// import de.micromata.opengis.kml.v_2_2_0.*;
// import de.micromata.opengis.kml.v_2_2_0.LineString;
// import de.micromata.opengis.kml.v_2_2_0.Polygon;

// import org.locationtech.jts.geom.*;
// import org.locationtech.jts.geom.Coordinate;
// import org.locationtech.jts.geom.Geometry;
// import org.locationtech.jts.index.strtree.STRtree;
// import org.springframework.stereotype.Service;
// import org.apache.commons.lang3.tuple.Pair;
// // import org.locationtech.jts.geom.Polygon; // ✅ Use this for geospatial logic

// import java.io.File;
// import java.util.*;

// @Service
// public class KMLOverlapService {
//     private final GeometryFactory factory = new GeometryFactory();

//     public List<String> detectPartialOverlaps(String folderPath) {
//         File folder = new File(folderPath);
//         File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".kml"));
//         if (files == null) return Collections.emptyList();

//         List<Pair<String, Geometry>> geometries = new ArrayList<>();
//         STRtree index = new STRtree();

//         for (File file : files) {
//             Geometry g = extractGeometry(file);
//             if (g != null) {
//                 Pair<String, Geometry> entry = Pair.of(file.getName(), g);
//                 geometries.add(entry);
//                 index.insert(g.getEnvelopeInternal(), entry);
//             }
//         }

//         Set<String> results = new LinkedHashSet<>();

//         for (Pair<String, Geometry> outer : geometries) {
//             @SuppressWarnings("unchecked")
//             List<Pair<String, Geometry>> candidates = index.query(outer.getRight().getEnvelopeInternal());
//             for (Pair<String, Geometry> inner : candidates) {
//                 if (outer.getLeft().equals(inner.getLeft())) continue;

//                 Geometry g1 = outer.getRight();
//                 Geometry g2 = inner.getRight();

//                 boolean partialOverlap = g1.intersects(g2) &&
//                                          !g1.contains(g2) &&
//                                          !g2.contains(g1) &&
//                                          !g1.equalsTopo(g2) &&
//                                          !g1.touches(g2);

//                 if (partialOverlap) {
//                     String result = "Partial overlap between: " + outer.getLeft() + " ⟷ " + inner.getLeft();
//                     results.add(result);
//                 }
//             }
//         }
//         return new ArrayList<>(results);
//     }

//     private Geometry extractGeometry(File file) {
//         try {
//             Kml kml = Kml.unmarshal(file);
//             Feature feature = kml.getFeature();
//             if (feature instanceof Document) {
//                 for (Feature f : ((Document) feature).getFeature()) {
//                     if (f instanceof Placemark) {
//                         return convertToJTS(((Placemark) f).getGeometry());
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("Failed to parse KML: " + file.getName());
//         }
//         return null;
//     }

//     private Geometry convertToJTS(de.micromata.opengis.kml.v_2_2_0.Geometry kmlGeom) {
//         if (kmlGeom instanceof Polygon) {
//             List<Coordinate> coords = ((Polygon) kmlGeom).getOuterBoundaryIs().getLinearRing().getCoordinates()
//                 .stream().map(c -> new Coordinate(c.getLongitude(), c.getLatitude())).toList();
//             return factory.createPolygon(coords.toArray(new Coordinate[0]));
//         } else if (kmlGeom instanceof LineString) {
//             List<Coordinate> coords = ((LineString) kmlGeom).getCoordinates()
//                 .stream().map(c -> new Coordinate(c.getLongitude(), c.getLatitude())).toList();
//             return factory.createLineString(coords.toArray(new Coordinate[0]));
//         }
//         return null;
//     }
// }
