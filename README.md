# JavaShebang 

Executes java source like shell scripts:

```java

#!/usr/bin/env jsb

/**
 * Boot class example wiath a dependency from a custom maven repository.
 * @maven-dependency http://maven.javaexperience.eu/:javaexperience:rpc:1.3.2
 */

public class RpcDemoServer
{
	public static void main(String... args) throws Throwable
	{
		ExampleRpcServer.main(args);
	}
}

```

---

## Is it offers someting more than Java 11's single file source code execution?

Eg.: java HelloWorld.java which compiles and runs a single class file. 

Yes it offers 3 extra features:

### Direct execution

Using the [shebang](https://en.wikipedia.org/wiki/Shebang_\(Unix\)) sequence at
the beginning of the file you can execute this scripts directly from the shell.

To achieve this:
1) start your java-script with this sequence:
	#!/usr/bin/env jsb  

2) And make the jsb command available to your $PATH. (See "How to install")

Click here to a working script [HelloWorld example](./examples/hello_world.jsb)


### Automatic class import

You just have to write eg.: Map or ArrayList and (if there's only one 
exists on the classpath) this generates import declarations automatically.
Of course using import decalarations are still permitted and required in case
when multiple class with the same name appears in the classpath.
  
Your can turn off this feature by adding this annotation: `@autoimport-off`.
In this case you have to specify all used classes manually.


### Maven dependency resolution

The key feature of this little project. You can specify maven dependencies at
the beginning of the script. Before it begin to compile all dependencies will be
resolved. The resolved jar files added to the classpath and beign discovered
(for the automatic class importer).

You can also specify custom repositories as the source of the jar files.
See more in the `Use cases`

---

## Use cases

### Run simple applications without a project enviroment

Java 11's single file source code execution is also covers this usecase.
This can do too: [calculator](./examples/swing_calculator.jsb)

### Adhoc applications

Need an adhoc [FTP Server](./examples/ftp_server.jsb)? 
Or a demo [SNMP Server](./examples/snmp_demo.jsb)?

### Boot applications

Some project in the maven repositories already have some demo class you can
execute. But JavaShebang offers a more comfortable solution to boot you
application located in a maven repository:  

[JMonkeyEngine example collection](./examples/jmonkeyengine_examples.jsb)
(First boot takes long time, because it have to download a huger asset jar)  

[Javaexperience RPC Queue demo server](./examples/jvx_rpc_server_demo.jsb)

### Prototyping and demo applications

Using JavaShebang can be an easy way to introduce the features of you java
project by shipping a script that can be executed in an instance (without
messing with a new project/environemnt scraping the right dependencies, building
a project etc.)

For example this is how [teavm](http://teavm.org/) can compile a demo
application: [TeaSite compilation](./examples/teasite_compilation.jsb)

---

## How to install

### What is this $PATH stuff?

That's and environment variable the stores the directories where you shell
looking for executable files when you execute a command.  
To see you $PATH, command this: `echo $PATH`.  
You can spot the location of a command (eg.: ls command) by invoking
`which ls` which mostly returns `/bin/ls`.

Note: in linux systems the ~/bin/ (bin folder in your home) if exists its
	added to you $PATH. So it's a good directory to place this command.
	Another note: adding ~/bin/ to you $PATH happens after shell initialisation
	time when the ~/.profile called (mostly after login) so if you created right
	now you have to re-login to take the effect.

### From source

- Clone the project `git clone https://github.com/danko-david/java-shebang.git`
- Go into `cd java-shebang`
- Run the installer: ./scripts/install.sh (consult the script for details)
- Done

### Download

- Download the executable jar file from the
[repository](http://maven.javaexperience.eu/javaexperience/JavaShebang/),
from a versioned directory.
and copy the JavaShebang-*-jar-with-dependencies.jar file to `~/bin/jsb.jar`
- create the script file `~/bin/jsb` with this content:
```
#!/bin/bash
java -jar jsb.jar "$@"
```
- make executable `chmod 755 ~/bin/jsb.jar`
- Done

### What happens if the installation is done?

The command `jsb` should be available in your shell
```
	user@system:~$ jsb
	Specify a script file
```
	And script files can be executed directly eg.: `./hello_world.jsb`


### I'm totally losted, how to investigate what went wrong?

If you have docker you can study how JSB work on a working system, by running
the docker starter script: [./scripts/docker/run_docker.sh](/scripts/docker/run_docker.sh)

Yo can enter to the demo user called `user` by running `sudo -i -u user bash`
inside the container as root.

The git repository is copied into the directory `/home/user/JavaShebang`, so
you can access the directory `examples` in there.

To study the system:
- Try run some JSB script file eg.: `/home/user/JavaShebang/examples/hello_world.jsb`
- Also try to run script file by jsb command: `jsb /home/user/JavaShebang/examples/hello_world.jsb`
- This command `jsb` is located in `/home/user/bin/jsb`. Try call `which jsb`.
- Also study that this directory `/home/user/bin/` is present in you $PATH. Try call: `echo $PATH`

---

## How is this work?

When you start your application by invoking the script: 

(with some arguments eg.: ./hello_world.jsb myArg1)
- The kernel founds the [shebang](https://en.wikipedia.org/wiki/Shebang_\(Unix\))
sequence at the beginning of the file (eg: #!/usr/bin/env jsb).
- The kernel translates to execute this: /usr/bin/env jsb ./hello_world.jsb myArg1
- The `env` command is used to pick up the jsb command from your $PATH,
	wherever it's located (Using this method you don't need to hard code the
	path of the jsb command and ruin all script when you move elsewhere)
- jsb is also a script file and in this turn, this become to execute.
- inside the jsb script, the final resolution happens, and the kernel
	executes this: `java -jar /path/to/jsb-jar-with-dependencies.jar ./hello_world.jsb myArg1`
- JavaShebang started and picks up the first argument as a script file `./hello_world.jsb`
- It loads the file and removes the first line (where the shebang is located
	in order to compile with internal javac source compiler)
- Resolves the maven dependencies and add to the current class path
- Discovers all classes (for automatic class import) and adds the found ones.
- Compiles the source
- Find the `public static void main` function of the class and passes the remaining arguments `myArg1`

This might looks overcompilcated but shebang stuffs works this way.

To avoid some intermediate steps you can write shebang like this:
`#!/usr/bin/java -jar /path/to/jsb-jar-with-dependencies.jar`
But this breaks the flexibility and doesn't means a big improvement in boot time.
