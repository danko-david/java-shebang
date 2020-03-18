#!/usr/bin/env jsb

/**
 * @maven-dependency http://maven.javaexperience.eu/:javaexperience:teasite:1.3.8
 */

public class TeasiteCompilation
{
	public static void main(String... args) throws Throwable
	{
		String targetDir = "./teasite_compilation/";
		
		TeaVmWebCompiler compiler = new TeaVmWebCompiler(TeasiteCompilationApp.class, new File(targetDir));
		compiler.compile(true);
		compiler.emitMergedOutput(targetDir+"/teasite_out.js");
		try(PrintWriter pw = new PrintWriter(targetDir+"/index.html"))
		{
			List<String> css = CollectionTools.inlineArrayList
			(
				"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
			);
			List<String> preJs = CollectionTools.inlineArrayList
			(
				"https://code.jquery.com/jquery-2.2.4.min.js",
				"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
			);
			
			List<String> postJs = new OneShotList<>("./teasite_out.js");
			TeasiteBackendTools.renderBootPage(pw, css, preJs, postJs, null);
			pw.flush();
		}
	}
	
	public static class TeasiteCompilationApp
	{
		public static void main(String[] args) throws Throwable
		{
			NativeJsSupportTeaVM.init();
			
			H root = BaseLayout.getDesignedRoot();
			
			H header = new H(VanillaTools.getDom().querySelector(".site-header"));
			H links = new H("div").attrs("style", "padding-bottom:20px");
			
			header.addChilds
			(
				new H("h1").attrs("style", "padding: 20px", "#text", "Teasite Compilation Test"),
				links
			);
		}
	}
}
