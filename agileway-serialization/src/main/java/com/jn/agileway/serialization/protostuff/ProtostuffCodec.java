package com.jn.agileway.serialization.protostuff;

import com.jn.agileway.serialization.AbstractCodec;
import com.jn.agileway.serialization.CodecException;

public class ProtostuffCodec<T> extends AbstractCodec<T> {

    public ProtostuffCodec() {

    }

    public ProtostuffCodec(Class<T> targetType) {
        setTargetType(targetType);
    }

    @Override
    public byte[] encode(T obj) throws CodecException {
        try {
            return Protostuffs.serialize(obj);
        } catch (Throwable ex) {
            throw new CodecException(ex.getMessage(), ex);
        }
    }

    @Override
    public T decode(byte[] bytes) throws CodecException {
        return decode(bytes, (Class<T>) getTargetType());
    }

    @Override
    public T decode(byte[] bytes, Class<T> targetType) throws CodecException {
        try {
            return Protostuffs.deserialize(bytes, targetType);
        } catch (Throwable ex) {
            throw new CodecException(ex.getMessage(), ex);
        }
    }

}
