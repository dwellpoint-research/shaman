package org.shaman.evolution;

import jinngine.geometry.Box;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.Scene;
import jinngine.physics.constraint.joint.UniversalJoint;
import jinngine.physics.force.GravityForce;

/**
 * @author Johan Kaers
 */
public class FigureUtils
{
    // Initial position of figure.
    public static final double XPOS = -10.0;
    public static final double YPOS = -18.0;
    public static final double ZPOS = -25.0;

    public static final double JOINT_DIRECTION1_LIMIT = Math.PI / 2.0; //6.0;      // Universal joint direction 1 limits
    public static final double JOINT_DIRECTION2_LIMIT = Math.PI / 2.0; //6.0;      // Universal joint direction 2 limits


    protected UniversalJoint connectToParent(Scene scene, String name, Body parent, int side, double xSize, double ySize, double zSize)
    {
        Box boxGeometry, parentGeometry;
        Vector3 parentDimensions;
        Body box;
        double xPos, yPos, zPos, jointXPos, jointYPos, jointZPos;

        // Create a new Box of the given dimensions
        boxGeometry = new Box(xSize, ySize, zSize);
        box = new Body(name, boxGeometry);

        // Find (centre) position of the Box, starting from the parent's (centre) position.
        parentGeometry = (Box)parent.getGeometries().next();
        parentDimensions = parentGeometry.getDimentions();
        xPos = parent.getPosition().get(0);
        yPos = parent.getPosition().get(1);
        zPos = parent.getPosition().get(2);
        jointXPos = xPos;
        jointYPos = yPos;
        jointZPos = zPos;
        if      (side == 0) // right of parent
        {
            xPos      = parent.getPosition().get(0) + ((parentDimensions.get(0) / 2) + (xSize / 2));
            box.setPosition(xPos, yPos, zPos);
        }
        else if (side == 1) // left
        {
            xPos      = parent.getPosition().get(0) - ((parentDimensions.get(0) / 2) + (xSize / 2));
            box.setPosition(xPos, yPos, zPos);
        }
        box.setPosition(xPos, yPos, zPos);

        UniversalJoint joint;
        Vector3 jointPosition, jointNormal1, jointNormal2;

        // Find position of joint between parent and Box. Set ortogonal normal vector of joint axes.
        if (side == 0)       // right of parent
        {
            jointXPos = parent.getPosition().get(0) + (parentDimensions.get(0) / 2);
            jointNormal1 = new Vector3(1.0, 0.0, 0.0);
            jointNormal2 = new Vector3(0.0, 0.0, 1.0);
        }
        else if (side == 1)  // left
        {
            jointXPos = parent.getPosition().get(0) - (parentDimensions.get(0) / 2);
            jointNormal1 = new Vector3(1.0, 0.0, 0.0);
            jointNormal2 = new Vector3(0.0, 0.0, -1.0);
        }
        else
        {
            jointNormal1 = jointNormal2 = null;
        }
        jointPosition = new Vector3(jointXPos, jointYPos, jointZPos);
        joint = new UniversalJoint(parent, box, jointPosition, jointNormal1, jointNormal2);
        joint.getFirstAxisControler().setLimits(-JOINT_DIRECTION1_LIMIT, JOINT_DIRECTION1_LIMIT);
        joint.getSecondAxisControler().setLimits(-JOINT_DIRECTION2_LIMIT, JOINT_DIRECTION2_LIMIT);

        // Add box, joint with parent and gravity to the scene.
        scene.addConstraint(joint);
        scene.addBody(box);
        scene.addForce(new GravityForce(box));

        return joint;
    }
}
