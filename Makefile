JCC=javac

all:	
	$(JCC) *java net/*java proc/*java util/*java xml/*java proc/nrl/*java

clean:
	rm -f *class net/*class proc/*class util/*class xml/*class proc/nrl/*class
