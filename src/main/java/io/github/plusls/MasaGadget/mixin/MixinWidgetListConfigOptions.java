package io.github.plusls.MasaGadget.mixin;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collections;
import java.util.List;

@Mixin(WidgetListConfigOptions.class)
public abstract class MixinWidgetListConfigOptions extends WidgetListConfigOptionsBase<GuiConfigsBase.ConfigOptionWrapper, WidgetConfigOption> {
    public MixinWidgetListConfigOptions(int x, int y, int width, int height, int configWidth) {
        super(x, y, width, height, configWidth);
    }

    /**
     * @author plusls
     * @reason allow search name and gui display name
     */
    @Overwrite(remap = false)
    protected List<String> getEntryStringsForFilter(GuiConfigsBase.ConfigOptionWrapper entry) {
        IConfigBase config = entry.getConfig();

        if (config != null) {
            return ImmutableList.of(getTranslatedGuiDisplayName(config).toLowerCase(), config.getName().toLowerCase());
        }

        return Collections.emptyList();
    }

    /**
     * @author plusls
     * @reason fix render width error
     */
    @Overwrite(remap = false)
    protected void reCreateListEntryWidgets() {
        this.maxLabelWidth = this.getMaxGuiDisplayNameLengthWrapped(this.listContents);
        super.reCreateListEntryWidgets();
    }

    static private String getTranslatedGuiDisplayName(IConfigBase config) {
        return StringUtils.translate(config.getConfigGuiDisplayName());
    }

    private int getMaxGuiDisplayNameLengthWrapped(List<GuiConfigsBase.ConfigOptionWrapper> wrappers) {
        int width = 0;

        for (GuiConfigsBase.ConfigOptionWrapper wrapper : wrappers) {
            if (wrapper.getType() == GuiConfigsBase.ConfigOptionWrapper.Type.CONFIG) {
                width = Math.max(width, this.getStringWidth(getTranslatedGuiDisplayName(wrapper.getConfig())));
            }
        }
        return width;
    }

    // fix upper case when search Disable Hotkeys
    @Override
    protected boolean matchesFilter(List<String> entryStrings, String filterText) {
        filterText = filterText.toLowerCase();
        if (filterText.isEmpty()) {
            return true;
        }

        for (String str : entryStrings) {
            if (this.matchesFilter(str, filterText)) {
                return true;
            }
        }
        return false;
    }
}

