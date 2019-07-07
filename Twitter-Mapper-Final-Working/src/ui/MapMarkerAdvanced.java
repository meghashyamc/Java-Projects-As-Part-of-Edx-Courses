package ui;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Layer;
import twitter4j.Status;
import util.ImageCache;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import static javax.swing.JComponent.TOOL_TIP_TEXT_KEY;

public class MapMarkerAdvanced extends MapMarkerSimple{


    private Image image;
    private Status status;

    public MapMarkerAdvanced(Layer layer, Coordinate coord, Color color, Status status) {
        super(layer, coord);
        setColor(Color.GRAY);
        setBackColor(color);
        this.status = status;
        this.image = util.Util.imageFromURL(status.getUser().getProfileImageURL());

    }


    public void paint(Graphics g, Point position, int radius) {
        int size = radius * 2;
        if (g instanceof Graphics2D && this.getBackColor() != null) {
            Graphics2D g2 = (Graphics2D)g;
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(3));
            g2.setPaint(this.getBackColor());
            g.fillOval(position.x - radius, position.y - radius, size, size);
            g2.setComposite(oldComposite);


        }

        g.setColor(this.getColor());
        g.drawOval(position.x - radius, position.y - radius, size, size);
        if (this.getLayer() == null || this.getLayer().isVisibleTexts()) {
            this.paintText(g, position);

        }


        g.drawImage(image,position.x-radius/2, position.y-radius/2, size/2, size/2 , this.getBackColor(), null);
        this.paintText(g, position);
    }

    public Status getStatus(){
        return status;
    }




}
