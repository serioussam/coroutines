# Coroutines #

This coroutine library is forked from library written by Matthias Mann http://www.matthiasmann.de/content/view/24/26/

Coroutines are Subroutines which allow multiple entry points with suspend/resume. They implement a generic form of Cooperative 
multitasking without use of Threads.

## How to Use ##

### Maven ###
Dependency:

    <dependency>
        <groupId>de.matthiasmann</groupId>
        <artifactId>coroutines</artifactId>
        <version>1.0</version>
    </dependency>
    
Task:

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-antrun-plugin</artifactId>
        <version>1.7</version>
        <executions>
            <execution>
                <id>process-classes</id>
                <phase>process-classes</phase>
                <configuration>
                    <target>
                        <taskdef name="coroutines"
                             classname="de.matthiasmann.coroutines.instrument.InstrumentationTask"
                             classpathref="maven.runtime.classpath" />
                        <coroutines verbose="true">
                             <fileset dir="${project.build.outputDirectory}" />
                        </coroutines>
                    </target>
                </configuration>
                <goals>
                     <goal>run</goal>
                </goals>
            </execution>
        </executions>
    </plugin>

### Ant ###
Dependency:
   use the jar along with asm-debug-all 4.0 jar

Task:

    <taskdef name="coroutines"
         classname="de.matthiasmann.coroutines.instrument.InstrumentationTask"
         classpath="coroutines-1.0.jar:asm-all-debug-4.0.jar:${run.classpath}" />
    <coroutines verbose="true">
         <fileset dir="${build.classes.dir}" />
    </coroutines>


### Generator ###

To write a generator just extend the CoIterator class and provide implementation for run method.

    public class TestIterator extends CoIterator<String> implements Serializable {
        @Override
        protected void run() throws SuspendExecution {
            produce("A");
            produce("B");
            for(int i = 0; i < 4; i++) {
                produce("C" + i);
            }
            produce("D");
            produce("E");
        }
    }

User Code

    Iterator<String> iter1 = new TestIterator();
    while(iter.hasNext()) {
        System.out.println(iter.next());
    }


In this sample produce method will generate the result and suspend the execution of Iterator. When user code calls 
hasNext it will invoke the Coroutine which will produce the result and Suspend the execution again. Which will be returned 
by the next method.

Note: Please Don't catch the SuspendedException as its used as a mechanism to save the execution stack and yield the Coroutine.
But you can always do catch(Throwable)


### Coroutine Sample ###

    public class TestCoroutine {
        private static class TestCoRunnable implements CoRunnable {
            @Override
            public void coExecute() throws SuspendExecution {
                System.out.println("1");
                Coroutine.yield(); // Suspends execution at this point.
                System.out.println("2");
                Coroutine.yield();
                System.out.println("3");
                Coroutine.yield();
                System.out.println("4");
            }
        }
    
        private static Coroutine co = new Coroutine(new TestCoRunnable());
    
        public static void main(String ... args) {
            co.run(); // prints "1" and suspends
            co.run(); // prints "2" and suspends
            co.run(); // prints "3" and suspends
            co.run(); // prints "4" and suspends
            co.run(); // Throws Illegal State Exception as Coroutine is finished executing.
        }
    }

### Contact ###

This library was originally written by Matthias Mann. I have forked it and made it working with Java 7 and ASM 4.0 and Mavenized the project. I am 
maintaining this for some time till I find some more people to work on it.
