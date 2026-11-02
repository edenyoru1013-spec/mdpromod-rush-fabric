package net.mdpromod;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 场地按钮的纯几何命中，不依赖实体静态初始化。 */
final class DuelFieldControls {
    private DuelFieldControls() {
    }

    /** 控件命中用有厚度的盒，而不是只和顶面求交；侧边、键帽边缘也能正常按。 */
    static AABB hitBox(DuelFieldLayout.Control control) {
        double halfW = control.width() / 32.0 + 0.12;
        double halfD = control.depth() / 32.0 + 0.12;
        double x = control.x() / 16.0;
        double z = control.z() / 16.0;
        double top = Math.max(0.24, control.y() / 16.0 + 0.12);
        return new AABB(x - halfW, -0.02, z - halfD, x + halfW, top, z + halfD);
    }

    static DuelFieldLayout.Control clickedControl(Vec3 eye, Vec3 look, double maxDistance) {
        Vec3 end = eye.add(look.scale(maxDistance));
        double bestDistance = Double.MAX_VALUE;
        DuelFieldLayout.Control best = null;
        for (DuelFieldLayout.Control control : DuelFieldLayout.controls()) {
            var clip = hitBox(control).clip(eye, end);
            if (clip.isEmpty()) continue;
            double distance = clip.get().distanceToSqr(eye);
            if (distance >= bestDistance) continue;
            bestDistance = distance;
            best = control;
        }
        return best;
    }
}
