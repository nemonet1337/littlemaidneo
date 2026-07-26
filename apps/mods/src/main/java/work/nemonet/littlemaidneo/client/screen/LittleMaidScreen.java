package work.nemonet.littlemaidneo.client.screen;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidScreenHandler;
import net.minecraft.ChatFormatting;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.network.*;
import work.nemonet.littlemaidneo.util.Tuple;
public class LittleMaidScreen
        extends AbstractContainerScreen<LittleMaidScreenHandler> {
    private IconButtonWidget changeMovingModeButton;

    private static final Identifier GUI = Identifier.fromNamespaceAndPath(
            LittleMaidNeo.MODID,
            "textures/gui/container/littlemaidinventory2.png");
    // littlemaidinventory2.png のキャンバスサイズ（blit の UV 正規化に必要）
    private static final int TEXTURE_SIZE = 256;
    private static final ItemStack ARMOR = Items.LEATHER_CHESTPLATE.getDefaultInstance();
    private static final ItemStack BOOK = Items.BOOK.getDefaultInstance();
    private static final ItemStack NOTE = Items.NOTE_BLOCK.getDefaultInstance();
    private static final ItemStack FEATHER = Items.FEATHER.getDefaultInstance();
    private static final ItemStack IRON_SWORD = Items.IRON_SWORD.getDefaultInstance();
    private static final ItemStack IRON_AXE = Items.IRON_AXE.getDefaultInstance();
    private static final ItemStack CHEST = Items.CHEST.getDefaultInstance();
    private final LittleMaidEntity owner;
    private Component stateText;
    private final MaidMode prevMaidMode;
    private MaidMode movingMode;
    private int workItemSlotSize;
    private boolean isSettingWISS;

    public LittleMaidScreen(
            LittleMaidScreenHandler screenContainer,
            Inventory inv,
            Component titleIn) {
        super(screenContainer, inv, titleIn, 176, 208);
        owner = screenContainer.getGuiEntity();
        workItemSlotSize = screenContainer.getWorkItemSlotSize();
        prevMaidMode = movingMode = owner.getMaidMode();
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
        this.addRenderableWidget(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        BOOK,
                        Component.translatable(
                                "gui.littlemaidneo.littlemaid.tooltip.open_target_tag_setting"),
                        button -> NetworkHandler.sendOpenTargetTagScreenC2S(this.owner)));
        this.addRenderableWidget(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        NOTE,
                        Component.translatable(
                                "gui.littlemaidneo.littlemaid.tooltip.open_sound_pack_select"),
                        button -> minecraft.setScreen(
                                new SoundPackSelectScreen<>(title, owner))));
        this.addRenderableWidget(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        ARMOR,
                        Component.translatable(
                                "gui.littlemaidneo.littlemaid.tooltip.open_model_select"),
                        button -> minecraft.setScreen(
                                new ModelSelectScreen<>(title, owner.level(), owner))));
        this.changeMovingModeButton = this.addRenderableWidget(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        FEATHER,
                        Component.translatable(
                                "gui.littlemaidneo.littlemaid.tooltip.change_moving_mode"),
                        button -> {
                            if (this.owner.isStrike()) {
                                return;
                            }
                            switch (movingMode) {
                                case ESCORT -> movingMode = MaidMode.FREEDOM;
                                case FREEDOM -> movingMode = MaidMode.STROLL;
                                case STROLL -> movingMode = MaidMode.TRACER;
                                case TRACER -> movingMode = MaidMode.ESCORT;
                            }
                            stateText = getStateText();
                            updateStatusIcons();
                        }));
        this.addRenderableWidget(
                new IconButtonWidget(
                        left - size,
                        top + size * ++layer,
                        FEATHER,
                        Component.empty(),
                        button -> NetworkHandler.sendSetBloodSuckC2S(
                                this.owner,
                                !this.owner.isBloodSuck())) {
                    private static final Component changeBloodSuck = Component.translatable(
                            "gui.littlemaidneo.littlemaid.tooltip.change_blood_suck");
                    private static final Component toBloodSuck = changeBloodSuck
                            .copy()
                            .append(
                                    Component.translatable(
                                            "gui.littlemaidneo.littlemaid.tooltip.change_blood_suck.to_blood_suck"));
                    private static final Component isBloodSuck = changeBloodSuck
                            .copy()
                            .append(
                                    Component.translatable(
                                            "gui.littlemaidneo.littlemaid.tooltip.change_blood_suck.is_blood_suck"));

                    @Override
                    public ItemStack getIconItem() {
                        return LittleMaidScreen.this.owner.isBloodSuck()
                                ? IRON_AXE
                                : IRON_SWORD;
                    }

                    @Override
                    public void extractContents(
                            GuiGraphicsExtractor context,
                            int mouseX,
                            int mouseY,
                            float delta) {
                        super.extractContents(context, mouseX, mouseY, delta);
                        setTooltip(
                                Tooltip.create(
                                        LittleMaidScreen.this.owner.isBloodSuck()
                                                ? isBloodSuck
                                                : toBloodSuck));
                    }
                });
        layer = -1;
        this.addRenderableWidget(
                new IconButtonWidget(
                        right,
                        top + size * ++layer,
                        BOOK,
                        Component.translatable(
                                "gui.littlemaidneo.littlemaid.tooltip.open_maid_manager"),
                        button -> NetworkHandler.sendOpenMaidManagerScreenC2S()));

        this.addRenderableWidget(
                new IconButtonWidget(
                        right,
                        top + 75,
                        CHEST,
                        Component.translatable(
                                "gui.littlemaidneo.littlemaid.tooltip.setting_work_item_slot"),
                        button -> isSettingWISS = true));

        stateText = getStateText();
        updateStatusIcons();
    }

    public Component getStateText() {
        if (owner.isStrike()) {
            return Component.translatable(
                    "state." + LittleMaidNeo.MODID + ".Strike").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        }
        // 待機中は移動モードより待機表示を優先
        if (owner.isInSittingPose()) {
            return Component.translatable("state." + LittleMaidNeo.MODID + ".Wait");
        }
        Optional<String> modeNameOpt = owner.getModeName();
        if (modeNameOpt.isPresent()) {
            String modeName = modeNameOpt.get();
            // 移動×お仕事の組み合わせ表示名（例: 護衛剣士 = Escort_Fencer）
            String compoundKey = "state." + LittleMaidNeo.MODID + "." + movingMode.getName() + "_" + modeName;
            if (net.minecraft.client.resources.language.I18n.exists(compoundKey)) {
                return Component.translatable(compoundKey);
            }
            MutableComponent stateText = Component.translatable(
                    "state." + LittleMaidNeo.MODID + "." + movingMode.getName());
            return stateText.append(" : ").append(
                    Component.translatable("mode." + LittleMaidNeo.MODID + "." + modeName));
        }
        return Component.translatable(
                "state." + LittleMaidNeo.MODID + "." + movingMode.getName());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // モード名は SynchedEntityData(MODE_NAME) でサーバーから遅延同期されるため、
        // 開いた直後の値が古いことがある。毎 tick 取り直すことで表示の追従ズレを防ぐ（コストは軽微）。
        stateText = getStateText();
        updateStatusIcons();
    }

    private void updateStatusIcons() {
        if (owner == null)
            return;

        boolean isStrike = owner.isStrike();
        boolean isWait = owner.isInSittingPose();

        ItemStack movingIcon;
        Component movingTooltip;
        if (isStrike) {
            movingIcon = Items.BARRIER.getDefaultInstance();
            movingTooltip = Component.translatable("state." + LittleMaidNeo.MODID + ".Strike")
                    .append("\n")
                    .append(Component.translatable("gui.littlemaidneo.littlemaid.tooltip.strike_warning")
                            .withStyle(ChatFormatting.GOLD));
        } else if (isWait) {
            movingIcon = Items.CLOCK.getDefaultInstance();
            movingTooltip = Component.translatable("state." + LittleMaidNeo.MODID + ".Wait");
        } else {
            switch (movingMode) {
                case ESCORT -> {
                    movingIcon = Items.COMPASS.getDefaultInstance();
                    movingTooltip = Component.translatable("state." + LittleMaidNeo.MODID + ".Escort");
                }
                case FREEDOM -> {
                    movingIcon = Items.FEATHER.getDefaultInstance();
                    movingTooltip = Component.translatable("state." + LittleMaidNeo.MODID + ".Freedom");
                }
                case STROLL -> {
                    movingIcon = Items.LEATHER_BOOTS.getDefaultInstance();
                    movingTooltip = Component.translatable("state." + LittleMaidNeo.MODID + ".Stroll");
                }
                case TRACER -> {
                    movingIcon = Items.REDSTONE.getDefaultInstance();
                    movingTooltip = Component.translatable("state." + LittleMaidNeo.MODID + ".Tracer");
                }
                default -> {
                    movingIcon = Items.FEATHER.getDefaultInstance();
                    movingTooltip = Component.empty();
                }
            }
        }

        if (changeMovingModeButton != null) {
            changeMovingModeButton.setIconItem(movingIcon);
            changeMovingModeButton.setTooltip(Tooltip.create(movingTooltip));
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY,
            float partialTicks) {
        super.extractRenderState(context, mouseX, mouseY, partialTicks);
        this.extractTooltip(context, mouseX, mouseY);
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                context,
                (this.width - this.imageWidth) / 2 + 26,
                (this.height - this.imageHeight) / 2 + 5,
                (this.width - this.imageWidth) / 2 + 78,
                (this.height - this.imageHeight) / 2 + 59,
                20,
                0.0625f,
                mouseX,
                mouseY,
                owner);

        if (owner.isStrike()) {
            int px = (this.width - this.imageWidth) / 2 + 26;
            int py = (this.height - this.imageHeight) / 2 + 5;
            int pw = 52;
            int ph = 54;
            context.fill(px, py, px + pw, py + ph, 0x15FF0000);
            context.fill(px, py, px + pw, py + 1, 0x80FF0000);
            context.fill(px, py + ph - 1, px + pw, py + ph, 0x80FF0000);
            context.fill(px, py, px + 1, py + ph, 0x80FF0000);
            context.fill(px + pw - 1, py, px + pw, py + ph, 0x80FF0000);
        }

        if (isSettingWISS) {
            renderWISSSetting(context, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();
        // お仕事アイテムスロット数設定中に、クリックした場合
        if (isSettingWISS) {
            // スロットをクリックしたなら、そのスロットの一つ手前までで設定する
            getMaidSlotPos(mouseX, mouseY).ifPresent(pos -> {
                workItemSlotSize = convSlotIndex(pos.a(), pos.b());
                NetworkHandler.sendSetWorkItemSlotSizeC2S(
                        owner,
                        workItemSlotSize);
            });

            isSettingWISS = false;
            return true;
        }

        return super.mouseClicked(event, handled);
    }

    public Optional<Tuple<Integer, Integer>> getMaidSlotPos(
            double x,
            double y) {
        float left = (width - imageWidth) / 2F;
        float top = (height - imageHeight) / 2F;
        float baseLeft = left + 7;
        float baseTop = top + 75;
        int size = 18;
        int slotCol = 9;
        int slotRow = 2;
        if (baseLeft <= x &&
                x < baseLeft + size * slotCol &&
                baseTop <= y &&
                y < baseTop + size * slotRow) {
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
    protected void extractLabels(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY) {
        int textX = 8;
        context.text(font, this.stateText, textX, 61, 0xFF404040, false);
        // お給料状態表示（未払い日数）
        int unpaid = this.menu.getUnpaidDays();
        Component salaryText;
        if (owner.isStrike()) {
            salaryText = Component.translatable("gui.littlemaidneo.littlemaid.salary.strike")
                    .withStyle(ChatFormatting.RED);
        } else if (unpaid > 0) {
            salaryText = Component.translatable("gui.littlemaidneo.littlemaid.salary.unpaid", unpaid)
                    .withStyle(ChatFormatting.GOLD);
        } else {
            salaryText = Component.translatable("gui.littlemaidneo.littlemaid.salary.ok")
                    .withStyle(ChatFormatting.DARK_GREEN);
        }
        context.text(font, salaryText, textX, 71, 0xFF404040, false);
        String insideSkirt = Component.translatable(
                "entity.littlemaidneo.little_maid_mob.InsideSkirt").getString();
        context.text(
                font,
                insideSkirt,
                168 - font.width(insideSkirt),
                65,
                0xFF404040,
                false);
        float left = (width - imageWidth) / 2F;
        float top = (height - imageHeight) / 2F;
        if (left + 7 <= mouseX &&
                mouseX < left + 96 &&
                top + 7 <= mouseY &&
                mouseY < top + 60) {
            drawArmor(context);
        } else {
            drawHealth(context, mouseX, mouseY);
        }
    }

    protected void drawHealth(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY) {
        float left = (width - imageWidth) / 2F;
        float top = (height - imageHeight) / 2F;
        if (left + 98 <= mouseX &&
                mouseX < left + 98 + 5 * 9 &&
                top + 7 <= mouseY &&
                mouseY < top + 7 + 2 * 9) {
            String healthStr = Mth.ceil(owner.getHealth()) +
                    " / " +
                    Mth.ceil(owner.getMaxHealth());
            context.text(
                    font,
                    healthStr,
                    98 + (int) ((5 * 9 - font.width(healthStr)) / 2F),
                    16 - (int) (font.lineHeight / 2F),
                    0x404040,
                    false);
        } else {
            float health = (owner.getHealth() / owner.getMaxHealth()) * 20F;
            drawHealth(context, 7, Mth.clamp(health - 10, 0, 10));
            drawHealth(context, 16, Mth.clamp(health, 0, 10));
        }
    }

    protected void drawArmor(GuiGraphicsExtractor context) {
        float armor = owner.getArmorValue();
        drawArmor(context, 7, Mth.clamp(armor - 10, 0, 10));
        drawArmor(context, 16, Mth.clamp(armor, 0, 10));
    }

    private static final Identifier HEART_CONTAINER = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");
    private static final Identifier HEART_HALF = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/half");
    private static final Identifier HEART_FULL = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
    private static final Identifier ARMOR_EMPTY = Identifier.fromNamespaceAndPath("minecraft", "hud/armor_empty");
    private static final Identifier ARMOR_HALF = Identifier.fromNamespaceAndPath("minecraft", "hud/armor_half");
    private static final Identifier ARMOR_FULL = Identifier.fromNamespaceAndPath("minecraft", "hud/armor_full");

    protected void drawHealth(
            GuiGraphicsExtractor context,
            int y,
            float health) {
        drawPointBar(context, 98, y, health, 5, HEART_CONTAINER, HEART_FULL, HEART_HALF);
    }

    protected void drawArmor(
            GuiGraphicsExtractor context,
            int y,
            float health) {
        drawPointBar(context, 98, y, health, 5, ARMOR_EMPTY, ARMOR_FULL, ARMOR_HALF);
    }

    private void drawPointBar(
            GuiGraphicsExtractor context,
            int x,
            int y,
            float num,
            int row,
            Identifier emptySprite,
            Identifier fullSprite,
            Identifier halfSprite) {
        for (int i = 0; i < row; i++) {
            int ix = x + i * 9;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, emptySprite, ix, y, 9, 9);
            if (1 < num) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, ix, y, 9, 9);
            } else if (0 < num) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, halfSprite, ix, y, 9, 9);
            }
            num -= 2;
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY,
            float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI,
                relX,
                relY,
                0.0f,
                0.0f,
                this.imageWidth,
                this.imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }

    @Override
    public void extractContents(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY,
            float delta) {
        super.extractContents(context, mouseX, mouseY, delta);
        if (!isSettingWISS) {
            drawWorkItemSlotOverlay(context, workItemSlotSize);
        }
    }

    public void renderWISSSetting(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY) {
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
    public void drawWorkItemSlotOverlay(GuiGraphicsExtractor context, int num) {
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
        if (prevMaidMode != movingMode) {
            NetworkHandler.sendSetMovingStateC2S(owner, movingMode);
        }
    }

    public static class IconButtonWidget extends Button {

        public static final int DEFAULT_SIZE = 20;
        private ItemStack iconItem;

        public IconButtonWidget(
                int x,
                int y,
                ItemStack iconItem,
                Component tooltip,
                OnPress onPress) {
            this(
                    x,
                    y,
                    DEFAULT_SIZE,
                    DEFAULT_SIZE,
                    Component.empty(),
                    onPress,
                    Supplier::get,
                    iconItem);
            this.setTooltip(Tooltip.create(tooltip));
        }

        public IconButtonWidget(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                CreateNarration narrationSupplier,
                ItemStack iconItem) {
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
        public void extractContents(
                GuiGraphicsExtractor context,
                int mouseX,
                int mouseY,
                float delta) {
            extractDefaultSprite(context);
            context.item(
                    getIconItem(),
                    this.getX() - 8 + this.width / 2,
                    this.getY() - 8 + this.height / 2);
        }
    }
public static class StatusIconWidget extends Button {
        private ItemStack iconItem;
        private final boolean flat;

        public StatusIconWidget(
                int x,
                int y,
                int width,
                int height,
                ItemStack iconItem,
                Component tooltip,
                OnPress onPress,
                boolean flat) {
            super(x, y, width, height, Component.empty(), onPress, Supplier::get);
            this.iconItem = iconItem;
            this.flat = flat;
            this.setTooltip(Tooltip.create(tooltip));
        }

        public void setIconItem(ItemStack stack) {
            this.iconItem = stack;
        }

        public ItemStack getIconItem() {
            return iconItem;
        }

        @Override
        public void extractContents(
                GuiGraphicsExtractor context,
                int mouseX,
                int mouseY,
                float delta) {
            if (!flat) {
                extractDefaultSprite(context);
            }
            if (!iconItem.isEmpty()) {
                context.item(
                        getIconItem(),
                        this.getX() - 8 + this.width / 2,
                        this.getY() - 8 + this.height / 2);
            }
        }
    }
}
