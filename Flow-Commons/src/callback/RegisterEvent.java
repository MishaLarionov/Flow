package callback;

import java.util.UUID;

/**
 * Created by Netdex on 1/16/2016.
 */
public class RegisterEvent {
    public enum RegisterType {
        REGISTER,
        UNREGISTER
    }

    public final UUID UUID;
    public final RegisterType TYPE;

    public RegisterEvent(UUID UUID, RegisterType TYPE) {
        this.UUID = UUID;
        this.TYPE = TYPE;
    }

}
