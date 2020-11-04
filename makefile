zipname=peersim-1.0.5.zip
fastjsonname=fastjson.jar
LIB_JARS=`find -L lib/ -name "*.jar" | tr [:space:] :`

build:
	rm -rf classes
	mkdir -p classes
	javac -sourcepath src -classpath $(LIB_JARS) -d classes `find -L . -name "*.java"`

download:
	rm -rf lib
	wget -O $(zipname) https://sourceforge.net/projects/peersim/files/peersim-1.0.5.zip/download
	wget -O $(fastjsonname) https://search.maven.org/remote_content\?g\=com.alibaba\&a\=fastjson\&v\=LATEST
	unzip $(zipname) "*.jar"
	mv $(basename $(zipname)) lib
	mv $(fastjsonname) lib
	rm $(zipname)

pingpong:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/pingpong/config.txt

broadcasttree:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/broadcasttree/config.txt

gossip:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/collect/config-gossip.txt

plumtree:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/collect/config-plumtree.txt

plumtree-query:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/collect/config-plumtree-query.txt

gossip-collect-query:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/collect/config-gossip-collect-query.txt