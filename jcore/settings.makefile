# debug or release version ##############################################
DEFINES += -DPRODUCTION
#DEFINES += -DDEBUG
DEFINES += -DMONITOR
#DEFINES += -O2 -mcpu=i686 -march=i686 --inline  -finline-functions -finline-limit=100000  -foptimize-sibling-calls -fstrength-reduce -fcse-follow-jumps -fschedule-insns2 -falign-functions -frerun-loop-opt -fgcse
DEFINES += -O2 
#DEFINES += -O3
#DEFINES += -O3 --inline -finline-functions -finline-limit=100000

# static system limits ##################################################
DEFINES += -DGLOBAL_MEMSIZE=200000000
DEFINES += -DRECEIVE_PORTAL_QUOTA=100000
DEFINES += -DCREATEDOMAIN_PORTAL_QUOTA=5000
DEFINES += -DNUMBER_DZMEM=20000
DEFINES += -DMAX_EVENT_THREADSWITCH=4000000
DEFINES += -DHEAP_BYTES_INIT="(512 * 1024)"
DEFINES += -DHEAP_BYTES_DOMAINZERO="(2 * 1024 * 1024)"
DEFINES += -DCODE_BYTES_DOMAINZERO="(4 * 1024 * 1024)"
DEFINES += -DCODE_BYTES_DOMAININIT="(256 * 1024)"
DEFINES += -DMAX_NUMBER_DOMAINS=200
#DEFINES += -DCHARS_8BIT
DEFINES += -DALL_ARRAYS_32BIT
#DEFINES += -DINIT_LIB=\"init.jll\"
DEFINES += -DINIT_LIB=\"init2.jll\"
#DEFINES += -DDOMAIN_SCRATCHMEM_SIZE=4096
DEFINES += -DDOMAIN_SCRATCHMEM_SIZE="(4096*100)"


# portal call handling ##################################################
#DEFINES += -DCOPY_TO_DOMAINZERO
#DEFINES += -DPORTAL_INTERCEPTOR
#DEFINES += -DPORTAL_TRANSFER_INTERCEPTOR
DEFINES += -DDIRECT_SEND_PORTAL
DEFINES += -DSERVICE_EAGER_CLEANUP
DEFINES += -DNOTIFY_SERVICE_CLEANUP
#DEFINES += -DNEW_PORTALCALL
#DEFINES += -DNEW_COPY
#DEFINES += -DNEW_SERVICE_THREADS # create a new thread when a service thread blocks

# scheduler settings ####################################################
#DEFINES += -DSMP activates SMP support (APIC, JAVASCHEDULER are required)
#DEFINES += -DJAVASCHEDULER # activates the two level Java scheduling
#DEFINES += -DAPIC # use local APIC as timer instead of PIT)
#DEFINES += -DNOPREEMPT # activates support for non-preemptable regions
#DEFINES += -DTIMESLICING_TIMER_IRQ
DEFINES += -DTIMER_HZ=10

# scheduler settings ####################################################
#DEFINES += -DPORTAL_HANDOFF   # handoff in portal call
DEFINES += -DVISIBLE_PORTALS  # scheduler is informed about portal activities
#DEFINES += -DCONT_PORTAL     # caller may continue a preempted portal call
#DEFINES += -DNEW_SCHED
#DEFINES += -DSCHED_GLOBAL_RR
#DEFINES += -DSCHED_LOCAL_RR
#DEFINES += -DSCHED_LOCAL_JAVA

# scheduler debugging ###################################################
#DEFINES += -DCHECK_RUNNABLE_IN_RUNQ
#DEFINES += -DCHECK_STACKTRACE
#DEFINES += -DTRACESCHEDULER
#DEFINES += -DDBG_THREAD
#DEFINES += -DCHECK_CURRENT

# gc settings ###########################################################
#DEFINES += -DVERBOSE_GC
DEFINES += -DNOTICE_GC
DEFINES += -DGC_USE_NEW
DEFINES += -DGC_NEW_IMPL
#DEFINES += -DGC_COMPACTING_IMPL
#DEFINES += -DGC_BITMAP_IMPL
DEFINES += -DGC_CHUNKED_IMPL
#DEFINES += -DGC_USE_ONLY_ONE=new
#DEFINES += -DGC_USE_ONLY_ONE=compacting
DEFINES += -DENABLE_GC
#DEFINES += -DROLLFORWARD_ON_PREEMPTION
DEFINES += -DROLLFORWARD_ON_GC
#DEFINES += -DGC_FAST_MEMCPY
#DEFINES += -DGC_USE_MMX
DEFINES += -DMPROTECT_HEAP
#DEFINES += -DDBG_GC
#DEFINES += -DDBG_STACKMAP

# optimizations #########################################################
#DEFINES += -DUSE_PUSHED_METHODDESC
DEFINES += -DFASTER_METHOD_LOOKUP
DEFINES += -DUSE_LIB_INDEX

