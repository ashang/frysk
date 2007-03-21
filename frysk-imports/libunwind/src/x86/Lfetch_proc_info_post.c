#define UNW_LOCAL_ONLY
#include <libunwind.h>
#if defined(UNW_LOCAL_ONLY) && !defined(UNW_REMOTE_ONLY)
#include "Gfetch_proc_info_post.c"
#endif
