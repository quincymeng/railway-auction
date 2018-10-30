package railway.g8;

import railway.sim.utils.Bid;
import railway.sim.utils.BidInfo;
import railway.sim.utils.Coordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

import javafx.util.Pair;

import java.util.HashMap;


class Connection {
    int row;
    int column;

    public Connection(int row, int column) {
        this.row = row;
        this.column = column;
    }
}

public class Player implements railway.sim.Player {
    // Random seed of 42.
    private int seed = 42;
    private Random rand;

    private double budget;

    private List<BidInfo> availableBids = new ArrayList<>();

    private List<List<Integer>> infra;
    private int[][] transit;

    //hashmap of all our connections- and then map connection 1 etc. to amount of traffic between 
    private HashMap<Connection, Integer> connections = new HashMap<Connection, Integer>();

    //okay new HashMap that just uses bid ID instead 
    //maps bid ID to amount of traffic on that link 
    private HashMap<Integer, Integer> bidIdTraffic = new HashMap<Integer, Integer>();
    private HashMap<Integer, Double> bidIdMinBid = new HashMap<Integer, Double>();


    //Hahsmap for edgeweights
    private HashMap<Integer, Integer> bidIdEdgeWeight = new HashMap<Integer, Integer>();


    private int totalTraffic = 0;


    public Player() {
        rand = new Random();
    }

    public void init(
            String name,
            double budget,
            List<Coordinates> geo,
            List<List<Integer>> infra, //
            int[][] transit, //
            List<String> townLookup) {

        this.budget = budget;
        this.transit = transit;
        this.infra = infra;

        GraphUtility gu = new GraphUtility(geo, infra, transit, townLookup);
        // System.out.println("DEBUGGGGG ////////");
        // System.out.println(townLookup.get(0));
        // System.out.println(townLookup.get(1));
        // for(Object i: gu.path[2][0]){
        //     System.out.print(townLookup.get((Integer) i));
        //     System.out.print(i);
        //     System.out.print(",");
        // }
        // System.out.println("");
        int townsize = geo.size();
        int[][] edgeWeight = new int[townsize][townsize];
        for(int i=0;i<townsize;i++){
            for(int j=i+1;j<townsize;j++){
                if(gu.adj[i][j]==Double.POSITIVE_INFINITY) continue;
                edgeWeight[i][j] = edgeWeight[j][i] = transit[i][j];
            }
        }
        for(int i=0;i<townsize;i++){
            for(int j=i+1;j<townsize;j++){
                List<Integer> townPath = gu.path[i][j];
                if(townPath.size()<3)
                    continue;
                for(int k=0;k<townPath.size()-1;k++){
                    edgeWeight[townPath.get(k)][townPath.get(k+1)] += transit[i][j];
                }
            }
        }

        buildEdgeHashMap(edgeWeight); 
        buildHashMap();
    }



    //finds single link with highest traffic 
    private int calculateHighestTraffic() {
        int currentMax = 0;
        int best = 0;

        //System.out.println("Printing my hashmap"); 
        for (BidInfo b : availableBids) {
            int i = b.id;
            //System.out.println("Key: " + i + " Value: " + bidIdTraffic.get(i)); 
            if (bidIdTraffic.get(i) > currentMax) {
                currentMax = bidIdTraffic.get(i);
                best = i;
                //System.out.println("Found best: " + i + "," + currentMax); 
            }
        }
        return best;
    }

    private int highestTrafficEdgeWeight(){
        int currentMax = 0;
        int best = 0;

        //System.out.println("Printing my hashmap"); 
        for (BidInfo b : availableBids) {
            int i = b.id;
            //System.out.println("Key: " + i + " Value: " + bidIdTraffic.get(i)); 
            if (bidIdEdgeWeight.get(i) > currentMax) {
                currentMax = bidIdEdgeWeight.get(i);
                best = i;
                //System.out.println("Found best: " + i + "," + currentMax); 
            }
        }
        return best;
    }

