package railway.g1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import railway.sim.utils.*;
// To access data classes.


public class Player implements railway.sim.Player {
    // Random seed of 42.
    private int seed = 42;
    private Random rand = new Random(seed);
    private double START_BUDGET;
    
    private double[][] min_price; //minimum price to buy  link i,j
    private double[][] revenue; //revenue of link i,j
    private int[][] min_path; //record whether link i-j is a minimum path from i to j
    private Map<String, Integer> map; // this map is used to query index of town from townLookup
    
    private List<BidInfo> availableBids = new ArrayList<>();
    private List<BidInfo> lastState;
    
    // The coordinates of stations, infrastructure and raw transit files are stored for future reference.
    List<Coordinates> geo;
    List<List<Integer>> infra;
    int[][] transit;
    
    
    // Keep track of player owned links. It maps a link infra.get(i).get(j), denoted by an integer 
    // pair [i, j], to all player indexes who own that link (null if it is not sold yet). 
    Map<Pair, List<Integer>> playerOwnedLinks;
    
    private List<String> players = new ArrayList<String>();
    private List<Double> budgets = new ArrayList<Double>();
    
    public class Pair implements Serializable{
		private static final long serialVersionUID = 3520054221183875559L;
		
		int i1;
    	int i2;
    	Pair(int i1, int i2){
    		this.i1 = i1;
    		this.i2 = i2;
    	}

    	@Override
        public boolean equals(Object o) {
    		try {
    			return (i1 == ((Pair)o).i1 && i2 == ((Pair)o).i2);
    		}
    		catch (Exception e) {
    			return false;
    		}
        }
        
        @Override
        public int hashCode() {
        	return Integer.valueOf(i1 * geo.size() + i2).hashCode();
        }
        
        @Override
        public String toString() {
        	return (i1 + " " + i2);
        }
    }

    private double getDistance(int t1, int t2) {
        return Math.pow(
                Math.pow(geo.get(t1).x - geo.get(t2).x, 2) +
                        Math.pow(geo.get(t1).y - geo.get(t2).y, 2),
                0.5);
    }

    /**
     * This function will generate a revenue matrix for the problem
     * It will use dijkstra method to find the shortest path for i th node to j th node
     * And store the result in revenue[i][j], also this will give us the minimum price to buy links
     * @return the revenur matrix
     */
    private double[][] getRevenue() {
        int n = geo.size();
        // Create the graph.
        WeightedGraph g = new WeightedGraph(n);
        for (int i=0; i<infra.size(); ++i) {
            for (int j=0; j<infra.get(i).size(); ++j) {
                g.addEdge(i, infra.get(i).get(j), getDistance(i, infra.get(i).get(j)));
            }
        }
       // System.out.println("tag2");
        // Compute the revenue between any two nodes.
        revenue = new double[n][n];
        min_price = new double[n][n];
        min_path =  new int[n][n];
        for (int i=0; i<n; ++i) {
            int[][] prev = Dijkstra.dijkstra(g, i);
            for (int j=i+1; j<n; ++j) {
                List<List<Integer>> allPaths = Dijkstra.getPaths(g, prev, j);
                //System.out.println("tag3"+ j +i + n);
                double cost = 0;
                for (int k=0; k<allPaths.get(0).size()-1; ++k) {
                    cost += getDistance(allPaths.get(0).get(k), allPaths.get(0).get(k+1));
                }

                for (int k=0; k<allPaths.size(); ++k) {
                    //System.out.println("tag4"+" "+allPaths.get(k).get(0)+" "+allPaths.get(k).get(allPaths.get(0).size()-1));
                    min_path[allPaths.get(k).get(0)][allPaths.get(k).get(allPaths.get(0).size()-1)] = 1;            //record minimum path here
                    //System.out.println("tag6");
                    min_path[allPaths.get(k).get(allPaths.get(0).size()-1)][allPaths.get(k).get(0)] = 1;            //record minimum path here
                }
                //System.out.println("tag5");
                revenue[i][j] = cost * transit[i][j] * 10;
                min_price[i][j] = revenue[i][j]/allPaths.size();
            }
        }
        return revenue;
    }


    public Player() {
        rand = new Random();
    }

