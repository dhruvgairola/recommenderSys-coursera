package com.dhruvg.lenskit.core;

import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Hello world!
 */
public class ProgAssignment1
{
	private static final String RATINGS_FILE = "prog_assignment_1/recsys-data-ratings.csv";
	private static final int[] MY_MOVIES = { 114, 602, 558 };
	private static final int[] TESTING = { 11, 121, 8587 };
	private static final int NO_TOP_MOVIES = 5;

	public static void main(String[] args) throws IOException
	{
		new ProgAssignment1(NO_TOP_MOVIES, RATINGS_FILE, MY_MOVIES);
	}

	public ProgAssignment1(int numTopMovies, String ratingsFile, int[] comparisonMovies) throws IOException
	{
		Multimap<String, Movie> userToMovies = getUserToMovieMap(ratingsFile);
		Set<Movie> cmpMovies = getMovieSet(comparisonMovies);
		Map<Integer, Integer> formula_x = new HashMap<Integer, Integer>();
		Map<Integer, Integer> formula_not_x = new HashMap<Integer, Integer>();
		Map<Integer, HashMap<Integer, Integer>> formula_x_and_y = new HashMap<Integer, HashMap<Integer, Integer>>();
		Map<Integer, HashMap<Integer, Integer>> formula_not_x_and_y =
		        new HashMap<Integer, HashMap<Integer, Integer>>();

		useSimpleFormula(formula_x, formula_x_and_y, userToMovies, cmpMovies);
		printSimpleFormula(formula_x, formula_x_and_y, cmpMovies, numTopMovies);
		useAdvancedFormula(formula_not_x, formula_not_x_and_y, userToMovies, cmpMovies);
		printAdvancedFormula(formula_not_x, formula_not_x_and_y, formula_x, formula_x_and_y, cmpMovies, numTopMovies);

	}

	private void printAdvancedFormula(Map<Integer, Integer> formula_not_x,
	        Map<Integer, HashMap<Integer, Integer>> formula_not_x_and_y, Map<Integer, Integer> formula_x,
	        Map<Integer, HashMap<Integer, Integer>> formula_x_and_y, Set<Movie> cmpMovies, int numTopMovies)
	{
		for (Movie f : cmpMovies)
		{
			PriorityQueue<Float[]> q = getAdvancedSortedTuples(formula_x_and_y, formula_x, formula_not_x_and_y, formula_not_x, f);
			StringBuilder sb = new StringBuilder();
			sb.append(f.getMovieId());

			q.poll(); // discard first one cos its equal to the cmp movie

			for (int i = 0; i < numTopMovies; i++)
			{
				Float[] tuple = q.poll();
				sb.append("," + tuple[0].intValue() + "," + String.format("%.3g", tuple[1]));
			}

			System.out.println(sb.toString());
		}
	}

	private PriorityQueue<Float[]> getAdvancedSortedTuples(Map<Integer, HashMap<Integer, Integer>> formula_x_and_y,
	        Map<Integer, Integer> formula_x, Map<Integer, HashMap<Integer, Integer>> formula_not_x_and_y,
	        Map<Integer, Integer> formula_not_x, Movie f)
	{
		PriorityQueue<Float[]> topSortedMovies = new PriorityQueue<Float[]>(10000, new Comparator<Float[]>() {

			@Override
			public int compare(Float[] o1, Float[] o2)
			{
				if (o1[1] > o2[1]) return -1;
				else if (o1[1] < o2[1]) return 1;
				return 0;
			}
		});

		for (int movieId : formula_x_and_y.keySet())
		{
			HashMap<Integer, Integer> m = formula_x_and_y.get(movieId);
			HashMap<Integer, Integer> m2 = formula_not_x_and_y.get(movieId);


			float fraction = (float) (((float) m.get(f.getMovieId()) + 0.00001) / formula_x.get(f.getMovieId()));
			float fraction2 = 1.0f;
			if (m2.containsKey(f.getMovieId()))
			{
				fraction2 = (float) (((float) m2.get(f.getMovieId()) + 0.00001) / formula_not_x.get(f.getMovieId()));
			} else
			{
				fraction2 = (float) ((0.00001) / (5564 - formula_x.get(f.getMovieId())));
			}


			Float[] tuple = new Float[2];
			tuple[0] = (float) movieId;
			tuple[1] = fraction / fraction2;

			topSortedMovies.add(tuple);
		}

		return topSortedMovies;
	}

	private void printSimpleFormula(Map<Integer, Integer> formula_x, Map<Integer, HashMap<Integer, Integer>> formula_x_and_y,
	        Set<Movie> cmpMovies, int numTopMovies)
	{
		for (Movie f : cmpMovies)
		{
			PriorityQueue<Float[]> q = getSimpleSortedTuples(formula_x_and_y, formula_x, f);
			StringBuilder sb = new StringBuilder();
			sb.append(f.getMovieId());

			q.poll(); // discard first one cos its equal to the cmp movie

			for (int i = 0; i < numTopMovies; i++)
			{
				Float[] tuple = q.poll();
				sb.append("," + tuple[0].intValue() + "," + String.format("%.2g", tuple[1]));
			}

			System.out.println(sb.toString());
		}
	}

