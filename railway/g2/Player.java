package railway.g2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

// To access data classes.
import railway.sim.utils.*;

public class Player implements railway.sim.Player {
    // Random seed of 42.
    private int seed = 42;
    private Random rand;

    private double budget;
    private int[][] transit;

    private List<BidInfo> availableBids = new ArrayList<>();
    private List<Coordinates> geo = new ArrayList<>();
    private List<List<Integer>> infra = new ArrayList<List<Integer>>();
    

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
        this.geo = geo;
        this.infra = infra;
        this.transit = transit;
    }

    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
        // The random player bids only once in a round.
        // This checks whether we are in the same round.
        // Random player doesn't care about bids made by other players.

        // System.out.println("1 THIS SHOULD PRINT TWICE");

        // if (availableBids.size() != 0) {
        //     return null;
        // }
        // System.out.println("2 THIS SHOULD PRINT TWICE");


        for (BidInfo bi : allBids) {
            if (bi.owner == null) {
                availableBids.add(bi);
            }
        }


        if (availableBids.size() == 0) {
            return null;
        }
        // bi.amount = transit[t1][t2] * li.distance * 10
        //so sort availableBids by amount and choose the highest
        Collections.sort(availableBids,new Comparator<BidInfo>(){
            public int compare(BidInfo o1, BidInfo o2) {
                if(o1.amount > o2.amount){
                    return 1;
                }
                if(o1.amount == o2.amount){
                    return 0;
                }
                return -1;
            }
        });
        // for(int i=0;i<availableBids.size();i++){
        //     availableBids.get(i).amount=availableBids.get(i).amount+100;
        // }
        BidInfo curBid = availableBids.get(availableBids.size()-1);
        double amount = curBid.amount;

        System.out.println("curBid:"+ curBid.town1+curBid.town2);

        // Don't bid if the random bid turns out to be beyond our budget.
        if (budget - amount < 0.) {
            return null;
        }

        System.out.print("Current bid: " + currentBids.size());

        // Check if another player has made a bid for this link.
        for (Bid b : currentBids) {
            // System.out.println("amount+bidder"+b.amount+b.bidder);
            if (b.id1 == curBid.id || b.id2 == curBid.id) {
                if (budget - b.amount - 10000 < 0.) {
                    return null;
                }
                else if (amount>curBid.amount+30000){
                    System.out.println("Stop bidding");
                    break;
                }
                else {
                    if(b.bidder.equals("g2"))
                    {
                        break;
                    }
                    // System.out.println(availableBids.size() + " " + allBids.size());

                    amount = b.amount + 10000;
                    // System.out.println("Our latest bid: " + amount);
                }

                break;
            }
        }

        Bid bid = new Bid();
        bid.amount = amount;
        bid.id1 = curBid.id;
        // availableBids = new ArrayList<>();
        return bid;
    }

    public void updateBudget(Bid bid) {
        if (bid != null) {
            budget -= bid.amount;
        }

        availableBids = new ArrayList<>();
    }
}



// public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
//     // The random player bids only once in a round.
//     // This checks whether we are in the same round.
//     // Random player doesn't care about bids made by other players.
//     if (availableBids.size() != 0) {
//         return null;
//     }
// 
//     for (BidInfo bi : allBids) {
//         if (bi.owner == null) {
//             availableBids.add(bi);
//         }
//     }

//     if (availableBids.size() == 0) {
//         return null;
//     }

//     BidInfo randomBid = availableBids.get(rand.nextInt(availableBids.size()));
//     double amount = randomBid.amount;

//     // Don't bid if the random bid turns out to be beyond our budget.
//     if (budget - amount < 0.) {
//         return null;
//     }

//     // Check if another player has made a bid for this link.
//     for (Bid b : currentBids) {
//         if (b.id1 == randomBid.id || b.id2 == randomBid.id) {
//             if (budget - b.amount - 10000 < 0.) {
//                 return null;
//             }
//             else {
//                 amount = b.amount + 10000;
//             }

//             break;
//         }
//     }

//     Bid bid = new Bid();
//     bid.amount = amount;
//     bid.id1 = randomBid.id;

//     return bid;
// }