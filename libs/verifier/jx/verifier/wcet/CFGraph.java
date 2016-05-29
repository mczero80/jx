package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import java.util.Vector;
import java.util.Enumeration;

//Control Flow Graph
public class CFGraph {
    private CFGNodeList nodes;
    private BasicBlockList bBlocks = new BasicBlockList();
    public CFGNodeList getNodes() {return nodes;}
    public CFGraph(BCLinkList code) {
	nodes = new CFGNodeList();
	buildGraph(code);
    }


    //build the control flow graph for the method
    //at first builds a graph with no restrictions, when the graph is complete, all nodes with
    //more than two edges are split, so that every node has at most outdgree 2
    public void buildGraph(BCLinkList code) {
	//start with address 0 as first node. 
	nodes.addNode(code.getFirst());
	
	//create edges for all new nodes
	while(nodes.newNodes.size() > 0) {
	    CFGNode act = (CFGNode)nodes.newNodes.firstElement();
	    nodes.newNodes.removeElementAt(0);

	    // check if the node is an if-node or a normal node.
	    if (act.getBCode().getTargets().length == 2) {
		//if node is an if-node, create the two edges, with target[0] and [1] as beginning
		//the target nodes are automatically put into nodes.newNodes!
		CFGBBEdge.createEdge(act, act.getBCode().getTargets()[0], nodes, CFGNode.IF, bBlocks);
		CFGBBEdge.createEdge(act, act.getBCode().getTargets()[1], nodes, CFGNode.IF, bBlocks);
		
	    } else if (act.getBCode().getTargets().length <= 1) {
		//if not an if-node, create single edge, with node bc as 
		//first bytecode.
		//the target nodes are automatically put into nodes.newNodes!
		CFGBBEdge.createEdge(act, act.getBCode(), nodes, CFGNode.NORMAL, bBlocks);
	    } else {
		throw new Error("Internal Error; bc with "+ 
				act.getBCode().getTargets().length + " targets:"+
				"\n\t" + act.getBCode().toString());
	    }
	    //check if BB is protected by exception handler; if so, add edge from BB to eh
	    if (act.getBCode().eHandlers!= null) {
		ExceptionHandler[] eHandlers = act.getBCode().eHandlers;
		for (int i = 0; i < eHandlers.length; i++) {
		    CFGBBEdge tmpEdge = CFGBBEdge.
			createEdge(act, eHandlers[i].getHandler(), nodes, CFGNode.EXCEPTION, bBlocks);
		    tmpEdge.eHandler = -eHandlers[i].getHandler().getAddress();
		}
	    }
	}
	checkOutDegrees();
    }

