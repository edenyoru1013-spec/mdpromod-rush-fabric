package net.mdpromod;

/** 当前客户端在核心里的席位：0 = p1，1 = p2。只影响文案和“自己/对方”显示。 */
final class ClientDuelSeat {
    private static volatile int seat;

    private ClientDuelSeat() {
    }

    static void set(int value) {
        seat = value & 1;
    }

    static int get() {
        return seat;
    }
}
