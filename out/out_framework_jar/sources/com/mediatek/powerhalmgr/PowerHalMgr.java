package com.mediatek.powerhalmgr;
/* loaded from: classes4.dex */
public class PowerHalMgr {
    public static final int DFPS_MODE_ARR = 2;
    public static final int DFPS_MODE_DEFAULT = 0;
    public static final int DFPS_MODE_FRR = 1;
    public static final int DFPS_MODE_INTERNAL_SW = 3;
    public static final int DFPS_MODE_MAXIMUM = 4;
    public static final int MTKPOWER_DISP_MODE_DEFAULT = 0;
    public static final int MTKPOWER_DISP_MODE_EN = 1;
    public static final int MTKPOWER_DISP_MODE_NUM = 2;
    public static final int MTKPOWER_HINT_ALWAYS_ENABLE = 268435455;
    public static final int MTKPOWER_SCREEN_OFF_DISABLE = 0;
    public static final int MTKPOWER_SCREEN_OFF_ENABLE = 1;
    public static final int MTKPOWER_SCREEN_OFF_WAIT_RESTORE = 2;
    public static final int MTKPOWER_STATE_DEAD = 3;
    public static final int MTKPOWER_STATE_DESTORYED = 2;
    public static final int MTKPOWER_STATE_PAUSED = 0;
    public static final int MTKPOWER_STATE_RESUMED = 1;
    public static final int MTKPOWER_STATE_STOPPED = 4;
    public static final int PERF_RES_AI_APUSYS_BOOST_IPU_IF = 25231360;
    public static final int PERF_RES_AI_MDLA_FREQ_MAX = 25214976;
    public static final int PERF_RES_AI_MDLA_FREQ_MIN = 25198592;
    public static final int PERF_RES_AI_VPU_FREQ_MAX_CORE_0 = 25182208;
    public static final int PERF_RES_AI_VPU_FREQ_MAX_CORE_1 = 25182464;
    public static final int PERF_RES_AI_VPU_FREQ_MIN_CORE_0 = 25165824;
    public static final int PERF_RES_AI_VPU_FREQ_MIN_CORE_1 = 25166080;
    public static final int PERF_RES_CFP_DOWN_LOADING = 50365184;
    public static final int PERF_RES_CFP_DOWN_OPP = 50366208;
    public static final int PERF_RES_CFP_DOWN_TIME = 50365696;
    public static final int PERF_RES_CFP_ENABLE = 50364416;
    public static final int PERF_RES_CFP_POLLING_MS = 50364672;
    public static final int PERF_RES_CFP_UP_LOADING = 50364928;
    public static final int PERF_RES_CFP_UP_OPP = 50365952;
    public static final int PERF_RES_CFP_UP_TIME = 50365440;
    public static final int PERF_RES_CPUCORE_MAX_CLUSTER_0 = 8404992;
    public static final int PERF_RES_CPUCORE_MAX_CLUSTER_1 = 8405248;
    public static final int PERF_RES_CPUCORE_MIN_CLUSTER_0 = 8388608;
    public static final int PERF_RES_CPUCORE_MIN_CLUSTER_1 = 8388864;
    public static final int PERF_RES_CPUFREQ_CCI_FREQ = 4259840;
    public static final int PERF_RES_CPUFREQ_MAX_CLUSTER_0 = 4210688;
    public static final int PERF_RES_CPUFREQ_MAX_CLUSTER_1 = 4210944;
    public static final int PERF_RES_CPUFREQ_MAX_HL_CLUSTER_0 = 4243456;
    public static final int PERF_RES_CPUFREQ_MAX_HL_CLUSTER_1 = 4243712;
    public static final int PERF_RES_CPUFREQ_MIN_CLUSTER_0 = 4194304;
    public static final int PERF_RES_CPUFREQ_MIN_CLUSTER_1 = 4194560;
    public static final int PERF_RES_CPUFREQ_MIN_HL_CLUSTER_0 = 4227072;
    public static final int PERF_RES_CPUFREQ_MIN_HL_CLUSTER_1 = 4227328;
    public static final int PERF_RES_CPUFREQ_PERF_MODE = 4276224;
    public static final int PERF_RES_CUSTOM_RESOURCE_1 = 268435456;
    public static final int PERF_RES_DISP_DECOUPLE = 37781504;
    public static final int PERF_RES_DISP_DFPS_FPS = 37748992;
    public static final int PERF_RES_DISP_DFPS_MODE = 37748736;
    public static final int PERF_RES_DISP_IDLE_TIME = 37797888;
    public static final int PERF_RES_DISP_VIDEO_MODE = 37765120;
    public static final int PERF_RES_DRAM_CM_MGR = 16842752;
    public static final int PERF_RES_DRAM_CM_MGR_CAM_ENABLE = 16843008;
    public static final int PERF_RES_DRAM_CM_RATIO_UP_X_0 = 16859136;
    public static final int PERF_RES_DRAM_CM_RATIO_UP_X_1 = 16859392;
    public static final int PERF_RES_DRAM_CM_RATIO_UP_X_2 = 16859648;
    public static final int PERF_RES_DRAM_CM_RATIO_UP_X_3 = 16859904;
    public static final int PERF_RES_DRAM_CM_RATIO_UP_X_4 = 16860160;
    public static final int PERF_RES_DRAM_OPP_MIN = 16777216;
    public static final int PERF_RES_DRAM_VCORE_BW_ENABLE = 16809984;
    public static final int PERF_RES_DRAM_VCORE_BW_THRES = 16810240;
    public static final int PERF_RES_DRAM_VCORE_BW_THRESH_LP3 = 16810496;
    public static final int PERF_RES_DRAM_VCORE_MIN = 16793600;
    public static final int PERF_RES_DRAM_VCORE_MIN_LP3 = 16793856;
    public static final int PERF_RES_DRAM_VCORE_POLICY = 16826368;
    public static final int PERF_RES_FPS_EARA_BENCH = 33718272;
    public static final int PERF_RES_FPS_EARA_THERMAL_ENABLE = 33783808;
    public static final int PERF_RES_FPS_FBT_BHR = 33701888;
    public static final int PERF_RES_FPS_FBT_BHR_OPP = 33685504;
    public static final int PERF_RES_FPS_FBT_BOOST_TA = 33751040;
    public static final int PERF_RES_FPS_FBT_DEQTIME_BOUND = 33619968;
    public static final int PERF_RES_FPS_FBT_FLOOR_BOUND = 33636352;
    public static final int PERF_RES_FPS_FBT_KMIN = 33652736;
    public static final int PERF_RES_FPS_FBT_MIN_RESCUE_PERCENT = 33603840;
    public static final int PERF_RES_FPS_FBT_RESCUE_C = 33800960;
    public static final int PERF_RES_FPS_FBT_RESCUE_F = 33800192;
    public static final int PERF_RES_FPS_FBT_RESCUE_PERCENT = 33800448;
    public static final int PERF_RES_FPS_FBT_SHORT_RESCUE_NS = 33603584;
    public static final int PERF_RES_FPS_FBT_ULTRA_RESCUE = 33800704;
    public static final int PERF_RES_FPS_FPSGO_ADJ_CNT = 33816832;
    public static final int PERF_RES_FPS_FPSGO_ADJ_DEBNC_CNT = 33817088;
    public static final int PERF_RES_FPS_FPSGO_ADJ_LOADING = 33816576;
    public static final int PERF_RES_FPS_FPSGO_ADJ_LOADING_TIMEDIFF = 33817344;
    public static final int PERF_RES_FPS_FPSGO_DEP_FRAMES = 33849344;
    public static final int PERF_RES_FPS_FPSGO_ENABLE = 33570816;
    public static final int PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST = 33734656;
    public static final int PERF_RES_FPS_FPSGO_IDLEPREFER = 33882112;
    public static final int PERF_RES_FPS_FPSGO_LLF_POLICY = 33833216;
    public static final int PERF_RES_FPS_FPSGO_LLF_TH = 33832960;
    public static final int PERF_RES_FPS_FPSGO_MARGIN_MODE = 33767424;
    public static final int PERF_RES_FPS_FPSGO_MARGIN_MODE_DBNC_A = 33767680;
    public static final int PERF_RES_FPS_FPSGO_MARGIN_MODE_DBNC_B = 33767936;
    public static final int PERF_RES_FPS_FPSGO_SP_CK_PERIOD = 33850112;
    public static final int PERF_RES_FPS_FPSGO_SP_NAME_ID = 33849600;
    public static final int PERF_RES_FPS_FPSGO_SP_SUB = 33849856;
    public static final int PERF_RES_FPS_FSTB_FORCE_VAG = 33587200;
    public static final int PERF_RES_FPS_FSTB_FPS_LOWER = 33554432;
    public static final int PERF_RES_FPS_FSTB_FPS_UPPER = 33554688;
    public static final int PERF_RES_FPS_FSTB_SOFT_FPS_LOWER = 33669120;
    public static final int PERF_RES_FPS_FSTB_SOFT_FPS_UPPER = 33669376;
    public static final int PERF_RES_FPS_GBE1_ENABLE = 33865728;
    public static final int PERF_RES_FPS_GBE2_ENABLE = 33865984;
    public static final int PERF_RES_FPS_GBE2_LOADING_TH = 33866496;
    public static final int PERF_RES_FPS_GBE2_MAX_BOOST_CNT = 33866752;
    public static final int PERF_RES_FPS_GBE2_TIMER2_MS = 33866240;
    public static final int PERF_RES_FPS_GBE_POLICY_MASK = 33867008;
    public static final int PERF_RES_GPU_FREQ_LOW_LATENCY = 12615680;
    public static final int PERF_RES_GPU_FREQ_MAX = 12599296;
    public static final int PERF_RES_GPU_FREQ_MIN = 12582912;
    public static final int PERF_RES_GPU_GED_CWAITG = 12633088;
    public static final int PERF_RES_GPU_GED_DVFS_LOADING_MODE = 12633600;
    public static final int PERF_RES_GPU_GED_GX_BOOST = 12633344;
    public static final int PERF_RES_GPU_GED_LOADING_BASE_DVFS_STEP = 12632832;
    public static final int PERF_RES_GPU_GED_MARGIN_MODE = 12632320;
    public static final int PERF_RES_GPU_GED_TIMER_BASE_DVFS_MARGIN = 12632576;
    public static final int PERF_RES_GPU_GED_UNUSED_1 = 12632064;
    public static final int PERF_RES_GPU_POWER_POLICY = 12648448;
    public static final int PERF_RES_IO_BLKDEV_READAHEAD = 46186496;
    public static final int PERF_RES_IO_BOOST_VALUE = 46137344;
    public static final int PERF_RES_IO_DATA_FS_BOOST = 46219264;
    public static final int PERF_RES_IO_EXT4_DATA_BOOST = 46202880;
    public static final int PERF_RES_IO_F2FS_EMMC_BOOST = 46170112;
    public static final int PERF_RES_IO_F2FS_UFS_BOOST = 46153728;
    public static final int PERF_RES_IO_F2FS_UFS_BOOST_ULTRA = 46153984;
    public static final int PERF_RES_IO_UCLAMP_MIN = 46137600;
    public static final int PERF_RES_NET_BT_A2DP_LOW_LATENCY = 42008576;
    public static final int PERF_RES_NET_MD_CERT_PID = 41992704;
    public static final int PERF_RES_NET_MD_CRASH_PID = 41992960;
    public static final int PERF_RES_NET_MD_GAME_MODE = 41992448;
    public static final int PERF_RES_NET_MD_LOW_LATENCY = 41992192;
    public static final int PERF_RES_NET_MD_WEAK_SIG_OPT = 41993216;
    public static final int PERF_RES_NET_NETD_BLOCK_UID = 41976064;
    public static final int PERF_RES_NET_NETD_BOOST_UID = 41975808;
    public static final int PERF_RES_NET_WIFI_CAM = 41943040;
    public static final int PERF_RES_NET_WIFI_LOW_LATENCY = 41959424;
    public static final int PERF_RES_NET_WIFI_SMART_PREDICT = 41959680;
    public static final int PERF_RES_PERF_TASK_TURBO = 50380800;
    public static final int PERF_RES_POWERHAL_SCN_CRASH = 54526208;
    public static final int PERF_RES_POWERHAL_SCREEN_OFF_STATE = 54525952;
    public static final int PERF_RES_POWERHAL_SPORTS_MODE = 54542336;
    public static final int PERF_RES_POWERHAL_SPORTS_MODE_APP_SMART_MODE = 54542592;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_ACTIVE_TIME = 54559232;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_DURATION = 54558976;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_EAS_BOOST = 54559488;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_ENABLE = 54560000;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_NOTIFY_FBC = 54560256;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_OPP = 54558720;
    public static final int PERF_RES_POWERHAL_TOUCH_BOOST_TIME_TO_LAST_TOUCH = 54559744;
    public static final int PERF_RES_POWERHAL_WHITELIST_ACT_SWITCH_TIME = 54575616;
    public static final int PERF_RES_POWERHAL_WHITELIST_APP_LAUNCH_TIME_COLD = 54575104;
    public static final int PERF_RES_POWERHAL_WHITELIST_APP_LAUNCH_TIME_WARM = 54575360;
    public static final int PERF_RES_POWER_CPUFREQ_ABOVE_HISPEED_DELAY = 29392896;
    public static final int PERF_RES_POWER_CPUFREQ_HISPEED_FREQ = 29360128;
    public static final int PERF_RES_POWER_CPUFREQ_MIN_SAMPLE_TIME = 29376512;
    public static final int PERF_RES_POWER_CPUFREQ_POWER_MODE = 29409280;
    public static final int PERF_RES_POWER_CPUIDLE_MCDI_ENABLE = 29605888;
    public static final int PERF_RES_POWER_HINT_EXT_HINT = 54591744;
    public static final int PERF_RES_POWER_HINT_EXT_HINT_HOLD_TIME = 54592000;
    public static final int PERF_RES_POWER_HINT_HOLD_TIME = 54591488;
    public static final int PERF_RES_POWER_HPS_HEAVY_TASK = 29474816;
    public static final int PERF_RES_POWER_HPS_POWER_MODE = 29491200;
    public static final int PERF_RES_POWER_HPS_RUSH_BOOST_ENABLE = 29458432;
    public static final int PERF_RES_POWER_HPS_RUSH_BOOST_THRESH = 29458688;
    public static final int PERF_RES_POWER_HPS_THRESH_DOWN = 29425920;
    public static final int PERF_RES_POWER_HPS_THRESH_UP = 29425664;
    public static final int PERF_RES_POWER_HPS_TIMES_DOWN = 29442304;
    public static final int PERF_RES_POWER_HPS_TIMES_UP = 29442048;
    public static final int PERF_RES_POWER_PPM_HICA_VAR = 29540352;
    public static final int PERF_RES_POWER_PPM_LIMIT_BIG = 29556736;
    public static final int PERF_RES_POWER_PPM_MODE = 29523968;
    public static final int PERF_RES_POWER_PPM_ROOT_CLUSTER = 29507584;
    public static final int PERF_RES_POWER_PPM_SPORTS_MODE = 29573120;
    public static final int PERF_RES_POWER_PPM_USERLIMIT_BOOST = 29589504;
    public static final int PERF_RES_POWER_SYSLIMITER_120 = 29622784;
    public static final int PERF_RES_POWER_SYSLIMITER_60 = 29622272;
    public static final int PERF_RES_POWER_SYSLIMITER_90 = 29622528;
    public static final int PERF_RES_POWER_SYSLIMITER_DISABLE = 29638656;
    public static final int PERF_RES_SCHED_BOOST = 21037056;
    public static final int PERF_RES_SCHED_BOOST_VALUE_BG = 20972032;
    public static final int PERF_RES_SCHED_BOOST_VALUE_FG = 20971776;
    public static final int PERF_RES_SCHED_BOOST_VALUE_ROOT = 20971520;
    public static final int PERF_RES_SCHED_BOOST_VALUE_RT = 20972544;
    public static final int PERF_RES_SCHED_BOOST_VALUE_TA = 20972288;
    public static final int PERF_RES_SCHED_BTASK_ROTATE = 21086208;
    public static final int PERF_RES_SCHED_CACHE_AUDIT = 21102592;
    public static final int PERF_RES_SCHED_CPU_PREFER_TASK_1_BIG = 21069824;
    public static final int PERF_RES_SCHED_CPU_PREFER_TASK_1_LITTLE = 21070080;
    public static final int PERF_RES_SCHED_CPU_PREFER_TASK_1_RESERVED = 21070336;
    public static final int PERF_RES_SCHED_CPU_PREFER_TASK_2_BIG = 21070592;
    public static final int PERF_RES_SCHED_CPU_PREFER_TASK_2_LITTLE = 21070848;
    public static final int PERF_RES_SCHED_CPU_PREFER_TASK_2_RESERVED = 21071104;
    public static final int PERF_RES_SCHED_HEAVY_TASK_AVG_HTASK_AC = 21152000;
    public static final int PERF_RES_SCHED_HEAVY_TASK_AVG_HTASK_THRES = 21152256;
    public static final int PERF_RES_SCHED_HEAVY_TASK_THRES = 21151744;
    public static final int PERF_RES_SCHED_MIGRATE_COST = 21053440;
    public static final int PERF_RES_SCHED_MTK_PREFER_IDLE = 21135360;
    public static final int PERF_RES_SCHED_PLUS_DOWN_THROTTLE_NS = 21118976;
    public static final int PERF_RES_SCHED_PLUS_SYNC_FLAG = 21119488;
    public static final int PERF_RES_SCHED_PLUS_UP_THROTTLE_NS = 21119232;
    public static final int PERF_RES_SCHED_PREFER_CPU_BG = 21185024;
    public static final int PERF_RES_SCHED_PREFER_CPU_FG = 21184768;
    public static final int PERF_RES_SCHED_PREFER_CPU_ROOT = 21184512;
    public static final int PERF_RES_SCHED_PREFER_CPU_RT = 21185536;
    public static final int PERF_RES_SCHED_PREFER_CPU_TA = 21185280;
    public static final int PERF_RES_SCHED_PREFER_IDLE_BG = 20988416;
    public static final int PERF_RES_SCHED_PREFER_IDLE_FG = 20988160;
    public static final int PERF_RES_SCHED_PREFER_IDLE_ROOT = 20987904;
    public static final int PERF_RES_SCHED_PREFER_IDLE_RT = 20988928;
    public static final int PERF_RES_SCHED_PREFER_IDLE_TA = 20988672;
    public static final int PERF_RES_SCHED_TUNE_THRES = 21020672;
    public static final int PERF_RES_SCHED_UCLAMP_MIN_BG = 21004800;
    public static final int PERF_RES_SCHED_UCLAMP_MIN_FG = 21004544;
    public static final int PERF_RES_SCHED_UCLAMP_MIN_ROOT = 21004288;
    public static final int PERF_RES_SCHED_UCLAMP_MIN_RT = 21005312;
    public static final int PERF_RES_SCHED_UCLAMP_MIN_TA = 21005056;
    public static final int PERF_RES_SCHED_WALT = 21168128;
    public static final int PERF_RES_THERMAL_POLICY = 50331648;
    public static final int PERF_RES_TOUCH_CHANGE_RATE = 50397184;
    public static final int PERF_RES_UX_PREDICT_GAME_MODE = 50348288;
    public static final int PERF_RES_UX_PREDICT_LOW_LATENCY = 50348032;

    public int scnReg() {
        return -1;
    }

    public void scnConfig(int handle, int cmd, int param_1, int param_2, int param_3, int param_4) {
    }

    public void scnUnreg(int handle) {
    }

    public void scnEnable(int handle, int timeout) {
    }

    public void scnDisable(int handle) {
    }

    public int perfLockAcquire(int handle, int duration, int[] list) {
        return -1;
    }

    public void perfLockRelease(int handle) {
    }

    public void mtkPowerHint(int hint, int data) {
    }

    public int perfCusLockHint(int hint, int duration) {
        return -1;
    }

    public boolean setPriorityByUid(int action, int uid) {
        return false;
    }

    public boolean flushPriorityRules(int type) {
        return false;
    }
}
