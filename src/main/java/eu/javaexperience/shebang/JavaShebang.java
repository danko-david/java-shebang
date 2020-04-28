package eu.javaexperience.shebang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.mdkt.compiler.CustomInMemoryJavaCompiler;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;

import com.tobedevoured.naether.impl.NaetherImpl;

import java.net.URLClassLoader;

import static eu.javaexperience.shebang.ShebangTools.*;

/**
 * TODO features:
 * 	- add maven dependency (with repository)
 * 		mvn dependency:get -Dartifact=groupId:artifactId:version
 * 			-DrepoUrl=...
 * 		https://stackoverflow.com/questions/8563960/maven-command-to-update-repository-after-adding-dependency-to-pom
 * 
 *  - add source directory
 *  //- auto import class
 *  - specify reference directory
 *  - opt: incomplete (wrap as a class with main to compile)
 * 
 * 
 * */
public class JavaShebang
{
	protected static final OpenURLClassLoader CLASS_LOADER = new OpenURLClassLoader(ClassLoader.getSystemClassLoader());
	
	protected static boolean MISSING_RT_JAR = false;
	
	protected static boolean LOG_PREPARATION_TIME = null != System.getenv("JSB_VERBOSE_TIMES");
	
	public static List<String> collectClassFilesystems()
	{
		List<String> ret = new ArrayList<>();
		
		Consumer<String> add = new Consumer<String>()
		{
			@Override
			public void accept(String f)
			{
				if(new File(f).exists())
				{
					ret.add(f.toString());
				}
			}
		};
		
		URL[] urls = ((URLClassLoader)CLASS_LOADER).getURLs();
		for(URL url: urls)
		{
			add.accept(url.getFile());
		}
		
		String value = System.getProperty("sun.boot.class.path");
		if(null != value)
		{
			for(String f:value.split(":"))
			{
				add.accept(f);
			}
		}
		else
		{
			/**
			 * Java 11 doesn't have this boot path...
			 * And i found no way list them.
			 * So i use the backed in list of available classes
			 * This is basically bad, because over time the list base classes
			 * may modify... But this also faster
			 */
			MISSING_RT_JAR = true;
		}
		
		return ret;
	}
	
	public static __MultiMap<String, String> collectAvailableClasses() throws IOException
	{
		__MultiMap<String, String> ret = new __MultiMap<>();
		List<String> fss = collectClassFilesystems();
		
		Consumer<String> acceptClass = new Consumer<String>()
		{
			@Override
			public void accept(String name)
			{
				name = getSubstringBeforeFirstString(name, ".class", name);
				if(null != name)
				{
					name = name.replace('/', '.');
					if(name.startsWith("."))
					{
						name = getSubstringAfterFirstString(name, ".", name);
					}
				}
				else
				{
					return;
				}
				
				String shortName = null;
				if(name.contains("$"))
				{
					shortName = getSubstringAfterLastString(name, "$", name);
					if(null != tryParseInt(shortName))
					{
						return;
					}
				}
				else
				{
					shortName = getSubstringAfterLastString(name, ".", name);
				}
				ret.put(shortName, name);
			}
		};
		
		if(MISSING_RT_JAR)
		{
			try(InputStream is = ClassLoader.getSystemResourceAsStream("eu/javaexperience/shebang/shebang-fallback-base-classes.lst"))
			{
				for(String cls:new String(ShebangTools.loadAllFromInputStream(is)).split("\n"))
				{
					acceptClass.accept(cls);
				}
			}
		}
		
		for(String fs:fss)
		{
			File f = new File(fs);
			if(f.isDirectory())
			{
				find
				(
					f,
					(Consumer<File>) (a)->
					{
						if(a.isFile())
						{
							String name = a.toString();
							name = getSubstringAfterFirstString(name, fs, name);
							if(name.endsWith(".class"))
							{
								acceptClass.accept(name);
							}
						}
					}
				);
			}
			else if(f.toString().endsWith(".jar"))
			{
				try(ZipFile zip = new ZipFile(f))
				{
					for
					(
						Enumeration<? extends ZipEntry> ents = zip.entries();
						ents.hasMoreElements();
					)
					{
						ZipEntry e = ents.nextElement();
						String name = e.getName();
						if(name.endsWith(".class"))
						{
							acceptClass.accept(name);
						}
					}
				}
			}
		}
		
		return ret;
	}
	
	protected static String loadInstructionComments(String source)
	{
		//TODO check is it located before class definition
		return getFirstBetween(source, "/*", "*/", null);
	}
	
