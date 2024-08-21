package weka.classifiers.meta.spatial;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

/**
 * Torroidal Grid containing ClassifierNodes.
 * @author johan
 */
public class WraparoundGrid
{
    // Neighborhood definition: arrays of relative positions of neighbors
    public static final int [][]NEIGHBORS_L5 = new int[][]
            {
        new int[]{-1,  0},
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 1,  0}
            };
    public static final int [][]NEIGHBORS_L9 = new int[][]
            {
        new int[]{-2,  0},
        new int[]{-1,  0},
        new int[]{ 0, -2},
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 0,  2},
        new int[]{ 1,  0},
        new int[]{ 2,  0}
            };
    public static final int [][]NEIGHBORS_C9 = new int[][]
            {
        new int[]{-1, -1},
        new int[]{ 0, -1},
        new int[]{ 1, -1},
        new int[]{-1,  0},
        new int[]{ 1,  0},
        new int[]{-1,  1},
        new int[]{ 0,  1},
        new int[]{ 1,  1}
            };
    public static final int [][]NEIGHBORS_C13 = new int[][]
            {
        new int[]{-2,  0},
        new int[]{-1,  0},
        new int[]{ 0, -2},
        new int[]{ 0, -1},
        new int[]{ 0,  1},
        new int[]{ 0,  2},
        new int[]{ 1,  0},
        new int[]{ 2,  0},
        new int[]{-1, -1},
        new int[]{-1,  1},
        new int[]{ 1,  1},
        new int[]{ 1, -1}
            };

    public static int [][]NEIGHBORS = NEIGHBORS_C9; // Default: square 3x3 neighborhood

    private int    w, h;

    private ClassifierNode[][] nodes;
    // ----------
    private Map<ClassifierNode, int []> nodePos;

    public WraparoundGrid(int w, int h, Classifier[] classifiers, int[][] neigh) throws Exception
    {
        this.w     = w;
        this.h     = h;
        this.nodes = new ClassifierNode[w][h];
        this.nodePos = new HashMap<ClassifierNode, int[]>();
        this.NEIGHBORS = neigh;
        for(int i=0; i<this.nodes.length; i++)
            for (int j=0; j<this.nodes[i].length; j++)
            {
                this.nodes[i][j] = new ClassifierNode();
                this.nodes[i][j].setClassifier(classifiers[i]);
                this.nodePos.put(this.nodes[i][j], new int[]{i,j});
            }
    }

    public List<ClassifierNode> getNodes()
    {
        List<ClassifierNode> nodes = new LinkedList<ClassifierNode>();
        for(int i=0; i<this.nodes.length; i++)
            for (int j=0; j<this.nodes[i].length; j++) nodes.add(this.nodes[i][j]);
        return(nodes);
    }

    public List<ClassifierNode> getNeighbors(ClassifierNode node)
    {
        // Find the neighbors in grid. Wraparound in 2 directions.
        int []pos;
        List<ClassifierNode> nei = new LinkedList<ClassifierNode>();

        pos = this.nodePos.get(node);
        for(int []neipos: NEIGHBORS)
            nei.add(getNodeAt(pos[0]+neipos[0], pos[1]+neipos[1]));

        return(nei);
    }

    private ClassifierNode getNodeAt(int x, int y)
    {        
        if      (x < 0)       x = this.w + x;
        else if (x >= this.w) x = x - this.w;

        if      (y < 0)       y = this.h + y;
        else if (y >= this.h) y = y - this.h;

        return(this.nodes[x][y]);
    }
}
