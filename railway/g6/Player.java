package railway.g6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// To access data classes.
import railway.sim.utils.*;


class LinkInfo {
    public int town1;
    public int town2;

    public double distance;
}


public class Player implements railway.sim.Player {

    boolean toPrint = true;

    public void println(String s) {
        if (toPrint) {
            System.out.println(s);
        }
    }

    private static List<LinkInfo> links = new ArrayList<>();

    // Random seed of 42.
    private int seed = 42;
    private Random rand;

    private double budget;

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
        List<String> townLookup,
        List<BidInfo> allBids) {

        this.budget = budget;
    }

    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids, Bid lastRoundMaxBid) {
        // The random player bids only once in a round.
        // This checks whether we are in the same round.
        // Random player doesn't care about bids made by other players.

        //println("HELLOOOO\n\n\n");

        // only bids one round for now
        if (availableBids.size() != 0) {
            return null;
        }


        if (availableBids.size() == 0) {
        // updates available bids
            for (BidInfo bi : allBids) {
                if (bi.owner == null) {
                    availableBids.add(bi);
                }
            }

            if (availableBids.size() == 0) {
                return null;
            }
        }

        /* BidInfo contains minimum price
        for (BidInfo x : availableBids) {
            println(x.owner + x.amount);
        }*/






        BidInfo randomBid = availableBids.get(rand.nextInt(availableBids.size()));
        double amount = randomBid.amount;

        // Don't bid if the random bid turns out to be beyond our budget.
        if (budget - amount < 0.) {
            return null;
        }

        // Check if another player has made a bid for this link.
        for (Bid b : currentBids) {
            if (b.id1 == randomBid.id || b.id2 == randomBid.id) {
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
        bid.id1 = randomBid.id;

        return bid;
    }

    public void updateBudget(Bid bid) {
        if (bid != null) {
            budget -= bid.amount;
        }

        availableBids = new ArrayList<>();
    }


}
