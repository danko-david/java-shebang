package eu.javaexperience.shebang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * This class is a blob of some call from the Javaexperience-core library.
 * I do this way because i have to avoid the multiple appearance of the jvc-core
 * package to can use scripts with jvx-core dependency
 * (If a class appears at least twice the automatic import mechanism fails)
 * */
public class ShebangTools
{
	public static String getFirstBetween(String src, String before, String after, String _default)
	{
		int min = src.indexOf(before);
		if(min < 0)
		{
			return _default;
		}
		min+= before.length();
		
		int max = src.indexOf(after, min);
		if(max < 0)
		{
			return _default;
		}
		
		return src.substring(min, max);
	}
	
	public static String getSubstringAfterFirstString(String src, String search, String _default)
	{
		int last = src.indexOf(search);
		if(last == -1)
		{
			return _default;
		}
		return src.substring(last+search.length());
	}
	
	public static String getFileContents(String string) throws FileNotFoundException, IOException
	{
		try(FileInputStream fis = new FileInputStream(string))
		{
			return new String(loadAllFromInputStream(fis));
		}
	}
	
	public static byte[] loadAllFromInputStream(InputStream is) throws IOException
	{
		int ep = 0;
		int read = 0;
		byte[] ret = new byte[4096];
		
		while((read = is.read(ret, ep, ret.length-ep))>0)
		{
			if(ep + read == ret.length)
				ret = Arrays.copyOf(ret, ret.length*2);
			
			ep+= read;
		}

		return Arrays.copyOf(ret, ep);
	}
	
	public static int occurrenceIn(String subject, String search)
	{
		if(search.length() < 1)
		{
			return -1;
		}
		
		int occ = 0;
		int start = 0;
		do
		{
			int index = subject.indexOf(search, start);
			if(index < 0)
			{
				return occ;
			}
			
			++occ;
			start = index+search.length();
		}
		while(true);
	}
	
	public static String join(String delimiter, String... strArray)
	{
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<strArray.length;++i)
		{
			if(0 != i)
				sb.append(delimiter);
			
			sb.append(strArray[i]);
		}
		
		return sb.toString();
	}
	
	public static void propagateAnyway(Throwable t)
	{
		if(t instanceof InvocationTargetException)
		{
			Throwable tmp = ((InvocationTargetException)t).getCause();
			if(null != tmp)
			{
				t = tmp;
			}
		}
		
		if(t instanceof RuntimeException)
		{
			throw (RuntimeException) t;
		}
		else if(t instanceof Error)
		{
			throw (Error) t;
		}
		
		throw new RuntimeException(t);
	}
	
	public static Integer tryParseInt(String val)
	{
		try
		{
			return Integer.parseInt(val);
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public static String getSubstringBeforeFirstString(String src, String search, String _default)
	{
		int last = src.indexOf(search);
		if(last == -1)
		{
			return _default;
		}
		
		return src.substring(0,last);
	}
	
	public static String getSubstringAfterLastString(String src, String search, String _default)
	{
		int last = src.lastIndexOf(search);
		if(last == -1)
		{
			return _default;
		}
		
		return src.substring(last+search.length());
	}
	
	public static void find(File root, Consumer<File> res)
	{
		res.accept(root);
		if(root.isDirectory())
		{
			for(File f:root.listFiles())
			{
				find(f,res);
			}
		}
	}
	
	public static final Pattern SLASHES_LINUX_WINDOWS = Pattern.compile("[/\\\\]+");
	
	public static String normalizeSlashes(String file)
	{
		return SLASHES_LINUX_WINDOWS.matcher(file).replaceAll("/");
	}
}
