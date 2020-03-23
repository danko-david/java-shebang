package eu.javaexperience.shebang;

import java.net.URL;
import java.net.URLClassLoader;

public class OpenURLClassLoader extends URLClassLoader
{
	public OpenURLClassLoader(ClassLoader parent)
	{
		super(new URL[0], parent);
	}
	
	@Override
	public void addURL(URL url)
	{
		super.addURL(url);
	}
}
