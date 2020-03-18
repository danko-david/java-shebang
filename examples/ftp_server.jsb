#!/usr/bin/env jsb

/**
 * @maven-dependency org.apache.ftpserver:ftpserver-core:1.1.1
 */

public class AdhocFtpServer
{
	public static void main(String... args) throws Throwable
	{
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();

		// set the port of the listener
		factory.setPort(2121);

		// replace the default listener
		serverFactory.addListener("default", factory.createListener());
		
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		
		File usersFile = new File("./myusers.properties");
		if(!usersFile.exists())
		{
			usersFile.createNewFile();
		}
		
		userManagerFactory.setFile(usersFile);
		
		userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
		UserManager um = userManagerFactory.createUserManager();
		BaseUser user = new BaseUser();
		user.setName("user");
		user.setPassword("password");
		user.setHomeDirectory(".");
		um.save(user);
		
		serverFactory.setUserManager(um);
		
		// start the server
		FtpServer server = serverFactory.createServer();
		
		server.start();
		
	}
}
