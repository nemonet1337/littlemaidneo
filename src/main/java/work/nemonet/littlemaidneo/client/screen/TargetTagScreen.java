package work.nemonet.littlemaidneo.client.screen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.client.screen.component.*;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.network.C2SSetTargetTagsPacket;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TargetTagを設定するためのスクリーン
 * 閉じたときにパケットで結果を返す
 */
@OnlyIn(Dist.CLIENT)
public class TargetTagScreen extends Screen {
    private final Entity entity;
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags;
    private FilterableListGUI<TargetTagGUIElement> targetTagGui;

    public TargetTagScreen(Entity entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        super(Component.empty());
        this.entity = entity;
        this.targetTags = new HashMap<>(targetTags);
    }

    @Override
    protected void init() {
        assert this.minecraft != null;

        int searchInputHeight = 20;
        int elementWidth = font.lineHeight * 15;
        int elementHeight = font.lineHeight + 40;
        int widthStack = Mth.floor(this.width * 0.8f / elementWidth);
        int totalWidth = elementWidth * widthStack;

        // TargetTag用のFilterPredicate（エンティティタイプのキーと翻訳名で検索）
        FilterPredicate<TargetTagGUIElement> targetTagFilter = (targetTagGUIElement, filterText) -> {
            var type = targetTagGUIElement.getTargetIdentifier().getEntityType();
            String searchStr = (targetTagGUIElement.getTargetIdentifier().toString()
                    + "," + type.getDescription().getString()).toLowerCase();
            return searchStr.contains(filterText.toLowerCase());
        };

        // すべてのターゲットタグをリストアップ
        List<TargetTagGUIElement> elements = targetTags.entrySet().stream()
                .map(entry -> new TargetTagGUIElement(this.minecraft.font, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(e -> e.targetIdentifier.toString()))
                .collect(Collectors.toList());

        this.targetTagGui = FilterableListGUI.<TargetTagGUIElement>builder()
                .position(Mth.floor((this.width - totalWidth) / 2f), 0)
                .size(totalWidth, this.height)
                .elementSize(elementWidth, elementHeight)
                .items(elements)
                .filterBy(targetTagFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder("Search entities...")
                .build();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x40000000);

        targetTagGui.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return targetTagGui.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return targetTagGui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return targetTagGui.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return targetTagGui.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return targetTagGui.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (targetTagGui.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        // 全てのtargetTagsを収集
        Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> updatedTargetTags = new HashMap<>();
        for (TargetTagGUIElement element : targetTagGui.getListGUI().getAllElements()) {
            updatedTargetTags.put(element.getTargetIdentifier(), element.getTags());
        }
        send(updatedTargetTags);
    }

    public <T extends Entity & TargetTagManager> void send(
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> updatedTargetTags) {
        // noinspection unchecked
        C2SSetTargetTagsPacket.sendC2SPacket((T) entity, updatedTargetTags);
    }

    public static class TargetTagGUIElement extends GUIElement implements ListGUIElement {
        // 攻撃カテゴリの状態
        private enum AttackState {
            ATTACK_PROHIBITED("attack_prohibited", Items.BARRIER.getDefaultInstance()),
            PREEMPTIVE_ATTACK_PROHIBITED("preemptive_attack_prohibited", Items.SHIELD.getDefaultInstance()),
            PREEMPTIVE_ATTACK_ALLOWED("preemptive_attack_allowed", Items.IRON_SWORD.getDefaultInstance());

            private final String translationKey;
            private final ItemStack icon;

            AttackState(String translationKey, ItemStack icon) {
                this.translationKey = translationKey;
                this.icon = icon;
            }

            public AttackState next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }

        // 武器カテゴリの状態
        private enum WeaponState {
            NO_WEAPON_RESTRICTION("no_weapon_restriction", Items.AIR.getDefaultInstance()),
            MELEE_WEAPON_PROHIBITED("melee_weapon_prohibited", Items.IRON_SWORD.getDefaultInstance()),
            RANGED_WEAPON_PROHIBITED("ranged_weapon_prohibited", Items.BOW.getDefaultInstance());

            private final String translationKey;
            private final ItemStack icon;

            WeaponState(String translationKey, ItemStack icon) {
                this.translationKey = translationKey;
                this.icon = icon;
            }

            public WeaponState next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }

        // 接近カテゴリの状態
        private enum ApproachState {
            APPROACH_ALLOWED("approach_allowed", Items.AIR.getDefaultInstance()),
            APPROACH_PROHIBITED("approach_prohibited", Items.CREEPER_HEAD.getDefaultInstance());

            private final String translationKey;
            private final ItemStack icon;

            ApproachState(String translationKey, ItemStack icon) {
                this.translationKey = translationKey;
                this.icon = icon;
            }

            public ApproachState next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }

        private final TargetIdentifier targetIdentifier;
        private final MarginedClickable clickable = new MarginedClickable(4);
        private final List<Button> buttons;
        private AttackState attackState;
        private WeaponState weaponState;
        private ApproachState approachState;

        public TargetTagGUIElement(Font textRenderer,
                TargetIdentifier targetIdentifier, Set<TargetingSystem.TargetTag> tags) {
            super(textRenderer.lineHeight * 15, textRenderer.lineHeight + 40);
            this.targetIdentifier = targetIdentifier;

            // 現在のタグセットから状態を決定
            this.attackState = determineAttackState(tags);
            this.weaponState = determineWeaponState(tags);
            this.approachState = determineApproachState(tags);

            this.buttons = new ArrayList<>(3);

            // 攻撃ボタン
            this.buttons.add(new LittleMaidScreen.IconButtonWidget(0, 0, this.attackState.icon,
                    Component.translatable("gui.littlemaidrebirth.target_tag.tags." + this.attackState.translationKey),
                    (b) -> this.attackState = this.attackState.next()));

            // 武器ボタン
            this.buttons.add(new LittleMaidScreen.IconButtonWidget(0, 0, this.weaponState.icon,
                    Component.translatable("gui.littlemaidrebirth.target_tag.tags." + this.weaponState.translationKey),
                    (b) -> this.weaponState = this.weaponState.next()));

            // 接近ボタン
            this.buttons.add(new LittleMaidScreen.IconButtonWidget(0, 0, this.approachState.icon,
                    Component
                            .translatable("gui.littlemaidrebirth.target_tag.tags." + this.approachState.translationKey),
                    (b) -> this.approachState = this.approachState.next()));
        }

        private AttackState determineAttackState(Set<TargetingSystem.TargetTag> tags) {
            if (tags.contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED)) {
                return AttackState.ATTACK_PROHIBITED;
            } else if (tags.contains(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED)) {
                return AttackState.PREEMPTIVE_ATTACK_PROHIBITED;
            } else {
                return AttackState.PREEMPTIVE_ATTACK_ALLOWED;
            }
        }

        private WeaponState determineWeaponState(Set<TargetingSystem.TargetTag> tags) {
            boolean meleeProhibited = tags.contains(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
            boolean rangedProhibited = tags.contains(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);

            if (meleeProhibited && !rangedProhibited) {
                return WeaponState.MELEE_WEAPON_PROHIBITED;
            } else if (!meleeProhibited && rangedProhibited) {
                return WeaponState.RANGED_WEAPON_PROHIBITED;
            } else {
                return WeaponState.NO_WEAPON_RESTRICTION;
            }
        }

        private ApproachState determineApproachState(Set<TargetingSystem.TargetTag> tags) {
            return tags.contains(TargetingSystem.TargetTag.APPROACH_PROHIBITED)
                    ? ApproachState.APPROACH_PROHIBITED
                    : ApproachState.APPROACH_ALLOWED;
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            Font textRenderer = Minecraft.getInstance().font;

            // エンティティタイプ名を表示
            var entityTypeName = Component.translatable(this.targetIdentifier.getEntityType().getDescriptionId());
            int textWidth = textRenderer.width(entityTypeName);
            if (textWidth <= this.width) {
                context.drawString(textRenderer, entityTypeName,
                        this.x, this.y, 0xFFFFFFFF, true);
            } else {
                // 長すぎるテキストをスクロール表示
                double seconds = Util.getMillis() / 1000.0;
                var entityTypeNameStr = entityTypeName.getString();

                // スクロール速度 (ピクセル/秒)
                double scrollSpeed = 20.0;

                // 表示可能な幅（少し余裕を持たせる）
                int displayWidth = this.width - 8;

                // スクロールが必要な距離
                int scrollDistance = textWidth - displayWidth;

                // 一往復にかかる時間を計算（テキスト幅 + 表示幅分だけスクロール）
                double cycleTime = (scrollDistance + displayWidth) / scrollSpeed;

                // 現在のサイクル内での位置
                double cyclePosition = (seconds % cycleTime) / cycleTime;

                // スクロールオフセットを計算（左→右→左のパターン）
                int scrollOffset;
                if (cyclePosition < 0.8) {
                    // 80%の時間で左から右へスクロール
                    scrollOffset = (int) (cyclePosition / 0.8 * scrollDistance);
                } else {
                    // 20%の時間で一時停止
                    scrollOffset = scrollDistance;
                }

                // クリップ領域を設定してテキストを描画
                context.enableScissor(this.x, this.y, this.x + displayWidth, this.y + textRenderer.lineHeight);
                context.drawString(textRenderer, entityTypeNameStr,
                        this.x - scrollOffset, this.y, 0xFFFFFFFF, true);
                context.disableScissor();
            }

            // ボタンの位置を設定してレンダリング（エンティティタイプ名の下）
            int buttonY = this.y + textRenderer.lineHeight;
            int buttonX = this.x;

            // 攻撃ボタンの状態を更新
            LittleMaidScreen.IconButtonWidget attackButton = (LittleMaidScreen.IconButtonWidget) buttons.get(0);
            attackButton.setIconItem(attackState.icon);
            attackButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.littlemaidrebirth.target_tag.tags." + attackState.translationKey)));
            attackButton.setPosition(buttonX, buttonY);
            attackButton.render(context, mouseX, mouseY, delta);
            buttonX += attackButton.getWidth();

            // 武器ボタンの状態を更新
            LittleMaidScreen.IconButtonWidget weaponButton = (LittleMaidScreen.IconButtonWidget) buttons.get(1);
            weaponButton.setIconItem(weaponState.icon);
            weaponButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.littlemaidrebirth.target_tag.tags." + weaponState.translationKey)));
            weaponButton.setPosition(buttonX, buttonY);
            weaponButton.render(context, mouseX, mouseY, delta);
            buttonX += weaponButton.getWidth();

            // 接近ボタンの状態を更新
            LittleMaidScreen.IconButtonWidget approachButton = (LittleMaidScreen.IconButtonWidget) buttons.get(2);
            approachButton.setIconItem(approachState.icon);
            approachButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.littlemaidrebirth.target_tag.tags." + approachState.translationKey)));
            approachButton.setPosition(buttonX, buttonY);
            approachButton.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // ボタンのクリック処理を先にチェック
            for (Button buttonWidget : this.buttons) {
                if (buttonWidget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                clickable.click(mouseX, mouseY);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            // ボタンのクリック処理を先にチェック
            for (Button buttonWidget : this.buttons) {
                if (buttonWidget.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void setSelected(boolean b) {
            // 必要に応じて実装
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        public TargetIdentifier getTargetIdentifier() {
            return targetIdentifier;
        }

        public Set<TargetingSystem.TargetTag> getTags() {
            Set<TargetingSystem.TargetTag> tags = new HashSet<>();

            // 攻撃状態から対応するタグを追加
            switch (attackState) {
                case ATTACK_PROHIBITED -> tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                case PREEMPTIVE_ATTACK_PROHIBITED -> tags.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                case PREEMPTIVE_ATTACK_ALLOWED -> {
                    // 先制攻撃許可の場合、何も追加しない
                }
            }

            // 武器状態から対応するタグを追加
            switch (weaponState) {
                case MELEE_WEAPON_PROHIBITED -> tags.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
                case RANGED_WEAPON_PROHIBITED -> tags.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
                case NO_WEAPON_RESTRICTION -> {
                    // 武器制限なしの場合、何も追加しない
                }
            }

            // 接近状態から対応するタグを追加
            switch (approachState) {
                case APPROACH_PROHIBITED -> tags.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
                case APPROACH_ALLOWED -> {
                    // 接近許可の場合、何も追加しない
                }
            }

            return tags;
        }
    }
}