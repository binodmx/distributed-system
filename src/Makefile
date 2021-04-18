JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = app/App.java \
node/Node.java \
server/Server.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class