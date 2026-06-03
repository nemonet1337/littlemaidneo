package work.nemonet.littlemaidneo.client.screen;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
import net.neoforged.api.distmarker.Dist;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.client.screen.ModelSelectScreen;
import work.nemonet.littlemaidneo.client.screen.SoundPackSelectScreen;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.LittleMaidScreenHandler;
import net.minecraft.ChatFormatting;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.network.*;
import work.nemonet.littlemaidneo.util.Tuple;
public class LittleMaidScreen
        extends AbstractContainerScreen<LittleMaidScreenHandler> {
    private StatusIconWidget movingModeButton;
    private StatusIconWidget modeButton;

    private static final Identifier GUI = Identifier.fromNamespaceAndPath(
            "lmreengaged",
            "textures/gui/container/littlemaidinventory2.png");
    private static final Identifier ICONS = Identifier.parse(
            "textures/gui/icons.png");
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

    public LittleMaidScreen(
            LittleMaidScreenHandler screenContainer,
            Inventory inv,
            Component titleIn) {
        super(screenContainer, inv, titleIn, 176, 208);
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
        this.addRenderableWidget(
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
                                case ESCORT -> movingMode = MovingMode.FREEDOM;
                                case FREEDOM -> movingMode = MovingMode.TRACER;
                                case TRACER -> movingMode = MovingMode.ESCORT;
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

        int relX = (this.width - imageWidth) / 2;
        int relY = (this.height - imageHeight) / 2;
        movingModeButton = new StatusIconWidget(
                relX + 8,
                relY + 57,
                16,
                16,
                ItemStack.EMPTY,
                Component.empty(),
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
                    updateStatusIcons();
                },
                true);
        this.addRenderableWidget(movingModeButton);

        modeButton = new StatusIconWidget(
                relX + 26,
                relY + 57,
                16,
                16,
                ItemStack.EMPTY,
                Component.empty(),
                button -> {
                },
                true);
        this.addRenderableWidget(modeButton);

        stateText = getStateText();
        updateStatusIcons();
    }

    public Component getStateText() {
        if (owner.isStrike()) {
            return Component.translatable(
                    "state." + LittleMaidNeo.MODID + ".Strike").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        }
        MutableComponent stateText = Component.translatable(
                "state." + LittleMaidNeo.MODID + "." + movingMode.getName());
        owner
                .getModeName()
                .ifPresent(modeName -> stateText
                        .append(" : ")
                        .append(
                                Component.translatable(
                                        "mode." + LittleMaidNeo.MODID + "." + modeName)));
        return stateText;
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
        if (owner == null || movingModeButton == null || modeButton == null)
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

        movingModeButton.setIconItem(movingIcon);
        movingModeButton.setTooltip(Tooltip.create(movingTooltip));

        Optional<String> modeNameOpt = owner.getModeName();
        if (modeNameOpt.isPresent() && !isStrike) {
            String modeName = modeNameOpt.get();
            ItemStack modeIcon;
            Component modeTooltip = Component.translatable("mode." + LittleMaidNeo.MODID + "." + modeName);
            switch (modeName) {
                case "Fencer" -> modeIcon = Items.IRON_SWORD.getDefaultInstance();
                case "Archer" -> modeIcon = Items.BOW.getDefaultInstance();
                case "Cooking" -> modeIcon = Items.BREAD.getDefaultInstance();
                case "Ripper" -> modeIcon = Items.SHEARS.getDefaultInstance();
                case "Torcher" -> modeIcon = Items.TORCH.getDefaultInstance();
                case "Healer" -> modeIcon = Items.GOLDEN_APPLE.getDefaultInstance();
                default -> modeIcon = Items.BOOK.getDefaultInstance();
            }
            modeButton.setIconItem(modeIcon);
            modeButton.setTooltip(Tooltip.create(modeTooltip));
            modeButton.visible = true;
            modeButton.setPosition(movingModeButton.getX() + 18, movingModeButton.getY());
        } else {
            modeButton.visible = false;
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
        if (movingModeButton != null && movingModeButton.visible) {
            textX = 26;
        }
        if (modeButton != null && modeButton.visible) {
            textX = 44;
        }
        context.text(font, this.stateText, textX, 61, 0x404040, false);
        String insideSkirt = Component.translatable(
                "entity.littlemaidneo.little_maid_mob.InsideSkirt").getString();
        context.text(
                font,
                insideSkirt,
                168 - font.width(insideSkirt),
                65,
                0x404040,
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
            drawHealth(context, 98, 7, Mth.clamp(health - 10, 0, 10), 5);
            drawHealth(context, 98, 16, Mth.clamp(health, 0, 10), 5);
        }
    }

    protected void drawArmor(GuiGraphicsExtractor context) {
        float armor = owner.getArmorValue();
        drawArmor(context, 98, 7, Mth.clamp(armor - 10, 0, 10), 5);
        drawArmor(context, 98, 16, Mth.clamp(armor, 0, 10), 5);
    }

    protected void drawHealth(
            GuiGraphicsExtractor context,
            int x,
            int y,
            float health,
            int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 0, 52, 0, 61, 0);
    }

    protected void drawArmor(
            GuiGraphicsExtractor context,
            int x,
            int y,
            float health,
            int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 9, 34, 9, 25, 9);
    }

    protected void drawIcon(
            GuiGraphicsExtractor context,
            int x,
            int y,
            float num,
            int row,
            int baseU,
            int baseV,
            int overU,
            int overV,
            int halfU,
            int halfV) {
        for (int i = 0; i < row; i++) {
            context.blit(
                    ICONS,
                    x + i * 9,
                    y,
                    9,
                    9,
                    (float) baseU,
                    (float) baseV,
                    9.0f,
                    9.0f);
            if (1 < num) {
                context.blit(
                        ICONS,
                        x + i * 9,
                        y,
                        9,
                        9,
                        (float) overU,
                        (float) overV,
                        9.0f,
                        9.0f);
            } else if (0 < num) {
                context.blit(
                        ICONS,
                        x + i * 9,
                        y,
                        9,
                        9,
                        (float) halfU,
                        (float) halfV,
                        9.0f,
                        9.0f);
            }
            num -= 2;
        }
    }

    @Override
    public void extractContents(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY,
            float delta) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        context.blit(
                GUI,
                relX,
                relY,
                this.imageWidth,
                this.imageHeight,
                0.0f,
                0.0f,
                (float) this.imageWidth,
                (float) this.imageHeight);

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
        if (prevMovingMode != movingMode) {
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
