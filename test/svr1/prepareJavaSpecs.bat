set oldcp=%classpath%
set classpath=%classpath%;c:\workflow\specfiles

javac C:\workflow\specfiles\workflows\svr1\start\Realization.java C:\workflow\specfiles\workflows\svr1\middle\Realization.java C:\workflow\specfiles\workflows\svr1\end\Realization.java 
set classpath=%oldcp%
pause
