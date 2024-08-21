package org.shaman.evolution;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import jinngine.geometry.Box;
import jinngine.geometry.ConvexHull;
import jinngine.geometry.Geometry;
import jinngine.math.Matrix4;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.rendering.Rendering;
import org.shaman.exceptions.ConfigException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
public class PhysicsRendering extends Frame implements Rendering, GLEventListener, MouseListener, MouseMotionListener, KeyListener
{
    public static final String FRAME_TITLE = "Evolved physical figure";

    private java.util.List<DrawShape> toDraw = new ArrayList<>();
    private DrawShape floor;
    private final PhysicsFitnessFunction callback;
    private Integer browsePosition;
    private final EventCallback mouseCallback;
    private final GLCanvas canvas = new GLCanvas();
    private Animator animator = new Animator(this.canvas);
    private final GLU glu = new GLU();
    private double width;
    private double height;
    private double drawHeight;
    private Vector3 cameraTo = new Vector3(-12,-3,0).multiply(1);
    private final Vector3 cameraFrom = cameraTo.add(new Vector3(0,0.5,1).multiply(5));
    private Texture floorTexture;
    //camera transform
    public double[] proj = new double[16];
    public double[] camera = new double[16];
    public double zoom = 0.95;

    private FitnessLogPanel fitnessLogPanel;

    private interface DrawShape {
        public Iterator<Vector3[]> getFaces();
        public Matrix4 getTransform();
        public Body getReferenceBody();
    }

    public PhysicsRendering(PhysicsFitnessFunction callback, EventCallback mouseCallback) {
        this.callback = callback;
        this.mouseCallback = mouseCallback;
        setTitle(FRAME_TITLE);
        setSize(1024,(int)(1024/(1.77777))+150);
        addWindowListener(new WindowAdapter() {public void windowClosing(java.awt.event.WindowEvent e) {
            System.exit(0);}
        } );

        canvas.setIgnoreRepaint( true );
        canvas.addGLEventListener(this);
        canvas.setVisible(true);
        add(canvas, java.awt.BorderLayout.CENTER);
        setVisible(true);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        Evolution evolution = callback.getEnvironment().getEvolution();
        if (evolution.getFitnessBufferCopy() != null)
        {
            FitnessLogPanel logPanel = new FitnessLogPanel(evolution);
            logPanel.setMaximumFitness(PhysicsFitnessFunction.MAX_FITNESS);
            add(logPanel, BorderLayout.SOUTH);
            
            //canvas.createBufferStrategy(2);
            //logPanel.setBufferStrategy(canvas.getBufferStrategy());
            this.fitnessLogPanel = logPanel;
        }
        else this.fitnessLogPanel = null;
    }

    public void clearToDraw()
    {
        this.toDraw.clear();
    }

    @Override
    public void drawMe(final Geometry g)
    {
        if (g instanceof ConvexHull)
        {
            toDraw.add( new DrawShape() {
                @Override
                public Iterator<Vector3[]> getFaces() {
                    return ((ConvexHull)g).getFaces();
                }
                @Override
                public Matrix4 getTransform() {
                    return g.getTransform();
                }
                @Override
                public Body getReferenceBody() {
                    return g.getBody();
                }
            });
        }

        if (g instanceof Box) toDraw.add(makeBoxDrawShape((Box)g));
    }

    public void setFloor(Box floor)
    {
        this.floor = makeBoxDrawShape(floor);
    }

    private DrawShape makeBoxDrawShape(Box g)
    {
        final java.util.List<Vector3> vertices = new ArrayList<Vector3>();
        vertices.add( new Vector3(  0.5,  0.5,  0.5));
        vertices.add( new Vector3( -0.5,  0.5,  0.5));
        vertices.add( new Vector3(  0.5, -0.5,  0.5));
        vertices.add( new Vector3( -0.5, -0.5,  0.5));
        vertices.add( new Vector3(  0.5,  0.5, -0.5));
        vertices.add( new Vector3( -0.5,  0.5, -0.5));
        vertices.add( new Vector3(  0.5, -0.5, -0.5));
        vertices.add( new Vector3( -0.5, -0.5, -0.5));
        final ConvexHull hull = new ConvexHull(vertices);

        return new DrawShape() {
            @Override
            public Iterator<Vector3[]> getFaces() {
                return hull.getFaces();
            }
            @Override
            public Matrix4 getTransform() {
                return g.getTransform();
            }
            @Override
            public Body getReferenceBody() {
                return g.getBody();
            }
        };
    }

