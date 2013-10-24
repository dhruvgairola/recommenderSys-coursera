package edu.umn.cs.recsys.uu;

import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.ItemEventDAO;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;

/**
 * User-user item scorer.
 * 
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer
{
	private final UserEventDAO userDao;
	private final ItemEventDAO itemDao;
	public static final int TOP_N_USERS = 30;

	@Inject
	public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao)
	{
		userDao = udao;
		itemDao = idao;
	}

	@Override
	public void score(long user, @Nonnull MutableSparseVector scores)
	{
		SparseVector userVector = getUserRatingVector(user);
		List<Long> itemSet = new ArrayList<Long>(userVector.keySet());

		for (long k : scores.keyDomain())
		{
			if (!userVector.containsKey(k))
			{
				itemSet.add(k);
			}
		}

		// Score items for this user using user-user collaborative filtering

		PriorityQueue<SimilarUser> neighboursHeap = new PriorityQueue<SimilarUser>(TOP_N_USERS, new Comparator<SimilarUser>() {

			@Override
			public int compare(SimilarUser o1, SimilarUser o2)
			{
				Double sim1 = o1.getSimilarity();
				Double sim2 = o2.getSimilarity();
				return -1 * sim1.compareTo(sim2);
			}
		});

		MutableSparseVector mutableUserVector = userVector.mutableCopy();
		mutableUserVector.add(-1 * mutableUserVector.mean());

		for (long itemId : itemSet)
		{
			LongSet users = itemDao.getUsersForItem(itemId);

			for (long userId : users)
			{
				CosineVectorSimilarity sim = new CosineVectorSimilarity();
				SimilarUser simUser = new SimilarUser();
				SparseVector simUserVector = getUserRatingVector(userId);
				MutableSparseVector mutableSimUserVector = simUserVector.mutableCopy();
				mutableSimUserVector.add(-1 * mutableSimUserVector.mean());

				double similarity = sim.similarity(mutableUserVector, mutableSimUserVector);
				simUser.setSimilarity(similarity);
				simUser.setUserId(userId);
				simUser.setUserVector(simUserVector);
				neighboursHeap.offer(simUser);
			}

			int numCount = 0;
			List<SimilarUser> topN = new ArrayList<SimilarUser>(TOP_N_USERS);

			SimilarUser su = neighboursHeap.poll(); // The closest neighbour is itself, so pop it.

			if (su.similarity != 1.0d)
			{
				topN.add(su);
			}

			while (numCount < TOP_N_USERS)
			{
				if (!neighboursHeap.isEmpty())
				{
					topN.add(neighboursHeap.poll());
				}

				numCount++;
			}
			
			if (scores.keyDomain().contains(itemId))
			{
				scores.set(itemId, getItemScore(userVector, topN, itemId));
			}
			neighboursHeap.clear();
		}


	}

	private double getItemScore(SparseVector userVector, List<SimilarUser> topN, long itemId)
	{
		double numerator = 0d;
		double denominator = 0d;

		for (SimilarUser top : topN)
		{
			numerator += top.getSimilarity() * (top.getUserVector().get(itemId) - top.getUserVector().mean());
			denominator += top.getSimilarity();
		}

		return denominator == 0d ? userVector.mean() : userVector.mean() + numerator / denominator;
	}

	public class SimilarUser
	{
		private double similarity;
		private long userId;
		private SparseVector userVector;

		public double getSimilarity()
		{
			return similarity;
		}

		public void setSimilarity(double similarity)
		{
			this.similarity = similarity;
		}

		public long getUserId()
		{
			return userId;
		}

		public void setUserId(long userId)
		{
			this.userId = userId;
		}

		public SparseVector getUserVector()
		{
			return userVector;
		}

		public void setUserVector(SparseVector userVector)
		{
			this.userVector = userVector;
		}

	}

	/**
	 * Get a user's rating vector.
	 * 
	 * @param user
	 *            The user ID.
	 * @return The rating vector.
	 */
	private SparseVector getUserRatingVector(long user)
	{
		UserHistory<Rating> history = userDao.getEventsForUser(user, Rating.class);
		if (history == null)
		{
			history = History.forUser(user);
		}
		return RatingVectorUserHistorySummarizer.makeRatingVector(history);
	}
}