    public void init(String name, double budget, List<Coordinates> geo, 
    				List<List<Integer>> infra, int[][] transit, List<String> townLookup) {
    	this.geo = geo;
    	this.infra = infra;
    	this.transit = transit;
    	START_BUDGET = budget;
    	this.budgets.add(START_BUDGET);
        this.players.add(name);
        //System.out.println("Player name: " + name);
        this.revenue = getRevenue();
        //System.out.println("tag2");
        map = new HashMap<String, Integer>();
        //System.out.println("tag3");
        for(int i=0;i<townLookup.size();i++)
            map.put(townLookup.get(i),i);
        
        // Initialize playerOwnedLinks
        playerOwnedLinks = new HashMap<Pair, List<Integer>>();
        for (int i = 0; i < infra.size(); i++) {
        	for (Integer j : infra.get(i)) {
        		playerOwnedLinks.putIfAbsent(new Pair(i, (int)j), new LinkedList<Integer>());
        		List<Integer> entries = playerOwnedLinks.get(new Pair(i, (int)j));
        		entries.add(-1);
        	}
        }
        
        /*for (Pair p : playerOwnedLinks.keySet()) {
        	System.out.print(p.i1 + " " + p.i2 + ": ");
        	for (String s : playerOwnedLinks.get(p))
        		System.out.print((s==null));
        	System.out.println();
        }*/
        

    }
    
    /**
     * Update ownerships and remaining budgets for all players.
     */
    public void updateStatus(List<BidInfo> currentState) {
    	if (lastState != null){
    		for (int i = 0; i < currentState.size(); i++) {
    			if ((currentState.get(i).owner != null) && !currentState.get(i).owner.equals(lastState.get(i).owner)) {
    				List<Integer> ownerList = playerOwnedLinks.get(new Pair(map.get(currentState.get(i).town1),
    															map.get(currentState.get(i).town2)));
    				System.out.println(new Pair(map.get(currentState.get(i).town1),
    															map.get(currentState.get(i).town2)));
    				int index = players.indexOf(currentState.get(i).owner);
    				if (index == -1) {
    					index = players.size();
    					players.add(currentState.get(i).owner);
    					budgets.add(START_BUDGET);
    				}
    				ownerList.remove(Integer.valueOf(-1));
    				ownerList.add(index);
    	            budgets.set(index, budgets.get(index) - currentState.get(i).amount);
    			}
    		}
    	}
		lastState = currentState;
    }
    
    /**
     * Calculate the expected amount of traffic for each link in the infrastructure, for one specific player.
     * If an indirect route involves a link not owned by any player yet, the expectation assumes no 
     * switching penalty, but the revenue is divided by all possible routes proportional to distance 
     * of each route. Note that we will get fraction population number.
     * @param playerOwnedLinks2 a current or hypothetical map keeping track of ownership of each link
     * @param player the name of player
     * @return a map from each link denoted by an integer pair [i, j] to its corresponding traffic.
     * @see playerOwnedLinks
     */
    public Map<Pair, Double> getHeatMap(Map<Pair, List<Integer>> playerOwnedLinks2, String player) {
    	Map<Pair, Double> heatmap = new HashMap<Pair, Double>();
        for(int i=0;i<infra.size();i++) {
            for(int j=0;j<infra.get(i).size();j++) {
                heatmap.put(new Pair(i,infra.get(i).get(j)),0.0);
            }
        }
    	// init - nothing is owned, info only from transit and infra
        int n=geo.size();
        WeightedGraph g = new WeightedGraph(n);
        for (int i=0; i<infra.size(); ++i) {
            for (int j=0; j<infra.get(i).size(); ++j) {
                g.addEdge(i, infra.get(i).get(j), getDistance(i, infra.get(i).get(j)));
            }
        }

        double trafficcheck = 0;
        for (int i=0;i<transit.length;i++) {
            for (int j=0;j<transit[i].length;j++) { //int j=0;j<transit[i].length;j++
                if(transit[i][j]==0) {
                    continue;
                }
                double totaltransit = transit[i][j];
                trafficcheck += totaltransit;
                //System.out.println("loop:"+totaltransit);
                int[][] prev = Dijkstra.dijkstra(g, i);
                List<List<Integer>> allP = Dijkstra.getPaths(g,prev,j);

                for(int a=0;a<allP.size();a++) {
                    for(int b=0;b<allP.get(a).size();b++) {
                        //System.out.println("a: " + a + ", b: "+ b);
                        //System.out.println(allP.get(a).get(b));
                    }
                }

                double distance = 0; //just use first path
                for(int a=0;a<allP.get(0).size();a++) {
                    if(a>0) {
                        distance += getDistance(allP.get(0).get(a),allP.get(0).get(a-1));
                    }
                }
                //System.out.println("distance: "+distance);

                int pathnum = allP.size();
                double transitpp = totaltransit / pathnum; 
                //System.out.println("transitpp: "+transitpp);

                for(int a=0;a<allP.size();a++) {
                    for(int b=0;b<allP.get(a).size();b++) {
                        //divide up transitpp based on distance
                        if(b>0) {
                            double currdist = getDistance(allP.get(a).get(b),allP.get(a).get(b-1));
                            double expectedtraffic = transitpp * (currdist/distance);
                            //System.out.println("exp: "+expectedtraffic); //1000
                            int t1 = allP.get(a).get(b-1); //0
                            int t2 = allP.get(a).get(b); //1

                            //find heatmap key
                            Pair link = new Pair(0,0);
                            for(Pair p:heatmap.keySet()) {
                                if (p.i1==t1) {
                                    if(p.i2==t2) {
                                        link = p;
                                    }
                                }
                            }
                            if (link.i1==0 && link.i2 == 0) {
                                for(Pair p:heatmap.keySet()) {
                                    if (p.i1==t2) {
                                        if(p.i2==t1) {
                                            link = p;
                                        }
                                    }
                                }
                            }

                            //System.out.println(heatmap.containsKey(link));
                            //System.out.println("currtraffic: "+heatmap.get(link));
                            heatmap.put(link,heatmap.get(link)+expectedtraffic);
                            //System.out.println("aftertraffic: "+heatmap.get(link));
                        }
                    }
                }

                //print heatmap
                /*for (Pair p:heatmap.keySet()) {
                    System.out.println("t1: "+p.i1+", t2: "+p.i2+", traffic: "+heatmap.get(p));
                }*/


            }
            //break;
        }

        double total = 0.0;
        for (Pair p:heatmap.keySet()) {
            System.out.println("t1: "+p.i1+", t2: "+p.i2+", traffic: "+heatmap.get(p));
            total += heatmap.get(p);
        }
        System.out.println("total traffic: "+total);
        System.out.println("traffic check: "+trafficcheck);


    	return heatmap;
    }
    //This is to Query the info of according bid
    private BidInfo QBidInfo(Bid bid, List<BidInfo> allBids){
        for(BidInfo bi : allBids){
            if(bi.id == bid.id1){
                return bi;
            }
        }
        return null;
    }
    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
    	// TODO Find a way such that we bid on a link that gives us 0 benefit externally (?)
    	// while giving other links that we owned higher traffics. I am not sure how to do this right now.
    	