	protected static String getLocalDependencyFile(Dependency dep)
	{
		Artifact art = dep.getArtifact();
		
		return	System.getProperty("user.home")+
				"/.m2/repository/"+
				art.getGroupId().replace('.', '/')+"/"+
				art.getArtifactId()+"/"+
				art.getVersion()+"/"+
				art.getArtifactId()+"-"+art.getVersion()+".jar"
		;
	}
	
	public static void addJarToClassPath(String path) throws MalformedURLException
	{
		CLASS_LOADER.addURL(new File(path).toURI().toURL());
		System.setProperty("java.class.path", System.getProperty("java.class.path")+":"+path);
	}
	
	protected static void addMavenDependencies(Set<String> deps) throws Exception
	{
		TIME_CHECKPOINT = System.currentTimeMillis();
		NaetherImpl naether = new NaetherImpl();
		
		for(String dep:deps)
		{
			if(occurrenceIn(dep, ":") > 2)
			{
				String[] parts = dep.split(":");
				int l = parts.length;
				dep = parts[l-3]+":"+parts[l-2]+":"+parts[l-1];
				
				naether.addRemoteRepositoryByUrl(join(":", Arrays.copyOf(parts, l-3)));
			}
			
			try
			{
				naether.addDependency(dep);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: before dependencies resolved tooks: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
		}
		
		try
		{
			naether.resolveDependencies();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: after dependencies resolved tooks: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
			TIME_CHECKPOINT = System.currentTimeMillis();
		}
		
		Collection<Dependency> ds = naether.currentDependencies();
		for(Dependency d:ds)
		{
			//TODO log System.out.println(d);
			addJarToClassPath(getLocalDependencyFile(d));
		}
		
		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: add dependencies ("+ds.size()+"): "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
			TIME_CHECKPOINT = System.currentTimeMillis();
		}
	}
	
	public static final Pattern NEW_LINE = Pattern.compile("(\r\n|\r|\n)");
	
	protected static boolean AUTO_IMPORT = true;
	
	protected static void processInstructions(String instr) throws Exception
	{
		if(null == instr)
		{
			return;
		}
		
		Set<String> deps = new HashSet<>();
		
		for(String line:NEW_LINE.split(instr))
		{
			line = line.trim();
			line = line.replaceAll("^\\*", "");
			line = line.trim();
			if(line.startsWith("@maven-dependency"))
			{
				String dep = getSubstringAfterFirstString(line, "@maven-dependency", line);
				dep = dep.trim();
				//"@maven-dependency:" is also allowed (not the : at the end)
				if(dep.startsWith(":"))
				{
					dep = dep.substring(1);
					dep = dep.trim();
				}
				deps.add(dep);
			}
			else if(line.startsWith("@autoimport-off"))
			{
				AUTO_IMPORT = false;
			}
			
		}
		
		addMavenDependencies(deps);
	}
	
	protected static long TIME_START = System.currentTimeMillis();
	protected static long TIME_CHECKPOINT;
	
	public static void main(String[] args) throws Throwable
	{
		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: enter jsb main "+(System.currentTimeMillis()-TIME_START)+" ms");
			TIME_CHECKPOINT = System.currentTimeMillis();
		}
		
		if(args.length < 1)
		{
			System.err.println("Specify a script file");
			System.exit(1);
		}
		
		//feed the file and parameters
		String content = getFileContents(args[0]);
		
