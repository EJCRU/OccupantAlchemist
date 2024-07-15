package org.spoofer.alchemist;

public final class EntryHash {
    private final Integer chance;
    private final Integer discount;

    public EntryHash(final Integer integer1, final Integer integer2) {
        this.chance = integer1;
        this.discount = integer2;
    }

    public Integer getInteger1() {
        return chance;
    }

    public Integer getInteger2() {
        return discount;
    }
}
