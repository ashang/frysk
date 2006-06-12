#! /bin/bash
#
# Temporary shell script
#
# As of June 8, 2006, there's a problem with 
# the test suite - either Frysk or Dogtail gets confused and attempts
# to run tests before other tests have completed - short-term workaround
# is to comment out these lines, run the tests separately, and read
# the datafiles from the CLI       

# Only CLI argument = name of FryskGui binary
# for example: /home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui

if [ $# -eq 1 ]
  then
  python TestCredits.py $1 
  python TestLicense.py $1 
  python TestCreateObservers.py $1
  python TestCreateObserversfromDataModel.py $1 new_fork_custom_observer.xml
  # Run this one last for now - 20060609 - it hangs all bash processes on 
  # the system - http://sourceware.org/bugzilla/show_bug.cgi?id=2741 
  python TestDruid.py $1 another_new_session.xml
else
  echo "usage: frysk_scripts.sh FryskGui_binary_path"
fi

