#!/bin/bash
# Scripts to install missing Debuginfo packages for Frysk
# Requires pid(s) as argument

# Run fdebuginfo and get the name of missing debuginfo packages
                                                                    
export packages=`@bindir@/fdebuginfo "$*" | grep "\-\-\-" |   
                 cut -d ' ' -f 1 | 
                 sort | uniq | 
                 grep '^/'| 
                 xargs rpm -qf --qf '%{SOURCERPM}\n' | 
                 sort | uniq | 
                 sed -e 's/-[0-9].*$/-debuginfo/g'`

if [ -n "$packages" ] 
then
    # Display missing packages
    echo ""
    echo "Missing Debuginfo package(s)"
    echo "============================"
    echo "$packages"
    echo ""

    # Install on user request
    #echo "Do you wish to install the above packages? [y/n]"
    read -p "Do you wish to install the above packages? [y/n]: " ch
    if [ "$ch" = "y" ]  
    then
       sudo yum install $packages
    fi

else
   echo "No missing debuginfo packages"
fi
