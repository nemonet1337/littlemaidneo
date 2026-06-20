package work.nemonet.littlemaidneo.client.screen;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.client.screen.component.*;
import work.nemonet.littlemaidneo.client.util.ClientScreenHelper;
import work.nemonet.littlemaidneo.entity.targeting.TargetIdentifier;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TargetTagを設定するためのスクリーン
 * 閉じたときにパケットで結果を返す
 */
public class TargetTagScreen extends AbstractFilterableListScreen<TargetTagScreen.TargetTagGUIElement> {
    private final Entity entity;
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags;

    public TargetTagScreen(Entity entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        super(Component.empty());
        this.entity = entity;
        this.targetTags = new HashMap<>(targetTags);
    }

    @Override
    protected void init() {
        assert this.minecraft != null;

        int searchInputHeight = 20;
        int widthStack = 1;
        int totalWidth = (int) (this.width * 0.8f);
        int elementWidth = totalWidth;
        int elementHeight = font.lineHeight + 48;

        // TargetTag用のFilterPredicate（エンティティタイプのキーと翻訳名で検索）
        FilterPredicate<TargetTagGUIElement> targetTagFilter = (targetTagGUIElement, filterText) -> {
            var type = targetTagGUIElement.getTargetIdentifier().getEntityType();
            String searchStr = (targetTagGUIElement.getTargetIdentifier().toString()
                    + "," + type.getDescription().getString()).toLowerCase();
            return searchStr.contains(filterText.toLowerCase());
        };

        // すべてのターゲットタグをリストアップ
        List<TargetTagGUIElement> elements = targetTags.entrySet().stream()
                .map(entry -> new TargetTagGUIElement(this.minecraft.font, entry.getKey(), entry.getValue(), elementWidth, elementHeight))
                .sorted(Comparator.comparing(e -> e.targetIdentifier.toString()))
                .collect(Collectors.toList());

        this.listGUI = FilterableListGUI.<TargetTagGUIElement>builder()
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x40000000);

        if (listGUI != null) {
            listGUI.extractRenderState(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (listGUI != null) {
            // 全てのtargetTagsを収集
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> updatedTargetTags = new HashMap<>();
            for (TargetTagGUIElement element : listGUI.getListGUI().getAllElements()) {
                updatedTargetTags.put(element.getTargetIdentifier(), element.getTags());
            }
            send(updatedTargetTags);
        }
    }

    public <T extends Entity & TargetTagManager> void send(
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> updatedTargetTags) {
        // noinspection unchecked
        NetworkHandler.sendSetTargetTagsC2S((T) entity, updatedTargetTags);
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
        private final Entity dummyEntity;

        public TargetTagGUIElement(Font textRenderer,
                TargetIdentifier targetIdentifier, Set<TargetingSystem.TargetTag> tags, int elementWidth, int elementHeight) {
            super(elementWidth, elementHeight);
            this.targetIdentifier = targetIdentifier;
            this.dummyEntity = targetIdentifier.getEntityType().create(Minecraft.getInstance().level, EntitySpawnReason.TRIGGERED);

            // 現在のタグセットから状態を決定
            this.attackState = determineAttackState(tags);
            this.weaponState = determineWeaponState(tags);
            this.approachState = determineApproachState(tags);

            this.buttons = new ArrayList<>(3);

            // 攻撃ボタン
            this.buttons.add(new LittleMaidScreen.IconButtonWidget(0, 0, this.attackState.icon,
                    Component.translatable("gui.littlemaidneo.target_tag.tags." + this.attackState.translationKey),
                    (b) -> this.attackState = this.attackState.next()));

            // 武器ボタン
            this.buttons.add(new LittleMaidScreen.IconButtonWidget(0, 0, this.weaponState.icon,
                    Component.translatable("gui.littlemaidneo.target_tag.tags." + this.weaponState.translationKey),
                    (b) -> this.weaponState = this.weaponState.next()));

            // 接近ボタン
            this.buttons.add(new LittleMaidScreen.IconButtonWidget(0, 0, this.approachState.icon,
                    Component
                            .translatable("gui.littlemaidneo.target_tag.tags." + this.approachState.translationKey),
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
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            Font textRenderer = Minecraft.getInstance().font;

            // エンティティタイプ名を表示
            var entityTypeName = Component.translatable(this.targetIdentifier.getEntityType().getDescriptionId());
            ClientScreenHelper.drawScrollingText(context, textRenderer, entityTypeName, this.x + 10, this.y + 4, this.width - 80, 0xFFFFFFFF, true);

            // ボタンの位置を設定してレンダリング（エンティティタイプ名の下）
            int buttonY = this.y + textRenderer.lineHeight + 8;
            int buttonX = this.x + 10;

            // 攻撃ボタンの状態を更新
            LittleMaidScreen.IconButtonWidget attackButton = (LittleMaidScreen.IconButtonWidget) buttons.getFirst();
            attackButton.setIconItem(attackState.icon);
            attackButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.littlemaidneo.target_tag.tags." + attackState.translationKey)));
            attackButton.setPosition(buttonX, buttonY);
            attackButton.extractRenderState(context, mouseX, mouseY, delta);
            buttonX += attackButton.getWidth() + 4;

            // 武器ボタンの状態を更新
            LittleMaidScreen.IconButtonWidget weaponButton = (LittleMaidScreen.IconButtonWidget) buttons.get(1);
            weaponButton.setIconItem(weaponState.icon);
            weaponButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.littlemaidneo.target_tag.tags." + weaponState.translationKey)));
            weaponButton.setPosition(buttonX, buttonY);
            weaponButton.extractRenderState(context, mouseX, mouseY, delta);
            buttonX += weaponButton.getWidth() + 4;

            // 接近ボタンの状態を更新
            LittleMaidScreen.IconButtonWidget approachButton = (LittleMaidScreen.IconButtonWidget) buttons.get(2);
            approachButton.setIconItem(approachState.icon);
            approachButton.setTooltip(Tooltip.create(
                    Component.translatable("gui.littlemaidneo.target_tag.tags." + approachState.translationKey)));
            approachButton.setPosition(buttonX, buttonY);
            approachButton.extractRenderState(context, mouseX, mouseY, delta);

            // 右側にMobのプレビューを表示
            if (this.dummyEntity instanceof LivingEntity living) {
                int previewX = this.x + this.width - 35;
                int previewY = this.y + this.height - 8;
                int scale = 18;
                try {
                    net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                            context,
                            previewX - 20, this.y + 4, previewX + 20, previewY,
                            scale, 0.0625f,
                            mouseX, mouseY,
                            living
                    );
                } catch (Exception e) {
                    // Ignore render exceptions for dummy entities
                }
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
            // ボタンのクリック処理を先にチェック
            for (Button buttonWidget : this.buttons) {
                if (buttonWidget.mouseClicked(event, handled)) {
                    return true;
                }
            }

            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                clickable.click(event.x(), event.y());
            }
            return super.mouseClicked(event, handled);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            // ボタンのクリック処理を先にチェック
            for (Button buttonWidget : this.buttons) {
                if (buttonWidget.mouseReleased(event)) {
                    return true;
                }
            }
            return super.mouseReleased(event);
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