    @Override
    public void start()
    {
        animator.start();
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        // Perform ratio time-steps on the model
        callback.tick();

        setTitle(FRAME_TITLE+": at "+((int)(this.callback.getTime()*1000))+"ms. Distance traveled: "+((int)(this.callback.getDistance()*1000))+" mm");

        // Clear buffer, etc.
        GL2 gl = (GL2)drawable.getGL();
        gl.glClearColor(1.0f, 1.0f,1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glLoadIdentity();

        // Set camera transform
        glu.gluLookAt(cameraFrom.x, cameraFrom.y, cameraFrom.z, cameraTo.x, cameraTo.y, cameraTo.z, 0, 1, 0);

        //copy camera transform
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, camera, 0);

        // Draw textured floor plain.
        {
            DrawShape shape = this.floor;

            gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
            gl.glPushMatrix();
            gl.glMultMatrixd(shape.getTransform().toArray(), 0);
            //gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);

            //gl.glDisable(GL2.GL_LIGHTING);

            // Draw texture floor and track centre of body?

            gl.glActiveTexture(GL2.GL_TEXTURE0);
            floorTexture.enable(gl);
            floorTexture.bind(gl);

            java.util.List<Vector3[]> faces = new LinkedList<>();
            Iterator<Vector3[]> i = shape.getFaces();
            while(i.hasNext()) faces.add(i.next());

            final float TEXTURE_ZOOM = 500f;

            //while(i.hasNext())
            {
                //gl.glBegin(GL2.GL_POLYGON);
                gl.glBegin(GL2.GL_QUADS);

                Vector3[] face = faces.get(3); // i.next();
                //compute normal
                Vector3 n =face[1].sub(face[0]).cross(face[2].sub(face[1])).normalize();

                float [][]texcoords = {{0.0f,0.0f}, {TEXTURE_ZOOM,0.0f}, {TEXTURE_ZOOM, TEXTURE_ZOOM}, {0.0f, TEXTURE_ZOOM}};
                int texi = 0;
                for ( Vector3 v: face) {
                    gl.glNormal3d(n.x, n.y, n.z);
                    gl.glTexCoord2f(texcoords[texi][0], texcoords[texi][1]);
                    texi++;
                    //gl.glTexCoord2f(1.0f, 1.0f);
                    //gl.glTexCoord2f(((int)v.x)*1000, ((int)v.y)*1000);
                    //gl.glColor3d(v.a1, v.a2, v.a3);
                    gl.glVertex3d(v.x, v.y, v.z);
                    //gl.glTexCoord2f(0.0f, 1.0f);
                }
                gl.glEnd();
            }
            floorTexture.disable(gl);
            //gl.glEnable(GL2.GL_LIGHTING);


            gl.glPopMatrix();
            gl.glPopAttrib();

            //gl.glLoadIdentity();
        }

        for (DrawShape shape: toDraw)
        {
            gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
            gl.glPushMatrix();
            gl.glMultMatrixd(shape.getTransform().toArray(), 0);


            if (shape.getReferenceBody().deactivated)
            {
                //float ambientLight[] = { 1.5f, 1.5f, 2.0f, 1.0f };
                //		float diffuseLight[] = { 0.8f, 0.0f, 0.8f, 1.0f };
                //		float specularLight[] = { 0.5f, 0.5f, 0.5f, 1.0f };
                //		float position[] = { -1.5f, 1.0f, -4.0f, 1.0f };

                // Assign created components to GL_LIGHT0
                //gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight,0);
                //		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuseLight,0);
                //		gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, specularLight,0);
                //		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, position,0);

            }

            //gl.glPushMatrix();
            //gl.glMultMatrixd(Matrix4.pack(shape.getTransform()),0);

            // Draw faces
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
            Iterator<Vector3[]> i = shape.getFaces();
            while (i.hasNext())
            {
                gl.glBegin(GL2.GL_POLYGON);
                Vector3[] face = i.next();
                //compute normal
                Vector3 n =face[1].sub(face[0]).cross(face[2].sub(face[1])).normalize();

                for ( Vector3 v: face) {
                    gl.glNormal3d(n.x, n.y, n.z);
                    //gl.glTexCoord2f(1.0f, 1.0f);
                    //gl.glColor3d(v.a1, v.a2, v.a3);
                    gl.glVertex3d(v.x, v.y, v.z);
                    gl.glTexCoord2f(0.0f, 1.0f);
                }
                gl.glEnd();
            }

            // Draw outlines
            gl.glPolygonMode( GL.GL_FRONT, GL2.GL_LINE );
            gl.glLineWidth(1.7f);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glScaled(1.01, 1.01, 1.01);
            i = shape.getFaces();
            while (i.hasNext())
            {
                gl.glBegin(GL2.GL_POLYGON);
                Vector3[] face = i.next();
                //compute normal
                Vector3 n =face[1].sub(face[0]).cross(face[2].sub(face[1])).normalize();

                for ( Vector3 v: face) {
                    gl.glNormal3d(n.x, n.y, n.z);
                    //gl.glTexCoord2f(1.0f, 1.0f);
                    gl.glColor3d(0.2,0.2, 0.2);
                    gl.glVertex3d(v.x, v.y, v.z);
                    //gl.glTexCoord2f(0.0f, 1.0f);
                }
                gl.glEnd();
            }
            gl.glEnable(GL2.GL_LIGHTING);

            gl.glPopMatrix();
            gl.glPopAttrib();
        }

        {
            //draw shadows
            gl.glLoadIdentity();

            gl.glDisable(GL2.GL_LIGHTING);

            // Set camera transform
            glu.gluLookAt(cameraFrom.x, cameraFrom.y, cameraFrom.z, cameraTo.x, cameraTo.y, cameraTo.z, 0, 1, 0);

            gl.glMultMatrixd(shadowProjectionMatrix(new Vector3(75, 350, -75), new Vector3(0, -20.0+0.02, 0), new Vector3(0, -1, 0)), 0);
            //gl.glColor3d(0.85, 0.85, 0.85);
            gl.glColor3d(0.25, 0.25, 0.25);

            for (DrawShape shape : toDraw)
            {
                gl.glPushMatrix();
                gl.glMultMatrixd(shape.getTransform().toArray(), 0);
//			gl.glMultMatrixd(Matrix4.pack(dt.shape.getTransform()),0);

                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
                Iterator<Vector3[]> i = shape.getFaces();
                while (i.hasNext())
                {
                    gl.glBegin(GL2.GL_POLYGON);
                    Vector3[] face = i.next();
                    for (Vector3 v : face)
                    {
                        gl.glVertex3d(v.x, v.y, v.z);
                    }
                    gl.glEnd();
                }

                gl.glPopMatrix();
            }

            gl.glEnable(GL2.GL_LIGHTING);
        }

        // Finish this frame
        gl.glFlush();


        if (this.fitnessLogPanel != null) this.fitnessLogPanel.draw();
    }

