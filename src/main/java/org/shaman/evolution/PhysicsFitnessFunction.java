/*********************************************************\
 *                                                       *
 *                     S H A M A N                       *
 *                   R E S E A R C H                     *
 *                                                       *
 *               Evolutionary Algorithms                 *
 *                                                       *
 *  April 2005 & January 2015 & March 2016               *
 *                                                       *
 *  by Johan Kaers  (johankaers@gmail.com)               *
 *  Copyright (c) 2015-6 Shaman Research                 *
 \*********************************************************/
package org.shaman.evolution;

import jinngine.collision.SAP2;
import jinngine.geometry.Box;
import jinngine.geometry.Geometry;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.ContactTrigger;
import jinngine.physics.DefaultDeactivationPolicy;
import jinngine.physics.DefaultScene;
import jinngine.physics.Scene;
import jinngine.physics.constraint.contact.ContactConstraint;
import jinngine.physics.constraint.joint.UniversalJoint;
import jinngine.physics.solver.NonsmoothNonlinearConjugateGradient;
import jinngine.rendering.Interaction;
import jinngine.rendering.Rendering;
import org.shaman.exceptions.ConfigException;
import org.shaman.exceptions.LearnerException;
import org.shaman.graph.GraphException;
import org.shaman.neural.NetworkGraph;
import org.shaman.neural.NetworkGraphFactory;
import org.shaman.neural.NeuralNet;
import org.shaman.neural.Neuron;

import java.util.Arrays;
import java.util.Iterator;

public class PhysicsFitnessFunction implements Rendering.Callback, FitnessFunction
{
    // Physics engine parameters
    public static final double SIMULATION_LENGTH = 40;          // Number of seconds to simulate to calculate fitness
    public static final double TIMESTEP_LENGTH = 0.05;          // Length in seconds of each time-step
    private static final String FLOOR_NAME = "floor";

    // Fitness selection
    public static final int DISTANCE_TOTAL_TRAVELLED = 1;        // Fitness is total distance travelled by Body
    public static final int DISTANCE_FROM_START = 2;        // Fitness is distance of body from start position
    public static final int DISTANCE = DISTANCE_FROM_START;

    public static final double MAX_FITNESS = 100.0;

    // Physics Engine 
    private Scene scene;                      // Scene containing bounding-box and figure
    private Body floor;                       // The fixed plane acting as the 'floor' on which the figure moves.
    private Body[] bodies;                    // e.g {body, arm1, arm2 }
    private UniversalJoint[] joints;          // e.g {shoulder1, shoulder2 }
    private boolean[] floorContact;           // [i] = true when bodies[i] is in contact with floor plane

    // Fitness simulation
    private PhysicsEnvironment environment;
    private double simTime;              // Simulation time. In [0, SIMULATION_LENGTH]
    private Vector3 startPos;            // Start position of body
    private Vector3 previousPos;         // Previous position of body
    private double distance;             // Total distance travelled by body

    // Control Neural Network
    private NeuralNet  brain;            // The control neural-net
    private double[]   sensorInput;      // Input layer sensor values.
    private double[]   motorOutput;      // Output layer joint motor forces 
    private int[][]    weightIndex;      // Genotype of neuron index mapping
    private double[][] neuronWeights;    // Genotype weight buffers

    // Visualization
    private PhysicsRendering rendering;

    // *********************************************************\
    // *    Calculate Fitness by Measuring Distance Moved      *
    // *********************************************************/
    public double fitness(Genotype gen) throws LearnerException
    {
        return (fitness(gen, false));
    }

    public double fitness(Genotype gen, boolean show) throws LearnerException
    {
        NumberGenotype netgen;

        // Reset distance measurement
        this.previousPos = null;
        this.distance = 0;

        // Run physical simulation of figure controlled by brain
        netgen = (NumberGenotype) gen;
        initializeSimulation(netgen);

        if (show) showSimulation();
        else      runSimulation();

        //System.err.println("Traveled distance = "+this.distance);

        // Fitness is distance moved by the body.
        return (this.distance);
    }

