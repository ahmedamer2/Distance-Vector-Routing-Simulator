/*
 * Complete this class.
 * Student Name: Ahmed Amer
 * Student ID No.: 30063097
 */
import javax.swing.*;
import java.util.HashMap;

public class RouterNode {
    private static final boolean POISONREVERSE = true;
    private int myID;
    private GuiTextArea myGUI;
    private RouterSimulator sim;
    private int[] costs = new int[RouterSimulator.NUM_NODES];
    private int[] hop = new int[RouterSimulator.NUM_NODES];
    private HashMap<Integer, int[]> distance = new HashMap<>();

    //--------------------------------------------------
    public RouterNode(int ID, RouterSimulator sim, int[] costs) {
        myID = ID;
        this.sim = sim;
        myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");
        System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);

        int [][] temp = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];
        for(int i = 0 ; i < RouterSimulator.NUM_NODES; i++){
            for(int j = 0; j < RouterSimulator.NUM_NODES; j++ ){
                temp[i][j] = (i==j) ? 0 : RouterSimulator.INFINITY;
            }
            distance.put(i, temp[i]);
        }

        distance.put(myID, costs);

        for(int i = 0 ; i < RouterSimulator.NUM_NODES; i++){
            hop[i] = (costs[i] != RouterSimulator.INFINITY) ? i : -1; //if costs[i] is infinity then set the value to -1
        }
        printDistanceTable();

        updateAll();

    }

    private void updateAll() {
        for(int i = 0 ; i < RouterSimulator.NUM_NODES; i++){
            if( i != myID && costs[i] != RouterSimulator.INFINITY ){
                RouterPacket pkt = new RouterPacket(myID, i, distance.get(myID));

                if(POISONREVERSE){
                    int[] temp = new int[RouterSimulator.NUM_NODES];

                    for(int j = 0; j < RouterSimulator.NUM_NODES; j++){
                        temp[j] = (hop[j] == i) ? RouterSimulator.INFINITY : distance.get(myID)[j];
                    }
                    pkt = new RouterPacket(myID, i, temp);
                }

                sendUpdate(pkt);
            }
        }
    }

    //--------------------------------------------------
    public void recvUpdate(RouterPacket pkt) {
        boolean changesReceived = false;

        //update any changes made to the distance tables
        for(int i = 0; i < RouterSimulator.NUM_NODES; i++ ){
            if(distance.get(pkt.sourceid)[i] != pkt.mincost[i]){
                distance.get(pkt.sourceid)[i] = pkt.mincost[i];
                changesReceived = true;
            }
        }

        //if a change to one of the neighbouring distance tables was made
        //then recalculate the distances again to see if they are shorter
        if(changesReceived){
            boolean changesMade = false;

            for(int i = 0; i < RouterSimulator.NUM_NODES; i++){
                //perform the belman-ford equation and find out if the distance is shorter
                //if this path is picked v.s if it is shorter when the direct path is picked
                if(i != myID){
                    int old = distance.get(myID)[i];
                    int current = distance.get(hop[i])[i] + distance.get(myID)[hop[i]];

                    if(old != current){
                        distance.get(myID)[i] = current;
                        changesMade = true;
                    }
                    //if directly routing to the destination is faster then make that the length
                    if(costs[i] < distance.get(myID)[i]){
                        distance.get(myID)[i] = costs[i];
                        hop[i] = i;
                        changesMade = true;
                    }
                    //get the new distance vectors
                    for(int j = 0; j < RouterSimulator.NUM_NODES; j++) {
                        int alternateCost = distance.get(myID)[i] + distance.get(i)[j];
                        if ( alternateCost < distance.get(myID)[j]) {
                            distance.get(myID)[j] = alternateCost;
                            hop[j] = hop[i];
                            changesMade = true;
                        }
                    }
                }
                //if changes where made then update all the neighbours
                if(changesMade)
                    updateAll();
            }
        }
    }


    //--------------------------------------------------
    private void sendUpdate(RouterPacket pkt) {
        sim.toLayer2(pkt);

    }


    //--------------------------------------------------
    public void printDistanceTable() {
        myGUI.println("Current table for " + myID +
                "  at time " + sim.getClocktime());
        myGUI.println("DistanceTable");
        String header = F.format("dist |", 11);
        for(int i = 0 ; i < RouterSimulator.NUM_NODES; i++){
            header+= F.format(Integer.toString(i), 10);
        }
        header+="\n";
        for(int i = 0; i < RouterSimulator.NUM_NODES+1; i++){
            header+="----------";
        }
        myGUI.println(header);

        String cost = F.format("cost |", 10);
        for(int i = 0; i<RouterSimulator.NUM_NODES; i++) {
            cost += F.format(Integer.toString(distance.get(myID)[i]), 10);
        }
        myGUI.println(cost);

        String route = F.format("route |", 9);
        for(int i = 0; i< RouterSimulator.NUM_NODES; i++){
            if(hop[i] != -1)
                route+= F.format(Integer.toString(hop[i]), 10);
            else
                route+= F.format("----", 13);
        }

        myGUI.println(route);
    }

    //--------------------------------------------------
    public void updateLinkCost(int dest, int newcost) {
        //for debugging purpose to make sure the new link cost is incorprated correctly
        if(false)
            System.out.println("recived a new cost for node "+myID+" link changed: " + dest+": newCost = " + newcost );


        //update with the new cost
        costs[dest] = newcost;

        //if we are currently routing through the changed link then set the
        //distance to the new cost.
        if(hop[dest] == dest){
            distance.get(myID)[dest] = newcost;
        }

        //if routing directly is less than routing through what we currently have
        //then update the distance table with the new cost
        if(distance.get(myID)[dest] > costs[dest]){
            distance.get(myID)[dest] = costs[dest];
            hop[dest] = dest;
        }

        //check through all of the routes to see if the current routing we have is bigger
        //than the routing we previously had
        for(int i = 0; i < RouterSimulator.NUM_NODES; i++){
            int updatedCost = distance.get(myID)[dest] + distance.get(dest)[i];

            if(updatedCost < distance.get(myID)[dest]){
                distance.get(myID)[dest] = updatedCost;
                hop[i] = hop[dest];
            }
        }
        //notify all neighbours
        updateAll();
    }

}
