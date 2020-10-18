zipname=peersim-1.0.5.zip
LIB_JARS=`find -L lib/ -name "*.jar" | tr [:space:] :`

build:
	mkdir -p classes
	javac -sourcepath src -classpath $(LIB_JARS) -d classes `find -L . -name "*.java"`

download:
	rm -rf lib
	wget -O $(zipname) https://sourceforge.net/projects/peersim/files/peersim-1.0.5.zip/download
	unzip $(zipname) "*.jar"
	mv $(basename $(zipname)) lib
	rm $(zipname)

pingpong:
	java -cp $(LIB_JARS):classes peersim.Simulator src/sims/pingpong/config.txt

tree:
	java -cp $(LIB_JARS):classes peersim.Simulator src/jiahaoliu/example/broadcasttree/config-treebroadcast.txt

