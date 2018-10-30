package railway.g6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;



import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

// To access data classes.
import railway.sim.utils.*;

public class Player implements railway.sim.Player {
    // Random seed of 42.
    private int seed = 42;
    private Random rand;

    String name;
    private double budget;
    private List<Coordinates> geo;
    private List<List<Integer>> infra;
    private int[][] transit;
    private List<String> townLookup;
     private double[][] revenue;

    private class playerNames
    {
        String name;

        public String getName()
        {
            return name;
        }

    }
    private List<playerNames> origPlayers;


    private List<BidInfo> availableBids = new ArrayList<>();

    public Player() {
        rand = new Random();
        origPlayers = new ArrayList<playerNames>();
        for(int i=1;i<=8;i++)
        {
            playerNames tmp = new playerNames();
            tmp.name = "g" + Integer.toString(i);
            origPlayers.add(tmp);
        }
    }

    public void init(
        String name,
        double budget,
        List<Coordinates> geo,
        List<List<Integer>> infra,
        int[][] transit,
        List<String> townLookup) {

        this.budget = budget;
        this.name = name;
        this.geo = deepClone(geo);
        this.infra = deepClone(infra);
        this.transit = deepClone(transit);
        this.townLookup = deepClone(townLookup);

         revenue = getRevenue(); 
    }

    // public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
    //     // The random player bids only once in a round.
    //     // This checks whether we are in the same round.
    //     // Random player doesn't care about bids made by other players.
    //     if (availableBids.size() != 0) {
    //         return null;
    //     }

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


    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
        

        availableBids.clear();

        for (BidInfo bi : allBids) {
            if (bi.owner == null) {
                availableBids.add(bi);
            }
        }

        if (availableBids.size() == 0) {
            return null;
        }


        double max_ratio = 0;

        for(Bid curr : currentBids)
        {
            
                double dist =0;
                for(BidInfo tmp: allBids)
                {
                    if(tmp.id == curr.id1 || tmp.id == curr.id2)
                    {
                        dist+= getDistance(tmp.town1,tmp.town2);
                    }
                }

                max_ratio = Math.max(max_ratio, curr.amount/dist);
        }


        Boolean bid_in_round =  false;
        double profit = -10000000;
        double fin_amt = 0;
        int fin_bid_idx = -1;

        double max_bid = 0,our_bid=0;

       for(int i=0;i<availableBids.size();i++)
       {
             BidInfo opp = deepClone(availableBids.get(i));
            double min_amt = opp.amount,prev_bid=0;
            for(Bid curr: currentBids)
            {
                if(!curr.bidder.equals(name) && curr.id1 == opp.id && curr.id2 < 0)
                {
                    min_amt = Math.max(curr.amount,min_amt);
                }
                else if (!curr.bidder.equals(name) && curr.id2>=0 && (curr.id1 == opp.id || curr.id2 == opp.id))
                {
                     double dist_tot =0,dist=getDistance(opp.town1,opp.town2);

                    for(BidInfo tmp: allBids)
                    {
                     if(tmp.id == curr.id1 || tmp.id == curr.id2)
                        {
                            dist_tot+= getDistance(tmp.town1,tmp.town2);
                        }
                    }

                    min_amt = Math.max(min_amt, curr.amount*dist/dist_tot);
                    

                }
                else if(curr.bidder.equals(name) && curr.id1 == opp.id)
                {
                    prev_bid = curr.amount;
                }
                else if(curr.bidder.equals(name))
                {
                    our_bid = curr.amount;
                }
            }

            max_bid = Math.max(min_amt,max_bid);

            double act_dist = getDistance(opp.town1,opp.town2);
            opp.owner = name;
            double bid_amount =  Math.max(min_amt+10000,max_ratio*act_dist+10000);
            //System.out.println("amount bid on current link is " + min_amt);
            // System.out.println("max bid is" + max_bid + " our prev bid was" + prev_bid);
           if(prev_bid>=min_amt || budget<bid_amount || max_bid<=our_bid ) break;

           if(min_amt>= budget/2) continue;

            //System.out.println("prev bid is" + prev_bid +  "min_amt is " + min_amt + "bid amt is " + bid_amount + "budget is " + budget);

            opp.amount = bid_amount;


            // BidInfo tmp = new BidInfo();
            // int idx = -1;

            // for(int k=0;k<allBids.size();k++)
            // {
            //     if(opp.id == allBids.get(k).id)
            //     {
            //         tmp = allBids.get(k);
            //         idx = k;
            //         break;
            //     }
            // }

            // allBids.set(idx,opp);
           
            //Double curr_Profit = getProfitsPerPlayer(revenue,allBids);

            int t1=-1,t2=-1;

             for(int z=0;z<townLookup.size();z++)
                {
                    if(townLookup.get(z).equals(opp.town1))
                        t1 = z;
                    else if (townLookup.get(z).equals(opp.town2))
                        t2 = z;
                }

            //Double curr_Profit =  ((double)transit[t1][t2] + (double)transit[t2][t1])*getDistance(opp.town1,opp.town2)*10 - bid_amount;
                Double curr_Profit =  revenue[t1][t2] - bid_amount;
               // System.out.println("traffic between towns " + t1 + " and " + t2 +"is" + transit[t1][t2]);
                //System.out.println("availabale bids are " + availableBids.size() );
            //curr_Profit-= bid_amount;
           // System.out.println("profit expected is " + curr_Profit);
            if(curr_Profit>profit)
            {
                profit = curr_Profit;
                fin_amt = bid_amount;
                fin_bid_idx = i;
                bid_in_round = true;
            }

            //allBids.set(idx,tmp);




       }

