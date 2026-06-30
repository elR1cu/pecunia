package com.pecunia.id;

import com.github.f4b6a3.uuid.alt.GUID;
import com.pecunia.shared.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class Uuidv7IdGenerator implements IdGenerator {
    @Override
    public UUID newId() {
        return GUID.v7().toUUID();
    }
}
