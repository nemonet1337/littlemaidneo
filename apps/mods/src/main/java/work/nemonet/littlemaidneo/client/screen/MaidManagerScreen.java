package work.nemonet.littlemaidneo.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.input.MouseButtonEvent;
import work.nemonet.littlemaidneo.client.screen.component.FilterPredicate;
import work.nemonet.littlemaidneo.client.screen.component.FilterableListGUI;
import work.nemonet.littlemaidneo.client.screen.component.GUIElement;
import work.nemonet.littlemaidneo.client.screen.component.ListGUIElement;
import work.nemonet.littlemaidneo.client.screen.component.TextInputGUI;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidManager;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.network.C2SCallWaitPayload;
import work.nemonet.littlemaidneo.network.NetworkHandler;
import work.nemonet.littlemaidneo.client.util.ClientScreenHelper;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * メイドさん管理情報を表示するためのスクリーン
 */
public class MaidManagerScreen extends AbstractFilterableListScreen<MaidManagerScreen.LMInfoGUIElement> {
    private final List<MaidManager.LMInfo> lmInfoList;
    private TextInputGUI groupInput;

    public MaidManagerScreen(List<MaidManager.LMInfo> lmInfoList) {
        super(Component.translatable("gui.littlemaidneo.maidmanager.title"));
        this.lmInfoList = lmInfoList;
    }

    public String getGroupInputText() {
        return groupInput == null ? "" : groupInput.getText();
    }

