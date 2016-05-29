package jx.zero;

public interface DomainMemoryManager extends Portal {

    /**
     * Allocate memory for the fields of an object.
     * The first word in this memory is used for the
     * vtable pointer.
     */
    int allocObject(VMClass cl);

    /**
     * Allocate memory for an array.
     * The size of one array element is one word (32 Bits)
     */
    int allocArray(VMClass elementClass, int size);

    /**
     * Allocate memory for the thread control block.
     */
    int allocTCB(int sizeInBytes);

    /**
     * Allocate memory for a stack.
     */
    int allocStack(int sizeInBytes);

    /**
     * Allocate memory for the static fields of a class.
     * The size of a field is 32 Bits.
     */
    int allocStaticFields(int numberOfFields);

    /**
     * Allocate memory for the code of a method.
     */
    int allocCode(int numberOfCodeBytes);

    /**
     * Informs the memory manager that the current thread
     * is about to die.
     */
    void threadExiting();

}
