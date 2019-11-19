import java.util.LinkedList;

public class Blockchain {
    private LinkedList<Block> blocks = new LinkedList();

    public Blockchain() throws Exception{
        blocks.add(new Block());
    }

    public void addBlock(String data) throws Exception{
        Block prevBlock = blocks.peek();
        Block newBlock = new Block(data, prevBlock.getHash());
        blocks.add(newBlock);
    }

    public LinkedList<Block> getBlocks() {
        return blocks;
    }

}
