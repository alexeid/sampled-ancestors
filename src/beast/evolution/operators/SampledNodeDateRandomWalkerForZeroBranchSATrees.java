package beast.evolution.operators;

import beast.core.Description;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.ZeroBranchSANode;
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Description("Randomly select a sampled node and shifts the date of the node within a given window")
public class SampledNodeDateRandomWalkerForZeroBranchSATrees extends TipDatesRandomWalker {


    boolean useNodeNumbers;


    @Override
    public void initAndValidate() throws Exception {
        windowSize = windowSizeInput.get();
        useGaussian = useGaussianInput.get();

        // determine taxon set to choose from
        if (m_taxonsetInput.get() != null) {
            useNodeNumbers = false;
            List<String> sTaxaNames = new ArrayList<String>();
            for (String sTaxon : treeInput.get().getTaxaNames()) {
                sTaxaNames.add(sTaxon);
            }

            List<String> set = m_taxonsetInput.get().asStringList();
            int nNrOfTaxa = set.size();
            taxonIndices = new int[nNrOfTaxa];
            int k = 0;
            for (String sTaxon : set) {
                int iTaxon = sTaxaNames.indexOf(sTaxon);
                if (iTaxon < 0) {
                    throw new Exception("Cannot find taxon " + sTaxon + " in tree");
                }
                taxonIndices[k++] = iTaxon;
            }
        } else {
            useNodeNumbers = true;
        }
    }

    @Override
    public double proposal() {
        // randomly select a leaf node
        Tree tree = treeInput.get();

        Node node;
        if (useNodeNumbers) {
            int leafNodeCount = tree.getLeafNodeCount();
            int i = Randomizer.nextInt(leafNodeCount);
            node = tree.getNode(i);
//            do {
//                int i = Randomizer.nextInt(leafNodeCount);
//                node = tree.getNode(i);
//            }  while (!node.isLeaf());
        }  else {
            int i = Randomizer.nextInt(taxonIndices.length);
            node = tree.getNode(taxonIndices[i]);
        }

        double value = node.getHeight();

        if (value == 0.0) {
            return Double.NEGATIVE_INFINITY;
        }
        double newValue = value;
        if (useGaussian) {
            newValue += Randomizer.nextGaussian() * windowSize;
        } else {
            newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        Node fake = null;
        double lower, upper;

        if (((ZeroBranchSANode)node).isDirectAncestor()) {
            fake = node.getParent();
            lower = getOtherChild(fake, node).getHeight();
            if (fake.getParent() != null) {
                upper = fake.getParent().getHeight();
            } else upper = Double.POSITIVE_INFINITY;
        } else {
            //lower = Double.NEGATIVE_INFINITY;
            lower = 0.0;
            upper = node.getParent().getHeight();
        }

        if (newValue < lower || newValue > upper) {
            return Double.NEGATIVE_INFINITY;
        }

        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }

        if (fake != null) {
            fake.setHeight(newValue);
        }
        node.setHeight(newValue);

        if (newValue < 0) {
            for (int i=0; i<tree.getNodeCount(); i++){
                double oldHeight = tree.getNode(i).getHeight();
                tree.getNode(i).setHeight(oldHeight-newValue);
            }
        }  else {
            boolean dateShiftDown = true;
            for (int i=0; i< tree.getLeafNodeCount(); i++){
                if (tree.getNode(i).getHeight() == 0){
                    dateShiftDown = false;
                    break;
                }
            }
            if (dateShiftDown) {
                ArrayList<Double> tipNodeHeights= new ArrayList<Double>();
                for (int i=0; i<tree.getLeafNodeCount(); i++){
                    tipNodeHeights.add(tree.getNode(i).getHeight());
                }
                Collections.sort(tipNodeHeights);
                double shiftDown = tipNodeHeights.get(0);
                for (int i=0; i<tree.getNodeCount(); i++){
                    double oldHeight = tree.getNode(i).getHeight();
                    tree.getNode(i).setHeight(oldHeight-shiftDown);
                }
            }
        }

        boolean check = true;
        for (int i=0; i<tree.getNodeCount(); i++){
            if (tree.getNode(i).getHeight() < 0) {
                System.out.println("Negative height found");
                System.exit(0);
            }
            if (tree.getNode(i).getHeight() == 0) {
                check = false;
            }
        }
        if (check) {
            System.out.println("There is no 0 height node");
            System.exit(0);
        }






        //tree.setEverythingDirty(true);

        return 0.0;
    }
}

// TODO 1. Should the height of a leaf be greater than zero?
// TODO 2. Should we use reflect if a chosen value is out of the bounds?
// TODO 3. Look at the optimise and getPerformanceSuggestion, can we leave them as they are?
