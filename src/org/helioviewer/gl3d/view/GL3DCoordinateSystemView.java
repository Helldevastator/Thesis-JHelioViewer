package org.helioviewer.gl3d.view;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.HEEQCoordinateSystem;
import org.helioviewer.gl3d.wcs.HeliocentricCartesianCoordinateSystem;
import org.helioviewer.gl3d.wcs.StonyhurstCoordinateSystem;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerPositionedMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;

/**
 * This view is responsible for providing information about the orientation of
 * the image layer. The orientation vector should give the image plane normal
 * and the coordinate system should define in which coordinate system this
 * normal is defined in.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCoordinateSystemView extends AbstractGL3DView implements GL3DView {
    private CoordinateSystem coordinateSystem;

    private MetaDataView metaDataView;

    private CoordinateVector orientation;

    public void render3D(GL3DState state) {
        GL2 gl = state.gl;

        this.renderChild(gl);
    }

    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystem;
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (this.metaDataView == null) {
            metaDataView = getAdapter(MetaDataView.class);
            MetaData metaData = metaDataView.getMetaData();

            initialiseCoordinateSystem(metaData);
        }
    }

    private void initialiseCoordinateSystem(MetaData metaData) {
        if (metaData instanceof HelioviewerOcculterMetaData) {
            // LASCO
            Log.debug("GL3DCoordinateSystemView: Creating LASCO Image Layer");
            this.coordinateSystem = getDefaultCoordinateSystem();
            this.orientation = getDefaultOrientation();
        } else if (metaData instanceof HelioviewerMetaData) {
            HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaData;
            if (hvMetaData.getInstrument().equalsIgnoreCase("MDI")) {
                // MDI
                Log.debug("GL3DCoordinateSystemView: Creating MDI Image Layer!");
                this.coordinateSystem = getDefaultCoordinateSystem();
                this.orientation = getDefaultOrientation();
            } else if (hvMetaData.getInstrument().equalsIgnoreCase("HMI")) {
                // HMI
                Log.debug("GL3DCoordinateSystemView: Creating HMI Image Layer!");
                this.coordinateSystem = getDefaultCoordinateSystem();
                this.orientation = getDefaultOrientation();
            } else if (hvMetaData.getInstrument().equalsIgnoreCase("EIT")) {
                // EIT
                Log.debug("GL3DCoordinateSystemView: Creating EIT Image Layer!");
                this.coordinateSystem = getDefaultCoordinateSystem();
                this.orientation = getDefaultOrientation();
            } else if (hvMetaData.getInstrument().equalsIgnoreCase("AIA")) {
                // AIA
                Log.debug("GL3DCoordinateSystemView: Creating AIA Image Layer!");
                this.coordinateSystem = getDefaultCoordinateSystem();
                this.orientation = getDefaultOrientation();
                System.out.println("AIA-CoordinateSystem :  " + this.orientation);
            }
            
            else
            {
                // STEREO
                Log.debug("GL3DCoordinateSystemView: Creating STEREO Image Layer!");
                if (metaData instanceof HelioviewerPositionedMetaData) {
                    HelioviewerPositionedMetaData hpmd = ((HelioviewerPositionedMetaData) metaData);
                    if (hpmd.isStonyhurstProvided()) {
                        Calendar c = new GregorianCalendar();
                        c.setTime(hpmd.getDateTime().getTime());
                        double b0 = Astronomy.getB0InRadians(c);
                        this.coordinateSystem = new StonyhurstCoordinateSystem(b0);
                        this.orientation = this.coordinateSystem.createCoordinateVector(Math.toRadians(hpmd.getStonyhurstLongitude()), Math.toRadians(hpmd.getStonyhurstLatitude()), hpmd.getDobs());
                        System.out.println("Stonyhurst-CoordinateSystem :  " + this.orientation);
                        Log.debug("GL3DCoordinateSystemView: Providing Stonyhurst Coordinate System and orientation");
                    } else if (hpmd.isHEEQProvided()) {
                        Calendar c = new GregorianCalendar();
                        c.setTime(hpmd.getDateTime().getTime());
                        double b0 = Astronomy.getB0InRadians(c);
                        this.coordinateSystem = new HEEQCoordinateSystem(b0);
                        this.orientation = this.coordinateSystem.createCoordinateVector(hpmd.getHEEQX(), hpmd.getHEEQY(), hpmd.getHEEQZ());
                        System.out.println("HEEQ-CoordinateSystem :  " + this.orientation);
                        Log.debug("GL3DCoordinateSystemView: Providing HEEQ Coordinate System and orientation");
                    } else {
                        this.coordinateSystem = getDefaultCoordinateSystem();
                        this.orientation = getDefaultOrientation();
                    }
                }
                
                
                System.out.println("ORIENTATION: =================================================================================================================================================================================================================");
                
                /*this.coordinateSystem = getDefaultCoordinateSystem();
                
                double angle = Math.random() * Math.PI * 2;
                
                GL3DVec3d o = new GL3DVec3d(0, 0, 1);
                GL3DMat4d r1 = GL3DMat4d.rotation((Math.random() * Math.PI * 2) - Math.PI, GL3DVec3d.XAxis);
                GL3DMat4d r2 = GL3DMat4d.rotation((Math.random() * Math.PI * 2) - Math.PI, GL3DVec3d.YAxis);
                o = r1.multiply(o);
                o = r2.multiply(o);
                
                this.orientation = getDefaultCoordinateSystem().createCoordinateVector(o.x, o.y, o.z);
                
                System.out.println(this.orientation);*/
                
                // DEBUG
                /*double x = this.orientation.getValue(0);
                double y = this.orientation.getValue(1);
                this.orientation.setValue(0, y);
                this.orientation.setValue(1, x);*/
                
            }
            
        } else {
            Log.warn("GL3DCoordinateSystemView: Unknown Image Layer!");
            this.coordinateSystem = getDefaultCoordinateSystem();
            this.orientation = getDefaultOrientation();
        }
        /*
         * 
         * if(metaData instanceof HelioviewerPositionedMetaData) {
         * HelioviewerPositionedMetaData hpmd = ((HelioviewerPositionedMetaData)
         * metaData); if(hpmd.isStonyhurstProvided()) { Calendar c = new
         * GregorianCalendar(); c.setTime(hpmd.getDateTime().getTime()); double
         * b0 = Astronomy.getB0InRadians(c); this.coordinateSystem = new
         * StonyhurstCoordinateSystem(b0); this.orientation =
         * this.coordinateSystem
         * .createCoordinateVector(Math.toRadians(hpmd.getStonyhurstLongitude
         * ()), Math.toRadians(hpmd.getStonyhurstLatitude()), hpmd.getDobs());
         * Log.debug(
         * "GL3DCoordinateSystemView: Providing Stonyhurst Coordinate System and orientation"
         * ); } else if(hpmd.isHEEQProvided()) { Calendar c = new
         * GregorianCalendar(); c.setTime(hpmd.getDateTime().getTime()); double
         * b0 = Astronomy.getB0InRadians(c); this.coordinateSystem = new
         * HEEQCoordinateSystem(b0); this.orientation =
         * this.coordinateSystem.createCoordinateVector(hpmd.getHEEQX(),
         * hpmd.getHEEQY(), hpmd.getHEEQZ()); Log.debug(
         * "GL3DCoordinateSystemView: Providing HEEQ Coordinate System and orientation"
         * ); // } else if(hpmd.isHEEProvided()) { // this.coordinateSystem =
         * new HEECoordinateSystem(); // this.orientation =
         * this.coordinateSystem.createCoordinateVector(hpmd.getHEEX(),
         * hpmd.getHEEY(), hpmd.getHEEZ()); // Log.debug(
         * "GL3DCoordinateSystemView: Providing HEE Coordinate System and orientation"
         * ); // } else if(hpmd.isCarringtonProvided()) { //
         * this.coordinateSystem = new
         * CarringtonCoordinateSystem(hpmd.getDateTime().getTime()); //
         * this.orientation =
         * this.coordinateSystem.createCoordinateVector(Math.toRadians
         * (hpmd.getCrln()), Math.toRadians(hpmd.getCrlt()), hpmd.getDobs()); //
         * Log.debug(
         * "GL3DCoordinateSystemView: Providing Carrington Coordinate System and orientation"
         * ); } else { // this.coordinateSystem = new
         * HEEQCoordinateSystem(((HelioviewerMetaData
         * )metaData).getDateTime().getTime()); // this.orientation =
         * this.coordinateSystem.createCoordinateVector(1, 0, 0);
         * this.coordinateSystem = getDefaultCoordinateSystem();
         * this.orientation = getDefaultOrientation(); // Log.warn(
         * "GL3DCoordinateSystemView: No usable coordinates given for orientation"
         * ); }
         * 
         * metaDataView.addViewListener(new ViewListener() {
         * 
         * public void viewChanged(View sender, ChangeEvent aEvent) {
         * coordinateSystem.fireCoordinateSystemChanged(); } }); } else
         * if(metaData instanceof HelioviewerMetaData) { HelioviewerMetaData hmd
         * = (HelioviewerMetaData)metaData; this.coordinateSystem = new
         * StonyhurstCoordinateSystem(hmd.getDateTime().getTime()); // double
         * longitude = 0; // double radius = Constants.SunMeanDistanceToEarth;
         * Calendar c = new GregorianCalendar();
         * c.setTime(hmd.getDateTime().getTime()); double b0 =
         * Astronomy.getB0InRadians(c); // this.orientation =
         * this.coordinateSystem.createCoordinateVector(longitude, latitude,
         * radius);
         * 
         * // //Default CoordinateSystem this.coordinateSystem = new
         * HEEQCoordinateSystem(b0); // //DefaultOrientation this.orientation =
         * this.coordinateSystem.createCoordinateVector(1, 0, 0); Log.debug(
         * "GL3DCoordinateSystemView: Providing Default HEEQ Coordinate System and orientation"
         * ); } else { //Default CoordinateSystem //DefaultOrientation
         * this.coordinateSystem = getDefaultCoordinateSystem();
         * this.orientation = getDefaultOrientation(); Log.debug(
         * "GL3DCoordinateSystemView: Providing Default Coordinate System and orientation"
         * ); }
         */

        Log.debug("GL3DCoordinateSystem: CoordinateSystemView produced a " + this.coordinateSystem.getClass());
    }

    private static CoordinateSystem getDefaultCoordinateSystem() {
        return new HeliocentricCartesianCoordinateSystem();
        // return new HEECoordinateSystem();
    }

    private static CoordinateVector getDefaultOrientation() {
        return getDefaultCoordinateSystem().createCoordinateVector(0, 0, 1);
    }

    public CoordinateVector getOrientation() {
        return this.orientation;
    }
}
