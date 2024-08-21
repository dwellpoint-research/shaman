/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.examples;


import jinngine.collision.SAP2;
import jinngine.geometry.Box;
import jinngine.geometry.UniformCapsule;
import jinngine.math.Vector3;
import jinngine.physics.*;
import jinngine.physics.force.GravityForce;
import jinngine.physics.solver.NonsmoothNonlinearConjugateGradient;
import jinngine.rendering.Interaction;
import jinngine.rendering.Rendering;

public class BasicExample implements Rendering.Callback {	
	private final Scene scene;
	
	public BasicExample() {
		
		// start jinngine 
		scene = new DefaultScene(new SAP2(), new NonsmoothNonlinearConjugateGradient(44), new DisabledDeactivationPolicy());
		scene.setTimestep(0.1);
		
		// add boxes to bound the world
		Body floor = new Body("floor", new Box(1500,20,1500));
		floor.setPosition(new Vector3(0,-30,0));
		floor.setFixed(true);
		
		Body back = new Body( "back", new Box(200,200,20));		
		back.setPosition(new Vector3(0,0,-55));
		back.setFixed(true);

		Body front = new Body( "front", new Box(200,200,20));		
		front.setPosition(new Vector3(0,0,-7));
		front.setFixed(true);

		Body left = new Body( "left", new Box(20,200,200));		
		left.setPosition(new Vector3(-35,0,0));
		left.setFixed(true);

		Body right = new Body( "right", new Box(20,200,200));		
		right.setPosition(new Vector3(10,0,0));
		right.setFixed(true);		
		
		Box boxgeometry = new Box(6,6,6);
		Body box = new Body( "box", boxgeometry );
		box.setPosition(new Vector3(-10,-11,-25));
				
		// add all to scene
		scene.addBody(floor);
		scene.addBody(back);
		scene.addBody(front);		
		scene.addBody(left);
		scene.addBody(right);
		scene.addBody(box);

		// put gravity on box
		scene.addForce( new GravityForce(box));		
		
		// handle drawing
		Rendering rendering = new jinngine.rendering.jogl.JoglRendering(this, new Interaction(scene));
		rendering.drawMe(boxgeometry);

		rendering.start();
	}

	@Override
	public void tick() {
		// each frame, to a time step on the Scene
		scene.tick();
	}
	
	public static void main( String[] args) {
		new BasicExample();
	}
}
