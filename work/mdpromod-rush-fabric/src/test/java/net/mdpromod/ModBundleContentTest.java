package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** 同大小资源也必须按内容判定，避免新旧 duel_host.dll 同字节数时跳过覆盖。 */
class ModBundleContentTest {

    @Test
    void sameSizeDifferentBytesAreNotEqual() throws Exception {
        byte[] oldDll = "AAAA".getBytes(StandardCharsets.UTF_8);
        byte[] newDll = "BBBB".getBytes(StandardCharsets.UTF_8);
        assertFalse(ModBundle.streamsEqual(
                new ByteArrayInputStream(oldDll), new ByteArrayInputStream(newDll)));
    }

    @Test
    void identicalBytesAreEqual() throws Exception {
        byte[] bytes = "same-content".getBytes(StandardCharsets.UTF_8);
        assertTrue(ModBundle.streamsEqual(
                new ByteArrayInputStream(bytes), new ByteArrayInputStream(bytes)));
    }
}
