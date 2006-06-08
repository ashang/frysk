#! /bin/bash
#
# Temporary shell script
#
# As of June 8, 2006, there's a problem with 
# the test suite - either Frysk or Dogtail gets confused and attempts
# to run tests before other tests have completed - short-term workaround
# is to comment out these lines, run the tests separately, and read
# the datafiles from the CLI       

python TestCredits.py
python TestLicense.py
python TestDruid.py another_new_session.xml
python TestCreateObservers.py
python TestCreateObserversfromDataModel.py new_fork_custom_observer.xml
