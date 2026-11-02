package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** 双席位卡面可见性：p2 必须拿到自己的 owner UUID，而不是永远显示成对手卡背。 */
class DuelSceneOwnerTest {

    @Test
    void bothSeatsKeepTheirOwnCardFaceOwner() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        DuelAnimationScene scene = new DuelAnimationScene();
        scene.setOwners(p1, p2);

        assertEquals(p1, scene.ownerOf(0));
        assertEquals(p2, scene.ownerOf(1));
        assertNull(scene.ownerOf(2));
        assertEquals(0, scene.viewerSide(p1));
        assertEquals(1, scene.viewerSide(p2));
        assertEquals(-1, scene.viewerSide(UUID.randomUUID()));
    }
}
