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
 * @author Johan Kaers
 */
public class FigureJointArms extends FigureUtils implements PhysicsFigure
{
    @Override
    public int getNumberOfBodies()
    {
        return 5;
    }

    @Override
    public int getNumberOfJoints()
    {
        return 4;
    }

    @Override
    public AbstractDistribution[] makeGenotypeDistributions(int numberOfNeuronWeights)
    {
        AbstractDistribution []genotypePDFs;
        RandomEngine random;
        int i;

        random = new MersenneTwister((int) System.currentTimeMillis());

        // Prepare the genotype PDFs
        genotypePDFs = new AbstractDistribution[numberOfNeuronWeights + 15];

        // The neuron weights.
        for (i=0; i< numberOfNeuronWeights; i++)
        {
            genotypePDFs[i] = new Normal(0.0, 1.0, new MersenneTwister(random.nextInt()));
        }

        // Body cube dimension
        genotypePDFs[i++]   = new Uniform(1.0, 5.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++]   = new Uniform(1.0, 5.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++]   = new Uniform(1.0, 5.0, new MersenneTwister(random.nextInt()));

        // Upper Arm1 X,Y and Z dimensions.
        genotypePDFs[i++] = new Uniform(1.0, 6.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.3, 3.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.1, 2.0, new MersenneTwister(random.nextInt()));

        // Upper Arm2 X,Y and Z dimensions.
        genotypePDFs[i++] = new Uniform(1.0, 6.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.3, 3.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.1, 2.0, new MersenneTwister(random.nextInt()));

        // Lower Arm1 X,Y and Z dimensions.
        genotypePDFs[i++] = new Uniform(1.0, 6.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.3, 3.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.1, 2.0, new MersenneTwister(random.nextInt()));

        // Lower Arm2 X,Y and Z dimensions.
        genotypePDFs[i++] = new Uniform(1.0, 6.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.3, 3.0, new MersenneTwister(random.nextInt()));
        genotypePDFs[i++] = new Uniform(0.1, 2.0, new MersenneTwister(random.nextInt()));

        return genotypePDFs;
    }

    @Override
    public Components build(Scene scene, NumberGenotype genotype)
    {
        Geometry bodyGeometry;
        Body body, upperArm1, upperArm2, lowerArm1, lowerArm2;
        UniversalJoint shoulder1, shoulder2, elbow1, elbow2;

        // Locate the figure dimensions from the end of the genotype vector.
        double []genes = genotype.getGenes();
        int i = genes.length - 15;

        // Create the Body box.
        bodyGeometry = new Box(genes[i++], genes[i++], genes[i++]);
        body = new Body("body", bodyGeometry);
        body.setPosition(new Vector3(XPOS, YPOS, ZPOS));
        scene.addBody(body);
        scene.addForce(new GravityForce(body));

        // Connect the two upper arms to it.
        shoulder1 = connectToParent(scene, "upperarm1", body, 0, genes[i++], genes[i++], genes[i++]);
        upperArm1 = shoulder1.getBodies().getSecond();
        shoulder2 = connectToParent(scene, "upperarm2", body, 1, genes[i++], genes[i++], genes[i++]);
        upperArm2 = shoulder2.getBodies().getSecond();

        // Connect the lower arms to the upper arms
        elbow1 = connectToParent(scene, "lowerarm1", upperArm1, 0, genes[i++], genes[i++], genes[i++]);
        lowerArm1 = elbow1.getBodies().getSecond();
        elbow2 = connectToParent(scene, "lowerarm2", upperArm2, 1, genes[i++], genes[i++], genes[i++]);
        lowerArm2 = elbow2.getBodies().getSecond();

        // Return components for integration with the scene
        PhysicsFigure.Components components = new PhysicsFigure.Components();
        components.bodies = new Body[]{body, upperArm1, upperArm2, lowerArm1, lowerArm2};
        components.joints = new UniversalJoint[]{shoulder1, shoulder2, elbow1, elbow2};

        return components;
    }
}