    @Override
    protected void init() {
        assert this.minecraft != null;

        int searchInputHeight = 20;
        int groupInputHeight = 20;
        int elementWidth = font.lineHeight * 18;
        int elementHeight = font.lineHeight * 6 + 20;
        int widthStack = Mth.floor(this.width * 0.8f / elementWidth);
        int totalWidth = elementWidth * widthStack;
        int totalHeight = Mth.floor(this.height * 0.85f);
        int listX = Mth.floor((this.width - totalWidth) / 2f);
        int listY = Mth.floor((this.height - totalHeight) / 2f) + groupInputHeight + 4;

        FilterPredicate<LMInfoGUIElement> lmInfoFilter = (lmInfoGUIElement, filterText) -> {
            var info = lmInfoGUIElement.getLMInfo();
            String searchStr = (info.name() + "," + info.status().name() + "," + info.group()).toLowerCase();
            return searchStr.contains(filterText.toLowerCase());
        };

        String currentWorldId = this.minecraft.level.dimension().identifier().toString();

        List<LMInfoGUIElement> elements = lmInfoList.stream()
                .map(info -> new LMInfoGUIElement(this, this.minecraft.font, info))
                .sorted(createSortComparator(currentWorldId))
                .collect(Collectors.toList());

        this.groupInput = new TextInputGUI(listX, listY - groupInputHeight - 4, totalWidth, groupInputHeight, 32);
        this.groupInput.setPlaceholder(Component.translatable("gui.littlemaidneo.maidmanager.group_hint").getString());

        this.listGUI = FilterableListGUI.<LMInfoGUIElement>builder()
                .position(listX, listY)
                .size(totalWidth, totalHeight - groupInputHeight - 4)
                .elementSize(elementWidth, elementHeight)
                .items(elements)
                .filterBy(lmInfoFilter)
                .withScrollBar()
                .searchInputHeight(searchInputHeight)
                .withPlaceholder(Component.translatable("gui.littlemaidneo.maidmanager.search").getString())
                .build();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x40000000);
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (groupInput != null) {
            groupInput.extractRenderState(context, mouseX, mouseY, delta);
        }
        if (listGUI != null) {
            listGUI.extractRenderState(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (groupInput != null && groupInput.mouseClicked(event, handled)) {
            if (listGUI != null) {
                listGUI.setFocused(false);
            }
            return true;
        }
        if (super.mouseClicked(event, handled)) {
            if (groupInput != null) {
                groupInput.setFocused(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (groupInput != null && groupInput.isFocused() && groupInput.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (groupInput != null && groupInput.isFocused() && groupInput.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    private static Comparator<LMInfoGUIElement> createSortComparator(String currentWorldId) {
        return Comparator
                // 1. ステータス（最優先）
                .comparing((LMInfoGUIElement e) -> e.getLMInfo().status().name())
                // 2. ワールド優先度（同一ワールドを優先）
                .thenComparing(e -> {
                    String worldId = e.getLMInfo().getWorldId();
                    return worldId.equals(currentWorldId) ? 0 : 1;
                })
                // 3. ワールド名（異なるワールド間でのソート）
                .thenComparing(e -> e.getLMInfo().getWorldId())
                // 4. 名前
                .thenComparing(e -> e.getLMInfo().name())
                // 5. 距離（同一ワールドの場合のみ）
                .thenComparing(e -> {
                    String worldId = e.getLMInfo().getWorldId();
                    if (!worldId.equals(currentWorldId)) {
                        return Double.MAX_VALUE; // 異なるワールドは最後
                    }

                    var client = Minecraft.getInstance();
                    if (client == null || client.player == null) {
                        return Double.MAX_VALUE;
                    }

                    return e.getLMInfo().getEntityClient(client.level)
                            .map(entity -> client.player.distanceToSqr(
                                     entity.getX(), entity.getY(), entity.getZ()))
                            .orElse(Double.MAX_VALUE);
                });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static class LMInfoGUIElement extends GUIElement implements ListGUIElement {
        private final MaidManagerScreen screen;
        private final MaidManager.LMInfo lmInfo;
        private final LittleMaidScreen.IconButtonWidget inventoryButton;
        private final Button callWaitButton;
        private final Button groupButton;

        public LMInfoGUIElement(MaidManagerScreen screen, Font textRenderer, MaidManager.LMInfo lmInfo) {
            super(textRenderer.lineHeight * 18, textRenderer.lineHeight * 6 + 20);
            this.screen = screen;
            this.lmInfo = lmInfo;

            this.inventoryButton = new LittleMaidScreen.IconButtonWidget(
                    0, 0,
                    new ItemStack(Items.CHEST),
                    Component.translatable("gui.littlemaidneo.maidmanager.open_inventory"),
                    (button) -> openInventory());
            this.callWaitButton = new Button.Builder(
                    Component.literal("call"),
                    onPress -> lmInfo.getEntityClient(Minecraft.getInstance().level)
                            .filter(e -> e instanceof LittleMaidEntity)
                            .map(e -> (LittleMaidEntity) e)
                            .ifPresent(e -> NetworkHandler.sendCallWaitC2S(e,
                                    TameableUtil.isWait(e)
                                            ? C2SCallWaitPayload.State.CALL
                                            : C2SCallWaitPayload.State.WAIT)))
                    .size(30, 20)
                    .build();
            this.groupButton = new Button.Builder(
                    Component.translatable("gui.littlemaidneo.maidmanager.set_group"),
                    onPress -> {
                        String group = this.screen.getGroupInputText();
                        lmInfo.setGroup(group);
                        NetworkHandler.sendSetMaidGroupC2S(lmInfo.id(), group);
                    })
                    .size(40, 20)
                    .build();
        }

        private boolean canInteractWithMaid() {
            var client = Minecraft.getInstance();
            if (client == null || client.level == null || client.player == null) {
                return false;
            }

            // ワールドの同一性をチェック
            String worldId = lmInfo.getWorldId();
            if (worldId.isEmpty() || !worldId.equals(client.level.dimension().identifier().toString())) {
                return false;
            }

            return lmInfo.getEntityClient(client.level)
                    .map(entity -> {
                        // 8ブロック以内かチェック
                        double squaredDistance = client.player.distanceToSqr(
                                entity.getX(), entity.getY(), entity.getZ());
                        return squaredDistance < 64.0; // 8 * 8
                    })
                    .orElse(false);
        }

        private void openInventory() {
            var client = Minecraft.getInstance();
            if (client == null || client.level == null) {
                return;
            }

            // エンティティが存在し、8ブロック以内の場合のみインベントリを開く
            if (canInteractWithMaid()) {
                lmInfo.getEntityClient(client.level)
                        .ifPresent(NetworkHandler::sendOpenInventoryC2S);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            var client = Minecraft.getInstance();
            if (client == null || client.level == null || client.player == null) {
                return;
            }
            Font textRenderer = Minecraft.getInstance().font;

            // 1. リトルメイド名を表示
            context.fill(this.x, this.y,
                    this.x + this.width - textRenderer.lineHeight, this.y + textRenderer.lineHeight,
                    0xFF000000);
            String name = lmInfo.getEntityClient(client.level)
                    .map(entity -> entity.getName().getString())
                    .orElse(lmInfo.name());
            ClientScreenHelper.drawScrollingText(context, textRenderer, name, this.x, this.y,
                    this.width - textRenderer.lineHeight, 0xFFFFFFFF, false);

            // 2. ステータス / ロード状態を表示
            var statusText = lmInfo.status().getText().copy();

            // SOUL_WITHIN以外の場合はロード状態も表示
            if (lmInfo.status() != MaidManager.Status.SOUL_WITHIN) {
                var loadedText = lmInfo.isLoaded()
                        ? Component.literal("Loaded").withStyle(ChatFormatting.GRAY)
                        : Component.literal("Unloaded").withStyle(ChatFormatting.GRAY);
                statusText = statusText
                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                        .append(loadedText);
            }

            String group = lmInfo.group();
            if (!group.isEmpty()) {
                statusText = statusText
                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(group).withStyle(ChatFormatting.YELLOW));
            }
            context.text(textRenderer, statusText,
                    this.x, this.y + textRenderer.lineHeight, 0xFFCCCCCC, true);

            // 3. ワールド名を表示
            String worldId = lmInfo.getWorldId();
            if (!worldId.isEmpty()) {
                ClientScreenHelper.drawScrollingText(context, textRenderer, worldId, this.x,
                        this.y + textRenderer.lineHeight * 2,
                        this.width - textRenderer.lineHeight * 3, 0xFFAAAAAA, true);
            }

            // 4. XYZ座標と距離を表示（worldIdが空でない場合のみ）
            if (!worldId.isEmpty()) {
                BlockPos pos = lmInfo.getEntityClient(client.level)
                        .map(Entity::blockPosition)
                        .orElse(lmInfo.getLastPos());

                var coordText = Component.literal(String.format("XYZ: %d, %d, %d",
                        pos.getX(), pos.getY(), pos.getZ())).copy();

                // 距離計算（同一ワールドの場合のみ）
                if (worldId.equals(client.level.dimension().identifier().toString())) {
                    double squaredDistance = client.player
                            .distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    double distance = Math.sqrt(squaredDistance);

                    // 8ブロック以内なら白、そうでないならグレー
                    ChatFormatting distanceColor = distance <= 8.0 ? ChatFormatting.WHITE : ChatFormatting.GRAY;
                    Component distanceText = Component.literal(String.format(" (%.0fm)", distance))
                            .withStyle(distanceColor);
                    coordText.append(distanceText);
                }

                ClientScreenHelper.drawScrollingText(context, textRenderer, coordText, this.x,
                        this.y + textRenderer.lineHeight * 3,
                        this.width - textRenderer.lineHeight * 3, 0xFFAAAAAA, true);
            }

            lmInfo.getEntityClient(client.level)
                    .filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e)
                    .ifPresent(e -> {
                        // モード名を表示
                        if (e instanceof LittleMaidEntity littleMaid) {
                            littleMaid.getModeName()
                                    .ifPresent(modeName -> context.text(textRenderer, modeName,
                                            this.x, this.y + textRenderer.lineHeight * 4,
                                            0xFFFFFFFF, true));
                        }

                        // エンティティを描画（右側）
                        int entityX = this.x + this.width - 20;
                        int entityY = this.y + this.height - textRenderer.lineHeight;
                        int entitySize = 20;
                        InventoryScreen.extractEntityInInventoryFollowsMouse(context,
                                entityX - entitySize, entityY - entitySize * 2,
                                entityX + entitySize, entityY,
                                entitySize, 0.0625f, mouseX, mouseY, e);
                    });

            int buttonX = this.x;
            int buttonY = this.y + this.height - textRenderer.lineHeight - 20;
            groupButton.setPosition(buttonX, buttonY);
            groupButton.extractRenderState(context, mouseX, mouseY, delta);
            buttonX += groupButton.getWidth();

            if (canInteractWithMaid()) {
                inventoryButton.setPosition(buttonX, buttonY);
                inventoryButton.extractRenderState(context, mouseX, mouseY, delta);
                lmInfo.getEntityClient(client.level)
                        .filter(e -> e instanceof LittleMaidEntity)
                        .map(e -> (LittleMaidEntity) e)
                        .ifPresent(e -> {
                            if (TameableUtil.isWait(e)) {
                                callWaitButton.setMessage(Component.literal("call"));
                            } else {
                                callWaitButton.setMessage(Component.literal("wait"));
                            }
                        });
                buttonX += inventoryButton.getWidth();
                callWaitButton.setPosition(buttonX, buttonY);
                callWaitButton.extractRenderState(context, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
            if (groupButton.mouseClicked(event, handled)) {
                return true;
            }
            if (canInteractWithMaid()
                    && (inventoryButton.mouseClicked(event, handled)
                            || callWaitButton.mouseClicked(event, handled))) {
                return true;
            }
            return super.mouseClicked(event, handled);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (groupButton.mouseReleased(event)) {
                return true;
            }
            if (canInteractWithMaid()
                    && (inventoryButton.mouseReleased(event)
                            || callWaitButton.mouseReleased(event))) {
                return true;
            }
            return super.mouseReleased(event);
        }

        @Override
        public void setSelected(boolean selected) {
            // 必要に応じて実装
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        public MaidManager.LMInfo getLMInfo() {
            return lmInfo;
        }
    }
}