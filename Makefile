JCC=javac

all:	
	$(JCC) -g *java net/*java proc/*java util/*java xml/*java proc/nrl/*java demo/*java

clean:
	rm -f *class net/*class proc/*class util/*class xml/*class proc/nrl/*class demo/*class
