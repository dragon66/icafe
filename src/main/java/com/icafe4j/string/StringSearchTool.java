//From Scott Robert Ladd, "Java algorithms", chap. 3
package com.icafe4j.string;

public abstract class StringSearchTool
{
	public static int NOT_FOUND = -1;
	public static int SEARCH_EXACT = 0;
	public static int SEARCH_CASELESS = 1;

	protected String pattern;
	protected int search;
	
	public StringSearchTool()
	{
		search = SEARCH_CASELESS;
		pattern = null;
	}
	
	public StringSearchTool(final String p)
	{
		search = SEARCH_CASELESS;
		setPattern(p);
	}
	
	public StringSearchTool(final String p, int type)
	{
		search = type;
		setPattern(p);
	}
	
	public synchronized String getPattern()
	{
		return pattern;
    }

	public synchronized void setPattern(final String p)
	{
		if ((search == SEARCH_CASELESS))
		{
			pattern = p.toUpperCase();
		}
		else
		//	pattern=new String(p);
		    pattern = p;
	}

	public synchronized int getPatternLength()
	{
		return pattern.length();
	}

	public int getSearchType()
	{
		return search;
	}

	public int find(final String target)
	{
		return find(target,0);
	}

	public abstract int find(final String target,int start)
		;
}