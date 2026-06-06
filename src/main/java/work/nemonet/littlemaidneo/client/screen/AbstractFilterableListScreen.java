package work.nemonet.littlemaidneo.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import work.nemonet.littlemaidneo.client.screen.component.FilterableListGUI;
import work.nemonet.littlemaidneo.client.screen.component.GUIElement;

public abstract class AbstractFilterableListScreen<T extends GUIElement> extends Screen {
    protected FilterableListGUI<T> listGUI;

    protected AbstractFilterableListScreen(Component titleIn) {
        super(titleIn);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (this.listGUI != null) {
            return this.listGUI.mouseClicked(event, handled);
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.listGUI != null) {
            return this.listGUI.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.listGUI != null) {
            return this.listGUI.mouseDragged(event, deltaX, deltaY);
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.listGUI != null && this.listGUI.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (this.listGUI != null) {
            return this.listGUI.keyPressed(event);
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.listGUI != null && this.listGUI.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }
}