	private void useAdvancedFormula(Map<Integer, Integer> formula_not_x,
	        Map<Integer, HashMap<Integer, Integer>> formula_not_x_and_y, Multimap<String, Movie> userToMovies,
	        Set<Movie> cmpMovies)
	{
		for (String userId : userToMovies.keySet())
		{
			List<Movie> movies = (List<Movie>) userToMovies.get(userId);

			for (Movie f : cmpMovies)
			{
				if (!movies.contains(f))
				{
					if (formula_not_x.containsKey(f.getMovieId()))
					{
						formula_not_x.put(f.getMovieId(), formula_not_x.get(f.getMovieId()) + 1);
					} else
					{
						formula_not_x.put(f.getMovieId(), 1);
					}

					for (Movie movie : movies)
					{
						if (!formula_not_x_and_y.containsKey(movie.getMovieId()))
						{
							formula_not_x_and_y.put(movie.getMovieId(), new HashMap<Integer, Integer>());
						}

						Map<Integer, Integer> m = formula_not_x_and_y.get(movie.getMovieId());

						if (m.containsKey(f.getMovieId()))
						{
							m.put(f.getMovieId(), m.get(f.getMovieId()) + 1);
						} else
						{
							m.put(f.getMovieId(), 1);
						}

					}
				}
			}

		}
	}

	private void useSimpleFormula(Map<Integer, Integer> formula_x, Map<Integer, HashMap<Integer, Integer>> formula_x_and_y,
	        Multimap<String, Movie> userToMovies, Set<Movie> cmpMovies)
    {
		for (String userId : userToMovies.keySet())
		{
			List<Movie> movies = (List<Movie>) userToMovies.get(userId);

			for (Movie f : cmpMovies)
			{
				if (movies.contains(f))
				{
					if (formula_x.containsKey(f.getMovieId()))
					{
						formula_x.put(f.getMovieId(), formula_x.get(f.getMovieId()) + 1);
					} else
					{
						formula_x.put(f.getMovieId(), 1);
					}

					for (Movie movie : movies)
					{
						if (!formula_x_and_y.containsKey(movie.getMovieId()))
						{
							formula_x_and_y.put(movie.getMovieId(), new HashMap<Integer, Integer>());
						}

						Map<Integer, Integer> m = formula_x_and_y.get(movie.getMovieId());

						if (m.containsKey(f.getMovieId()))
						{
							m.put(f.getMovieId(), m.get(f.getMovieId()) + 1);
						} else
						{
							m.put(f.getMovieId(), 1);
						}

					}
				}
			}

		}
    }

	private PriorityQueue<Float[]> getSimpleSortedTuples(Map<Integer, HashMap<Integer, Integer>> formula_x_and_y,
	        Map<Integer, Integer> formula_x, Movie f)
    {
		PriorityQueue<Float[]> topSortedMovies = new PriorityQueue<Float[]>(10000, new Comparator<Float[]>() {

			@Override
			public int compare(Float[] o1, Float[] o2)
			{
				if (o1[1] > o2[1]) return -1;
				else if (o1[1] < o2[1]) return 1;
				return 0;
			}
		});
		
		for (int movieId : formula_x_and_y.keySet())
		{
			HashMap<Integer, Integer> m = formula_x_and_y.get(movieId);
			
			float fraction = (float) (((float) m.get(f.getMovieId()) + 0.00001) / formula_x.get(f.getMovieId()));
			
			Float[] tuple = new Float[2];
			tuple[0] = (float) movieId;
			tuple[1] = fraction;

			topSortedMovies.add(tuple);
		}

		return topSortedMovies;
    }

	private Set<Movie> getMovieSet(int[] arr)
	{
		Set<Movie> s = new HashSet<Movie>();

		for (int a : arr)
		{
			Movie m = new Movie();
			m.setMovieId(a);
			s.add(m);
		}

		return s;
	}

	private Multimap<String, Movie> getUserToMovieMap(String file) throws IOException
	{
		CSVReader reader = new CSVReader(new FileReader(RATINGS_FILE));
		Multimap<String, Movie> userToMovies = ArrayListMultimap.create();

		String[] nextLine;
		while ((nextLine = reader.readNext()) != null)
		{
			Movie m = new Movie();
			m.setMovieId(Integer.parseInt(nextLine[1]));
			m.setRating(Float.parseFloat(nextLine[2]));

			userToMovies.put(nextLine[0], m);
		}

		return userToMovies;
	}

	public class Movie
	{
		private int movieId;
		private float rating;

		public int getMovieId()
		{
			return movieId;
		}

		public void setMovieId(int movieId)
		{
			this.movieId = movieId;
		}

		public float getRating()
		{
			return rating;
		}

		public void setRating(float rating)
		{
			this.rating = rating;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + movieId;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Movie other = (Movie) obj;
			if (!getOuterType().equals(other.getOuterType())) return false;
			if (movieId != other.movieId) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "movie id is " + movieId;
		}

		private ProgAssignment1 getOuterType()
		{
			return ProgAssignment1.this;
		}
	}
}
