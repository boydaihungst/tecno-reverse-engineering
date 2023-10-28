package com.transsion.server.foldable;

import android.text.TextUtils;
import com.transsion.foldable.TranFoldingScreenManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
/* loaded from: classes2.dex */
public class TranFoldingScreenController {
    private static final String TAG = "TranFoldingScreenController";
    private static final boolean OS_FOLDABLE_SCREEN_SUPPORT = TranFoldingScreenManager.isFoldableDevice();
    private static final List<String> RESIZE_MODE_UNRESIZEABLE_BLOCK_LIST = Arrays.asList("com.rubygames.assassin", "com.taobao.taobao", "tv.twitch.android.app", "com.xvideostudio.videoeditor", "com.cyberlink.youcammakeup", "com.adobe.lrmobile", "com.yuapp.beautycamera.selfie.makeup", "com.robtopx.geometryjumplite", "com.echannels.moismartservices", "net.upx.proxy.browser", "com.zhiliaoapp.musically.go", "com.duowan.mobile", "com.daraz.android", "com.vectorunit.purple.googleplay", "com.block.jigsaw.puzzle.gallery", "vStudio.Android.Camera360", "com.lemon.lvoverseas", "com.telkomsel.telkomselcm", "share.sharekaro.pro", "com.merriamwebster", "com.happyelements.AndroidAnimal", "com.rubygames.hunterassassin2", "bounce.Coloring.Pages", "mx.com.suburbia.shoppingapp", "jp.naver.linecamera.android", "com.vkontakte.android", "com.facebook.lite", "com.alibaba.aliexpresshd");
    private static final List<String> FORCE_RESTART_PACKAGE_LIST = Arrays.asList("com.rubygames.assassin", "tv.twitch.android.app", "com.lomotif.android", "com.ykb.android", "com.vido.particle.ly.lyrical.status.maker", "in.mohalla.video", "sg.bigo.live", "com.lazada.android", "com.accordion.analogcam", "com.roblox.client", "com.echannels.moismartservices", "com.zhiliaoapp.musically.go", "com.eastudios.tongits", "net.peakgames.Yuzbir", "in.mohalla.sharechat", "com.myntra.android", "com.telkomsel.telkomselcm", "share.sharekaro.pro", "com.domobile.applock", "com.musixmusicx", "com.nexstreaming.app.kinemasterfree", "com.happyelements.AndroidAnimal", "bounce.Coloring.Pages", "mx.com.suburbia.shoppingapp", "jp.naver.linecamera.android", "com.vkontakte.android", "com.kms.free", "com.google.android.apps.youtube.kids");
    private static final List<String> RESIZE_MODE_FORCE_RESIZEABLE_WHITE_LIST = Arrays.asList("com.netease.cloudmusic", "com.turkcell.bip");
    static final HashMap<String, Integer> COMPATIBLE_MODE_DEFAULT_VALUE_MAP = new HashMap<>();
    private static final List<String> USE_DEFALUT_NOTCH_SIZE_PACKAGE = Arrays.asList("com.tencent.tmgp.pubgmhd", "com.tencent.ig");

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void initCompatibleModeDefaultValueData() {
        HashMap<String, Integer> hashMap = COMPATIBLE_MODE_DEFAULT_VALUE_MAP;
        hashMap.put("com.ludo.king", 1);
        hashMap.put("com.wave.personal", 1);
        hashMap.put("com.zong.customercare", 1);
        hashMap.put("com.jazz.jazzworld", 1);
        hashMap.put("pk.com.telenor.phoenix", 1);
        hashMap.put("com.techlogix.mobilinkcustomer", 1);
        hashMap.put("com.tonielrosoft.classicludofree", 1);
        hashMap.put("com.martinvillar.android.bibliaenfrances", 1);
        hashMap.put("io.chingari.app", 1);
        hashMap.put("com.calculator.hideu", 1);
        hashMap.put("com.cybertron.movingbricks", 1);
        hashMap.put("com.cybertron.bubbleshooter", 1);
        hashMap.put("com.pure.indosat.care", 1);
        hashMap.put("com.fingersoft.hillclimb", 1);
        hashMap.put("com.cybertron.tombrunner", 1);
        hashMap.put("com.cybertron.duckmageddon", 1);
        hashMap.put("com.cybertron.spritewar", 1);
        hashMap.put("com.gameberry.ludo.star2", 1);
        hashMap.put("com.PlayMax.playergames", 1);
        hashMap.put("com.cybertron.hexagonfall", 1);
        hashMap.put("com.cybertron.jellybomb", 1);
        hashMap.put("com.yalla.yallagames", 1);
        hashMap.put("com.cybertron.retrospeed", 1);
        hashMap.put("app.com.brochill", 1);
        hashMap.put("com.bubbleshooter.popbubbles.collectcards", 1);
        hashMap.put("com.tatay.manokNaPula", 1);
        hashMap.put("com.cybertron.endlesssiege", 1);
        hashMap.put("com.mydito", 1);
        hashMap.put("free.alquran.holyquran", 1);
        hashMap.put("com.AppRocks.now.prayer", 1);
        hashMap.put("com.ea.gp.fifamobile", 1);
        hashMap.put("app.bpjs.mobile", 1);
        hashMap.put("com.miniclip.eightballpool", 1);
        hashMap.put("com.rollic.tanglemaster3D", 1);
        hashMap.put("app.com.brd", 1);
        hashMap.put("com.govpk.citizensportal", 1);
        hashMap.put("com.cybertron.acebrawlbattle", 1);
        hashMap.put("com.cybertron.shamansway", 1);
        hashMap.put("com.cybertron.doomsdayhero", 1);
        hashMap.put("com.zmtd.sssblock.ps", 1);
        hashMap.put("com.beachracingpinpin.yingjia", 1);
        hashMap.put("com.fairkash.loan.money.pesa", 1);
        hashMap.put("com.waterpuzzle1.pl", 1);
        hashMap.put("com.t2ksports.nba2k20and", 1);
        hashMap.put("com.ziipin.softkeyboard.sa", 1);
        hashMap.put("com.kwai.video", 1);
        hashMap.put("com.grability.rappi", 1);
        hashMap.put("com.bubbleshooter.popbubbles.shootbubblesgame", 1);
        hashMap.put("com.downlood.sav.whmedia", 1);
        hashMap.put("com.sankuai.meituan", 1);
        hashMap.put("com.cam001.selfie", 1);
        hashMap.put("com.cardfeed.video_public", 1);
        hashMap.put("com.roidapp.photogrid", 2);
        hashMap.put("com.cyberlink.youcammakeup", 1);
        hashMap.put("tv.twitch.android.app", 1);
        hashMap.put("com.scooper.fuuun", 1);
        hashMap.put("vStudio.Android.Camera360", 1);
        hashMap.put("ae.etisalat.switchtv", 2);
        hashMap.put("com.snb.alahlimobile", 2);
        hashMap.put("com.alahli.mobile.android", 2);
        hashMap.put("com.ss.android.article.news", 1);
        hashMap.put("com.scooper.fuuun", 2);
        hashMap.put("photo.photoeditor.snappycamera.prettymakeup", 1);
        hashMap.put("com.lenovo.anyshare.gps", 1);
        hashMap.put("com.teenpatti.hd.gold", 1);
        hashMap.put("com.ss.android.ugc.aweme.lite", 1);
        hashMap.put("com.tv.v18.viola", 2);
        hashMap.put("com.youdao.note", 1);
        hashMap.put("com.gridplus.collagemaker", 2);
        hashMap.put("com.blacklightsw.ludo", 1);
        hashMap.put("com.knockdown.bottle.game", 1);
        hashMap.put("com.pozitron.hepsiburada", 2);
        hashMap.put("com.mavi.kartus", 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getCompatibleModeDefaultValue(String packageName) {
        Integer defaultValue = COMPATIBLE_MODE_DEFAULT_VALUE_MAP.get(packageName);
        if (defaultValue != null) {
            return defaultValue.intValue();
        }
        return -1;
    }

    public static boolean isPkgInUnresizeableBlockList(String packageName) {
        if (!OS_FOLDABLE_SCREEN_SUPPORT || TextUtils.isEmpty(packageName)) {
            return false;
        }
        return RESIZE_MODE_UNRESIZEABLE_BLOCK_LIST.contains(packageName);
    }

    public static boolean shouldRestartPackage(String packageName) {
        if (!OS_FOLDABLE_SCREEN_SUPPORT || TextUtils.isEmpty(packageName)) {
            return false;
        }
        return FORCE_RESTART_PACKAGE_LIST.contains(packageName);
    }

    public static boolean isPkgInForceResizeableWhiteList(String packageName) {
        if (!OS_FOLDABLE_SCREEN_SUPPORT || TextUtils.isEmpty(packageName)) {
            return false;
        }
        return RESIZE_MODE_FORCE_RESIZEABLE_WHITE_LIST.contains(packageName);
    }

    public static boolean isPkgInUseDefaultNotchSizeList(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        return USE_DEFALUT_NOTCH_SIZE_PACKAGE.contains(packageName);
    }
}
