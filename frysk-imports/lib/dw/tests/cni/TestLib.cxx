#include <sys/types.h>
#include <unistd.h>
#include <gcj/cni.h>

#include "lib/dw/tests/TestLib.h"

jint
lib::dw::tests::TestLib::getPid(){
	return (jint) getpid();
}

jlong
lib::dw::tests::TestLib::getFuncAddr(){
	return (jlong) &getFuncAddr;	
}
