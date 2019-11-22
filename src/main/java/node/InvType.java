package node;

public enum InvType {
    None(0), Block(1), Tx(2);

    private final byte number;
    private InvType(int number) {
        this.number = (byte)number;
    }

    public byte getNumber() {
        return number;
    }

    public static InvType valueOf(int number) {
        for (InvType it : InvType.values()) {
            if (it.number == number)
                return it;
        }

        return None;
    }
}
