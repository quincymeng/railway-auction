package railway.g7;

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
    private String name;
    private List<Coordinates> geo;
    private List<List<Integer>> infra;
    private int[][] transit;
    private List<String> townLookup;
    private WeightedGraph graph;
    private List<RouteValue> rankedRouteValue;
    private List<BidInfo> allBids;

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
        this.name = name;
        this.budget = budget;
        this.geo = geo;
        this.infra = infra;
        this.transit = transit;
        this.townLookup = townLookup;
        initializeGraph();
        // List<List<Integer>> links = getMostVolumePerKm();
        // for (int i = 0; i < links.size(); i++) {
        //     System.out.println("The %s link is: ");
        //     for (int j = 0; j < links.get(i).size(); j++) {
        //         System.out.print(links.get(i).get(j) + " ");
        //     }
        // }
        List<List<Integer>> bridges = findBridges();
        //System.out.println("The bridges are:");
        for (int i = 0; i < bridges.size(); i++) {
            for (int j = 0; j < bridges.get(i).size(); j++) {
                //System.out.print(bridges.get(i).get(j) + " ");
            }
            //System.out.println();
        }
        rankedRouteValue = new ArrayList<RouteValue>();
        gatherAllVolumePerKm();
        for (int i = 0; i < rankedRouteValue.size(); i++) {
            //System.out.println("route number: " + i + ", volume: " + rankedRouteValue.get(i).getVolumePerKm() + ", distance: " + rankedRouteValue.get(i).getDistance());
        }
    }

    private void initializeGraph() {
        graph = new WeightedGraph(townLookup.size());
        for (int i = 0; i < townLookup.size(); i++) {
            graph.setLabel(townLookup.get(i));
        }

        for (int i = 0; i < infra.size(); i++) {
            for (int j = 0; j < infra.get(i).size(); j++) {
                int source = i;
                int target = infra.get(i).get(j);
                // graph.addEdge(source, target, transit[source][target]);
                double distance = calcEuclideanDistance(geo.get(source), geo.get(target));
                graph.addEdge(source, target, distance);
            }
        }
    }

    private double calcEuclideanDistance(Coordinates a, Coordinates b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
    }

    private List<List<Integer>> getLinks(int source, int target) {
        int[][] prev = Dijkstra.dijkstra(graph, source);
        return Dijkstra.getPaths(graph, prev, target);
    }

    // private List<List<Integer>> getMostVolumePerKm() {
    //     double max = 0;
    //     List<List<Integer>> maxLinks = new ArrayList<List<Integer>>();
    //     for (int i = 0; i < transit.length; i++) {
    //         for (int j = i + 1; j < transit[i].length; j++) {
    //             List<List<Integer>> links = getLinks(i, j);
    //             System.out.println("transit: s=" + i + ", t=" + j);
    //             for (int m = 0; m < links.size(); m++) {
    //                 for (int n = 0; n < links.get(m).size(); n++) {
    //                     System.out.print(links.get(m).get(n) + " ");
    //                 }
    //             }
    //             int distance = 0;
    //             for (int x = 0; x < links.get(0).size() - 1; x++) {
    //                 distance += graph.getWeight(links.get(0).get(x), links.get(0).get(x + 1));
    //             }

    //             double volumePerKm = (double)transit[i][j] / distance;
    //             if (volumePerKm > max) {
    //                 max = volumePerKm;
    //                 maxLinks = links;
    //             }
    //         }
    //     }

    //     System.out.println("most volume per km: " + max);
    //     return maxLinks;
    // }

    private RouteValue getRouteValue(int source, int target) {
        List<List<Integer>> links = getLinks(source, target);
        int distance = 0;
        for (int i = 0; i < links.get(0).size() - 1; i++) {
            distance += graph.getWeight(links.get(0).get(i), links.get(0).get(i + 1));
        }
        double volumePerKm = (double) transit[source][target] / distance;
        return this.new RouteValue(links, volumePerKm, distance);
    }

    private void gatherAllVolumePerKm() {
        // double max = 0;
        List<List<Integer>> maxLinks = new ArrayList<List<Integer>>();
        for (int i = 0; i < transit.length; i++) {
            for (int j = i + 1; j < transit[i].length; j++) {
                rankedRouteValue.add(getRouteValue(i, j));
                // List<List<Integer>> links = getLinks(i, j);
                // System.out.println("transit: s=" + i + ", t=" + j);
                // for (int m = 0; m < links.size(); m++) {
                //     for (int n = 0; n < links.get(m).size(); n++) {
                //         System.out.print(links.get(m).get(n) + " ");
                //     }
                // }
                // int distance = 0;
                // for (int x = 0; x < links.get(0).size() - 1; x++) {
                //     distance += graph.getWeight(links.get(0).get(x), links.get(0).get(x + 1));
                // }

                // double volumePerKm = (double)transit[i][j] / distance;
                // RouteValue rv = new RouteValue(links, volume, distance);
                // rankedRouteValue.add(rv);
                // if (volumePerKm > max) {
                //     max = volumePerKm;
                //     maxLinks = links;
                // }
            }
        }
        Collections.sort(rankedRouteValue, Collections.reverseOrder());
    }

    private List<List<Integer>> findBridges() {
        List<List<Integer>> bridges = new ArrayList<List<Integer>>();
        for (int i = 0; i < infra.size(); i++) {
            for (int j = 0; j < infra.get(i).size(); j++) {
                int source = i;
                int target = infra.get(i).get(j);
                double weight = graph.getWeight(source, target);
                graph.removeEdge(source, target);
                boolean bridgeFound = false;
                // System.out.println("source: " + source + ", target: " + target);
                for (int s = 0; s < townLookup.size(); s++) {
                    int[][] prev = Dijkstra.dijkstra(graph, s);
                    for (int t = s + 1; t < prev.length; t++) {
                        if (prev[t][0] == 0) {
                            bridgeFound = true;
                            // System.out.println("s: " + s + ", t: " + t);
                            break;
                        }
                    }

                    if (bridgeFound) {
                        break;
                    }
                }

                if (bridgeFound) {
                    List<Integer> bridge = new ArrayList<Integer>();
                    bridge.add(source);
                    bridge.add(target);
                    bridges.add(bridge);
                }

                graph.addEdge(source, target, weight);
            }
        }

        return bridges;
    }

    // return null if owned by other; return bidInfo if not
    public BidInfo checkOwnershipByTownID(int id1, int id2){
        String name1 = townLookup.get(id1);
        String name2 = townLookup.get(id2);
        for (BidInfo bi : allBids) {
            if (((bi.town1.equals(name1) && bi.town2.equals(name2))||(bi.town1.equals(name2) && bi.town2.equals(name1)))) {
                if (bi.owner!=null&& !bi.owner.equals(this.name)){
                    return null;
                }
                else{
                    return bi;
                }
            }
        }
        return null;
    }

    public int getBidID(int id1, int id2){
        String name1 = townLookup.get(id1);
        String name2 = townLookup.get(id2);
        for (BidInfo bi : allBids){
            if ((bi.town1.equals(name1) && bi.town2.equals(name2))||(bi.town1.equals(name2) && bi.town2.equals(name1))){
                return bi.id;
            }
        }
        return -1;
    }

    public LinkValue getLongestPair(RouteValue route){
        List<LinkValue> doubleLinks= new ArrayList<LinkValue>();
        for (int i=0; i< route.routes.get(0).size()-1;i++){
            return null;
        }
        Collections.sort(doubleLinks, Collections.reverseOrder());
        if (doubleLinks.size()>0){
            return doubleLinks.get(0);
        }
        else{
            return null;
        }
    }

    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {
        // The random player bids only once in a round.
        // This checks whether we are in the same round.
        // Random player doesn't care about bids made by other players.
        this.allBids = allBids; 
        for (BidInfo bi : allBids) { 
            if (bi.owner == null) {
                availableBids.add(bi);
            }
        } 
        //System.out.println(availableBids.size());
        if (availableBids.size()==0){
            return null;
        } 

        RouteValue routeToBid=null; 
        BidInfo linkToBid =null; 
        BidInfo secondLinkToBid = null;
        LinkValue linkValueToBid =null; 
        LinkValue secondLinkValueToBid = null;
        List<BidInfo> bids=null; 
        List<LinkValue> linkinfos = null;
        for (int i=0; i< rankedRouteValue.size();i++){
            routeToBid = rankedRouteValue.get(i); 
            List<LinkValue> bidLinks=routeToBid.linkValues; 
            boolean ownedByOther =false; // start out as false
            bids = new ArrayList<BidInfo>(); 
            linkinfos = new ArrayList<LinkValue>(); 
            for (int j=0; j<bidLinks.size();j++){ // go through all in bidLinks 
                LinkValue currLink = bidLinks.get(j); 
                //System.out.println(currLink.town1 +"-"+ currLink.town2 +" "+currLink.distance);
                BidInfo alink = checkOwnershipByTownID(currLink.town1, currLink.town2);
                if (alink == null){
                    ownedByOther=true;
                    rankedRouteValue.remove(i); // remove from consideration
                    i--;
                    break;
                }
                else{
                    if(alink.owner==null || !alink.owner.equals(this.name)){
                        bids.add(alink);
                        linkinfos.add(currLink);
                    }
                }
                if (!ownedByOther){
                    break; // no need to go on with this for loop! Route identified! 
                }
            }

        }
        if (bids.size()==0){ // 
            linkToBid = availableBids.get(rand.nextInt(availableBids.size()));
        } 
        else if (bids.size()==1){ 
            linkToBid = bids.get(0); 
            linkValueToBid = linkinfos.get(0);
            //rankedRouteValue.remove(routeToBid);
        }
        else{
            linkToBid = bids.get(0); 
            linkValueToBid = linkinfos.get(0);
            secondLinkToBid = bids.get(1);
            secondLinkValueToBid = linkinfos.get(1);
        }


        // make linkToBid 
        //System.out.println(linkToBid.id);
        // Don't bid if the random bid turns out to be beyond our budget.
                // get the first two bids
        //System.out.println(linkValueToBid.town1);
        double amount = linkValueToBid.distance * transit[linkValueToBid.town1][linkValueToBid.town2];
        amount += linkToBid.amount;
        if (secondLinkToBid != null) {
            amount += secondLinkValueToBid.distance * transit[secondLinkValueToBid.town1][secondLinkValueToBid.town2];
            amount += secondLinkToBid.amount;
        }

        // taking into account the entire route 
        amount += routeToBid.volPerKm * routeToBid.distance; // the entire distance? 

        // Don't bid if the random bid turns out to be beyond our budget.
        if (budget - amount < 0.) {
            return null;
        }

        // Check if another player has made a bid for this link.
        for (Bid b : currentBids) {
            if (b.id1 == linkToBid.id || b.id2 == linkToBid.id) {
                if (budget - b.amount - 10 < 0.) {
                    return null;
                }
                else {
                    amount = b.amount + 10;
                }

                break;
            }
        }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              

        Bid bid = new Bid();
        bid.amount = amount;
        bid.id1 = linkToBid.id;

        for (Bid bi: currentBids){
            if(bi.id1==bid.id1){
                return null;
            }
        }

        return bid;
    }

    public void updateBudget(Bid bid) {
        if (bid != null) {
            budget -= bid.amount;
        }

        availableBids = new ArrayList<>();
    }

    public BidInfo getBidInfo(int id1, int id2, List<BidInfo> allBids){
        String name1 = townLookup.get(id1);
        String name2 = townLookup.get(id2);
        for (BidInfo bi : allBids){
            if ((bi.town1.equals(name1) && bi.town2.equals(name2))||(bi.town1.equals(name2) && bi.town2.equals(name1))){
                return bi;
            }
        }
        return null;
    }

    private class LinkValue implements Comparable<LinkValue>{
        int town1;
        int town2;
        int townMid; // used for bidding pair of links
        double distance;
        int bidID=-1;

        public LinkValue (int id1, int id2){
            town1 = id1;
            town2 = id2;
            distance = graph.getWeight(id1,id2);
        }

        public LinkValue (int id1, int id2, int id3, double dist){
            town1 = id1;
            town2 = id2;
            townMid = id3;
            distance = dist;
        }

        @Override
        public int compareTo(LinkValue lv) {
            return (int) Math.signum(distance - lv.distance);
        }
    }

    private class RouteValue implements Comparable<RouteValue>{
        List<List<Integer>> routes;
        double volPerKm;
        double distance;
        List<LinkValue> linkValues= new ArrayList<LinkValue>();

        public RouteValue (List<List<Integer>> r, double v, double d){
            routes = copyListofList(r);
            volPerKm = v;
            distance = d;
            for (int i=0; i < routes.get(0).size()-1;i++){
                linkValues.add(Player.this.new LinkValue(routes.get(0).get(i),routes.get(0).get(i+1)));
            }
            Collections.sort(linkValues, Collections.reverseOrder());
        }

        private List<List<Integer>> copyListofList(List<List<Integer>> list) {
            List<List<Integer>> results = new ArrayList<List<Integer>>();
            for (int i = 0; i < list.size(); i++) {
                List<Integer> result = new ArrayList<Integer>();
                for (int j = 0; j < list.get(i).size(); j++) {
                    result.add(list.get(i).get(j));
                }
                results.add(result);
            }
            return results;
        }

        // return true if link is owned by someone else

        public List<List<Integer>> getRoutes() {
            return copyListofList(routes);
        }

        public double getVolumePerKm() {
            return volPerKm;
        }

        public double getDistance() {
            return distance;
        }

        @Override
        public int compareTo(RouteValue rv) {
            if (volPerKm != rv.volPerKm) {
                return (int) Math.signum(volPerKm - rv.volPerKm);
            }
            else {
                return (int) Math.signum(distance - rv.distance);
            }
        }
    }
}