    private void buildEdgeHashMap(int[][] edgeWeight) {
        int bidID = 0;
        for (int l = 0; l < infra.size(); l++) {
            List<Integer> row = infra.get(l);
            for (int i = 0; i < row.size(); i++) {
                int there = row.get(i);
                Connection pair = new Connection(l, there);
                int traffic = edgeWeight[l][there];
                bidIdEdgeWeight.put(bidID, traffic);
                bidID++;
            }
        }
    }
    //adds to both hashmaps, and updates total traffic 
    private void buildHashMap() {
        int bidID = 0;
        for (int l = 0; l < infra.size(); l++) {
            List<Integer> row = infra.get(l);
            for (int i = 0; i < row.size(); i++) {
                int there = row.get(i);
                Connection pair = new Connection(l, there);
                int traffic = transit[l][there];
                connections.put(pair, traffic);
                bidIdTraffic.put(bidID, traffic);
                totalTraffic += traffic;
                bidID++;
            }
        }
    }

    private void printRow(int[] row) {
        for (int i : row) {
            System.out.print(i);
            System.out.print("\t");
        }
        System.out.println();
    }

    private void printAllInfo(List<Bid> currentBids, List<BidInfo> allBids) {
        System.out.println("This is our budget: " + budget);
        System.out.println("This is the infra: " + infra);


        System.out.println("This is the transit: ");
        for (int[] row : transit) {
            printRow(row);
        }

        System.out.println("The current bids are");
        for (Bid b : currentBids) {
            System.out.println(b.bidder + " bids $" + b.amount + "for: " + "link 1 ID: " + b.id1 + " Link 2 ID: " + b.id2);
        }

        System.out.println("All bids info: ");
        for (BidInfo b : allBids) {
            System.out.println("Bid id: " + b.id + " town 1: " + b.town1 + " town 2: " + b.town2 + " bidded amount: "
                    + b.amount + " owner: " + b.owner);
        }
    }


    //bets percentage of budget = percent of traffic this link corresponds to
    private double calculateBid(int id) {
        double bid = bidIdMinBid.get(id);
        int traffic = bidIdTraffic.get(id);
        float percent = ((float) traffic / (float) totalTraffic);
        //System.out.println("Traffic: " + traffic + " totalTraffic: " + totalTraffic + " percent: " + percent); 
        double fractionOfBudget = (double) (budget * percent);
        bid += (.1 * fractionOfBudget);

        System.out.println("Minimum bid: " + bidIdMinBid.get(id) + " our addition: " + (.1 * fractionOfBudget));
        if (bid < budget) {
            return bid;
        } else {
            return bidIdMinBid.get(id);
        }
    }

    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {

        if (availableBids.size() != 0) {
            return null;
        }

        //adding all available bids and adding to hashmap of bid id to minimum bid
        for (BidInfo bi : allBids) {
            if (bi.owner == null) {
                availableBids.add(bi);
                bidIdMinBid.put(bi.id, bi.amount);
            }
        }

        if (availableBids.size() == 0) {
            return null;
        }


        int bidID = highestTrafficEdgeWeight(); 
        //int bidID = calculateHighestTraffic();
        double cashMoney = calculateBid(bidID);

        Bid bid = new Bid();
        bid.id1 = bidID;
        bid.amount = cashMoney;

        //System.out.println("Bid before checking other playerse: " + cashMoney + "  " + bid.amount);

        // Check if another player has made a bid for this link.
        for (Bid b : currentBids) {
            if (b.id1 == bidID || b.id2 == bidID) {
                if (budget - b.amount - 10000 < 0.) {
                    return null;
                } else {
                    bid.amount = b.amount + 10000;
                }
            }
        }

        //System.out.println("Bid amount: " + bid.amount + " Current budget " + budget);
        if (bid.amount > budget) {
            return null;
        }

        return bid;
    }

    public void updateBudget(Bid bid) {
        if (bid != null) {
            budget -= bid.amount;
        }

        availableBids = new ArrayList<>();
    }
}