    	// Update status & heat map
    	updateStatus(allBids);
        getHeatMap(playerOwnedLinks,"g1");
        
    	// Random player code below
    	
    	// The random player bids only once in a round.
        // This checks whether we are in the same round.
        // Random player doesn't care about bids made by other players.
    	
    	
        double amount = -1;
        int id = -1;
        double maxamount = amount;
        int maxid = id;
        if (availableBids.size() != 0) {
            return null;
        }
        List<BidInfo> ourlinks = new ArrayList<>();
        for(BidInfo bi : allBids){
            if (bi.owner == "g1"){
                ourlinks.add(bi);
            }
        }
        for (BidInfo bi : allBids) {
            if (bi.owner == null) {
                if(ourlinks.size()==0){
                    if(min_path[map.get(bi.town1)][map.get(bi.town2)]!=1){
                        continue;
                    }
                }      //we will start with a shortest link
                availableBids.add(bi);
                amount = revenue[map.get(bi.town1)][map.get(bi.town2)];
                for(BidInfo bi1 : ourlinks){
                    if(bi1.town1.equals(bi.town1)){
                        if(min_path[map.get(bi1.town2)][map.get(bi.town2)]==1){
                            amount = amount + min_price[map.get(bi1.town2)][map.get(bi.town2)];
                            continue;
                        }
                    }
                    if(bi1.town2.equals(bi.town1)){
                        if(min_path[map.get(bi1.town1)][map.get(bi.town2)]==1){
                            amount = amount + min_price[map.get(bi1.town1)][map.get(bi.town2)];
                            continue;
                        }

                    }
                    if(bi1.town1.equals(bi.town2)){
                        if(min_path[map.get(bi1.town2)][map.get(bi.town1)]==1){
                            amount = amount + min_price[map.get(bi1.town2)][map.get(bi.town1)];
                            continue;
                        }

                    }
                    if(bi1.town2.equals(bi.town2)){
                        if(min_path[map.get(bi1.town1)][map.get(bi.town1)]==1){
                            amount = amount + min_price[map.get(bi1.town1)][map.get(bi.town1)];
                            continue;
                        }

                    }
                }
                if(amount > maxamount){
                    maxamount = amount;
                    maxid = bi.id;
                }

            }


        }
        System.out.println(maxamount+" "+maxid);
        if (availableBids.size() == 0) {
            return null;
        }



//        BidInfo randomBid = availableBids.get(rand.nextInt(availableBids.size()));

        // Don't bid if the random bid turns out to be beyond our budget.
//        if (budget - amount < 0.) {
//            return null;
//        }

        // Check if another player has made a bid for this link.
//
//        for (BidInfo bi : currentBids) {
//
//        }

        Bid bid = new Bid();
        bid.amount = maxamount;
        bid.id1 = maxid;

        return bid;
    }

    public void updateBudget(Bid bid) {
    	// TODO
        if (bid != null) {
            //budget.set(0, budget.get(0) - bid.amount);
        }

        availableBids = new ArrayList<>();
    }
}
