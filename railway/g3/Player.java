package railway.g3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// To access data classes.
import railway.sim.utils.*;

public class Player implements railway.sim.Player {
    // Random seed of 42.
    private int seed = 42;
    private Random rand;

    private double budget;
    private ArrayList<String> townLookup;
    private List<List<Integer>> infra;
    private int[] connections;
    //private BidInfo the_bid;

    private List<BidInfo> availableBids = new ArrayList<>();

    public Player() {
        rand = new Random();
    }

    public void init(
        String name,
        double budget,
        List<Coordinates> geo,
        List<List<Integer>> infra,
        int[][] transit,
        List<String> townLookup) {

        this.budget = budget;
        this.townLookup = (ArrayList)townLookup;
        this.infra = infra;

        connections = new int[geo.size()];
        for(int i = 0; i < connections.length; ++i) {
            List<Integer> row = infra.get(i);
            connections[i] += row.size();
            for(int j = 0; j < row.size(); ++j) {
                ++connections[row.get(j)];
            }
        }

    }

    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
        // The random player bids only once in a round.
        // This checks whether we are in the same round.
        // Random player doesn't care about bids made by other players.
        if (availableBids.size() != 0) {
            return null;
        }

        for (BidInfo bi : allBids) {
            if (bi.owner == null) {
                availableBids.add(bi);
            }
        }

        if (availableBids.size() == 0) {
            return null;
        }

        int max_connections = -1;
        BidInfo max_bid = null;
        for (BidInfo cur_bid : availableBids) {
            String t1 = cur_bid.town1;
            String t2 = cur_bid.town2;
            int t1_i = townLookup.indexOf(t1);
            int t2_i = townLookup.indexOf(t2);
            //System.out.println("towns " + t1 + ", " + t2);

            int num_connections = connections[t1_i] + connections[t2_i];
            //System.out.println("num_connections = " + num_connections);
            if (num_connections > max_connections) {
                max_connections = num_connections;
                max_bid = cur_bid;
            }

        }
        //BidInfo randomBid = availableBids.get(rand.nextInt(availableBids.size()));
        
        double amount = max_bid.amount;

        //System.out.println("OWNER: " + max_bid.owner);
        // Don't bid if the random bid turns out to be beyond our budget.
        if (budget - amount < 0.) { 
            return null;
        }

        // Check if another player has made a bid for this link.
        for (Bid b : currentBids) {
            if (b.id1 == max_bid.id || b.id2 == max_bid.id) {
                if (budget - b.amount - 10000 < 0.) {
                    return null;
                }
                else {
                    amount = b.amount + 10000;
                }

                break;
            }
        }

        Bid bid = new Bid();
        bid.amount = amount;
        bid.id1 = max_bid.id;

        //the_bid = max_bid;

        return bid;
    }

    /*public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {




        return bid;
    }*/

    public void updateBudget(Bid bid) {
        if (bid != null) {
            budget -= bid.amount;
        }

        availableBids = new ArrayList<>();
    }
}