    //split all nodes with outdegree > 2 
    //should not be called until all BBs are created. Else, problems arise for bytecodes
    //with more than one node, as newly created edges pointing to that bytecode do not
    //necessarily have to point to the first node!
    private void checkOutDegrees() {
	CFGNode actNode = null;
    NODESFOR:
	for (Enumeration e = nodes.getElements(); e.hasMoreElements();) {
	    actNode = (CFGNode) e.nextElement();
	    if (actNode.oEdges.size() <= 1) {
		actNode.type = CFGNode.NORMAL;
		continue NODESFOR;
	    }
	    if (actNode.oEdges.size() == 2) {
		if (((CFGBBEdge)actNode.oEdges.elementAt(0)).type == CFGNode.IF) {
		    //If one of two edges is an if-edge, the other one must be too, node as well
		    actNode.type = CFGNode.IF;
		    //FEHLER taugt nicht für switch
		    actNode.openIf[0] = new OpenIfListElement(actNode.getAddress(), 0);
		    actNode.openIf[1] = new OpenIfListElement(actNode.getAddress(), 1);
		} else {
		    //two edges and no if edge --> exception
		    actNode.type = CFGNode.EXCEPTION;
		    //FEHLER exception ja, aber welche!!!
		    ((WCETState)actNode.getBCode().beforeState).simulateException = actNode.getBCode().eHandlers[0];
		    int tmp = -actNode.getBCode().eHandlers[0].getHandler().getAddress();
		    if (((CFGBBEdge)actNode.oEdges.elementAt(0)).type == CFGNode.EXCEPTION) {
			actNode.openIf[0] = new OpenException(tmp, 1);
			actNode.openIf[1] = new OpenException(tmp, 0, 
							      ((OpenException)actNode.openIf[0]).getId());
		    } else {
			actNode.openIf[0] = new OpenException(tmp, 0);
			actNode.openIf[1] = new OpenException(tmp, 1, 
							      ((OpenException)actNode.openIf[0]).getId());
		    }
		}
		continue NODESFOR;
	    }
	    while (actNode.oEdges.size() > 2) {
		//FEHLER debug
		if (true) throw new Error("actNode.oEdges.size() > 2");
		//if node has outdegree > 2, split nodes; 
		//start with creating nodes for exceptions, then for ifs, so the last node keeps
		//the "NORMAL" edge
		CFGNode newNode = new CFGNode(actNode.getBCode());
		//adds node, even if node with same bytecode already exists
		nodes.addNode(newNode);
		//all edges that point to actNode must be changed to point to newNode
		for (Enumeration inEnum = actNode.iEdges.elements();
		     inEnum.hasMoreElements();) {
		    CFGEdge actEdge = (CFGEdge) inEnum.nextElement();
		    actEdge.setTarget(newNode);
		}
		newNode.iEdges = actNode.iEdges;
		//insert edge from newNode to actNode
		actNode.iEdges = new Vector(1);
		CFGEdge newEdge = new CFGBBEdge(newNode, actNode, new BasicBlock(), 
						CFGNode.NORMAL);
		actNode.iEdges.addElement(newEdge);
		newNode.oEdges.addElement(newEdge);
		//change edge from actNode.oEdges to start at newNode
		CFGBBEdge changeEdge = null;
		for (int i = 0; i < newNode.oEdges.size(); i++) {
		    CFGBBEdge tmp = (CFGBBEdge)actNode.oEdges.elementAt(i);
		    if (tmp.type == CFGNode.EXCEPTION)
			changeEdge = tmp;
		}
		if (changeEdge == null) {
		    for (int i = 0; i < newNode.oEdges.size(); i++) {
			CFGBBEdge tmp = (CFGBBEdge)actNode.oEdges.elementAt(i);
			if (tmp.type == CFGNode.IF)
			    changeEdge = tmp;
		    }
		}
		if (changeEdge == null) {
		    changeEdge = (CFGBBEdge)actNode.oEdges.elementAt(2);
		}
		newNode.oEdges = new Vector(1);
		newNode.oEdges.addElement(changeEdge);
		actNode.oEdges.removeElement(changeEdge);
		changeEdge.setSource(newNode);
		newNode.type = changeEdge.type;
	    }
	    //set type of actNode, depending on last two edges
	    if (((CFGBBEdge)actNode.oEdges.elementAt(0)).type == CFGNode.IF ||
		((CFGBBEdge)actNode.oEdges.elementAt(1)).type == CFGNode.IF )
		actNode.type = CFGNode.IF;
	    else if (((CFGBBEdge)actNode.oEdges.elementAt(0)).type == CFGNode.EXCEPTION ||
		     ((CFGBBEdge)actNode.oEdges.elementAt(1)).type == CFGNode.EXCEPTION )
		actNode.type = CFGNode.EXCEPTION;
	    else
		actNode.type = CFGNode.NORMAL;
	}

    }

    /**Simplify the CFG. After simplification only one node should be left.
     * @return true if graph could be reduced to one node, else false.
     */
    public boolean simplify() {
	//FEHLER debug
	System.out.print("Simplifying...");


	//apply rule 1, as long as there are any changes
	//then apply rule 2 as long as possible, 
	//if rule 2 cannot be applied anymore apply rule3 and try with rule 2 again
	//do until no rules can be applied anymore
    SR1:
	while(true) {
	    for(Enumeration nodesEnum = nodes.getElements(); 
		nodesEnum.hasMoreElements(); ) {
		if(simplificationRule2((CFGNode) nodesEnum.nextElement())) 
		    continue SR1;
	    }
	    //if this point is reached, Simplification rule2 could not be applied to any node --> continue with rule1
	    for(Enumeration nodesEnum = nodes.getElements(); 
		nodesEnum.hasMoreElements(); ) {
		if(simplificationRule1((CFGNode) nodesEnum.nextElement())) 
		    continue SR1;
	    }
	    
	    //if this point is reached, Simplification rule1 could not be applied to any node --> continue with rule3
	    for(Enumeration nodesEnum = nodes.getElements(); 
		nodesEnum.hasMoreElements(); ) {
		if(simplificationRule3((CFGNode) nodesEnum.nextElement())) 
		    continue SR1;
	    }
	    break SR1;
	}
	//FEHLER debug
	System.out.println("done with simplifying");
	//for (Enumeration e = nodes.getElements(); e.hasMoreElements();) {
	//    System.out.println(e.nextElement().toString());
	//}

	//tree should be reduced to one node!
	int tmpint = 0;
	for (Enumeration e = nodes.getElements(); e.hasMoreElements();) {
	    tmpint++;
	    e.nextElement();
	}
	if(tmpint != 1) {
	    //FEHLER debug
	    System.out.println("********************************************************\n"
			       + "More than one node left after CFG simplification!\n"+
			       "********************************************************");
 	    return false;
	}
	return true;
    }

