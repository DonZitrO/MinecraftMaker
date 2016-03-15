package com.minecade.minecraftmaker.schematic.block;

/**
 * Represents a stack of BaseItems.
 *
 * <p>This class may be removed in the future.</p>
 */
public class BaseItemStack extends BaseItem {

    private int amount = 1;

    /**
     * Construct the object with default stack size of one, with data value of 0.
     *
     * @param id with data value of 0.
     */
    public BaseItemStack(int id) {
        super(id);
    }

    /**
     * Construct the object.
     *
     * @param id type ID
     * @param amount amount in the stack
     */
    public BaseItemStack(int id, int amount) {
        super(id);
        this.amount = amount;
    }

    /**
     * Construct the object.
     *
     * @param id type ID
     * @param amount amount in the stack
     * @param data data value
     */
    public BaseItemStack(int id, int amount, short data) {
        super(id, data);
        this.amount = amount;
    }

    /**
     * Get the number of items in the stack.
     * 
     * @return the amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Set the amount of items in the stack.
     * 
     * @param amount the amount to set
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
