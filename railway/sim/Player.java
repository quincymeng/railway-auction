package railway.sim;

import java.util.List;
import railway.sim.utils.*;

public interface Player {
    // Initialization function.
    // name: name of current player.
    // budget: maximum amount allocated for bidding.
    // geo: Town indices and their coordinates.
    // infra: Town indices and other towns they are connected to.
    // transit: Town indices and the number of passengers between them.
    // townLookup: Town indices and names of towns.
    // allBids: list of all available bids and their minimum amount.
    public void init(String name,
        double budget,
        List<Coordinates> geo,
        List<List<Integer>> infra,
        int[][] transit,
        List<String> townLookup,
        List<BidInfo> allBids);

    // The bid placed for a round.
    // Returns null if they don't want to place a bid.
    // currentBids: bids being placed in this round in reverse order -
    //   most recent bid is first.
    // allBids: shows those bids available and bids owned by other players
    //   i.e. the results of previous rounds.
    public Bid getBid(List<Bid> currentBids, List<BidInfo> allBids, Bid lastRoundMaxBid);

    // Indicates to the player whether they have won the previous bid of link/pair of links.
    // A null bid indicates that they did not win their bid.
    public void updateBudget(Bid bid);
}
