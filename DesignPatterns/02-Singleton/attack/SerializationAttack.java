package attack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationAttack {

    public static void main(String[] args) throws Exception {
        // dcl: Serializable, no readResolve -> deserializing quietly makes a second instance.
        dcl.Counter dclInstance = dcl.Counter.getInstance();
        assert roundTrip(dclInstance) != dclInstance;
        System.out.println("serialization: broke dcl (no readResolve)");

        // holder: readResolve swaps the copy for the real instance.
        holder.Counter holderInstance = holder.Counter.getInstance();
        holderInstance.increment();
        holder.Counter back = roundTrip(holderInstance);
        assert back == holderInstance;
        assert back.count() == 1;
        System.out.println("serialization: holder survived via readResolve");

        // enum: survives with no extra code — enums serialize by name.
        enumsingleton.Counter e = enumsingleton.Counter.INSTANCE;
        assert roundTrip(e) == e;
        System.out.println("serialization: enum survived, zero code");

        System.out.println("serialization attack: all asserts passed");
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
