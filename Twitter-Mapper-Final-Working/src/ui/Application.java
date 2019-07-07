package ui;

import javafx.scene.control.Tooltip;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Layer;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import query.Query;
import sun.awt.X11.InfoWindow;
import twitter.LiveTwitterSource;
import twitter.PlaybackTwitterSource;
import twitter.TwitterSource;
import util.SphericalGeometry;
import util.Util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.Timer;

import static util.Util.imageFromURL;

/**
 * The Twitter viewer application
 * Derived from a JMapViewer demo program written by Jan Peter Stotz
 */
public class Application extends JFrame {
    // The content panel, which contains the entire UI
    private final ContentPanel contentPanel;
    // The provider of the tiles for the map, we use the Bing source
    private BingAerialTileSource bing;
    // All of the active queries
    private List<Query> queries = new ArrayList<>();
    // The source of tweets, a TwitterSource, either live or playback
    private TwitterSource twitterSource;

    /**
     * Constructs the {@code Application}.
     */
    public Application() {
        super("Twitter content viewer");
        setSize(300, 300);
        initialize();

        bing = new BingAerialTileSource();

        // Do UI initialization
        contentPanel = new ContentPanel(this);
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Always have map markers showing.
        map().setMapMarkerVisible(true);
        // Always have zoom controls showing,
        // and allow scrolling of the map around the edge of the world.
        map().setZoomContolsVisible(true);
        map().setScrollWrapEnabled(true);

        // Use the Bing tile provider
        map().setTileSource(bing);

        //NOTE This is so that the map eventually loads the tiles once Bing attribution is ready.
        Coordinate coord = new Coordinate(0, 0);

        Timer bingTimer = new Timer();
        TimerTask bingAttributionCheck = new TimerTask() {
            @Override
            public void run() {
                // This is the best method we've found to determine when the Bing data has been loaded.
                // We use this to trigger zooming the map so that the entire world is visible.
                if (!bing.getAttributionText(0, coord, coord).equals("Error loading Bing attribution data")) {
                    map().setZoom(2);
                    bingTimer.cancel();
                }
            }
        };
        bingTimer.schedule(bingAttributionCheck, 100, 200);

        // Set up a motion listener to create a tooltip showing the tweets at the pointer position
        map().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                ICoordinate pos = map().getPosition(p);

                // TODO: Use the following method to set the text that appears at the mouse cursor
                ToolTipManager.sharedInstance().setInitialDelay(0);
                List<MapMarker> markersNearMousePointer = getMarkersCovering(pos, pixelWidth(p));

                if(markersNearMousePointer.size()==0){
                    map().setToolTipText(null);
                }

                else if (markersNearMousePointer.size()==1){
                    map().setToolTipText(getTextForThisMarker(markersNearMousePointer.get(0)));
                }

                else if(markersNearMousePointer.size()>1){

                    ToolTipManager.sharedInstance().setDismissDelay(8000);
                    String textForAllMarkers="";

                    for(MapMarker m: markersNearMousePointer){


                        textForAllMarkers+= (getTextForThisMarker(m)+"<br>");
                    }

                    map().setToolTipText(textForAllMarkers);
                }
            }
        });
    }


    /**
     * @param args Application program arguments (which are ignored)
     */
    public static void main(String[] args) {
        new Application().setVisible(true);
    }

    private void initialize() {
        // To use the live twitter stream, use the following line
        // twitterSource = new LiveTwitterSource();

        // To use the recorded twitter stream, use the following line
        // The number passed to the constructor is a speedup value:
        //  1.0 - play back at the recorded speed
        //  2.0 - play back twice as fast
        twitterSource = new LiveTwitterSource();

        queries = new ArrayList<>();
    }

    /**
     * A new query has been entered via the User Interface
     * @param   query   The new query object
     */
    public void addQuery(Query query) {
        queries.add(query);
        Set<String> allterms = getQueryTerms();
        twitterSource.setFilterTerms(allterms);
        contentPanel.addQuery(query);
        twitterSource.addObserver(query);
        // TODO: This is the place where you should connect the new query to the twitter source
    }

    /**
     * return a list of all terms mentioned in all queries. The live twitter source uses this
     * to request matching tweets from the Twitter API.
     * @return
     */
    private Set<String> getQueryTerms() {
        Set<String> ans = new HashSet<>();
        for (Query q : queries) {
            ans.addAll(q.getFilter().terms());
        }
        return ans;
    }

    //getter for the current Twitter source
    public TwitterSource getTwitterSource(){
        return twitterSource;
    }

    // How big is a single pixel on the map?  We use this to compute which tweet markers
    // are at the current most position.
    private double pixelWidth(Point p) {
        ICoordinate center = map().getPosition(p);
        ICoordinate edge = map().getPosition(new Point(p.x + 1, p.y));
        return SphericalGeometry.distanceBetween(center, edge);
    }

    // Get those layers (of tweet markers) that are visible because their corresponding query is enabled
    private Set<Layer> getVisibleLayers() {
        Set<Layer> ans = new HashSet<>();
        for (Query q : queries) {
            if (q.getVisible()) {
                ans.add(q.getLayer());
            }
        }
        return ans;
    }

    // Get all the markers at the given map position, at the current map zoom setting
    private List<MapMarker> getMarkersCovering(ICoordinate pos, double pixelWidth) {
        List<MapMarker> ans = new ArrayList<>();
        Set<Layer> visibleLayers = getVisibleLayers();
        for (MapMarker m : map().getMapMarkerList()) {
            if (!visibleLayers.contains(m.getLayer())) continue;
            double distance = SphericalGeometry.distanceBetween(m.getCoordinate(), pos);
            if (distance < m.getRadius() * pixelWidth) {
                ans.add(m);
            }
        }
        return ans;
    }
    //Get the image and Tweet text to be displayed for the given marker
    private String getTextForThisMarker(MapMarker m){

        //The default image's URL when a Twitter user doesn't have a profile picture
        String defaultImageURL = "http://png-2.findicons.com/files/icons/1995/web_application/48/smiley.png";
        String imageURL = ((MapMarkerAdvanced) m).getStatus().getUser().getBiggerProfileImageURL();

        BufferedImage img = null;
        try {
            img = ImageIO.read(new URL(imageURL));
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        if (img == null){
            imageURL = defaultImageURL;
        }

        String html = "<html><img src=\"" + imageURL + "\"" + ">";
        String tweetText = ((MapMarkerAdvanced) m).getStatus().getText();
        return (html + tweetText);

    }

    public JMapViewer map() {
        return contentPanel.getViewer();
    }

    // Update which queries are visible after any checkBox has been changed
    public void updateVisibility() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.out.println("Recomputing visible queries");
                for (Query q : queries) {
                    JCheckBox box = q.getCheckBox();
                    Boolean state = box.isSelected();
                    q.setVisible(state);
                }
                map().repaint();
            }
        });
    }

    // A query has been deleted, remove all traces of it
    public void terminateQuery(Query query) {
        // TODO: This is the place where you should disconnect the expiring query from the twitter source
        queries.remove(query);
        Set<String> allterms = getQueryTerms();
        twitterSource.setFilterTerms(allterms);
        twitterSource.deleteObserver(query);
    }
}
