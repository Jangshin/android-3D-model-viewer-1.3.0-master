package org.andresoviedo.app.model3D.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.andresoviedo.app.model3D.entities.Camera;
import org.andresoviedo.app.model3D.model.Object3D;
import org.andresoviedo.app.model3D.model.Object3DBuilder;
import org.andresoviedo.app.model3D.model.Object3DData;
import org.andresoviedo.app.model3D.model.Object3DImpl;
import org.andresoviedo.app.model3D.services.SceneLoader;
import org.andresoviedo.app.model3D.util.GLUtil;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Toast;

public class ModelRenderer implements GLSurfaceView.Renderer {

	private final static String TAG = ModelRenderer.class.getName();

	//3D窗口（父组件）
	private ModelSurfaceView main;
	// 屏幕宽度
	private int width;
	//
	//屏幕高度
	private int height;
	// 出点的处理程序
	private Camera camera;
	//
	private float near = 1f;
	//
	private float far = 10f;

	private Object3DBuilder drawer;
	// 线框相关的形状（它应该只由线组成）
	private Map<Object3DData, Object3DData> wireframes = new HashMap<Object3DData, Object3DData>();
	// 加载的纹理
	private Map<byte[], Integer> textures = new HashMap<byte[], Integer>();
	// 相应的opengl边框
	private Map<Object3DData, Object3DData> boundingBoxes = new HashMap<Object3DData, Object3DData>();
	// 相应的opengl边界框
	private Map<Object3DData, Object3DData> normals = new HashMap<Object3DData, Object3DData>();

	// 投影3D世界的3D矩阵
	private final float[] modelProjectionMatrix = new float[16];
	private final float[] modelViewMatrix = new float[16];
	// “模型视图投影矩阵”
	private final float[] mvpMatrix = new float[16];

	// 灯光渲染所需的灯光位置
	private final float[] lightPosInEyeSpace = new float[4];

	/**
	 * 为指定的曲面视图构造一个新的渲染器
	 *
	 * @param modelSurfaceView
	 *            the 3D window
	 */
	public ModelRenderer(ModelSurfaceView modelSurfaceView) {
		this.main = modelSurfaceView;
	}

	public float getNear() {
		return near;
	}

