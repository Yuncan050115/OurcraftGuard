package com.ourcraft.guard.processors;

import org.bukkit.Location;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家移动状态（每玩家一份，存于 ConcurrentHashMap）。
 *
 * 在原 VelocityGuard 基础上扩展了 abilityExemptUntilMs 字段，用于实现
 * 原版附魔/能力位移的免检窗口。
 */
public class PlayerMovementState {

    public double trackedSpeed;
    public double trackedVelocityY;
    public boolean wasOnGround;
    public Location lastPosition;
    public Location lastValidPosition;
    public long lastPacketMs;
    public double violationBuffer;

    public long lastDamageMs;
    public boolean lastDragonDamage;
    public long lastRiptideMs;
    public boolean wasGliding;
    public long elytraLandingMs;
    public long lastSlimeContactMs;

    public long blockedUntilMs;
    public long settleUntilMs;
    public volatile boolean awaitingTeleport;
    public int teleportAnchorTxnId;

    public boolean awaitingSetback;
    public Location setbackTarget;
    public long lastSetbackMs;
    public int setbackAnchorTxnId;

    public int airTicks;
    public boolean wasInCreative;

    public final ConcurrentLinkedDeque<long[]> transactionsSent = new ConcurrentLinkedDeque<>();
    private final AtomicInteger transactionIdCounter = new AtomicInteger(0x56470000);

    public volatile long playerClockAtLeast;
    public volatile long transactionPingNanos;
    public int lastTransactionReceivedId;

    public long timerBalanceRealTime;
    public long lastMovementPlayerClock;
    public long knownPlayerClockTime;
    public boolean hasGottenMovementAfterTransaction;
    public double timerViolations;

    public PlayerMovementState(Location startPosition, long currentTimeMs) {
        this.lastPosition      = startPosition.clone();
        this.lastValidPosition = startPosition.clone();
        this.lastPacketMs      = currentTimeMs;
        initTimer();
    }

    private void initTimer() {
        long nowNano = System.nanoTime();
        this.playerClockAtLeast        = nowNano;
        this.lastMovementPlayerClock   = nowNano;
        this.knownPlayerClockTime      = nowNano;
        this.timerBalanceRealTime      = nowNano - 1_000_000_000L;
        this.hasGottenMovementAfterTransaction = false;
        this.timerViolations           = 0.0;
    }

    public void reset(Location position, long currentTimeMs) {
        this.trackedSpeed     = 0.0;
        this.trackedVelocityY = 0.0;
        this.wasOnGround      = true;
        this.lastPosition     = position.clone();
        this.lastValidPosition = position.clone();
        this.lastPacketMs     = currentTimeMs;
        this.violationBuffer  = 0.0;
        this.airTicks         = 0;
        this.timerViolations  = 0.0;
        this.awaitingSetback  = false;
        this.awaitingTeleport = false;
    }

    public int nextTransactionId() {
        return transactionIdCounter.getAndIncrement();
    }

    public int lastSentTransactionId() {
        return transactionIdCounter.get() - 1;
    }

    public boolean transactionAcknowledged(int anchorId) {
        return Integer.compareUnsigned(lastTransactionReceivedId, anchorId) > 0;
    }

    public void onTransactionSent(int id, long sendNano) {
        transactionsSent.add(new long[]{id, sendNano});
        while (transactionsSent.size() > 400) {
            transactionsSent.pollFirst();
        }
    }

    public boolean onTransactionResponse(int id, long nowNano) {
        boolean found = false;
        for (long[] pair : transactionsSent) {
            if (pair[0] == id) { found = true; break; }
        }
        if (!found) return false;

        long[] data;
        do {
            data = transactionsSent.pollFirst();
            if (data == null) break;
            playerClockAtLeast   = data[1];
            transactionPingNanos = nowNano - data[1];
            lastTransactionReceivedId = (int) data[0];
        } while (data[0] != id);
        return true;
    }
}