# heap checking #########################################################
#DEFINES += -DCHECK_HEAPUSAGE
#DEFINES += -DCHECKHEAP_VERBOSE
#DEFINES += -DCHECK_HEAP_AFTER_ALLOC 
#DEFINES += -DCHECK_HEAP_BEFORE_ALLOC
#DEFINES += -DCHECK_HEAP_BEFORE_GC

# memory revocation #####################################################
#DEFINES += -DMEMORY_REVOKE
#DEFINES += -DREVOKE_USING_CLI
#DEFINES += -DREVOCATION_CHECK
#DEFINES += -DREVOKE_USING_SPINLOCK

# memory settings  ######################################################
#DEFINES += -DREDIRECT_INVALID_DZ
DEFINES += -DFAST_MEMORY_CALLS
DEFINES += -DATOMIC_MEMORY
#DEFINES += -DMEMORY_USE_NEW
DEFINES += -DMEMORY_USE_ORG
#DEFINES += -DMEMORY_USE_SHARED
#DEFINES += -DENABLE_MAPPING

# memory debugging ######################################################
#DEFINES += -DDEBUG_REVOCATION
#DEFINES += -DDEBUG_MEMORY_CREATION
#DEFINES += -DDEBUG_MEMORY_REFCOUNT
#DEFINES += -DDEBUG_HANDLE
#DEFINES += -DASSERT_ALIGNED_CODE
#DEFINES += -DENFORCE_FMA

# system analysis #######################################################
#DEFINES += -DPROFILE_EVENT_THREADSWITCH_IPSAMPLING
#DEFINES += -DPROFILE_SAMPLE_PMC0
#DEFINES += -DPROFILE_SAMPLE_PMC1
#DEFINES += -DPROFILE_SAMPLE_PMC_DIFF 

#DEFINES += -DEVENT_LOG
#DEFINES += -DPROFILE_EVENT_THREADSWITCH
#DEFINES += -DPROFILE_EVENT_MEMORY_ALLOC         # log stages of MemoryManager.alloc() 
#DEFINES += -DPROFILE_EVENT_MEMORY_ALLOC_AMOUNT  # log amount of memory allocation 
#DEFINES += -DPROFILE_EVENT_JXMALLOC
#DEFINES += -DPROFILE_EVENT_PORTAL               # log portal execution stages 
#DEFINES += -DPROFILE_EVENT_CREATEDOMAIN
#DEFINES += -DEVENT_CALIBRATION                  # measure the time it takes to log events 

#DEFINES += -DPROFILE
#DEFINES += -DPROFILE_SAMPLE
#DEFINES += -DSAMPLING_TIMER_IRQ
#DEFINES += -DPREEMPTION_SAMPLE
DEFINES += -DPROFILE_HEAPUSAGE
#DEFINES += -DPROFILE_SAMPLE_HEAPUSAGE
#DEFINES += -DPROFILE_GC
#DEFINES += -DPROFILE_AGING
#DEFINES += -DPROFILE_AGING_CREATION_ONLY
#DEFINES += -DMINILZO
#DEFINES += -DBINARY_DATA_TRANSMISSION
#DEFINES += -DCOMPACT_EIP_INFO
#DEFINES += -DDEBUGSUPPORT_DUMP
#DEFINES += -DCPU_USAGE_STATISTICS
#DEFINES += -DCOPY_STATISTICS
#DEFINES += -DMALLOC_STAT


# emulation #############################################################
# FRAMEBUFFER_EMULTION in KERNEL mode uses VESA >=2.0 linear framebuffer
DEFINES += -DFRAMEBUFFER_EMULATION
DEFINES += -DFRAMEBUFFER_EMULATION_ENFORCE_SHM
DEFINES += -DDISK_EMULATION
DEFINES += -DNET_EMULATION
#DEFINES += -DTIMER_EMULATION

# info about the hardware ###############################################
DEFINES += -DCPU_MHZ=500
DEFINES += -DHAVE_RDTSC

# debugging settings ####################################################
DEFINES += -DUSE_MAGIC

# statistics settings ###################################################
#DEFINES += -DHEAP_STATISTICS
#DEFINES += -DIRQ_STATISTICS
#DEFINES += -DPORTAL_STATISTICS

# misc ##################################################################
#DEFINES += -DUSE_EKHZ
#DEFINES += -DLOG_PRINTF  # log all output to mem (transfer with printflog in monitor)
#DEFINES += -DCHECK_SERIAL
#DEFINES += -DCHECK_SERIAL_IN_PORTAL
#DEFINES += -DCHECK_SERIAL_IN_TIMER
#DEFINES += -DCHECK_SERIAL_IN_YIELD
#DEFINES += -DSERIAL_BAUD_115200
#DEFINES += -DSERIAL_BAUD_9600
#DEFINES += -DSAMPLE_FASTPATH # system panics when a slow operation is performed on the sampled path
#DEFINES += -DNO_DEBUG_OUT
#DEFINES += -DJAVA_MONITOR_COMMANDS
#DEFINES += -DSTACK_ON_HEAP

