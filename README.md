# lingua-reproducer-memory-leak
Reproducer for a memory leak in Lingua 1.1.0 when using it from a web application

# Building

The application is built using Maven. In order to use the newest snapshot of Lingua, it needs to be pulled from a Maven repository. It's possible to install the snapshot into the local repository by using

`mvn install:install-file -Dfile=${pathToLinguaRepo}/build/libs/lingua-1.2.0-SNAPSHOT-with-dependencies.jar -DgroupId=com.github.pemistahl -DartifactId=lingua -Dversion=1.2.0-SNAPSHOT -Dpackaging=jar`

# Reproducing the problem

To reproduce the problem, do the following:
1. deploy the application on a Java application server with a suitably large heap, e.g. Wildfly 24 using -Xmx6G
2. get the model initialized, e.g. by accessing http://localhost:8080/lingua-reproducer-memory-leak/detectLanguage
3. undeploy or redeploy the application
4. repeat the first three steps a few times. This will eventually lead to an OutOfMemoryError either on deployment or when the models are initialized

# Possible causes

In order for a web application to be shut down properly, the threads and associated resources created by the web appllication must be released when the application is undeployed. This does not happen for the example application. As discussed in [Lingua #110](https://github.com/pemistahl/lingua/issues/110), the likely culprit are the Kotlin coroutines.

If you take a heap dump using [Eclipse Memory Analyzer](https://www.eclipse.org/mat/) on Wildfly 24 after 1 or 2 redeploys, and then run a leak suspects report, you will see that instances of the ModuleClassLoader still hang around. Each instance retains about 2,2GB heap each. The Leak Suspects report of Eclipse Memory Analyzer points to the threads created by Kotlin to handle the coroutines.

Stack Trace from the leak suspects report:
```
DefaultDispatcher-worker-2
  at sun.misc.Unsafe.park(ZJ)V (Native Method)
  at java.util.concurrent.locks.LockSupport.parkNanos(J)V (LockSupport.java:338)
  at shadow.kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park()V (CoroutineScheduler.kt:795)
  at shadow.kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark()V (CoroutineScheduler.kt:740)
  at shadow.kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker()V (CoroutineScheduler.kt:711)
  at shadow.kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run()V (CoroutineScheduler.kt:665
```

Classes related to coroutines still present in the dump (seem to be similar to those from [Lingua #110](https://github.com/pemistahl/lingua/issues/110)):
`shadow.kotlinx.coroutines.scheduling.CoroutineScheduler$WorkerState`, `shadow.kotlinx.coroutines.scheduling.WorkQueue`, `shadow.kotlinx.coroutines.scheduling.CoroutineScheduler$WorkerState`