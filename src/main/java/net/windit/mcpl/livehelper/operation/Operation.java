package net.windit.mcpl.livehelper.operation;

import java.util.HashMap;
import java.util.Map;

public abstract class Operation {
    protected final String data;

    protected Operation(String data) {
        this.data = data;
    }

    public abstract void doIt();

    @Override
    public String toString() {
        return getClass().getTypeName() + "{" + "data='" + data + '\'' + '}';
    }

    public enum Type {
        COMMAND("command", OperationCommand.class),
        HEAL("heal", OperationHeal.class),
        HEALALL("healall", OperationHealAll.class),
        FEED("feed", OperationFeed.class),
        EFFECT("effect", OperationEffect.class),
        SPAWN("spawn", OperationSpawn.class),
        SPAWNFORALL("spawnforall", OperationSpawnForAll.class);
        private static final Map<String, Type> TYPES = new HashMap<>();

        static {
            for (Type type : values()) {
                TYPES.put(type.name().toLowerCase(), type);
            }
        }

        private final String name;
        private final Class<? extends Operation> clazz;

        Type(String name, Class<? extends Operation> clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        public static Type getTypeByName(String name) {
            return TYPES.get(name.toLowerCase());
        }

        public Class<? extends Operation> getClazz() {
            return clazz;
        }

        public Operation createOperation(String data) {
            try {
                return clazz.getDeclaredConstructor(String.class).newInstance(data);
            } catch (Exception e) {
                return null;
            }
        }
    }
}