    // *********************************************************\
    // *   Initialization of Physics Engine and Neural Net     *
    // *********************************************************/
    public void initialize(PhysicsEnvironment environment) throws ConfigException
    {
        double[] sensorValues;
        double[] motorOutputs;
        NeuralNet net;
        int inSize, outSize, layer1Size, layer2Size;
        NetworkGraph mlpgraph;

        this.environment = environment;

        // Make space for touch, orientation and joint sensors. And Joint motor outputs
        sensorValues = new double[this.environment.getFigureTemplate().getNumberOfBodies() + (4 * this.environment.getFigureTemplate().getNumberOfJoints()) + 1];
        motorOutputs = new double[this.environment.getFigureTemplate().getNumberOfJoints() * 2];
        this.sensorInput = sensorValues;
        this.motorOutput = motorOutputs;

        try
        {
            net = new NeuralNet();

            // Make a fully connected multi-layer neural net graph with the right number of neurons in the layers
            inSize = sensorValues.length;
            layer1Size = 10;
            layer2Size = 7;
            outSize = motorOutputs.length;
            mlpgraph = NetworkGraphFactory.makeMLP(inSize, layer1Size, layer2Size, outSize);

            // Convert graph to real network of neurons
            net.setNeuronType(Neuron.ACTIVATION_SIGMOID_TANH, new double[]{1.0});
            net.create(mlpgraph);

            int i, numneu;
            Neuron neunow;

            // Initialize (non-input) neuron weights
            numneu = net.getNumberOfNeurons();
            for (i = inSize; i < numneu; i++)
            {
                neunow = net.getNeuron(i);
                neunow.setActivation(Neuron.ACTIVATION_SIGMOID_TANH);
                neunow.setActivationParameters(new double[]{1.0});
                neunow.initWeights(Neuron.WEIGHT_INIT_RANDOM, 0.01);
            }


            int j, pos, len;
            int[][] weightIndex;
            double[][] neuronWeights;
            Neuron neuron;

            // Find out which positions in the genotype correspond to weight neuron's weight
            weightIndex = new int[net.getNumberOfNeurons()][];
            neuronWeights = new double[net.getNumberOfNeurons()][];
            pos = 0;
            for (i = 0; i < net.getNumberOfNeurons(); i++)
            {
                neuron = net.getNeuron(i);
                len = neuron.getWeights().length;
                weightIndex[i] = new int[neuron.getWeights().length];
                neuronWeights[i] = new double[neuron.getWeights().length];
                for (j = 0; j < len; j++) weightIndex[i][j] = pos++;
            }
            this.weightIndex = weightIndex;
            this.neuronWeights = neuronWeights;
            this.brain = net;
        }
        catch (LearnerException | GraphException ex)
        {
            throw new ConfigException(ex);
        }
    }

    public NeuralNet getNeuralNet()
    {
        return (this.brain);
    }

    public PhysicsEnvironment getEnvironment()
    {
        return this.environment;
    }

    private void initializeSimulation(NumberGenotype gen) throws LearnerException
    {
        // Setup physics engine to simulate the figure.
        initWorld(gen);

        // Initialize fitness tracking
        initFitness();

        // Initialize the figure's control Neural Network
        initBrain(gen);
    }

    public void initWorld(NumberGenotype genotype)
    {
        // Setup default scene with floor plane
        Scene scene = new DefaultScene(new SAP2(), new NonsmoothNonlinearConjugateGradient(75), new DefaultDeactivationPolicy());
        scene.setTimestep(TIMESTEP_LENGTH);

        Body floor = new Body(FLOOR_NAME, new Box(1500, 20, 1500));
        floor.setPosition(new Vector3(0, -30, 0));
        floor.setFixed(true);
        scene.addBody(floor);
        this.floor = floor;

        // Build the 3-body figure with dimensions and brains encoded in the genotype.
        buildFigure(scene, genotype);
        this.scene = scene;
    }