		String instComments = loadInstructionComments(content);
		
		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: load file tooks: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
			TIME_CHECKPOINT = System.currentTimeMillis();
		}
		
		processInstructions(instComments);
		
		//remove the shebang
		String src = content;
		if(content.startsWith("#!"))
		{
			src = getSubstringAfterFirstString(content, "\n", content);
		}
		
		if(null == src)
		{
			System.err.println("Can't read the source to compile.");
			System.exit(2);
		}
		
		compileAndRun(src, Arrays.copyOfRange(args, 1, args.length));
	}
	
	public static Set<String> getReferencedClasses(String src)
	{
		//ways simpler and faster than using AST 
		Set<String> ret = new HashSet<>();
		Pattern p = Pattern.compile("[a-zA-Z_$][a-zA-Z_$0-9]*", Pattern.MULTILINE);
		Matcher m = p.matcher(src);
		while(m.find())
		{
			ret.add(m.group());
		}
		
		return ret;
	}
	
	protected static boolean isClassAvailable(String name)
	{
		try
		{
			//Don't initialise the class it might fail (eg for Teasite classes) and
			//it also increases the time of run and memory usage
			Class c = Class.forName(name, false, CLASS_LOADER);
			return Modifier.isPublic(c.getModifiers());
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static List<String> getAccessibleClasses(List<String> clss)
	{
		List<String> ret = new ArrayList<>();
		for(String c:clss)
		{
			if(isClassAvailable(c))
			{
				ret.add(c);
			}
		}
		return ret;
	}
	
	protected static String selectPreferredPackageClass(String cls, List<String> classes, String... preference)
	{
		for(String p:preference)
		{
			for(String c:classes)
			{
				if(c.startsWith(p))
				{
					return c;
				}
			}
		}
		
		return null;
	}
	
	protected static void appendImport(StringBuilder sb, String cls)
	{
		sb.append("import ");
		sb.append(cls.replace('$', '.'));
		sb.append(";");
	}
	
	public static void checkAndAddAutoImport
	(
		StringBuilder sb,
		__MultiMap<String, String> knownClasses,
		Set<String> reqClasses,
		Set<String> nonResolv
	)
	{
		//check can load
		//check public class
		
		for(String req:reqClasses)
		{
			if(nonResolv.contains(req))
			{
				continue;
			}
			
			List<String> k = knownClasses.getList(req);
			if(null != k)
			{
				k = getAccessibleClasses(k);
				if(k.size() == 0)
				{
					System.err.println("No variant of `"+req+"` found");
				}
				else if(k.size() == 1)
				{
					appendImport(sb, k.get(0));
				}
				else
				{
					String pref = selectPreferredPackageClass(req, k, "java.lang.", "java.util.", "java.", "javax.");
					if(null != pref)
					{
						appendImport(sb, pref);
						continue;
					}
					
					System.err.println("Multiple public variant of `"+req+"` is availabe `"+k+"`. Specify with import which one you like to use.");
				}
			}
		}
	}
	
	public static Class compileClass(String className, String content) throws Exception
	{
		TIME_CHECKPOINT = System.currentTimeMillis();
		CustomInMemoryJavaCompiler compiler = new CustomInMemoryJavaCompiler().useParentClassLoader(CLASS_LOADER).ignoreWarnings();
		//make compiled classed available for the current context
		Thread.currentThread().setContextClassLoader(compiler.getClassloader());
		try
		{
			return compiler.compile(className, content);
		}
		finally
		{
			if(LOG_PREPARATION_TIME)
			{
				System.err.println("JSB: class compilation tooks: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
				TIME_CHECKPOINT = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * not a complete, but heuristic solution. AST would be better, see this:
	 *  String text = "example / * * / text"
	 */
	protected static String stripComments(String source)
	{
		source = Pattern.compile("/\\*.*?\\*/", Pattern.MULTILINE | Pattern.DOTALL).matcher(source).replaceAll("");
		source = source.replaceAll("//.*$", "");
		return source;
	}
	
	protected static String findClassName(String strippedSource)
	{
		Pattern p = Pattern.compile("class\\s+(?<cls>[^\\s]+)\\s+", Pattern.MULTILINE);
		Matcher m = p.matcher(strippedSource);
		if(m.find())
		{
			return m.group("cls");
		}
		
		System.err.println("No class name found");
		System.exit(3);
		return null;//keep compiler happy
	}
	
	public static void compileAndRun(String src, String[] args) throws Exception
	{
		TIME_CHECKPOINT = System.currentTimeMillis();
		
		String stripSource = stripComments(src);
		String clsName = findClassName(stripSource);
		
		StringBuilder sb = new StringBuilder();

		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: prepare source tooks: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
			TIME_CHECKPOINT = System.currentTimeMillis();
		}
		
		//TODO exclude import with exact names
		if(AUTO_IMPORT)
		{
			TIME_CHECKPOINT = System.currentTimeMillis();
			
			Set<String> reqClasses = getReferencedClasses(stripSource);
			Set<String> selectedClasses = new HashSet<>();
			__MultiMap<String, String> knownClasses = collectAvailableClasses();
			
			checkAndAddAutoImport(sb, knownClasses, reqClasses, selectedClasses);
			
			if(LOG_PREPARATION_TIME)
			{
				System.err.println("JSB: auto import tooks: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
				TIME_CHECKPOINT = System.currentTimeMillis();
			}
		}
		
		sb.append("\n");
		sb.append(src);
		
		Class c = compileClass(clsName, sb.toString());
		
		Method main = c.getDeclaredMethod("main", new Class[] {String[].class});
		if(null == main)
		{
			System.err.println("No main method found");
			System.exit(4);
		}
		
		if(LOG_PREPARATION_TIME)
		{
			System.err.println("JSB: before invoke `main`: "+(System.currentTimeMillis()-TIME_CHECKPOINT)+" ms, total: "+(System.currentTimeMillis()-TIME_START));
			TIME_CHECKPOINT = System.currentTimeMillis();
		}
		main.invoke(null, new Object[] {args});
	}

}