    public SimState simulate(SimState initialState)   {
	return ((CFGEdge)nodes.getFirst().oEdges.elementAt(0)).simulate(initialState);

    }

    //Simplification rule one: If a point has only one outgoing edge, it can
    //be merged into all incoming edges
    private boolean simplificationRule1(CFGNode node) {
	if(node.oEdges.size() == 1 && node.getAddress() != 0) {
	    //FEHLER debug
	    //System.out.println("SR1 :" + node);

	    for (int i = 0; i < node.iEdges.size(); i++) {
		
		CFGEdge in = (CFGEdge)node.iEdges.elementAt(i);
		CFGEdge out = (CFGEdge)node.oEdges.elementAt(0);
		CFGEdge newEdge = new CFGR1Edge(in, out);
		in.getSource().replaceOEdge(in, newEdge);
		out.getTarget().replaceIEdge(out, newEdge);
		nodes.removeNode(in.getTarget());
		
	    }
	    return true;
	}
	return false;
    }

    //rules 2a & 2b:
    //2a: both edges of a node point to the same target --> unify edges
    //2b: one edge points back to the node itself --> eliminate edge and mark node
    private boolean simplificationRule2(CFGNode node){
	if (node.oEdges.size() == 2 && node.getAddress() != 0) {
	    CFGEdge out0 = (CFGEdge)node.oEdges.elementAt(0);
	    CFGEdge out1 = (CFGEdge)node.oEdges.elementAt(1);
	    
	    if (out0.getTarget() == out1.getTarget()) {
	//FEHLER debug
		//System.out.println("SR2a :" + node);
		//2a
		//if both edges point to the same target
		//mark the edges:
		//if 
		//FEHLER stimmt das...
		//it does not matter if "true" and "false" case are correct, it's only 
		//important that not both edges get the same case.
		out0.setIfCase(node.openIf[0]);
		out1.setIfCase(node.openIf[1]);

		//unify edges
		CFGEdge newEdge = new CFGR2aEdge(out0, out1);
		//replac oEdge in source
		node.oEdges.removeAllElements(); //all Elements can be removed as there are only two Elements: out0 and out1
		node.oEdges.addElement(newEdge);
		//replace iEdge in Target
		newEdge.getTarget().replaceIEdges(out0, out1, newEdge);
		//call simplification rule 1 to eliminate node
		simplificationRule1(node);
		return true;
	    } else if (out0.getTarget() == node || out1.getTarget() == node) {
		//FEHLER debug
		//System.out.println("SR2b :" + node);
		//2b
		//edge  points back to the node itself -->remove it and mark if!

		//find loop edge
		CFGEdge loop=null, exit=null;
		if (out0.getTarget() == out0.getSource()) {
		    loop = out0;
		    exit = out1;
		} else {
		    loop = out1;
		    exit = out0;
		}
		//mark node and remove edge from node
		node.markIf();
		node.oEdges.removeElement(loop);
		node.iEdges.removeElement(loop);

		//create new composite edge
		CFGEdge newEdge = new CFGR2bEdge(loop, exit);
		node.replaceOEdge(exit, newEdge);
		newEdge.getTarget().replaceIEdge(exit, newEdge);
		simplificationRule1(node);
		return true;
	    } 
	}
	return false;
    }

