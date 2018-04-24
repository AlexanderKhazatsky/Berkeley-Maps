import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.princeton.cs.algs4.MinPQ;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {

        MinPQ<SearchNode> searchQueue = new MinPQ<>();
        long endNodeID = g.closest(destlon, destlat);
        searchQueue.insert(new SearchNode(g, g.closest(stlon, stlat), endNodeID, null, 0));
        try {
            return routeFinder(g, endNodeID, searchQueue);
        } catch (java.util.NoSuchElementException o) {
            return new ArrayList<>();
        }
    }


    private static List<Long> routeFinder(GraphDB map, long endNodeID,
                                          MinPQ<SearchNode> searchQueue) {
        SearchNode finalNode = null;
        HashSet<Long> visited = new HashSet<>();
        while (finalNode == null) {
            SearchNode bestSearchNode = searchQueue.delMin();
            visited.add(bestSearchNode.nodeID);
            if (bestSearchNode.isFinished()) {
                finalNode = bestSearchNode;
            } else {
                for (long nodeID : map.adjacent(bestSearchNode.nodeID)) {
                    double distance = bestSearchNode.distanceTraveled
                            + map.distance(bestSearchNode.nodeID, nodeID);
                    if (!visited.contains(nodeID)) {
                        SearchNode newSearchNode = new SearchNode(map, nodeID,
                                endNodeID, bestSearchNode, distance);
                        searchQueue.insert(newSearchNode);
                    }
                }
            }
        }
        return path(finalNode);
    }

    private static List<Long> path(SearchNode finalNode) {
        SearchNode currSearchNode = finalNode;
        LinkedList<Long> path = new LinkedList<>();
        while (currSearchNode != null) {
            path.addFirst(currSearchNode.nodeID);
            currSearchNode = currSearchNode.previousNode;
        }
        return path;
    }


    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        ArrayList<NavigationDirection> directions = new ArrayList<>();
        NavigationDirection currNav = new NavigationDirection();
        currNav.direction = 0;
        Double distanceTracker = 0.0;
        String lastWay = "";
        for (int i = 0; i < route.size() - 1; i += 1) {
            for (GraphDB.Edge e : g.allNodes.get(route.get(i)).edges) {
                if (e.v1 == route.get(i + 1) || e.v2 == route.get(i + 1)) {
                    currNav.way = e.name;
                    break;
                }
            }
            if (lastWay.isEmpty() || currNav.way.equals(lastWay)) {
                distanceTracker += g.distance(route.get(i), route.get(i + 1));
            } else {
                NavigationDirection newStep = new NavigationDirection();
                newStep.way = lastWay;
                newStep.direction = currNav.direction;
                newStep.distance = distanceTracker;
                String stringStep = newStep.toString();
                System.out.println("Change: " + stringStep);
                directions.add(NavigationDirection.fromString(stringStep));
                distanceTracker = g.distance(route.get(i), route.get(i + 1));
                double angle = g.bearing(route.get(i - 1), route.get(i))
                        + g.bearing(route.get(i), route.get(i + 1));
                currNav.direction = calcDirection(angle);
            }
            //if (i > 0) {
                //.out.println(lastWay + " to " + currNav.way + ": "
                // + (g.bearing(route.get(i - 1), route.get(i))) + " "
                // + g.bearing(route.get(i), route.get(i + 1)));
                //System.out.println((g.bearing(route.get(i - 1), route.get(i))
                // + g.bearing(route.get(i), route.get(i + 1))));
            //}
            lastWay = currNav.way;
        }
        currNav.distance = distanceTracker;
        String currStep = currNav.toString();
        directions.add(NavigationDirection.fromString(currStep));
        System.out.println(directions);
        return directions;
    }

    private static int calcDirection(Double angle) {
        System.out.println("Angle: " + angle);
        int direction = 0;
        if (Math.abs(angle) <= 15) {
            direction = 1;
        } else if (angle < -15 && angle >= -30) {
            direction =  2;
        } else if (angle > 15 && angle <= 30) {
            direction =  3;
        } else if (angle < -30 && angle >= -100) {
            direction = 4;
        } else if (angle > 30 && angle <= 100) {
            direction = 5;
        } else if (angle < -100) {
            direction = 6;
        } else if (angle > 100) {
            direction = 7;
        }
//        System.out.println("Direction: " + direction);
        return direction;
    }


    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int LEFT = 4;
        public static final int RIGHT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }

    static class SearchNode implements Comparable {
        GraphDB map;
        long nodeID;
        long endNodeID;
        SearchNode previousNode;
        double distanceTraveled;
        double estimatedRemaining;

        SearchNode(GraphDB g, long curr, long end, SearchNode prev, double distTrav) {
            map = g;
            nodeID = curr;
            endNodeID = end;
            previousNode = prev;
            distanceTraveled = distTrav;
            estimatedRemaining = map.distance(nodeID, endNodeID);
        }

        boolean isFinished() {
            return nodeID == endNodeID;
        }

        @Override
        public int compareTo(Object o) {
            SearchNode other = (SearchNode) o;
            double myDistance = this.distanceTraveled + this.estimatedRemaining;
            double otherDistance = other.distanceTraveled + other.estimatedRemaining;
            double difference = myDistance - otherDistance;
            if (difference < 0) {
                return -1;
            } else if (difference > 0) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            SearchNode other = (SearchNode) o;
            return this.nodeID == other.nodeID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeID);
        }
    }
}
