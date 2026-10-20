package proxy.solution;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;

// A dynamic proxy: one class that can stand in for ANY interface, generated at runtime.
// This is how Spring's @Transactional and Mockito's mocks work.
public final class CallCounter {

    private CallCounter() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T counting(Class<T> iface, T target, Map<String, Integer> counts) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> {
                    counts.merge(method.getName(), 1, Integer::sum);
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();     // rethrow the real exception, not the reflection wrapper
                    }
                });
    }
}
