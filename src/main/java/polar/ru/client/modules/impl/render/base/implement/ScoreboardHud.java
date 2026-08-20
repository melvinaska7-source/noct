package polar.ru.client.modules.impl.render.base.implement;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class ScoreboardHud
extends InterfaceProcessing {
    private static final float ROW_HEIGHT = 12.0f;
    private static final float HEADER_HEIGHT = 16.0f;
    private static final float PADDING = 4.0f;
    private static final float HEADER_GAP = 0.2f;
    private final AnimationUtils panelAlphaAnimation = HudFx.newAppearAnimation();

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private MCFontRenderer myfont(int size) {
        return Fonts.getTtfFont("myfont.ttf", size);
    }

    public ScoreboardHud(Draggable draggable) {
        super(draggable);
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float contentWidth;
        if (ScoreboardHud.mc.player == null || ScoreboardHud.mc.world == null) {
            return;
        }
        Scoreboard scoreboard = ScoreboardHud.mc.player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        float baseX = this.draggable.getX();
        float baseY = this.draggable.getY();
        int colorTheme = this.getStableThemeColor();
        NumberFormat numberFormat = objective.getNumberFormatOr((NumberFormat)StyledNumberFormat.RED);
        List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective).stream().filter(entry -> !entry.hidden()).sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).limit(15L).toList();
        if (entries.isEmpty()) {
            this.draggable.setWidth(0.0f);
            this.draggable.setHeight(0.0f);
            return;
        }
        float maxTextWidth = 0.0f;
        for (ScoreboardEntry entry2 : entries) {
            try {
                float scoreWidth;
                Team team = scoreboard.getScoreHolderTeam(entry2.owner());
                MutableText name = Team.decorateName((AbstractTeam)team, (Text)entry2.name());
                MutableText score = entry2.formatted(numberFormat);
                float nameWidth = this.issue(10).getWidth(name.getString());
                float textWidth = nameWidth + (scoreWidth = this.issue(10).getWidth(score.getString())) + 20.0f;
                if (!(textWidth > maxTextWidth)) continue;
                maxTextWidth = textWidth;
            }
            catch (Exception team) {}
        }
        float totalWidth = contentWidth = Math.max(maxTextWidth + 8.0f, 120.0f);
        float totalHeight = 20.2f + (float)entries.size() * 12.0f + 4.0f;
        this.panelAlphaAnimation.update(1.0f);
        float panelProgress = this.panelAlphaAnimation.getValue();
        int panelAlphaMul = (int)(255.0f * panelProgress);
        MatrixStack matrices = eventRender.getContext().getMatrices();
        float pivotX = baseX + totalWidth / 2.0f;
        float pivotY = baseY + totalHeight / 2.0f;
        float eased = HudFx.pushTransform(matrices, panelProgress, pivotX, pivotY);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, baseX, baseY, totalWidth, totalHeight, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, (int)(200.0f * eased));
            RenderUtils.drawShadow(matrices, baseX - 2.0f, baseY - 2.0f, totalWidth + 4.0f, totalHeight + 4.0f, 6.0f, shadowColor);
            float blueLineWidth = totalWidth * 0.4f - 5.0f;
            float blueLineX = baseX + (totalWidth - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = ColorUtils.setAlphaColor(colorTheme, panelAlphaMul);
            RenderUtils.drawRoundedRect(matrices, blueLineX, baseY - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        Text headerText = objective.getDisplayName();
        float headerY = baseY + (16.0f - 9.0f) / 2.0f;
        eventRender.getContext().drawText(ScoreboardHud.mc.textRenderer, headerText, (int)(baseX + 5.2f), (int)headerY, -1, false);
        float entryY = baseY + 16.0f + 0.2f + 4.0f;
        for (ScoreboardEntry entry3 : entries) {
            try {
                Team team = scoreboard.getScoreHolderTeam(entry3.owner());
                MutableText name = Team.decorateName((AbstractTeam)team, (Text)entry3.name());
                MutableText score = entry3.formatted(numberFormat);
                float rowTextY = entryY + (12.0f - 9.0f) / 2.0f;
                eventRender.getContext().drawText(ScoreboardHud.mc.textRenderer, (Text)name, (int)(baseX + 4.0f), (int)rowTextY, -1, false);
                float scoreX = baseX + totalWidth - 4.0f - (float)ScoreboardHud.mc.textRenderer.getWidth((StringVisitable)score);
                eventRender.getContext().drawText(ScoreboardHud.mc.textRenderer, (Text)score, (int)scoreX, (int)rowTextY, -1, false);
                entryY += 12.0f;
            }
            catch (Exception exception) {}
        }
        HudFx.popTransform(matrices);
        this.draggable.setWidth(totalWidth);
        this.draggable.setHeight(totalHeight);
    }

    private int getStableThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }
}

