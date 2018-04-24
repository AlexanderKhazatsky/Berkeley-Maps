import org.xml.sax.SAXException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    HashMap<Long, Node> allNodes;
    Trie allLocations;

    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        allNodes = new HashMap<>();
        allLocations = new Trie();
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        //System.out.println(getLocationsByPrefix("fin"));
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        HashMap<Long, Node> cleanedAllNodes = new HashMap<>();
        for (Node currNode : allNodes.values()) {
            if (!currNode.edges.isEmpty()) {
                cleanedAllNodes.put(currNode.v, currNode);
            }
        }
        allNodes = null;
        allNodes = cleanedAllNodes;
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        return allNodes.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        HashSet<Long> adj = new HashSet<>();
        for (Edge e : allNodes.get(v).edges) {
            if (e.v1 == v) {
                adj.add(e.v2);
            } else {
                adj.add(e.v1);
            }
        }
        return adj;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long bestNode = 0;
        double smallest = Double.POSITIVE_INFINITY;
        for (Node n : allNodes.values()) {
            double currDistance = distance(lon, lat, n.lon, n.lat);
            if (currDistance < smallest) {
                smallest = currDistance;
                bestNode = n.v;
            }
        }
        return bestNode;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return allNodes.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return allNodes.get(v).lat;
    }

    void addNode(long v, double lon, double lat) {
        Node newNode = new Node(v, lon, lat);
        allNodes.put(v, newNode);
    }

    void addEdge(long v1, long v2, String maxSpeed, String name) {
        Edge newEdge = new Edge(v1, v2, maxSpeed);
        newEdge.name = name;
        allNodes.get(v1).edges.add(newEdge);
        allNodes.get(v2).edges.add(newEdge);
    }

    void addWay(ArrayList<Long> verts, String maxSpeed, String name) {
        for (int i = 1; i < verts.size(); i++) {
            addEdge(verts.get(i - 1), verts.get(i), maxSpeed, name);
        }
    }

    public List<String> getLocationsByPrefix(String prefix) {
        //TreeSet<String> namesSeen = new TreeSet<>();
        return new ArrayList<>(allLocations.getPrefixes(prefix));
    } // End getMatching method

    public List<Map<String, Object>> getLocations(String locationName) {
        return allLocations.getMatches(locationName);
    }

    static class Node {
        long v;
        double lon;
        double lat;
        String name;
        HashSet<Edge> edges;

        Node(long v, double lon, double lat) {
            this.v = v;
            this.lon = lon;
            this.lat = lat;
            this.edges = new HashSet<>();
        } // End Node constructor

        void setName(String name) {
            this.name = name;
        }
    } // End Node class

    class Edge {
        long v1;
        long v2;
        String maxSpeed;
        String name;

        Edge(long v1, long v2, String maxSpeed) {
            this.v1 = v1;
            this.v2 = v2;
            this.maxSpeed = maxSpeed;
        } // End Edge constructor
    } // End Edge class

}