    //if one target of an if-node is an if, which has an edge to the other target of this
    //if-node, then unify the two ifs.
    public boolean simplificationRule3(CFGNode node) {
	if (node.oEdges.size() == 2 && node.getAddress() != 0) {
	    CFGEdge out0 = (CFGEdge)node.oEdges.elementAt(0);
	    CFGEdge out1 = (CFGEdge)node.oEdges.elementAt(1);
	    //if the target of out0 is an if, check if that if has a branch to the
	    //same node as out1. If true, unify the two ifs.
	    //FEHLER debug
	    //System.out.println("SR3 :" + node);

	    //check, if rule3 is applicable and to which branches
	    //  1----->2->4
	    //  \->3<-/
	    CFGEdge toIf2 = null, //1->2
		branch1 = null,  //1->3
		branch2 = null,  //2->3
		exit = null; //2->4
	    if (out0.getTarget().oEdges.size() == 2) {
		toIf2 = out0;
		branch1 = out1;
		if (((CFGEdge)toIf2.getTarget().oEdges.elementAt(0)).getTarget() ==
		    branch1.getTarget()) {
		    branch2 = (CFGEdge)toIf2.getTarget().oEdges.elementAt(0);
		    exit = (CFGEdge)toIf2.getTarget().oEdges.elementAt(1);
		} else if (((CFGEdge)toIf2.getTarget().oEdges.elementAt(1)).getTarget() ==
			   branch1.getTarget()) {
		    branch2 = (CFGEdge)toIf2.getTarget().oEdges.elementAt(1);
		    exit = (CFGEdge)toIf2.getTarget().oEdges.elementAt(0);
		}
	    } 
	    if (branch2 == null && out1.getTarget().oEdges.size() == 2) {
		toIf2 = out1;
		branch1 = out0;
		if (((CFGEdge)toIf2.getTarget().oEdges.elementAt(0)).getTarget() ==
		    branch1.getTarget()) {
		    branch2 = (CFGEdge)toIf2.getTarget().oEdges.elementAt(0);
		    exit = (CFGEdge)toIf2.getTarget().oEdges.elementAt(1);
		} else if (((CFGEdge)toIf2.getTarget().oEdges.elementAt(1)).getTarget() ==
			   branch1.getTarget()) {
		    branch2 = (CFGEdge)toIf2.getTarget().oEdges.elementAt(1);
		    exit = (CFGEdge)toIf2.getTarget().oEdges.elementAt(0);
		}
	    }
	    if (exit == null) 
		return false;
	    if (toIf2.getTarget().iEdges.size() != 1)
		return false;
	    CFGNode if1 = toIf2.getSource();
	    //FEHLER debug
	    //System.out.println("bi1: " + branch1 +
	    //	       "\nti2 " + toIf2 +
	    //	       "\nbi2 " +branch2 +
	    //	       "\nexit " +exit);
	    //add new node
	    CFGR3Node newNode = new CFGR3Node(if1.getBCode(),
					      branch1,
					      toIf2,
					      branch2,
					      exit);
	    nodes.addNode(newNode);
	    //change edge from all predecessors of if1  to newNode
	    for (Enumeration e = if1.iEdges.elements(); e.hasMoreElements();) {
		CFGEdge tmp =(CFGEdge)e.nextElement();
		tmp.setTarget(newNode);
		newNode.registerIEdge(tmp);
	    }
	    //create new edge, from newNode to target of both ifs; edge is just a pointer, so it should be empty
	    CFGEdge newEdge = new CFGBBEdge(newNode, branch1.getTarget(), null, CFGNode.NORMAL);
	    //newNode.registerOEdge(newEdge); //-done by CFGBBEdge
	    branch1.getTarget().replaceIEdge(branch1, newEdge);
	    branch2.getTarget().iEdges.removeElement(branch2);

	    //create new edge from newNode to other target of if2
	    newEdge = new CFGBBEdge(newNode, exit.getTarget(), null, CFGNode.NORMAL);
	    //newNode.registerOEdge(newEdge); //-done by CFGBBEdge
	    exit.getTarget().replaceIEdge(exit, newEdge); 

	    //remove old nodes and mark
	    nodes.removeNode(branch1.getSource());
	    nodes.removeNode(branch2.getSource());
	    branch1.getSource().markIf();
	    branch2.getSource().markIf();

	    //FEHLER debug
	    //System.out.println("newNode: " + newNode);
	    //System.out.println("\nGraph:\n"+this.toString());
	    return true;
	    
	}
	return false;
	
    }

    public String toString() {
	return nodes.toString();
    }
}
