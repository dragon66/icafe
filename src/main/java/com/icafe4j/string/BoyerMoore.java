package com.icafe4j.string;
/**
 * Boyer Moore algorithm for string searching 
 * From Scott Robert Ladd, "Java algorithms", chap. 3
 * <p>
 * Changes made by Wen YU at yuwen_66@yahoo.com
 * on November 21, 2006
 * <p>
 * Only the bad character heuristics is implemented 
 * The good suffix heuristics is much more complicated
 */
public class BoyerMoore extends StringSearchTool
{
	protected int [] delta;
	private final static int DELTA_SIZE = 65536;

	public BoyerMoore()
	{
		super();
	}
	
	public BoyerMoore(final String p)
	{
		super(p);
	}

    public BoyerMoore(final String p, int type)
	{
		super(p,type);
	}

	public synchronized void setPattern(final String p)
	{
		super.setPattern(p);
		int n;
		delta = new int[DELTA_SIZE];

		for(n = 0; n < DELTA_SIZE; ++n)
			delta[n] = pattern.length();
		for(n = 1;n < pattern.length(); ++n)
		{
			delta[pattern.charAt(n-1)] = pattern.length()-n;
		}
        // The following line is useless
		delta[pattern.charAt(pattern.length()-1)] = 1;
	}

	public int find(final String target, int start)
	{
		if ((pattern == null)||(start < 0))
			return NOT_FOUND;
		
		String target2 = target;

		if (search == SEARCH_CASELESS)
		    target2 = target.toUpperCase();
		
		int t = start + pattern.length();

        while (t <= target2.length())
        {
			int p = pattern.length();
			// The following line is added by Wen YU
			int m = 0;
			while (pattern.charAt(p-1) == target2.charAt(t-1))
			{
				if(p > 1)
				{
					p--;
					t--;
					// The following line is added by Wen YU
					m++;
				}
				else return t-1;
			}
			// The if statement is added by Wen Yu to avoid back shift and related 
			// deadlock
			if (delta[target2.charAt(t-1)] <= m)
			{
				t += m + 1;
			}
			else t += delta[target2.charAt(t-1)];
		}
		return NOT_FOUND;
	}
}
