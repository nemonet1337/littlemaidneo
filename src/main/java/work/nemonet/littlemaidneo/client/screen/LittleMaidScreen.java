package work.nemonet.littlemaidneo.client.screen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.client.screen.SoundPackSelectScreen;
import work.nemonet.littlemaidneo.util.Tuple;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidScreenHandler;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.network.*;

import java.util.Optional;
import java.util.function.Supplier;

//TODO モード名表示/移動状態をアイコンで表記
//TODO ストライキ時の表示改善
@OnlyIn(Dist.CLIENT)
public class LittleMaidScreen extends AbstractContainerScreen<LittleMaidScreenHandler> {
    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath("lmreengaged",
            "textures/gui/container/littlemaidinventory2.png");
    private static final ResourceLocation ICONS = ResourceLocation.parse("textures/gui/icons.png");
    private static final ItemStack ARMOR = Items.LEATHER_CHESTPLATE.getDefaultInstance();
    private static final ItemStack BOOK = Items.BOOK.getDefaultInstance();
    private static final ItemStack NOTE = Items.NOTE_BLOCK.getDefaultInstance();
    private static final ItemStack FEATHER = Items.FEATHER.getDefaultInstance();
    private static final ItemStack IRON_SWORD = Items.IRON_SWORD.getDefaultInstance();
    private static final ItemStack IRON_AXE = Items.IRON_AXE.getDefaultInstance();
    private static final ItemStack CHEST = Items.CHEST.getDefaultInstance();
    private final LittleMaidEntity owner;
    private Component stateText;
    private final MovingMode prevMovingMode;
    private MovingMode movingMode;
    private int workItemSlotSize;
    private boolean isSettingWISS;

    public LittleMaidScreen(LittleMaidScreenHandler screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.imageHeight = 208;
        owner = screenContainer.getGuiEntity();
        workItemSlotSize = screenContainer.getWorkItemSlotSize();
        prevMovingMode = movingMode = owner.getMovingMode();
    }

