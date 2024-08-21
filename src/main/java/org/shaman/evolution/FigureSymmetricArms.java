package org.shaman.evolution;

import cern.jet.random.AbstractDistribution;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import jinngine.geometry.Box;
import jinngine.geometry.Geometry;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.Scene;
import jinngine.physics.constraint.joint.UniversalJoint;
import jinngine.physics.force.GravityForce;

/**
 * Figure containing 3 bodies
 * - 'body' cube
 * - 'arm1' attached with 2D universal joint to 'body' at the middle of the right cube plane
 * - 'arm2' attached with 2D universal joint to 'body' at the middle of the left cube plane.
 * 
 * There are 4 shape parameters:
 * - body cube width
 * - arm width, height and depth
 *
 * @author Johan Kaers
 */
public class FigureSymmetricArms extends FigureUtils implements PhysicsFigure
{
    @Override
    public AbstractDistribution[] makeGenotypeDistributions(int numberOfNeuronWeights)
    {
        AbstractDistribution []genotypePDFs;
        RandomEngine random;
        int i;

        random = new MersenneTwister((int) System.currentTimeMillis());

        // Prepare the genotype PDFs
        genotypePDFs = new AbstractDistribution[numberOfNeuronWeights + 4];

        // The neuron weights.
        for (i=0; i< numberOfNeuronWeights; i++)
        {
            genotypePDFs[i] = new Normal(0.0, 1.0, new MersenneTwister(random.nextInt()));
        }

        // Body cube dimension
        genotypePDFs[i]   = new Uniform(1.0, 5.0, new MersenneTwister(random.nextInt()));

        // Arm X,Y and Z dimensions.
        genotypePDFs[i+1] = new Uniform(1.0, 6.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i+2] = new Uniform(0.3, 3.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i+3] = new Uniform(0.1, 2.0, new MersenneTwister(random.nextInt()));

        return genotypePDFs;
    }

    @Override
    public Components build(Scene scene, NumberGenotype gen)
    {
        double bodySize, armXSize, armYSize, armZSize;
        Geometry bodyGeometry;
        Body body, arm1, arm2;
        UniversalJoint shoulder1, shoulder2;

        // Get the figure dimensions from the end of the genotype vector.
        double []genotype = gen.getGenes();
        bodySize  = genotype[genotype.length-4];
        armXSize = genotype[genotype.length-3];
        armYSize = genotype[genotype.length-2];
        armZSize = genotype[genotype.length-1];

        // Create the Body box.
        bodyGeometry = new Box(bodySize, bodySize, bodySize);
        body = new Body("body", bodyGeometry);
        body.setPosition(new Vector3(XPOS, YPOS, ZPOS));
        scene.addBody(body);
        scene.addForce(new GravityForce(body));

        // Connect the two arms to it.
        shoulder1 = connectToParent(scene, "arm1", body, 0, armXSize, armYSize, armZSize);
        arm1 = shoulder1.getBodies().getSecond();
        shoulder2 = connectToParent(scene, "arm2", body, 1, armXSize, armYSize, armZSize);
        arm2 = shoulder2.getBodies().getSecond();

        // Return components for integration with the scene
        PhysicsFigure.Components components = new PhysicsFigure.Components();
        components.bodies = new Body[]{body, arm1, arm2};
        components.joints = new UniversalJoint[]{shoulder1, shoulder2};

        return components;
    }

    @Override
    public int getNumberOfBodies()
    {
        return 3;
    }

    @Override
    public int getNumberOfJoints()
    {
        return 2;
    }
}