    public void buildFigure(Scene scene, NumberGenotype genotype)
    {
        PhysicsFigure.Components components;

        // Build the figure with dimensions and brains encoded in the genotype.
        components = this.environment.getFigureTemplate().build(scene, genotype);;

        // Detect contact between bodies and the floor plane. Use this as sensor input.
        this.floorContact = new boolean[components.bodies.length];
        for (int i = 0; i < components.bodies.length; i++) scene.addTrigger(new FloorContact(components.bodies[i], i));

        // Keep track of the figure and the scene.
        this.bodies = components.bodies;
        this.joints = components.joints;
    }

    class FloorContact extends ContactTrigger
    {
        // Detect contact between given body and the floor plane to serve is sensor input
        public FloorContact(final Body body, final int contactIdx)
        {
            super(body, 2.0, new ContactTrigger.Callback()
            {
                @Override
                public void contactAboveThreshold(Body interactingBody, ContactConstraint constraint)
                {
                    floorContact[contactIdx] = true;
                }
                @Override
                public void contactBelowThreshold(Body interactingBody, ContactConstraint constraint)
                {
                    floorContact[contactIdx] = false;
                }
            });
        }
    }

    public void initFitness()
    {
        // Initialize distance tracking.
        this.simTime  = 0;
        this.distance = 0;
        this.previousPos = new Vector3(this.bodies[0].getPosition());
        this.startPos = new Vector3(this.bodies[0].getPosition());
    }

    private void initBrain(NumberGenotype genotype) throws LearnerException
    {
        int            i,j;
        double       []genweights;
        Neuron         neu;

        // Copy the evolved weights in the Neural Net
        genweights = genotype.getGenes();
        for (i=0; i<this.weightIndex.length; i++)
        {
            neu = this.brain.getNeuron(i);
            for (j=0; j<this.weightIndex[i].length; j++)
                this.neuronWeights[i][j] = genweights[this.weightIndex[i][j]];
            neu.setWeights(this.neuronWeights[i]);
        }
    }

    // *********************************************************\
    // *      Control and Physics simulation time-step         *
    // *********************************************************/
    private void showSimulation()
    {
        PhysicsRendering rendering;

        // Start or rest the visualization
        if (this.rendering == null)
        {
            rendering = new PhysicsRendering(this, new Interaction(this.scene));
        }
        else
        {
            rendering = this.rendering;
            rendering.clearToDraw();
        }

        for(Body body: Arrays.asList(this.bodies))
        {
            // Show (new) simulated figure.
            for (Iterator<Geometry> itGeom = body.getGeometries(); itGeom.hasNext(); ) rendering.drawMe(itGeom.next());
        }
        for (Iterator<Geometry> itGeom = this.floor.getGeometries(); itGeom.hasNext(); )
        {
            // And the textured floor.
            rendering.setFloor((Box)itGeom.next());
        }

        if (this.rendering == null) rendering.start();

        this.rendering = rendering;
    }

    private void runSimulation() throws LearnerException
    {
        // Run time-steps until the length of the simulation is reached.
        while(this.simTime <= SIMULATION_LENGTH) tick();

        // Fitness is distance from start position at the end of the simulation?
        if (DISTANCE == DISTANCE_FROM_START)
        {
            this.distance = this.bodies[0].getPosition().sub(this.startPos).norm();

            // Too much distance covered probably means numeric instability of the physics engine messed things up.
            if (this.distance > MAX_FITNESS) this.distance = 0;
        }
    }

    @Override
    public void tick()
    {
        if (this.simTime < SIMULATION_LENGTH)
        {
            this.simTime += TIMESTEP_LENGTH;

            // Read sensors, update brain state, apply motor outputs.
            updateBrain();

            this.scene.tick();

            // Track distance moved by the body
            trackDistance();
        }
        else
        {
            if (this.rendering != null)
            {
                if (this.rendering.getBrowsePosition() == null)
                {
                    // Start over with the current fittest genotype when evolution is running.
                    newSimulation(true);
                }
                else
                {
                    // ... or continue browser through (evolved) genotypes.
                    this.rendering.key('1');
                }
            }
        }
    }

