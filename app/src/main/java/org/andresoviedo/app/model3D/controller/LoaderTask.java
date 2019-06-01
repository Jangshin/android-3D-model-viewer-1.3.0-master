package org.andresoviedo.app.model3D.controller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.andresoviedo.app.model3D.model.Object3DBuilder;
import org.andresoviedo.app.model3D.model.Object3DData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This component allows loading the model without blocking the UI.
 *
 * @author andresoviedo
 */
public abstract class LoaderTask extends AsyncTask<Void, Integer, Object3DData> {


	protected final URL url;

	protected final Object3DBuilder.Callback callback;
	/**
	 * 将显示加载进度的对话框
	 */
	protected final ProgressDialog dialog;

	private final Activity parent;

	private final File currentDir;

	private final String assetsDir;

	private final String modelId;

	protected Exception error;

	/**
	 * 构建一个新的进度对话框，用于异步加载数据模型
	 *
	 * @param url        指向3d模型的URL
	 * @param currentDir 模型所在的目录（当模型是assert时为null）
	 * @param modelId    正在加载的数据的id
	 */
	public LoaderTask(Activity parent, URL url, File currentDir, String assetsDir, String modelId, Object3DBuilder.Callback callback) {
		this.parent = parent;
		this.url = url;
		this.currentDir = currentDir;
		this.assetsDir = assetsDir;
		this.modelId = modelId;
		this.dialog = new ProgressDialog(parent);
		this.callback = callback;
	}


	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog.setMessage("Loading...");
		this.dialog.setCancelable(false);
		this.dialog.show();
	}



	@Override
	protected Object3DData doInBackground(Void... params) {
		try {
			Object3DData data = build();
			callback.onLoadComplete(data);
			build(data);
			return  data;
		} catch (Exception ex) {
			error = ex;
			return null;
		}
	}

	protected abstract Object3DData build() throws Exception;

	protected abstract void build(Object3DData data) throws Exception;

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		switch (values[0]) {
			case 0:
				this.dialog.setMessage("分析模型...");
				break;
			case 1:
				this.dialog.setMessage("分配内存...");
				break;
			case 2:
				this.dialog.setMessage("数据加载...");
				break;
			case 3:
				this.dialog.setMessage("调整模型...");
				break;
			case 4:
				this.dialog.setMessage("构造3D模型...");
				break;
			case 5:

				break;
		}
	}

	@Override
	protected void onPostExecute(Object3DData data) {
		super.onPostExecute(data);
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
		if (error != null) {
			callback.onLoadError(error);
		} else {
			callback.onBuildComplete(data);
		}
	}


}