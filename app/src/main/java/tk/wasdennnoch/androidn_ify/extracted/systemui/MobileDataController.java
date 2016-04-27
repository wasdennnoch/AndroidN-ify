package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.format.Time;

import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.XposedHook;

import static android.net.NetworkStatsHistory.FIELD_RX_BYTES;
import static android.net.NetworkStatsHistory.FIELD_TX_BYTES;
import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;

public class MobileDataController {
    private static final String TAG = "MobileDataController";

    private static final long DEFAULT_WARNING_LEVEL = 2L * 1024 * 1024 * 1024;
    private static final int FIELDS = FIELD_RX_BYTES | FIELD_TX_BYTES;
    private static final StringBuilder PERIOD_BUILDER = new StringBuilder(50);
    private static final java.util.Formatter PERIOD_FORMATTER = new java.util.Formatter(
            PERIOD_BUILDER, Locale.getDefault());

    private final Context mContext;
    private final INetworkStatsService mStatsService;
    private final NetworkPolicyManager mPolicyManager;

    private INetworkStatsSession mSession;

    @SuppressLint("InlinedApi") // Is available in pre 23, but hidden
    public MobileDataController(Context context) {
        mContext = context;
        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyManager = NetworkPolicyManager.from(mContext);
    }

    @SuppressWarnings("deprecation")
    private static Time addMonth(Time t, int months) {
        final Time rt = new Time(t);
        rt.set(t.monthDay, t.month + months, t.year);
        rt.normalize(false);
        return rt;
    }

    private static String historyEntryToString(NetworkStatsHistory.Entry entry) {
        return entry == null ? null : "Entry[" +
                "bucketDuration=" + entry.bucketDuration +
                ",bucketStart=" + entry.bucketStart +
                ",activeTime=" + entry.activeTime +
                ",rxBytes=" + entry.rxBytes +
                ",rxPackets=" + entry.rxPackets +
                ",txBytes=" + entry.txBytes +
                ",txPackets=" + entry.txPackets +
                ",operations=" + entry.operations +
                ']';
    }

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Class<?> SubscriptionManager = XposedHelpers.findClass("android.telephony.SubscriptionManager", null);
        int getDefaultDataSubId = (int) XposedHelpers.callStaticMethod(SubscriptionManager, "getDefaultDataSubId");
        return (String) XposedHelpers.callMethod(tele, "getSubscriberId", getDefaultDataSubId);
    }

    private INetworkStatsSession getSession() {
        if (mSession == null) {
            try {
                mSession = mStatsService.openSession();
            } catch (RemoteException | RuntimeException e) {
                XposedHook.logW(TAG, "Failed to open stats session");
            }
        }
        return mSession;
    }

    private DataUsageInfo warn(String msg) {
        XposedHook.logW(TAG, "Failed to get data usage, " + msg);
        return null;
    }

    public DataUsageInfo getDataUsageInfo() {
        final String subscriberId = getActiveSubscriberId(mContext);
        if (subscriberId == null) {
            return warn("no subscriber id");
        }
        final INetworkStatsSession session = getSession();
        if (session == null) {
            return warn("no stats session");
        }
        final NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        final NetworkPolicy policy = findNetworkPolicy(template);
        try {
            final NetworkStatsHistory history = mSession.getHistoryForNetwork(template, FIELDS);
            final long now = System.currentTimeMillis();
            final long start, end;
            if (policy != null && policy.cycleDay > 0) {
                // period = determined from cycleDay
                XposedHook.logD(TAG, "Cycle day=" + policy.cycleDay + " tz="
                        + policy.cycleTimezone);
                //noinspection deprecation
                final Time nowTime = new Time(policy.cycleTimezone);
                nowTime.setToNow();
                //noinspection deprecation
                final Time policyTime = new Time(nowTime);
                policyTime.set(policy.cycleDay, policyTime.month, policyTime.year);
                policyTime.normalize(false);
                if (nowTime.after(policyTime)) {
                    start = policyTime.toMillis(false);
                    end = addMonth(policyTime, 1).toMillis(false);
                } else {
                    start = addMonth(policyTime, -1).toMillis(false);
                    end = policyTime.toMillis(false);
                }
            } else {
                // period = last 4 wks
                end = now;
                start = now - DateUtils.WEEK_IN_MILLIS * 4;
            }
            final long callStart = System.currentTimeMillis();
            final NetworkStatsHistory.Entry entry = history.getValues(start, end, now, null);
            final long callEnd = System.currentTimeMillis();
            XposedHook.logD(TAG, String.format("history call from %s to %s now=%s took %sms: %s",
                        new Date(start), new Date(end), new Date(now), callEnd - callStart,
                        historyEntryToString(entry)));
            if (entry == null) {
                return warn("no entry data");
            }
            final long totalBytes = entry.rxBytes + entry.txBytes;
            final DataUsageInfo usage = new DataUsageInfo();
            usage.usageLevel = totalBytes;
            usage.period = formatDateRange(start, end);
            if (policy != null) {
                usage.limitLevel = policy.limitBytes > 0 ? policy.limitBytes : 0;
                usage.warningLevel = policy.warningBytes > 0 ? policy.warningBytes : 0;
            } else {
                usage.warningLevel = DEFAULT_WARNING_LEVEL;
            }
            return usage;
        } catch (RemoteException e) {
            return warn("remote call failed");
        }
    }

    private NetworkPolicy findNetworkPolicy(NetworkTemplate template) {
        if (mPolicyManager == null || template == null) return null;
        final NetworkPolicy[] policies = mPolicyManager.getNetworkPolicies();
        if (policies == null) return null;
        for (final NetworkPolicy policy : policies) {
            if (policy != null && template.equals(policy.template)) {
                return policy;
            }
        }
        return null;
    }

    private String formatDateRange(long start, long end) {
        final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;
        synchronized (PERIOD_BUILDER) {
            PERIOD_BUILDER.setLength(0);
            return DateUtils.formatDateRange(mContext, PERIOD_FORMATTER, start, end, flags, null)
                    .toString();
        }
    }

    public static class DataUsageInfo {
        public String period;
        public long limitLevel;
        public long warningLevel;
        public long usageLevel;
    }

}