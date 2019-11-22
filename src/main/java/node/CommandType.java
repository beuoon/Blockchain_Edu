package node;

public enum CommandType {
    None(0), Block(1), Inv(2), GetBlock(3), GetData(4), Tx(5), Version(6);

    private final byte number;
    private CommandType(int number) {
        this.number = (byte)number;
    }

    public byte getNumber() {
        return number;
    }

    public static CommandType valueOf(int number) {
        for (CommandType ct : CommandType.values()) {
            if (ct.number == number)
                return ct;
        }

        return None;
    }
}
