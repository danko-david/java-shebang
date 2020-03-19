#!/usr/bin/env jsb

/**
 * @maven-dependency http://maven.javaexperience.eu/:javaexperience:lxe-tools:1.2.5
 * 
 * See the SNMP tree: snmpwalk -v 1 -c public 127.0.0.1:2100 .1
 */

import eu.javaexperience.log.LogLevel;

public class SnmpDemoServer
{
	public static void main(String... args) throws Throwable
	{
		JavaExperienceLoggingFacility.setFutureDefaultLoglevel(LogLevel.DEBUG);
		JavaExperienceLoggingFacility.addStdOut();
		SnmpMibDispatch disp = new SnmpMibDispatch();
		SnmpTools.addRoAccessorToPath(disp, new int[] {1, 25, 2}, SnmpDataType.octetString, ()->"Hello World", false);
		SnmpServer server = new SnmpServer("0.0.0.0", 2100, disp);
		server.start();
	}
}
