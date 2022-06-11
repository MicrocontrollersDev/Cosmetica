package com.eyezah.cosmetics.screens;

import com.eyezah.cosmetics.Cosmetica;
import com.eyezah.cosmetics.utils.Debug;
import com.eyezah.cosmetics.utils.LoadingTypeScreen;
import com.eyezah.cosmetics.utils.Response;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.eyezah.cosmetics.Cosmetica.clearAllCaches;

public class UpdatingSettingsScreen extends Screen implements LoadingTypeScreen {
	private Screen parentScreen;

	private Component reason = new TranslatableComponent("cosmetica.updating.message");
	private MultiLineLabel message;
	private int textHeight;

	public UpdatingSettingsScreen(Screen parentScreen, ServerOptions oldOptions, ServerOptions newOptions, boolean doReload) throws IOException, InterruptedException {
		super(new TranslatableComponent("cosmetica.updating"));
		this.parentScreen = parentScreen;

		Map<String, Object> changedSettings = new HashMap<>();

		doReload |= newOptions.regionSpecificEffects.appendToIfChanged(oldOptions.regionSpecificEffects, changedSettings);
		doReload |= newOptions.shoulderBuddies.appendToIfChanged(oldOptions.shoulderBuddies, changedSettings);
		doReload |= newOptions.hats.appendToIfChanged(oldOptions.hats, changedSettings);
		doReload |= newOptions.lore.appendToIfChanged(oldOptions.lore, changedSettings);
		boolean finalDoReload = doReload;

		if (!changedSettings.isEmpty()) {
			Thread requestThread = new Thread(() -> {
				Cosmetica.api.updateUserSettings(changedSettings).ifSuccessfulOrElse(response -> {
					if (response.booleanValue()) {
						Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(this.parentScreen));
					} else {
						Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new UnauthenticatedScreen(this.parentScreen, true)));
					}
				},
				e -> {
					e.printStackTrace();
					Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new UnauthenticatedScreen(this.parentScreen, true)));
				});

				if (finalDoReload) Minecraft.getInstance().tell(() -> clearAllCaches());
			});
			requestThread.start();
		} else {
			Debug.info("No settings changed.");
			if (doReload) clearAllCaches();
			Minecraft.getInstance().tell(this::onClose);
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.parentScreen);
	}

	@Override
	public Screen getParent() {
		return this.parentScreen;
	}

	@Override
	protected void init() {
		this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
		int var10001 = this.message.getLineCount();
		Objects.requireNonNull(this.font);
		this.textHeight = var10001 * 9;
	}

	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		int x = this.width / 2;
		int y = this.height / 2 - this.textHeight / 2;
		Objects.requireNonNull(this.font);
		drawCenteredString(poseStack, this.font, this.title, x, y - 9 * 2, 11184810);
		this.message.renderCentered(poseStack, this.width / 2, this.height / 2 - this.textHeight / 2);
		super.render(poseStack, i, j, f);
	}
}
