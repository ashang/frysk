# lcov may complain if it cannot find source or there is no corresponding 
# *.gcno file for a *.gcda file

# --directory frysk-core/frysk/proc/LinuxHost.gcda
# --directory frysk-core/frysk/proc/IsaIA32.gcda
# --directory frysk-gui/frysk/gui/monitor/ProcMenu.g
# --directory frysk-gui/frysk/gui/monitor/MenuBar.g
# --directory frysk-gui/frysk/gui/monitor/CoreDebugLogViewer.g
# --directory frysk-gui/frysk/gui/monitor/EventLogger.g
# --directory frysk-core/frysk/dom/cparser 
# --directory frysk-core/frysk/expr 


if [ "$1" = "info" ] ; then
/home/scox/lcov/lcov -o /tmp/frysk.info \
--directory frysk-core/frysk/cli/hpd \
--directory frysk-core/frysk/dom \
--directory frysk-core/frysk/event \
--directory frysk-core/frysk/lang \
--directory frysk-core/frysk/proc \
--directory frysk-gui/frysk/gui \
--directory frysk-gui/frysk/gui/common \
--directory frysk-gui/frysk/gui/common/dialogs \
--directory frysk-gui/frysk/gui/common/prefs \
--directory frysk-gui/frysk/gui/druid \
--directory frysk-gui/frysk/gui/memory \
--directory frysk-gui/frysk/gui/monitor \
--directory frysk-gui/frysk/gui/monitor/actions \
--directory frysk-gui/frysk/gui/monitor/datamodels \
--directory frysk-gui/frysk/gui/monitor/filters \
--directory frysk-gui/frysk/gui/monitor/observers \
--directory frysk-gui/frysk/gui/register \
--directory frysk-gui/frysk/gui/sessions \
--directory frysk-gui/frysk/gui/srcwin \
--directory frysk-gui/frysk/gui/srcwin/prefs \
--directory frysk-gui/frysk/gui/srcwin/tags \
--directory frysk-gui/frysk/vtecli \
--directory frysk-sys/frysk/sys \
--directory frysk-sys/frysk/sys/proc \
-c
elif [ "$1" = "zero" ] ; then
/home/scox/lcov/lcov \
--directory frysk-core/frysk/dom \
--directory frysk-core/frysk/dom/cparser \
--directory frysk-core/frysk/event \
--directory frysk-core/frysk/lang \
--directory frysk-core/frysk/proc \
--directory frysk-gui/frysk/gui \
--directory frysk-gui/frysk/gui/common \
--directory frysk-gui/frysk/gui/common/dialogs \
--directory frysk-gui/frysk/gui/common/prefs \
--directory frysk-gui/frysk/gui/druid \
--directory frysk-gui/frysk/gui/memory \
--directory frysk-gui/frysk/gui/monitor \
--directory frysk-gui/frysk/gui/monitor/actions \
--directory frysk-gui/frysk/gui/monitor/datamodels \
--directory frysk-gui/frysk/gui/monitor/filters \
--directory frysk-gui/frysk/gui/monitor/observers \
--directory frysk-gui/frysk/gui/register \
--directory frysk-gui/frysk/gui/sessions \
--directory frysk-gui/frysk/gui/srcwin \
--directory frysk-gui/frysk/gui/srcwin/prefs \
--directory frysk-gui/frysk/gui/srcwin/tags \
--directory frysk-gui/frysk/vtecli \
--directory frysk-sys/frysk/sys \
--directory frysk-sys/frysk/sys/proc \
-z
rm -rf /tmp/frysk.genhtml
elif [ "$1" = "genhtml" ] ; then
genhtml -o /tmp/frysk.genhtml /tmp/frysk.info
elif [ "$1" = "fixup" ] ; then
# If a *.gcda file does not have an equivalent *.gcno then remove it
for i in $(find . -name '*.gcda') 
 do 
   if [ -r $(dirname $i)/$(basename $i .gcda).gcno ] 
   then true 
   else /bin/rm $i 
   fi 
 done
else
echo Usage $0 "info|zero|fixup|genhtml"
fi