    private void trackDistance()
    {
        Vector3 bodyPosition, travelled;

        bodyPosition = new Vector3(this.bodies[0].getPosition());

        // Follow centre of body when floor is drawn.
        if (this.rendering != null) this.rendering.setCameraTo(bodyPosition);

        if (DISTANCE == DISTANCE_FROM_START)
        {
            // Track distance from start point
            this.distance = bodyPosition.sub(this.startPos).norm();
        }
        else
        {
            // Track total distance travelled by body
            travelled = this.previousPos.sub(bodyPosition);
            this.distance += travelled.norm();
            this.previousPos = bodyPosition;
        }
    }

    private void updateBrain()
    {
        int i, opos;

        // Read sensor values
        readSensors();

        // Apply input to brain
        this.brain.setInput(this.sensorInput);
        this.brain.updateSynchronous();
        this.brain.getOutput(this.motorOutput);

        // Set motor velocity outputs to Joints
        opos = 0;
        for (i=0; i<this.joints.length; i++)
        {
            this.joints[i].getFirstAxisControler().setMotorForce(1.0, this.motorOutput[opos++]);
            this.joints[i].getSecondAxisControler().setMotorForce(1.0, this.motorOutput[opos++]);
        }
    }

    private double []readSensors()
    {
        int      pos;
        double []sensorValues;

        sensorValues = this.sensorInput;

        // Set touch sensors to 1 when body is colliding with floor.
        pos = 0;
        for (int i=0; i<this.bodies.length; i++)
        {
            if (this.floorContact[i])
                sensorValues[pos+i] = 1.0;
            else sensorValues[pos+i] = 0.0;
        }
        pos += this.bodies.length;

        // Set joint angle orientation and angular velocity as joint sensor values
        for(UniversalJoint joint: this.joints)
        {
            sensorValues[pos++] = joint.getFirstAxisControler().getPosition();
            sensorValues[pos++] = Math.abs(joint.getFirstAxisControler().getVelocity());
            sensorValues[pos++] = joint.getSecondAxisControler().getPosition();
            sensorValues[pos++] = Math.abs(joint.getSecondAxisControler().getVelocity());
        }

        double wave;

        // Sine wave input
        wave = Math.sin((SIMULATION_LENGTH/20)*this.simTime *Math.PI*2);
        sensorValues[pos] = wave;

        return(sensorValues);
    }

    public double getDistance()
    {
        return this.distance;
    }

    public double getTime()
    {
        return this.simTime;
    }

    public void newSimulation(boolean fittest)
    {
        try
        {
            NumberGenotype genotype;

            genotype = null;
            if (fittest)
            {
                // Show the fittest genotype
                genotype = (NumberGenotype)this.environment.getEvolution().getFittest();
            }

            if (genotype == null)
            {
                // Generate a random genotype
                genotype = (NumberGenotype) this.environment.makeRandomGenotype();
            }

            // Remove previous figure and build new one from the genotype.
            for(Body body: this.bodies) this.scene.removeBody(body);
            for (UniversalJoint joint : this.joints) this.scene.removeConstraint(joint);
            buildFigure(this.scene, genotype);

            // Reset time and distance tracking.
            initFitness();

            // Install the new genotype
            initBrain(genotype);

            showSimulation();
        }
        catch(LearnerException ex)
        {
            ex.printStackTrace();;
        }
    }

    public void newSimulation(NumberGenotype genotype)
    {
        try
        {
            // Remove previous figure and build new one from the genotype.
            for(Body body: this.bodies) this.scene.removeBody(body);
            for (UniversalJoint joint : this.joints) this.scene.removeConstraint(joint);
            buildFigure(this.scene, genotype);

            // Reset time and distance tracking.
            initFitness();

            // Install the new genotype
            initBrain(genotype);

            showSimulation();
        }
        catch(LearnerException ex)
        {
            ex.printStackTrace();;
        }
    }
}
