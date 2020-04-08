package eu.javaexperience.shebang;

import java.util.regex.Pattern;

public class ListBaseClasses
{
	public static void main(String[] args) throws Throwable
	{
		//exclude listed packages (sun, oracle, etc) and anonymus classes
		Pattern exclude = Pattern.compile("(((sun|oracle|jdk)\\.)|(\\$\\d+$))");
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
