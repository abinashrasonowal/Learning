package srp.solution;

// ONE reason to change: the schema or the storage engine changes.
public class DatabaseRepository {
    public void save(Invoice invoice){
        System.out.println("saved invoice " + invoice.getId()
                + " of " + invoice.calculateTotal() + " to db");
    }
}
