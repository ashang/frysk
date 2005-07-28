#!/bin/sh

TIME ()
{
    name="$1" ; shift
    # On PPC both I/O and faults give back zero
    /usr/bin/time \
	--format="$name real %e user %U sys %S faults %F/%R switch %c waits %w" \
	"$@" > /dev/null 2> /tmp/clone.time.$name
}

OPTIME ()
{
    opcontrol --reset
    opcontrol --start --no-vmlinux
    TIME "$@"
    opcontrol --stop
    opreport > /tmp/clone.optime.$1
}

for prog in none NONE run RUN gdb GDB
do
  count=0
  while count=`expr $count + 50` ; test $count -le 3000
  do
    echo $prog $count
    case "${prog}" in
	none ) TIME "none.$count" ./prog/perf/clone $count ;;
	NONE ) OPTIME "NONE.$count" ./prog/perf/clone $count ;;
	run ) TIME "run.$count" ./prog/accu/run ./prog/perf/clone $count  ;;
	RUN ) OPTIME "RUN.$count" ./prog/accu/run ./prog/perf/clone $count  ;;
	gdb ) TIME "gdb.$count" gdb --args ./prog/perf/clone $count <<EOF ;;
run
EOF
	GDB ) OPTIME "GDB.$count" gdb --args ./prog/perf/clone $count <<EOF ;;
run
EOF
    esac
  done
done