       Bid bid = new Bid();

       if(bid_in_round)
       {

        bid.amount = fin_amt;
        bid.id1 = availableBids.get(fin_bid_idx).id;
        bid.bidder = name;
         return bid;
       }

       else 
        return null;

        // BidInfo randomBid = availableBids.get(rand.nextInt(availableBids.size()));
        // double amount = randomBid.amount;

        // Don't bid if the random bid turns out to be beyond our budget.
        // if (budget - amount < 0.) {
        //     return null;
        // }

        // // Check if another player has made a bid for this link.
        // for (Bid b : currentBids) {
        //     if (b.id1 == randomBid.id || b.id2 == randomBid.id) {
        //         if (budget - b.amount - 10000 < 0.) {
        //             return null;
        //         }
        //         else {
        //             amount = b.amount + 10000;
        //         }

        //         break;
        //     }
        // }

       

       
    }

    public void updateBudget(Bid bid) {
        if (bid != null) {
            budget -= bid.amount;
        }

        availableBids = new ArrayList<>();
    }


    private  <T extends Object> T deepClone(T object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(bais);
            return (T) objectInputStream.readObject();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    // private static double getDistance(Bid bid) {
    //     double dist = getDistance(bid.id1);

    //     if (bid.id2 != -1) {
    //         dist += getDistance(bid.id2);
    //     }

    //     return dist;
    // }

    private  double getDistance(String t1, String t2) {
        // return getDistance(townRevLookup.get(t1), townRevLookup.get(t2));

        int s=-1,e=-1;
        for(int i=0;i<townLookup.size();i++)
        {
            if(townLookup.get(i).equals(t1))
                s = i;
            else if (townLookup.get(i).equals(t2))
                e = i;
        }

        return getDistance(s,e);
    }

    // private static double getDistance(int linkId) {
    //      return links.get(linkId).distance;
    //  }

    private  double getDistance(int t1, int t2) {
        return Math.pow(
            Math.pow(geo.get(t1).x - geo.get(t2).x, 2) +
                Math.pow(geo.get(t1).y - geo.get(t2).y, 2),
            0.5);
    }


    private  double[][] getRevenue() {
        final int n = geo.size();

        // Create the graph.
        WeightedGraph g = new WeightedGraph(n);

        for (int i=0; i<n; ++i) {
            g.setLabel(townLookup.get(i));
        }

        for (int i=0; i<infra.size(); ++i) {
            for (int j=0; j<infra.get(i).size(); ++j) {
                g.addEdge(i, infra.get(i).get(j), getDistance(i, infra.get(i).get(j)));
            }
        }

        //g.print();

        // Compute the revenue between any two nodes.
        double[][] revenue = new double[n][n];

        for (int i=0; i<n; ++i) {
            int[][] prev = Dijkstra.dijkstra(g, i);
            for (int j=i+1; j<n; ++j) {
                List<List<Integer>> allPaths = Dijkstra.getPaths(g, prev, j);

                double cost = 0;
                for (int k=0; k<allPaths.get(0).size()-1; ++k) {
                    cost += getDistance(allPaths.get(0).get(k), allPaths.get(0).get(k+1));
                }

                revenue[i][j] = cost * transit[i][j] * 10;
            }
        }

        return revenue;
    }





    private  Double getProfitsPerPlayer(double[][] revenue,List<BidInfo> allBids) {
        // The graph now has nodes replicated for each player and a start and end node.
        final int n = geo.size() * (origPlayers.size() + 2);

        Map<String, Double> playerRev = new HashMap<>();
        for (int i=0; i<origPlayers.size(); ++i) {
            playerRev.put(origPlayers.get(i).getName(), 0.);
        }

        WeightedGraph g = new WeightedGraph(n);
        for (int i=0; i<geo.size(); ++i) {
            g.setLabel(townLookup.get(i) + "-s");
            g.setLabel(townLookup.get(i) + "-e");

            for (int j=0; j<origPlayers.size(); ++j) {
                g.setLabel(townLookup.get(i) + "-" + origPlayers.get(j).getName());
            }
        }

        for (BidInfo bi : allBids) {
            g.addEdge(bi.town1 + "-" + bi.owner,
                bi.town2 + "-" + bi.owner,
                getDistance(bi.town1,bi.town2));

            g.addDirectedEdge(bi.town1 + "-s",
                bi.town1 + "-" + bi.owner,
                0);
            g.addDirectedEdge(bi.town2 + "-s",
                bi.town2 + "-" + bi.owner,
                0);
            g.addDirectedEdge(bi.town1 + "-" + bi.owner,
                bi.town1 + "-e",
                0);
            g.addDirectedEdge(bi.town2 + "-" + bi.owner,
                bi.town2 + "-e",
                0);
        }

        for (int i=0; i<geo.size(); ++i) {
            for (int j=0; j<origPlayers.size(); ++j) {
                for (int k=j+1; k<origPlayers.size(); ++k) {
                    g.addEdge(townLookup.get(i) + "-" + origPlayers.get(j).getName(),
                        townLookup.get(i) + "-" + origPlayers.get(k).getName(),
                        200);
                }
            }
        }

        // g.print();

        for (int i=0; i<geo.size(); ++i) {
            int[][] prev = Dijkstra.dijkstra(g, g.getVertex(townLookup.get(i) + "-s"));
            for (int j=i+1; j<geo.size(); ++j) {
                List<List<Integer>> allPaths =
                    Dijkstra.getPaths(g, prev, g.getVertex(townLookup.get(j) + "-e"));

                // Cost per path.
                double cost = revenue[i][j] / allPaths.size();

                for (int p=0; p<allPaths.size(); ++p) {
                    double trueDist = 0.0;
                    Map<String, Double> playerDist = new HashMap<>();

                    for (playerNames pw : origPlayers) {
                        playerDist.put(pw.getName(), 0.);
                    }

                    for (int k=0; k<allPaths.get(p).size()-1; ++k) {
                        String a = g.getLabel(allPaths.get(p).get(k));
                        String b = g.getLabel(allPaths.get(p).get(k+1));

                        if (a.split("-")[1].equals(b.split("-")[1])) {
                            trueDist +=
                                getDistance(a.split("-")[0], b.split("-")[0]);

                            playerDist.put(
                                a.split("-")[1],
                                playerDist.get(a.split("-")[1]) +
                                    getDistance(a.split("-")[0], b.split("-")[0]));
                        }
                    }

                    for (Map.Entry<String, Double> entry : playerDist.entrySet()) {
                        playerRev.put(
                            entry.getKey(),
                            playerRev.get(entry.getKey()) + entry.getValue()/trueDist * cost);
                    }
                }
            }
        }

       

        return playerRev.get(name) - budget;
    }

}
