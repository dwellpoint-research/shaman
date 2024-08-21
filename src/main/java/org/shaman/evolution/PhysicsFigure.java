package org.shaman.evolution;

import cern.jet.random.AbstractDistribution;
import jinngine.physics.Body;
import jinngine.physics.Scene;
import jinngine.physics.constraint.joint.UniversalJoint;

/**
 * @author Johan Kaers
 */
public interface PhysicsFigure
{
    int getNumberOfBodies();

    int getNumberOfJoints();

    AbstractDistribution[]makeGenotypeDistributions(int numberOfNeuronWeights);

    Components build(Scene scene, NumberGenotype genotype);

    class Components
    {
        public Body[] bodies;
        public UniversalJoint[] joints;
    }
}