	public float getFar() {
		return far;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {

		//float[] backgroundColor = main.getModelActivity().getBackgroundColor();
		GLES20.glClearColor(1.0f,1.0f,1.0f,1.0f);


		//使用剔除去除背面。
         //不要移除背面，以便可以看到它们
		// GLES20.glEnable(GLES20.GL_CULL_FACE);

		// 启用隐藏表面消除的深度测试。
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// 当有透明度时，启用混合以组合颜色
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		// 创建3D世界组件
		camera = new Camera();

		//该组件将使用OpenGL绘制实际模型
		drawer = new Object3DBuilder();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		this.width = width;
		this.height = height;

		//根据几何体更改（例如屏幕旋转）调整视口
		GLES20.glViewport(0, 0, width, height);

		//
		//信息：设置摄像机位置（视图矩阵）
		// 相机有3个矢量（位置，视角矢量，以及向上视角）
		Matrix.setLookAtM(modelViewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView,
				camera.zView, camera.xUp, camera.yUp, camera.zUp);

		// 投影矩阵是我们想要投影的3D虚拟空间（立方体）
		float ratio = (float) width / height;
		Log.d(TAG, "projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10]");
		Matrix.frustumM(modelProjectionMatrix, 0, -ratio, ratio, -1, 1, getNear(), getFar());

		// 计算投影和视图转换
		Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
	}

	@Override
	public void onDrawFrame(GL10 unused) {

		//背景颜色
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// 根据我们现在看到的位置重新计算mvp矩阵
		if (camera.hasChanged()) {
			Matrix.setLookAtM(modelViewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView,
					camera.zView, camera.xUp, camera.yUp, camera.zUp);
			// Log.d("Camera", "Changed! :"+camera.ToStringVector());
			Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
			camera.setChanged(false);
		}

		SceneLoader scene = main.getModelActivity().getScene();
		if (scene == null) {
			//
			return;
		}


		//
		//相机知道与之碰撞的物体
		camera.setScene(scene);

		//动画场景
		scene.onDrawFrame();

		//
		if (scene.isDrawLighting()) {

			Object3DImpl lightBulbDrawer = (Object3DImpl) drawer.getPointDrawer();

			float[] lightModelViewMatrix = lightBulbDrawer.getMvMatrix(lightBulbDrawer.getMMatrix(scene.getLightBulb()),modelViewMatrix);

			// 计算眼睛空间中的光线位置以支持照明
			Matrix.multiplyMV(lightPosInEyeSpace, 0, lightModelViewMatrix, 0, scene.getLightBulb().getPosition(), 0);

			//绘制一个代表灯泡的点
			lightBulbDrawer.draw(scene.getLightBulb(), modelProjectionMatrix, modelViewMatrix, -1, lightPosInEyeSpace);
		}

		List<Object3DData> objects = scene.getObjects();
		for (int i=0; i<objects.size(); i++) {
			try {
				Object3DData objData = objects.get(i);
				boolean changed = objData.isChanged();

				Object3D drawerObject = drawer.getDrawer(objData, scene.isDrawTextures(), scene.isDrawLighting());
				// Log.d("ModelRenderer","Drawing object using '"+drawerObject.getClass()+"'");

				Integer textureId = textures.get(objData.getTextureData());
				if (textureId == null && objData.getTextureData() != null) {
					ByteArrayInputStream textureIs = new ByteArrayInputStream(objData.getTextureData());
					textureId = GLUtil.loadTexture(textureIs);
					textureIs.close();
					textures.put(objData.getTextureData(), textureId);
				}

				if (scene.isDrawWireframe() && objData.getDrawMode() != GLES20.GL_POINTS
						&& objData.getDrawMode() != GLES20.GL_LINES && objData.getDrawMode() != GLES20.GL_LINE_STRIP
						&& objData.getDrawMode() != GLES20.GL_LINE_LOOP) {

					try{
						// 仅绘制具有面（三角形）的对象的线框
						Object3DData wireframe = wireframes.get(objData);
						if (wireframe == null || changed) {
							Log.i("ModelRenderer","Generating wireframe model...");
							wireframe = Object3DBuilder.buildWireframe(objData);
							wireframes.put(objData, wireframe);
						}
						drawerObject.draw(wireframe,modelProjectionMatrix,modelViewMatrix,wireframe.getDrawMode(),
								wireframe.getDrawSize(),textureId != null? textureId:-1, lightPosInEyeSpace);
					}catch(Error e){
						Log.e("ModelRenderer",e.getMessage(),e);
					}
				} else if (scene.isDrawPoints() || (objData.getFaces() != null && !objData.getFaces().loaded())){
					drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix
							,GLES20.GL_POINTS, objData.getDrawSize(),
							textureId != null ? textureId : -1, lightPosInEyeSpace);
				} else {
					drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix,
							textureId != null ? textureId : -1, lightPosInEyeSpace);
				}

				//
				if (scene.isDrawBoundingBox() || scene.getSelectedObject() == objData) {
					Object3DData boundingBoxData = boundingBoxes.get(objData);
					if (boundingBoxData == null || changed) {
						boundingBoxData = Object3DBuilder.buildBoundingBox(objData);
						boundingBoxes.put(objData, boundingBoxData);
					}
					Object3D boundingBoxDrawer = drawer.getBoundingBoxDrawer();
					boundingBoxDrawer.draw(boundingBoxData, modelProjectionMatrix, modelViewMatrix, -1, null);
				}

				// 绘制边界框
				if (scene.isDrawNormals()) {
					Object3DData normalData = normals.get(objData);
					if (normalData == null || changed) {
						normalData = Object3DBuilder.buildFaceNormals(objData);
						if (normalData != null) {
							// 如果对象不是由三角形组成，则它可以为null
							normals.put(objData, normalData);
						}
					}
					if (normalData != null) {
						Object3D normalsDrawer = drawer.getFaceNormalsDrawer();
						normalsDrawer.draw(normalData, modelProjectionMatrix, modelViewMatrix, -1, null);
					}
				}

			} catch (IOException ex) {
				Toast.makeText(main.getModelActivity().getApplicationContext(),
						"There was a problem creating 3D object", Toast.LENGTH_LONG).show();
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public float[] getModelProjectionMatrix() {
		return modelProjectionMatrix;
	}

	public float[] getModelViewMatrix() {
		return modelViewMatrix;
	}

	public Camera getCamera() {
		return camera;
	}
}