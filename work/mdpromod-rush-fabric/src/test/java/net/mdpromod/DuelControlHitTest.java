package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** 阶段键 / 确认键的射线命中：左右两侧都必须能按到自己的按钮。 */
class DuelControlHitTest {

    @Test
    void p2EndButtonCanBeClickedFromItsSide() {
        DuelFieldLayout.Control end = DuelFieldLayout.controlById("p2_phase_ep").orElseThrow();
        Vec3 target = new Vec3(end.x() / 16.0, end.y() / 16.0, end.z() / 16.0);
        Vec3 eye = new Vec3(0.0, 1.62, -9.0);
        Vec3 look = target.subtract(eye).normalize();

        DuelFieldLayout.Control hit = DuelFieldControls.clickedControl(eye, look, 48.0);
        assertNotNull(hit);
        assertEquals("p2_phase_ep", hit.id());
    }

    @Test
    void p1BattleButtonCanBeClickedFromItsSide() {
        DuelFieldLayout.Control battle = DuelFieldLayout.controlById("p1_phase_bp").orElseThrow();
        Vec3 target = new Vec3(battle.x() / 16.0, battle.y() / 16.0, battle.z() / 16.0);
        Vec3 eye = new Vec3(0.0, 1.62, 9.0);
        Vec3 look = target.subtract(eye).normalize();

        DuelFieldLayout.Control hit = DuelFieldControls.clickedControl(eye, look, 48.0);
        assertNotNull(hit);
        assertEquals("p1_phase_bp", hit.id());
    }

    @Test
    void everyPhaseAndConfirmButtonIsClickableForBothSides() {
        for (String side : new String[]{"p1", "p2"}) {
            Vec3 eye = new Vec3(0.0, 1.62, side.equals("p1") ? 9.0 : -9.0);
            for (DuelFieldLayout.Control control : DuelFieldLayout.controls()) {
                if (!side.equals(control.owner())) continue;
                if (!"phase".equals(control.kind()) && !"confirm".equals(control.kind())) continue;
                Vec3 target = new Vec3(control.x() / 16.0, control.y() / 16.0, control.z() / 16.0);
                DuelFieldLayout.Control hit = DuelFieldControls.clickedControl(
                        eye, target.subtract(eye).normalize(), 48.0);
                assertNotNull(hit, control.id());
                assertEquals(control.id(), hit.id(), side + " 的 " + control.label());
            }
        }
    }
}