    //@Override
    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // Setup GL 
        GL2 gl = (GL2)drawable.getGL();
        gl.glEnable (GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        //enable vsync
        gl.setSwapInterval(1);

        // init some lighting
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        //gl.glShadeModel(GL.GL_FLAT);

        // Create light components
        float ambientLight[] = { 2.0f, 2.0f, 2.0f, 1.0f };
        float diffuseLight[] = { 0.2f, 0.2f, 0.2f, 1.0f };
        float specularLight[] = { 0.5f, 0.5f, 0.5f, 1.0f };
        float position[] = { -1.5f, 25.0f, -4.0f, 1.0f };

        // Assign created components to GL_LIGHT0
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight,0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight,0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularLight,0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, position,0);

        this.floorTexture = initializeTexture((GL3)drawable.getGL());
    }

    private Texture initializeTexture(GL3 gl3) {

        Texture t = null;

        try
        {
            t = TextureIO.newTexture(this.getClass().getResource("/data/floor_bw.jpg"), false, ".jpg");
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT); // GL3.GL_CLAMP_TO_EDGE);
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT); //GL3.GL_CLAMP_TO_EDGE);
        }
        catch (IOException | GLException ex)
        {
            ex.printStackTrace();
        }

        return t;
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable)
    {

    }

    @Override
    public void reshape(GLAutoDrawable drawable ,int x,int y, int w, int h) {
        // Setup wide screen view port
        GL2 gl = (GL2)drawable.getGL();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustum (-1.77777*zoom, 1.777777*zoom, -1.0*zoom, 1.0*zoom, 4.0, 100.0);
        this.height = h; this.width = w;
        this.drawHeight = (int)((double)width/1.77777);
        gl.glViewport (0, (int)((height-drawHeight)/2.0), (int)width, (int)drawHeight);
        //double[] proj = new double[16];
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, proj, 0);
    }

    public void setCameraTo(Vector3 cameraTo)
    {
        this.cameraTo = cameraTo;
    }


    private Matrix4 getCameraMatrix() {
        return new Matrix4(camera);
    }

    private Matrix4 getProjectionMatrix() {
        return new Matrix4(proj);
    }

    public void getPointerRay(Vector3 p, Vector3 d, double x, double y) {
        // clipping planes
        Vector3 near = new Vector3(2*x/(double)width-1,-2*(y-((height-drawHeight)*0.5))/(double)drawHeight+1, 0.7);
        Vector3 far = new Vector3(2*x/(double)width-1,-2*(y-((height-drawHeight)*0.5))/(double)drawHeight+1, 0.9);

        //inverse transform
        Matrix4 T = getProjectionMatrix().multiply(getCameraMatrix()).inverse();

        Vector3 p1 = new Vector3();
        Vector3 p2 = new Vector3();

        Matrix4.multiply(T,near,p1);
        Matrix4.multiply(T,far,p2);

        p.assign(p1);
        d.assign(p2.sub(p1).normalize());
    }
    /**
     * This is where the "magic" is done:
     *
     * Multiply the current ModelView-Matrix with a shadow-projetion
     * matrix.
     *
     * l is the position of the light source
     * e is a point on within the plane on which the shadow is to be
     *   projected.  
     * n is the normal vector of the plane.
     *
     * Everything that is drawn after this call is "squashed" down
     * to the plane. Hint: Gray or black color and no lighting 
     * looks good for shadows *g*
     */
    private double[] shadowProjectionMatrix(Vector3 l, Vector3 e, Vector3  n)
    {
        double d, c;
        double[] mat = new double[16];

        // These are c and d (corresponding to the tutorial)

        d = n.x*l.x + n.y*l.y + n.z*l.z;
        c = e.x*n.x + e.y*n.y + e.z*n.z - d;

        // Create the matrix. OpenGL uses column by column
        // ordering

        mat[0]  = l.x*n.x+c;
        mat[4]  = n.y*l.x;
        mat[8]  = n.z*l.x;
        mat[12] = -l.x*c-l.x*d;

        mat[1]  = n.x*l.y;
        mat[5]  = l.y*n.y+c;
        mat[9]  = n.z*l.y;
        mat[13] = -l.y*c-l.y*d;

        mat[2]  = n.x*l.z;
        mat[6]  = n.y*l.z;
        mat[10] = l.z*n.z+c;
        mat[14] = -l.z*c-l.z*d;

        mat[3]  = n.x;
        mat[7]  = n.y;
        mat[11] = n.z;
        mat[15] = -d;

        return mat;
    }


    public void getCamera(Vector3 from, Vector3 to) {
        from.assign(cameraFrom);
        to.assign(cameraTo);
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        Vector3 p = new Vector3();
        Vector3 d = new Vector3();
        getPointerRay(p, d, e.getX(), e.getY());
        mouseCallback.mousePressed((double)e.getX(), (double)e.getY(), p, d );
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseCallback.mouseReleased();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vector3 p = new Vector3();
        Vector3 d = new Vector3();
        getPointerRay(p, d, e.getX(), e.getY());
        mouseCallback.mouseDragged((double)e.getX(), (double)e.getY(), p, d );
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyPressed(KeyEvent arg0)
    {
        char charPressed = arg0.getKeyChar();
        if (charPressed ==' ')
        {
            mouseCallback.spacePressed();
        }
    }

    @Override
    public void keyReleased(KeyEvent arg0)
    {
        char charPressed = arg0.getKeyChar();
        key(charPressed);
    }
    
    public void key(char charPressed)
    {
        if (charPressed ==' ')
        {
            mouseCallback.spaceReleased();
        }
        else if (charPressed == 'n')
        {
            callback.newSimulation(false);
        }
        else if (charPressed == 'b')
        {
            callback.newSimulation(true);
        }
        else if (charPressed == 'c')
        {
            try
            {
                callback.getEnvironment().getEvolution().initRandomPopulation();
            }
            catch(ConfigException ex)
            {
                ex.printStackTrace();
            }
        }
        // Browse forward / back through (evolved) genotypes
        else if (charPressed == '1')
        {
            Evolution evolution = callback.getEnvironment().getEvolution();
            if (this.browsePosition == null) this.browsePosition = 0;
            else
            {
                this.browsePosition++;
                if (this.browsePosition >= evolution.getGenotypes().length) this.browsePosition = 0;
            }
            callback.newSimulation((NumberGenotype)evolution.getGenotypes()[this.browsePosition]);
        }
        else if (charPressed == '2')
        {
            Evolution evolution = callback.getEnvironment().getEvolution();
            if (this.browsePosition == null) this.browsePosition = 0;
            else
            {
                this.browsePosition--;
                if (this.browsePosition < 0) this.browsePosition = evolution.getGenotypes().length-1;
            }
            callback.newSimulation((NumberGenotype)evolution.getGenotypes()[this.browsePosition]);
        }
    }
    
    public Integer getBrowsePosition()
    {
        return this.browsePosition;
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }
}
