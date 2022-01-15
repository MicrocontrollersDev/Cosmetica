package com.eyezah.cosmetics.cosmetics.shoulderbuddies.model;

import com.eyezah.cosmetics.Cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Models {
	private static Map<ResourceLocation, BakedModel> BAKED_MODELS = new HashMap<>();
	private static final float RANDOM_NEXT_FLOAT = 0.211f; // generated by random.org. Guaranteed to be random.
	public static ModelBakery thePieShopDownTheRoad;
	public static final ResourceLocation TEST_LOCATION = new ResourceLocation("cosmetics:test");

	public static BakedModel getModel(ResourceLocation location) {
		return BAKED_MODELS.getOrDefault(location, Minecraft.getInstance().getModelManager().getMissingModel());
	}

	public static void loadTestResource() {
		if (!BAKED_MODELS.containsKey(TEST_LOCATION)) {
			Cosmetics.runOffthread(() -> {
				try (InputStream is = new FileInputStream(new File(FabricLoader.getInstance().getGameDirectory(), "test.json"))) {
					loadBakedModel(createBlockModel(TEST_LOCATION, is));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public static void loadBakedModel(BlockModel model) {
		ResourceLocation location = new ResourceLocation(model.name);
		BakedModel result = model.bake(thePieShopDownTheRoad, l -> Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation("minecraft:block/dirt"))/*TODO*/, BlockModelRotation.X0_Y0, location);

		if (result == null) {
			throw new RuntimeException("Null Baked Model for " + model.name);
		}

		BAKED_MODELS.put(location, result);
	}

	private static BlockModel createBlockModel(ResourceLocation location, InputStream json) {
		BlockModel result = BlockModel.fromStream(new InputStreamReader(json, StandardCharsets.UTF_8));
		result.name = location.toString();
		return result;
	}

	public static void renderModel(BakedModel model, PoseStack stack, MultiBufferSource multiBufferSource, int packedLight) {
		stack.pushPose();
		boolean isGUI3D = model.isGui3d();
		float transformStrength = 0.25F;
		float rotation = 0.0f;
		float transform = model.getTransforms().getTransform(ItemTransforms.TransformType.GROUND).scale.y();
		stack.translate(0.0D, rotation + transformStrength * transform, 0.0D);
		float xScale = model.getTransforms().ground.scale.x();
		float yScale = model.getTransforms().ground.scale.y();
		float zScale = model.getTransforms().ground.scale.z();

		stack.pushPose();

		final ItemTransforms.TransformType transformType = ItemTransforms.TransformType.FIXED;
		int overlayTyp = OverlayTexture.NO_OVERLAY;
		// ItemRenderer#render start
		stack.pushPose();

		model.getTransforms().getTransform(transformType).apply(false, stack);
		stack.translate(-0.5D, -0.5D, -0.5D);

		RenderType renderType = RenderType.cutoutMipped(); // hopefully this is the right one
		VertexConsumer vertexConsumer4 = multiBufferSource.getBuffer(renderType);
		renderModelLists(model, packedLight, overlayTyp, stack, vertexConsumer4);

		stack.popPose();
		// ItemRenderer#render end

		stack.popPose();
		if (!isGUI3D) {
			stack.translate(0.0F * xScale, 0.0F * yScale, 0.09375F * zScale);
		}

		stack.popPose();
	}

	private static void renderModelLists(BakedModel bakedModel, int packedLight, int overlayType, PoseStack poseStack, VertexConsumer vertexConsumer) {
		Random random = new Random();
		final long seed = 42L;
		Direction[] var10 = Direction.values();
		int var11 = var10.length;

		for(int var12 = 0; var12 < var11; ++var12) {
			Direction direction = var10[var12];
			random.setSeed(seed);
			renderQuadList(poseStack, vertexConsumer, bakedModel.getQuads(null, direction, random), packedLight, overlayType);
		}

		random.setSeed(seed);
		renderQuadList(poseStack, vertexConsumer, bakedModel.getQuads(null, null, random), packedLight, overlayType);
	}

	private static void renderQuadList(PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> list, int i, int j) {
		PoseStack.Pose pose = poseStack.last();
		Iterator var9 = list.iterator();

		while(var9.hasNext()) {
			BakedQuad bakedQuad = (BakedQuad)var9.next();
			int k = -1;

			float f = (float)(k >> 16 & 255) / 255.0F;
			float g = (float)(k >> 8 & 255) / 255.0F;
			float h = (float)(k & 255) / 255.0F;
			vertexConsumer.putBulkData(pose, bakedQuad, f, g, h, i, j);
		}
	}
}
