package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/** 效果选卡时，卡号可见性必须相对当前被问的席位。 */
class PromptVisibilityTest {

    @Test
    void p2OwnHandIsVisibleAndP1HandIsHidden() {
        JsonObject own = JsonParser.parseString(
                "{\"controller\":1,\"location\":2,\"position\":10,\"code\":120300030}")
                .getAsJsonObject();
        JsonObject opponent = JsonParser.parseString(
                "{\"controller\":0,\"location\":2,\"position\":10,\"code\":120264052}")
                .getAsJsonObject();

        assertEquals(120300030, DuelHostProcess.visibleCode(own, 1));
        assertEquals(0, DuelHostProcess.visibleCode(opponent, 1));
    }

    @Test
    void p1OwnHandIsVisibleAndP2HandIsHidden() {
        JsonObject own = JsonParser.parseString(
                "{\"controller\":0,\"location\":2,\"position\":10,\"code\":120264052}")
                .getAsJsonObject();
        JsonObject opponent = JsonParser.parseString(
                "{\"controller\":1,\"location\":2,\"position\":10,\"code\":120300030}")
                .getAsJsonObject();

        assertEquals(120264052, DuelHostProcess.visibleCode(own, 0));
        assertEquals(0, DuelHostProcess.visibleCode(opponent, 0));
    }

    @Test
    void publicGraveyardStaysVisible() {
        JsonObject grave = JsonParser.parseString(
                "{\"controller\":0,\"location\":16,\"position\":5,\"code\":120264052}")
                .getAsJsonObject();
        assertEquals(120264052, DuelHostProcess.visibleCode(grave, 1));
    }
}
