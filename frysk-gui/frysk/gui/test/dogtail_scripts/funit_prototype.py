from FryskHelpers import *

theTuple =  startFunitChild2 ()
ofile = theTuple[1]

# Read 1st line - extract PID value
s = ofile.readline()
values = s.split('.')
PID = values[0]
#print "PID = " + PID

# Throw away next (4) lines of output
s = ofile.readline()
s = ofile.readline()
s = ofile.readline()
s = ofile.readline()

# Fork a child process
print 'Fork a process'
returnString = signalFunitChild2(str(PID), SIGHUP, ofile)
print returnString

# Kill process
print 'Delete a fork'
returnString = signalFunitChild2(str(PID), SIGINT, ofile)
print returnString 

# Kill funit-child
print 'Kill funit-child'
returnString = signalFunitChild2(str(PID), SIGALRM, ofile)
print returnString 


