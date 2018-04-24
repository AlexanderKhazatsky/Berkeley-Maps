import java.util.TreeMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private Map<String, Double> parameters;
    private String[][] renderGrid;
    private double lonDPP;
    private double lonLength;
    private double latLength;
    private double ullon;
    private double ullat;
    private double lrlon;
    private double lrlat;
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private int depth;

    public Rasterer() {
        // YOUR CODE HERE
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        parameters = params;
        Map<String, Object> results = new TreeMap<>();
        doCalculations();
        results.put("raster_ul_lon", ullon);
        results.put("raster_ul_lat", ullat);
        results.put("raster_lr_lon", lrlon);
        results.put("raster_lr_lat", lrlat);
        results.put("depth", depth);
        renderGrid = new String[(int) (yMax - yMin)][(int) (xMax - xMin)];
        int colIndex = 0;
        int rowIndex = 0;
        for (int y = (int) yMin; y < yMax; y++) {
            for (int x = (int) xMin; x < xMax; x++) {
                renderGrid[rowIndex][colIndex++] = "d" + depth + "_x" + x + "_y" + y + ".png";
            }
            colIndex = 0;
            rowIndex += 1;
        }

        results.put("render_grid", renderGrid);

        if (ullon > lrlon) {
            results.put("query_success", false);
        } else {
            results.put("query_success", true);
        }
        return results;
    }

    private void doCalculations() {
        calcDDP();
        calcDepth();
        calcLengths();
        calcXMin();
        calcYMin();
        calcXMax();
        calcYMax();
        calcULLON();
        calcULLAT();
        calcLRLON();
        calcLRLAT();
    }

    private void calcDDP() {
        lonDPP =  (parameters.get("lrlon") - parameters.get("ullon")) / (parameters.get("w"));
    }

    private void calcDepth() {
        Double[] allDPPVals = new Double[8];
        allDPPVals[0] = 0.00034332275390625;
        allDPPVals[1] = 0.000171661376953125;
        allDPPVals[2] = 0.0000858306884765625;
        allDPPVals[3] = 0.00004291534423828125;
        allDPPVals[4] = 0.000021457672119140625;
        allDPPVals[5] = 0.000010728836059570312;
        allDPPVals[6] = 0.000005364418029785156;
        allDPPVals[7] = 0.0;

        for (int d = 0; d < allDPPVals.length; d++) {
            if (allDPPVals[d] <= lonDPP) {
                depth = d;
                return;
            }
        }
    }

    private void calcLengths() {
        lonLength = (MapServer.ROOT_LRLON - MapServer.ROOT_ULLON) / Math.pow(2, depth);
        latLength = (MapServer.ROOT_LRLAT - MapServer.ROOT_ULLAT) / Math.pow(2, depth);
    }

    private void calcXMin() {
        xMin = Math.floor((parameters.get("ullon") - MapServer.ROOT_ULLON) / lonLength);
    }

    private void calcLRLON() {
        lrlon = MapServer.ROOT_ULLON + (xMax) * lonLength;
    }

    private void calcYMin() {
        yMin = Math.floor((parameters.get("ullat") - MapServer.ROOT_ULLAT) / latLength);
    }

    private void calcLRLAT() {
        lrlat = MapServer.ROOT_ULLAT + (yMax) * latLength;
    }

    private void calcXMax() {
        xMax = Math.ceil((parameters.get("lrlon") - MapServer.ROOT_ULLON) / lonLength);
    }

    private void calcULLON() {
        ullon = MapServer.ROOT_ULLON + (xMin) * lonLength;
    }

    private void calcYMax() {
        yMax = Math.ceil((parameters.get("lrlat") - MapServer.ROOT_ULLAT) / latLength);
    }

    private void calcULLAT() {
        ullat = MapServer.ROOT_ULLAT + (yMin) * latLength;
    }
}
