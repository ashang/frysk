#! /bin/bash
#
# Temporary shell script
#
# As of June 8, 2006, there's a problem with 
# the test suite - either Frysk or Dogtail gets confused and attempts
# to run tests before other tests have completed - short-term workaround
# is to comment out these lines, run the tests separately, and read
# the datafiles from the CLI       

python TestCredits.py /home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui

python TestLicense.py /home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui

python TestCreateObservers.py /home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui

python TestCreateObserversfromDataModel.py /home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui new_fork_custom_observer.xml

# Run this one last for now - 20060609 - it hangs all bash processes on 
# the system - http://sourceware.org/bugzilla/show_bug.cgi?id=2741 
python TestDruid.py /home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui another_new_session.xml
