package org.mdkt.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.DynamicClassLoader;
import org.mdkt.compiler.ExtendedStandardJavaFileManager;
import org.mdkt.compiler.SourceCode;

import eu.javaexperience.shebang.ShebangTools;

/**
 * because the constructor of the CustomInMemoryJavaCompiler is private i have to copy allllll the source of the class...
 * SOLID sucks
 * 
 * package moved to the mdkt one because constructor of the class ExtendedStandardJavaFileManager is protected
 */

public class CustomInMemoryJavaCompiler
{
	protected JavaCompiler javac;
	protected DynamicClassLoader classLoader;
	protected Iterable<String> options;
	boolean ignoreWarnings = false;

	protected Map<String, SourceCode> sourceCodes = new HashMap<String, SourceCode>();
	
	protected static class ClassResourceFile
	{
		protected CompiledCode code;
		protected int flen;
		protected String fileName;
		public ClassResourceFile(CompiledCode cc)
		{
			code = cc;
			fileName = cc.getClassName().replace('.', '/')+".class";
			flen = fileName.length();
		}
		
		public boolean fileMatch(String file)
		{
			file = ShebangTools.normalizeSlashes(file);
			int l = file.length();
			
			//exact match or an extra / at the beginning
			return file.endsWith(fileName)
				&&
				(
					l == flen
					||
					(
						l-1 == flen
						&&
						file.startsWith("/")
					)
				);
		}
	}
	
	protected static DynamicClassLoader createDynamicClassLoader(ClassLoader cl)
	{
		return new DynamicClassLoader(cl)
		{
			@Override
			public URL getResource(String name)
			{
				for(ClassResourceFile c:compiledCodes)
				{
					if(c.fileMatch(name))
					{
						try
						{
							return new File(c.fileName).toURI().toURL();
						}
						catch (MalformedURLException e)
						{
							ShebangTools.propagateAnyway(e);
						}
					}
				}
				return super.getResource(name);
			}
			
			@Override
			public InputStream getResourceAsStream(String name)
			{
				for(ClassResourceFile c:compiledCodes)
				{
					if(c.fileMatch(name))
					{
						return new ByteArrayInputStream(c.code.getByteCode());
					}
				}
				return super.getResourceAsStream(name);
			}
			
			protected List<ClassResourceFile> compiledCodes = new ArrayList<>();
			
			@Override
			public void addCode(CompiledCode cc)
			{
				compiledCodes.add(new ClassResourceFile(cc));
				super.addCode(cc);
			}
		};
	}
	
	public CustomInMemoryJavaCompiler() {
		this.javac = ToolProvider.getSystemJavaCompiler();
		this.classLoader = createDynamicClassLoader(ClassLoader.getSystemClassLoader());
	}

	public CustomInMemoryJavaCompiler useParentClassLoader(ClassLoader parent) {
		this.classLoader = createDynamicClassLoader(parent);
		return this;
	}

	/**
	 * @return the class loader used internally by the compiler
	 */
	public ClassLoader getClassloader() {
		return classLoader;
	}

	/**
	 * Options used by the compiler, e.g. '-Xlint:unchecked'.
	 *
	 * @param options
	 * @return
	 */
	public CustomInMemoryJavaCompiler useOptions(String... options) {
		this.options = Arrays.asList(options);
		return this;
	}

	/**
	 * Ignore non-critical compiler output, like unchecked/unsafe operation
	 * warnings.
	 *
	 * @return
	 */
	public CustomInMemoryJavaCompiler ignoreWarnings() {
		ignoreWarnings = true;
		return this;
	}
	
	protected ExtendedStandardJavaFileManager fileManager;
	
	public void getCompiledFiles()
	{
	}
	
	/**
	 * Compile all sources
	 *
	 * @return Map containing instances of all compiled classes
	 * @throws Exception
	 */
	public Map<String, Class<?>> compileAll() throws Exception {
		if (sourceCodes.size() == 0) {
			throw new CompilationException("No source code to compile");
		}
		Collection<SourceCode> compilationUnits = sourceCodes.values();
		CompiledCode[] code;

		code = new CompiledCode[compilationUnits.size()];
		Iterator<SourceCode> iter = compilationUnits.iterator();
		for (int i = 0; i < code.length; i++) {
			code[i] = new CompiledCode(iter.next().getClassName());
		}
		DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
		fileManager = new ExtendedStandardJavaFileManager(javac.getStandardFileManager(null, null, null), classLoader);
		JavaCompiler.CompilationTask task = javac.getTask(null, fileManager, collector, options, null, compilationUnits);
		boolean result = task.call();
		if (!result || collector.getDiagnostics().size() > 0) {
			StringBuffer exceptionMsg = new StringBuffer();
			exceptionMsg.append("Unable to compile the source");
			boolean hasWarnings = false;
			boolean hasErrors = false;
			for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
				switch (d.getKind()) {
				case NOTE:
				case MANDATORY_WARNING:
				case WARNING:
					hasWarnings = true;
					break;
				case OTHER:
				case ERROR:
				default:
					hasErrors = true;
					break;
				}
				exceptionMsg.append("\n").append("[kind=").append(d.getKind());
				exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
				exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append("]");
			}
			if (hasWarnings && !ignoreWarnings || hasErrors) {
				throw new CompilationException(exceptionMsg.toString());
			}
		}

		Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
		for (String className : sourceCodes.keySet()) {
			classes.put(className, classLoader.loadClass(className));
		}
		return classes;
	}

	/**
	 * Compile single source
	 *
	 * @param className
	 * @param sourceCode
	 * @return
	 * @throws Exception
	 */
	public Class<?> compile(String className, String sourceCode) throws Exception {
		return addSource(className, sourceCode).compileAll().get(className);
	}

	/**
	 * Add source code to the compiler
	 *
	 * @param className
	 * @param sourceCode
	 * @return
	 * @throws Exception
	 * @see {@link #compileAll()}
	 */
	public CustomInMemoryJavaCompiler addSource(String className, String sourceCode) throws Exception {
		sourceCodes.put(className, new SourceCode(className, sourceCode));
		return this;
	}
}
