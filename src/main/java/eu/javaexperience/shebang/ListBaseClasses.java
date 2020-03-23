package eu.javaexperience.shebang;

import java.util.regex.Pattern;

public class ListBaseClasses
{
	public static void main(String[] args) throws Throwable
	{
		Pattern exclude = Pattern.compile("\\$\\d+$");
		__MultiMap<String, String> clss = JavaShebang.collectAvailableClasses();
		for(String cls:clss.values())
		{
			if(!exclude.matcher(cls).find())
			{
				System.out.println(cls);
			}
		}
	}
}