    @Override
    protected void init() {
        super.init();
        if (owner == null) {
            minecraft.setScreen(null);
            return;
        }
        int left = (int) ((this.width - imageWidth) / 2F) - 5;
        int right = (int) ((this.width - imageWidth) / 2F) + imageWidth + 5;
        int top = (int) ((this.height - imageHeight) / 2F);
        int size = 20;
        int layer = -1;
        this.addRenderableWidget(new IconButtonWidget(left - size, top + size * ++layer, BOOK,
                Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.open_target_tag_setting"),
                button -> OpenTargetTagScreenPacket.sendC2SPacket(this.minecraft.player)));
        this.addRenderableWidget(new IconButtonWidget(left - size, top + size * ++layer, NOTE,
                Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.open_sound_pack_select"),
                button -> minecraft.setScreen(new SoundPackSelectScreen<>(title, owner))));
        this.addRenderableWidget(new IconButtonWidget(left - size, top + size * ++layer, ARMOR,
                Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.open_model_select"),
                button -> minecraft.setScreen(new ModelSelectScreen<>(title, owner.level(), owner))));
        this.addRenderableWidget(new IconButtonWidget(left - size, top + size * ++layer, FEATHER,
                Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.change_moving_mode"),
                button -> {
                    if (this.owner.isStrike()) {
                        return;
                    }
                    switch (movingMode) {
                        case ESCORT -> movingMode = MovingMode.FREEDOM;
                        case FREEDOM -> movingMode = MovingMode.TRACER;
                        case TRACER -> movingMode = MovingMode.ESCORT;
                    }
                    stateText = getStateText();
                }));
        this.addRenderableWidget(new IconButtonWidget(left - size, top + size * ++layer, FEATHER,
                Component.empty(),
                button -> C2SSetBloodSuckPacket.sendC2SPacket(this.owner, !this.owner.isBloodSuck())) {
            private static final Component changeBloodSuck = Component
                    .translatable("gui.littlemaidrebirth.littlemaid.tooltip.change_blood_suck");
            private static final Component toBloodSuck = changeBloodSuck.copy().append(
                    Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.change_blood_suck.to_blood_suck"));
            private static final Component isBloodSuck = changeBloodSuck.copy().append(
                    Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.change_blood_suck.is_blood_suck"));

            @Override
            public ItemStack getIconItem() {
                return LittleMaidScreen.this.owner.isBloodSuck() ? IRON_AXE : IRON_SWORD;
            }

            @Override
            protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
                super.renderWidget(context, mouseX, mouseY, delta);

                setTooltip(Tooltip.create(LittleMaidScreen.this.owner.isBloodSuck() ? isBloodSuck : toBloodSuck));
            }
        });
        layer = -1;
        this.addRenderableWidget(new IconButtonWidget(right, top + size * ++layer, BOOK,
                Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.open_maid_manager"),
                button -> OpenMaidManagerScreenPacket.sendC2SPacket()));

        this.addRenderableWidget(new IconButtonWidget(right, top + 75, CHEST,
                Component.translatable("gui.littlemaidrebirth.littlemaid.tooltip.setting_work_item_slot"),
                button -> isSettingWISS = true));
        stateText = getStateText();
    }

    public Component getStateText() {
        if (owner.isStrike()) {
            return Component.translatable("state." + LittleMaidNeo.MODID + ".Strike");
        }
        MutableComponent stateText = Component.translatable("state." + LittleMaidNeo.MODID + "." + movingMode.getName());
        owner.getModeName().ifPresent(
                modeName -> stateText.append(" : ")
                        .append(Component.translatable("mode." + LittleMaidNeo.MODID + "." + modeName)));
        return stateText;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 少し重たいかもしれないが、screenを開く直前にsetModeNameした場合に取得がズレるので毎tickやる
        stateText = getStateText();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);
        this.renderTooltip(context, mouseX, mouseY);
        InventoryScreen.renderEntityInInventoryFollowsMouse(context,
                (this.width - this.imageWidth) / 2 + 26,
                (this.height - this.imageHeight) / 2 + 5,
                (this.width - this.imageWidth) / 2 + 78,
                (this.height - this.imageHeight) / 2 + 59,
                20, 0.0625f, mouseX, mouseY, owner);

        if (isSettingWISS) {
            renderWISSSetting(context, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // お仕事アイテムスロット数設定中に、クリックした場合
        if (isSettingWISS) {
            // スロットをクリックしたなら、そのスロットの一つ手前までで設定する
            getMaidSlotPos(mouseX, mouseY)
                    .ifPresent(pos -> {
                        workItemSlotSize = convSlotIndex(pos.a(), pos.b());
                        C2SSetWorkItemSlotSizePacket.sendC2SPacket(owner, workItemSlotSize);
                    });

            isSettingWISS = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public Optional<Tuple<Integer, Integer>> getMaidSlotPos(double x, double y) {
        float left = (width - imageWidth) / 2F;
        float top = (height - imageHeight) / 2F;
        float baseLeft = left + 7;
        float baseTop = top + 75;
        int size = 18;
        int slotCol = 9;
        int slotRow = 2;
        if (baseLeft <= x && x < baseLeft + size * slotCol
                && baseTop <= y && y < baseTop + size * slotRow) {
            int indexX = Mth.floor((x - baseLeft) / size);
            int indexY = Mth.floor((y - baseTop) / size);
            return Optional.of(new Tuple<>(indexX, indexY));
        }
        return Optional.empty();
    }

    public int convSlotIndex(int x, int y) {
        return y * 9 + x;
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        context.drawString(font, this.stateText.getString(), 8, 65, 0x404040, false);
        String insideSkirt = Component.translatable("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getString();
        context.drawString(font, insideSkirt, 168 - font.width(insideSkirt), 65, 0x404040, false);
        float left = (width - imageWidth) / 2F;
        float top = (height - imageHeight) / 2F;
        if (left + 7 <= mouseX && mouseX < left + 96 && top + 7 <= mouseY && mouseY < top + 60) {
            drawArmor(context);
        } else {
            drawHealth(context, mouseX, mouseY);
        }
    }

    protected void drawHealth(GuiGraphics context, int mouseX, int mouseY) {
        float left = (width - imageWidth) / 2F;
        float top = (height - imageHeight) / 2F;
        if (left + 98 <= mouseX && mouseX < left + 98 + 5 * 9 && top + 7 <= mouseY && mouseY < top + 7 + 2 * 9) {
            String healthStr = Mth.ceil(owner.getHealth()) + " / " + Mth.ceil(owner.getMaxHealth());
            context.drawString(font, healthStr,
                    98 + (int) ((5 * 9 - font.width(healthStr)) / 2F),
                    16 - (int) (font.lineHeight / 2F), 0x404040, false);
        } else {
            float health = (owner.getHealth() / owner.getMaxHealth()) * 20F;
            drawHealth(context, 98, 7, Mth.clamp(health - 10, 0, 10), 5);
            drawHealth(context, 98, 16, Mth.clamp(health, 0, 10), 5);
        }
        RenderSystem.setShaderTexture(0, GUI);
    }

    protected void drawArmor(GuiGraphics context) {
        float armor = owner.getArmorValue();
        drawArmor(context, 98, 7, Mth.clamp(armor - 10, 0, 10), 5);
        drawArmor(context, 98, 16, Mth.clamp(armor, 0, 10), 5);
    }

    protected void drawHealth(GuiGraphics context, int x, int y, float health, int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 0, 52, 0, 61, 0);
    }

    protected void drawArmor(GuiGraphics context, int x, int y, float health, int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 9, 34, 9, 25, 9);
    }

    protected void drawIcon(GuiGraphics context, int x, int y, float num, int row,
            int baseU, int baseV, int overU, int overV, int halfU, int halfV) {
        for (int i = 0; i < row; i++) {
            context.blit(ICONS, x + i * 9, y, baseU, baseV, 9, 9);
            if (1 < num) {
                context.blit(ICONS, x + i * 9, y, overU, overV, 9, 9);
            } else if (0 < num) {
                context.blit(ICONS, x + i * 9, y, halfU, halfV, 9, 9);
            }
            num -= 2;
        }
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        context.blit(GUI, relX, relY, 0, 0, this.imageWidth, this.imageHeight);

        if (!isSettingWISS) {
            drawWorkItemSlotOverlay(context, workItemSlotSize);
        }
    }

    public void renderWISSSetting(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int slotSize = 18;
        int top = relY + 75;
        int bottom = top + slotSize * 2;
        int left = relX + 7;
        int right = left + slotSize * 9;
        int color = 0x80000000;
        // スロットを抜いて黒くする
        context.fill(0, 0, this.width, top, color);
        context.fill(0, top, left, bottom, color);
        context.fill(right, top, this.width, bottom, color);
        context.fill(0, bottom, this.width, this.height, color);

        var optional = getMaidSlotPos(mouseX, mouseY);
        if (optional.isPresent()) {
            var pos = optional.get();
            int index = convSlotIndex(pos.a(), pos.b());
            drawWorkItemSlotOverlay(context, index);
        } else {
            drawWorkItemSlotOverlay(context, workItemSlotSize);
        }
    }

    // お仕事アイテムスロットをオーバーレイ表示する
    public void drawWorkItemSlotOverlay(GuiGraphics context, int num) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < num; i++) {
            int slotSize = 18;
            // (7, 75)が原点
            int baseX = relX + 7;
            int baseY = relY + 75;
            int x = baseX + slotSize * (i % 9);
            int y = baseY + slotSize * (i / 9);
            context.fill(x, y, x + slotSize, y + slotSize, 0x40FF4040);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (prevMovingMode != movingMode) {
            C2SSetMovingStatePacket.sendC2SPacket(owner, movingMode);
        }
    }

    public static class IconButtonWidget extends Button {
        public static final int DEFAULT_SIZE = 20;
        private ItemStack iconItem;

        public IconButtonWidget(int x, int y, ItemStack iconItem, Component tooltip, OnPress onPress) {
            this(x, y, DEFAULT_SIZE, DEFAULT_SIZE, Component.empty(), onPress, Supplier::get, iconItem);
            this.setTooltip(Tooltip.create(tooltip));
        }

        public IconButtonWidget(int x, int y, int width, int height, Component message,
                OnPress onPress, CreateNarration narrationSupplier, ItemStack iconItem) {
            super(x, y, width, height, message, onPress, narrationSupplier);
            this.iconItem = iconItem;
        }

        public ItemStack getIconItem() {
            return iconItem;
        }

        public void setIconItem(ItemStack stack) {
            this.iconItem = stack;
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);
            context.renderItem(getIconItem(), this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
        }
    }

}
