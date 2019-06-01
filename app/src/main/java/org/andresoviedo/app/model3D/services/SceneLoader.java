package org.andresoviedo.app.model3D.services;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.andresoviedo.app.model3D.model.Object3DBuilder;
import org.andresoviedo.app.model3D.model.Object3DBuilder.Callback;
import org.andresoviedo.app.model3D.model.Object3DData;
import org.andresoviedo.app.model3D.view.ModelActivity;
import org.andresoviedo.app.util.url.android.Handler;

import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


public class SceneLoader {


	private static float[] DEFAULT_COLOR = {1.0f, 1.0f, 0, 1.0f};

	protected final ModelActivity parent;

	private List<Object3DData> objects = new ArrayList<Object3DData>();

	private boolean drawWireframe = false;

	private boolean drawingPoints = false;

	private boolean drawBoundingBox = false;

	private boolean drawNormals = false;

	private boolean drawTextures = true;

	private boolean rotatingLight = false;

	private boolean drawLighting = false;

	private Object3DData selectedObject = null;

	private float[] lightPosition = new float[]{0, 0, 3, 1};


	private final Object3DData lightPoint = Object3DBuilder.buildPoint(new float[4]).setId("light").setPosition(lightPosition);

	public SceneLoader(ModelActivity main) {
		this.parent = main;
	}

	public void init() {

		if (parent.getParamFile() != null || parent.getParamAssetDir() != null) {

			//
			//初始化assert url处理程序
			Handler.assets = parent.getAssets();


			// 创建地址
			final URL url;
			try {
				if (parent.getParamFile() != null) {
					url = parent.getParamFile().toURI().toURL();
				} else {
					url = new URL("android://org.andresoviedo.dddmodel2/assets/" + parent.getParamAssetDir() + File.separator + parent.getParamAssetFilename());

				}
			} catch (MalformedURLException e) {
				Log.e("SceneLoader", e.getMessage(), e);
				throw new RuntimeException(e);
			}

			Object3DBuilder.loadV6AsyncParallel(parent, url, parent.getParamFile(), parent.getParamAssetDir(),
					parent.getParamAssetFilename(), new Callback() {

						long startTime = SystemClock.uptimeMillis();

						@Override
						public void onBuildComplete(Object3DData data) {
							final String elapsed = (SystemClock.uptimeMillis() - startTime)/1000+" 秒";
							makeToastText("加载成功 ("+elapsed+")", Toast.LENGTH_LONG);
						}

						@Override
						public void onLoadComplete(Object3DData data) {
							data.setColor(DEFAULT_COLOR);
							data.setScale(new float[]{5f, 5f, 5f});
							addObject(data);
						}

						@Override
						public void onLoadError(Exception ex) {
							Log.e("SceneLoader",ex.getMessage(),ex);
							Toast.makeText(parent.getApplicationContext(),
									"There was a problem building the model: " + ex.getMessage(), Toast.LENGTH_LONG)
									.show();
						}
					});
		}
	}

	private void makeToastText(final String text, final int toastDuration) {
		parent.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(parent.getApplicationContext(), text, toastDuration).show();
			}
		});
	}

	public Object3DData getLightBulb() {
		return lightPoint;
	}

	public void onDrawFrame(){
		animateLight();
	}

	private void animateLight() {
		if (!rotatingLight) return;

		long time = SystemClock.uptimeMillis() % 5000L;
		float angleInDegrees = (360.0f / 5000.0f) * ((int) time);
		lightPoint.setRotationY(angleInDegrees);
	}

	protected synchronized void addObject(Object3DData obj) {
		List<Object3DData> newList = new ArrayList<Object3DData>(objects);
		newList.add(obj);
		this.objects = newList;
		requestRender();
	}

	private void requestRender() {
		parent.getgLView().requestRender();
	}

	public synchronized List<Object3DData> getObjects() {
		return objects;
	}

	public void toggleWireframe() {
		if (this.drawWireframe && !this.drawingPoints) {
			this.drawWireframe = false;
			this.drawingPoints = true;
			makeToastText("点", Toast.LENGTH_SHORT);
		}
		else if (this.drawingPoints){
			this.drawingPoints = false;
			makeToastText("面", Toast.LENGTH_SHORT);
		}
		else {
			makeToastText("网格", Toast.LENGTH_SHORT);
			this.drawWireframe = true;
		}
		requestRender();
	}

	public boolean isDrawWireframe() {
		return this.drawWireframe;
	}

	public boolean isDrawPoints() {
		return this.drawingPoints;
	}

	public void toggleBoundingBox() {
		this.drawBoundingBox = !drawBoundingBox;
		requestRender();
	}

	public boolean isDrawBoundingBox() {
		return drawBoundingBox;
	}

	public boolean isDrawNormals() {
		return drawNormals;
	}

	public void toggleTextures() {
		this.drawTextures = !drawTextures;
	}

	public void toggleLighting() {

	}

	public boolean isDrawTextures() {
		return drawTextures;
	}

	public boolean isDrawLighting() {
		return drawLighting;
	}

	public Object3DData getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(Object3DData selectedObject) {
		this.selectedObject = selectedObject;
	}

}
