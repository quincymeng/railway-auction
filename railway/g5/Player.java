package railway.g5;

import java.util.HashMap;
import java.util.Map;
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

    private List<BidInfo> availableBids = new ArrayList<>();
	
    private List<String> ownedCities = new ArrayList<>();
    private Map<Integer, Integer> railValues = new HashMap<Integer, Integer>();

    public Player() {
        rand = new Random();
    }

    public void init(
        String name,
        double budget,
        List<Coordinates> geo,
        List<List<Integer>> infra,
        int[][] transit,
        List<String> townLookup){

        this.budget = budget;

        Map<Integer, Integer> cityTraffic = new HashMap<Integer, Integer>();
        for (int m=0; m<geo.size(); m++){
          cityTraffic.put(m, 0);
          for (int n=0; n<geo.size(); n++){
            if( n == m ) {
              for(int p=0; p<geo.size(); p++){
                cityTraffic.put(m, cityTraffic.get(m)+transit[n][p]);
              }
            } else {
              cityTraffic.put(m, cityTraffic.get(m)+transit[n][m]);
            }
          }
        }

        int id = 0;
        for (int i=0; i<infra.size(); ++i){
          for (int j=0; j<infra.get(i).size(); ++j){
            int irails = infra.get(i).size();
            int jrails = infra.get(infra.get(i).get(j)).size();

		//		System.out.printf("i is %d, irails starting at %d\n", i, irails);
		//		System.out.printf("j is %d, jrails starting at %d\n", j, jrails);

            for (int k=0; k<infra.size(); ++k){
              if (infra.get(k).contains(i) ){
                irails +=1;
			//	System.out.printf("found a %d in line %d, irails now %d\n", i, k, irails);
		          }
              if (infra.get(k).contains(infra.get(i).get(j)) ){
                jrails +=1;
			//	System.out.printf("found a %d in line %d, jrails now %d\n", j, k, jrails);
		          }
		        }
		        int value = 0;
            if (irails != 0){
              value += cityTraffic.get(i)/irails;
            }
            if (jrails != 0){
              value += cityTraffic.get(j)/jrails;
            }
            railValues.put(id, value);
            id++;
          }
        }
    }

    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids) {

        // Check if everyone else has dropped out
        // And mark the current winning/leading bid
        // Also mark links that haven't been bid on
        boolean uncontested = true;
        Bid maxBid = null;
        double unitPrice = 0.0;
		
        List<Integer> availableLinks = new ArrayList<>();
        Map<Integer, Double> minAmounts = new HashMap<Integer, Double>();
        for (BidInfo bi : allBids) {
            if (bi.owner == null) {
		
		// Check if connecting edge to owned edge exists
		if (ownedCities.contains(bi.town1)) {
			availableLinks.add(bi.id);
			minAmounts.put(bi.id, bi.amount);
		}
		else {
                	availableLinks.add(bi.id);
                	minAmounts.put(bi.id, bi.amount);
		}
            }
	    if (bi.owner == "g5") {
		ownedCities.add(bi.town1);
		ownedCities.add(bi.town2);
	    }
        }
        List<Integer> noBidLinks = availableLinks;

        Map<String, Integer> numBids = new HashMap<String, Integer>();
        for(Bid pastBid : currentBids){

          // Update links that haven't been bid on
          if(noBidLinks.contains(pastBid.id1)){
            noBidLinks.remove(pastBid.id1);
          }
          if(noBidLinks.contains(pastBid.id2)){
            noBidLinks.remove(pastBid.id2);
          }

          // Update min amount
          if(pastBid.id2 == -1){
            minAmounts.replace(pastBid.id1, minAmounts.get(pastBid.id1), pastBid.amount);
          }
          else{ // Update for both links

          }

          // Determine current winning bid
          String player = pastBid.bidder;
          double distance = 1;
          double curPrice = pastBid.amount / distance;

          // Count bids to see if players dropped out
          if(!numBids.containsKey(player)){
            numBids.put(player, 1);
          }
          else{
            int num = numBids.get(player);
            numBids.replace(player, num, num+1);
          }
        }

        int pastRounds = 0;
        if (numBids.containsKey("g5")){
          pastRounds = numBids.get("g5");
        }
        // A player could have made 1 less bid than us but still be in the running
        for (String key : numBids.keySet()){
          int playerBids = numBids.get(key);
          if (playerBids > pastRounds - 2){
            uncontested = false;
          }
        }


        if(uncontested){
          System.out.println("We are uncontested!");
          // Find links we'd like to buy at that price, or pairs of links

          // And buy the most valuable one
        }

        // If there are links without any bids
        if(noBidLinks.size() != 0){
          // Choose one and make a minimum bid
          int linkId = noBidLinks.get(rand.nextInt(noBidLinks.size()));

          Bid bid = new Bid();
          bid.id1 = linkId;
          bid.amount = minAmounts.get(linkId);
          return bid;
        }

        // If there are no rails we'd like to purchase at the highest price / distance
        // Drop out


        // Increment bid on a valuable enough link
        for (int linkId : availableLinks){
          if(railValues.get(linkId)*10 > minAmounts.get(linkId) + 10000){
            Bid bid = new Bid();
            bid.id1 = linkId;
            bid.amount = minAmounts.get(linkId) + 10000;
            return bid;
          }


        }


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
