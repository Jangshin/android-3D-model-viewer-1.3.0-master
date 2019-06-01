package org.andresoviedo.app.model3D.entities;


import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.app.model3D.model.Object3DData;
import org.andresoviedo.app.model3D.services.SceneLoader;

import java.util.ArrayList;
import java.util.List;


public class Camera {

	public static final float UP = 0.5f; // Forward speed.
	public static final float DOWN = -0.5f; // Backward speed.
	public static final float LEFT = 0.5f; // Left speed.
	public static final float RIGHT = -0.5f; // Right speed.
	public static final float STRAFE_LEFT = -0.5f; // Left straft speed.
	public static final float STRAFE_RIGHT = 0.5f; // Right straft speed.

	public static final int AIM = 10;

	public float xPos, yPos; // Camera position.
	public float zPos;
	public float xView, yView, zView; // Look at position.
	public float xUp, yUp, zUp; // Up direction.

	private SceneLoader scene;
	private final BoundingBox boundingBox = new BoundingBox("scene",-9,9,-9,9,-9,9);

	float xStrafe = 0, yStrafe = 0, zStrafe = 0; //
	float currentRotationAngle; //视角限制不会太近或者太远

	float[] matrix = new float[16];
	float[] buffer = new float[12 + 12 + 16 + 16];
	float[] buffer4x3 = new float[12];
	private boolean changed = false;

	public Camera() {
		//
		this(0, 0, 3, 0, 0, -1, 0, 1, 0);

	}

	public Camera(float xPos, float yPos, float zPos, float xView, float yView, float zView, float xUp, float yUp,
			float zUp) {

         //这里我们将相机设置为发送给我们的值。这主要是
        //用来设置一个默认位子

		this.xPos = xPos;
		this.yPos = yPos;
		this.zPos = zPos;
		this.xView = xView;
		this.yView = yView;
		this.zView = zView;
		this.xUp = xUp;
		this.yUp = yUp;
		this.zUp = zUp;
	}

	public void setScene(SceneLoader scene) {
		this.scene = scene;
	}

	private void normalize() {
		float xLook = 0, yLook = 0, zLook = 0;
		float xRight = 0, yRight = 0, zRight = 0;
		float xArriba = 0, yArriba = 0, zArriba = 0;
		float vlen;

		 //旋转相机需要一个方向矢量来旋转
         //首先，我们需要了解我们正在寻找的方向。
         //外观方向是视图减去位置（我们所在的位置）。
         //获取视图的方向
		xLook = xView - xPos;
		yLook = yView - yPos;
		zLook = zView - zPos;
		vlen = Matrix.length(xLook, yLook, zLook);
		xLook /= vlen;
		yLook /= vlen;
		zLook /= vlen;

		//接下来得到的轴是视图的垂直向量方向和向上的值。
        //我们使用它的交叉积来得到轴然后标准化

		xArriba = xUp - xPos;
		yArriba = yUp - yPos;
		zArriba = zUp - zPos;

		vlen = Matrix.length(xArriba, yArriba, zArriba);
		xArriba /= vlen;
		yArriba /= vlen;
		zArriba /= vlen;


		xView = xLook + xPos;
		yView = yLook + yPos;
		zView = zLook + zPos;
		xUp = xArriba + xPos;
		yUp = yArriba + yPos;
		zUp = zArriba + zPos;
	}

	public void MoveCameraZ(float direction) {
		//移动相机需要多一点，然后在z或上添加1/减1
         //需要正在看向的方向。
		float xLookDirection = 0, yLookDirection = 0, zLookDirection = 0;

		//
		//外观方向是视图减去位置（我们所在的位置）
		xLookDirection = xView - xPos;
		yLookDirection = yView - yPos;
		zLookDirection = zView - zPos;

		//
		//规范化方向。
		float dp = Matrix.length(xLookDirection, yLookDirection, zLookDirection);
		xLookDirection /= dp;
		yLookDirection /= dp;
		zLookDirection /= dp;

		// 调用UpdateCamera将我们的相机移动到我们想要的方向。
		UpdateCamera(xLookDirection, yLookDirection, zLookDirection, direction);
	}

	void UpdateCamera(float xDir, float yDir, float zDir, float dir) {

		Matrix.setIdentityM(matrix, 0);
		Matrix.translateM(matrix, 0, xDir * dir, yDir * dir, zDir * dir);

		Matrix.multiplyMV(buffer, 0, matrix, 0, getLocationVector(), 0);
		Matrix.multiplyMV(buffer, 4, matrix, 0, getLocationViewVector(), 0);
		Matrix.multiplyMV(buffer, 8, matrix, 0, getLocationUpVector(), 0);

		if (isOutOfBounds(buffer)) return;

		xPos = buffer[0] / buffer[3];
		yPos = buffer[1] / buffer[3];
		zPos = buffer[2] / buffer[3];
		xView = buffer[4] / buffer[7];
		yView = buffer[5] / buffer[7];
		zView = buffer[6] / buffer[7];
		xUp = buffer[8] / buffer[11];
		yUp = buffer[9] / buffer[11];
		zUp = buffer[10] / buffer[11];

		setChanged(true);
	}

	private boolean isOutOfBounds(float[] buffer) {
		if (boundingBox.outOfBound(buffer[0] / buffer[3],buffer[1] / buffer[3],buffer[2] / buffer[3])){
			Log.d("Camera", "Out of bounds scene bounds");
			return true;
		}
		List<Object3DData> objects = scene.getObjects();
		for (int i = 0; objects != null && i < objects.size(); i++) {
			BoundingBox boundingBox = objects.get(i).getBoundingBox();
			// Log.d("Camera","BoundingBox? "+boundingBox);
			if (boundingBox != null && boundingBox.insideBounds(
					buffer[0] / buffer[3]
					, buffer[1] / buffer[3]
					, buffer[2] / buffer[3] )) {
				Log.d("Camera", "Inside bounds of '" + objects.get(i).getId() + "'");
				return true;
			}
		}
		return false;
	}

	public void StrafeCam(float dX, float dY) {
		//如果我们要调用UpdateCamera（），我们将移动相机向前或向后


		float vlen;
		float xLook = 0, yLook = 0, zLook = 0;
		xLook = xView - xPos;
		yLook = yView - yPos;
		zLook = zView - zPos;
		vlen = Matrix.length(xLook, yLook, zLook);
		xLook /= vlen;
		yLook /= vlen;
		zLook /= vlen;


		float xArriba = 0, yArriba = 0, zArriba = 0;
		xArriba = xUp - xPos;
		yArriba = yUp - yPos;
		zArriba = zUp - zPos;
		// Normalize the Right.
		vlen = Matrix.length(xArriba, yArriba, zArriba);
		xArriba /= vlen;
		yArriba /= vlen;
		zArriba /= vlen;

		float xRight = 0, yRight = 0, zRight = 0;
		xRight = (yLook * zArriba) - (zLook * yArriba);
		yRight = (zLook * xArriba) - (xLook * zArriba);
		zRight = (xLook * yArriba) - (yLook * xArriba);
		vlen = Matrix.length(xRight, yRight, zRight);
		xRight /= vlen;
		yRight /= vlen;
		zRight /= vlen;


		float xSky = 0, ySky = 0, zSky = 0;

		xSky = (yRight * zLook) - (zRight * yLook);
		ySky = (zRight * xLook) - (xRight * zLook);
		zSky = (xRight * yLook) - (yRight * xLook);
		vlen = Matrix.length(xSky, ySky, zSky);
		xSky /= vlen;
		ySky /= vlen;
		zSky /= vlen;

		UpdateCamera(xSky, ySky, zSky, dX);
	}

	public void RotateCamera(float AngleDir, float xSpeed, float ySpeed, float zSpeed) {
		float xNewLookDirection = 0, yNewLookDirection = 0, zNewLookDirection = 0;
		float xLookDirection = 0, yLookDirection = 0, zLookDirection = 0;
		float CosineAngle = 0, SineAngle = 0;


		CosineAngle = (float) Math.cos(AngleDir);
		SineAngle = (float) Math.sin(AngleDir);
		xLookDirection = xView - xPos;
		yLookDirection = yView - yPos;
		zLookDirection = zView - zPos;

		float dp = 1 / (float) Math.sqrt(
				xLookDirection * xLookDirection + yLookDirection * yLookDirection + zLookDirection * zLookDirection);
		xLookDirection *= dp;
		yLookDirection *= dp;
		zLookDirection *= dp;

		xNewLookDirection = (CosineAngle + (1 - CosineAngle) * xSpeed) * xLookDirection;
		xNewLookDirection += ((1 - CosineAngle) * xSpeed * ySpeed - zSpeed * SineAngle) * yLookDirection;
		xNewLookDirection += ((1 - CosineAngle) * xSpeed * zSpeed + ySpeed * SineAngle) * zLookDirection;

		//
		//计算新的Y位置。
		yNewLookDirection = ((1 - CosineAngle) * xSpeed * ySpeed + zSpeed * SineAngle) * xLookDirection;
		yNewLookDirection += (CosineAngle + (1 - CosineAngle) * ySpeed) * yLookDirection;
		yNewLookDirection += ((1 - CosineAngle) * ySpeed * zSpeed - xSpeed * SineAngle) * zLookDirection;

		//
		//计算新的X位置。
		zNewLookDirection = ((1 - CosineAngle) * xSpeed * zSpeed - ySpeed * SineAngle) * xLookDirection;
		zNewLookDirection += ((1 - CosineAngle) * ySpeed * zSpeed + xSpeed * SineAngle) * yLookDirection;
		zNewLookDirection += (CosineAngle + (1 - CosineAngle) * zSpeed) * zLookDirection;

		//最后我们将新旋转添加到旧视图以正确旋转相机
		xView = xPos + xNewLookDirection;
		yView = yPos + yNewLookDirection;
		zView = zPos + zNewLookDirection;
	}

	public void Rotate(float incX, float incY) {
		RotateByMouse(AIM + incX, AIM + incY, AIM, AIM);
	}

	void RotateByMouse(float mousePosX, float mousePosY, float midX, float midY) {
		float yDirection = 0.0f; // Direction angle.
		float yRotation = 0.0f; // Rotation angle.


		if ((mousePosX == midX) && (mousePosY == midY))
			return;

		yDirection = (float) ((midX - mousePosX)) / 1.0f;
		yRotation = (float) ((midY - mousePosY)) / 1.0f;
		currentRotationAngle -= yRotation;

		if (currentRotationAngle > 1.5f) {
			currentRotationAngle = 1.5f;
			return;
		}

		if (currentRotationAngle < -1.5f) {
			currentRotationAngle = -1.5f;
			return;
		}

		float xAxis = 0, yAxis = 0, zAxis = 0;
		float xDir = 0, yDir = 0, zDir = 0;

		//获取视图的方向。
		xDir = xView - xPos;
		yDir = yView - yPos;
		zDir = zView - zPos;

		//
		//获得方向和向上的交叉结果。
		xAxis = (yDir * zUp) - (zDir * yUp);
		yAxis = (zDir * xUp) - (xDir * zUp);
		zAxis = (xDir * yUp) - (yDir * xUp);

		//
		float len = 1 / (float) Math.sqrt(xAxis * xAxis + yAxis * yAxis + zAxis * zAxis);
		xAxis *= len;
		yAxis *= len;
		zAxis *= len;

		//
		RotateCamera(yRotation, xAxis, yAxis, zAxis);
		RotateCamera(yDirection, 0, 1, 0);
	}


	public void translateCamera(float dX, float dY) {
		float vlen;

		float xLook = 0, yLook = 0, zLook = 0;
		xLook = xView - xPos;
		yLook = yView - yPos;
		zLook = zView - zPos;
		vlen = Matrix.length(xLook, yLook, zLook);
		xLook /= vlen;
		yLook /= vlen;
		zLook /= vlen;


       // Arriba是3D矢量，几乎**相当于2D用户Y矢量
		//获取向上矢量的方向
		float xArriba = 0, yArriba = 0, zArriba = 0;
		xArriba = xUp - xPos;
		yArriba = yUp - yPos;
		zArriba = zUp - zPos;
		vlen = Matrix.length(xArriba, yArriba, zArriba);
		xArriba /= vlen;
		yArriba /= vlen;
		zArriba /= vlen;


        //右边是3D矢量，相当于2D用户X矢量
        //为了计算右向量，我们必须计算出它的叉积
		//先前计算的矢量
		// A x B = (a1, a2, a3) x (b1, b2, b3) = (a2 * b3 - b2 * a3 , - a1 * b3 + b1 * a3 , a1 * b2 - b1 * a2)
		float xRight = 0, yRight = 0, zRight = 0;
		xRight = (yLook * zArriba) - (zLook * yArriba);
		yRight = (zLook * xArriba) - (xLook * zArriba);
		zRight = (xLook * yArriba) - (yLook * xArriba);
		vlen = Matrix.length(xRight, yRight, zRight);
		xRight /= vlen;
		yRight /= vlen;
		zRight /= vlen;


		 //一旦我们有了Look＆Right矢量，我们就可以重新计算最终的Arriba矢量在哪里，
         //因此它等同于用户2D Y向量
		xArriba = (yRight * zLook) - (zRight * yLook);
		yArriba = (zRight * xLook) - (xRight * zLook);
		zArriba = (xRight * yLook) - (yRight * xLook);
		vlen = Matrix.length(xArriba, yArriba, zArriba);
		xArriba /= vlen;
		yArriba /= vlen;
		zArriba /= vlen;

		float[] coordinates = new float[] { xPos, yPos, zPos, 1, xView, yView, zView, 1, xUp, yUp, zUp, 1 };

		if (dX != 0 && dY != 0) {


			xRight *= dY;
			yRight *= dY;
			zRight *= dY;
			xArriba *= dX;
			yArriba *= dX;
			zArriba *= dX;

			float rotX, rotY, rotZ;
			rotX = xRight + xArriba;
			rotY = yRight + yArriba;
			rotZ = zRight + zArriba;
			vlen = Matrix.length(rotX, rotY, rotZ);
			rotX /= vlen;
			rotY /= vlen;
			rotZ /= vlen;

			//在这种情况下，我们使用vlen角度，因为对角线不垂直到最初的Right和Arriba向量
			createRotationMatrixAroundVector(buffer, 24, vlen, rotX, rotY, rotZ);
		}
		else if (dX != 0){

			createRotationMatrixAroundVector(buffer, 24, dX, xArriba, yArriba, zArriba);
		}
		else{

			createRotationMatrixAroundVector(buffer, 24, dY, xRight, yRight, zRight);
		}
		multiplyMMV(buffer, 0, buffer, 24, coordinates, 0);

		if (isOutOfBounds(buffer)) return;

		xPos = buffer[0] / buffer[3];
		yPos = buffer[1] / buffer[3];
		zPos = buffer[2] / buffer[3];
		xView = buffer[4 + 0] / buffer[4 + 3];
		yView = buffer[4 + 1] / buffer[4 + 3];
		zView = buffer[4 + 2] / buffer[4 + 3];
		xUp = buffer[8 + 0] / buffer[8 + 3];
		yUp = buffer[8 + 1] / buffer[8 + 3];
		zUp = buffer[8 + 2] / buffer[8 + 3];

		setChanged(true);

	}

	public String locationToString() {
		return xPos + "," + yPos + "," + zPos;
	}

	public String ToStringVector() {
		return xPos + "," + yPos + "," + zPos + " ; " + xView + "," + yView + "," + zView + " ; " + xUp + "," + yUp
				+ "," + zUp;
	}

	public float[] getVectors() {

		return new float[] { 
				xPos, yPos, zPos, 1f, 
				xView, yView, yView, 1f,
				xUp, yUp, zUp, 1f };

	}

	public static void createRotationMatrixAroundVector(float[] matrix, int offset, float angle, float x, float y,
			float z) {
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		float cos_1 = 1 - cos;


		matrix[offset+0 ]=cos_1*x*x + cos     ;    	matrix[offset+1 ]=cos_1*x*y - z*sin   ;   matrix[offset+2 ]=cos_1*z*x + y*sin   ;   matrix[offset+3]=0   ;
		matrix[offset+4 ]=cos_1*x*y + z*sin   ;  	matrix[offset+5 ]=cos_1*y*y + cos     ;   matrix[offset+6 ]=cos_1*y*z - x*sin   ;   matrix[offset+7]=0   ;
		matrix[offset+8 ]=cos_1*z*x - y*sin   ;  	matrix[offset+9 ]=cos_1*y*z + x*sin   ;   matrix[offset+10]=cos_1*z*z + cos    ;   matrix[offset+11]=0  ;
		matrix[offset+12]=0           		 ;      matrix[offset+13]=0          		  ;   matrix[offset+14]=0          		  ;   matrix[offset+15]=1  ;

	}

	public static void multiplyMMV(float[] result, int retOffset, float[] matrix, int matOffet, float[] vector4Matrix,
			int vecOffset) {
		for (int i = 0; i < vector4Matrix.length / 4; i++) {
			Matrix.multiplyMV(result, retOffset + (i * 4), matrix, matOffet, vector4Matrix, vecOffset + (i * 4));
		}
	}

	public float[] getLocationVector() {
		return new float[] { xPos, yPos, zPos, 1f };
	}

	public float[] getLocationViewVector() {
		return new float[] { xView, yView, zView, 1f };
	}

	public float[] getLocationUpVector() {
		return new float[] { xUp, yUp, zUp, 1f };
	}

	public String intLocationToString() {
		return (float) (xPos) + "," + (float) yPos + "," + (float) zPos;
	}

	public boolean hasChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	@Override
	public String toString() {
		return "Camera [xPos=" + xPos + ", yPos=" + yPos + ", zPos=" + zPos + ", xView=" + xView + ", yView=" + yView
				+ ", zView=" + zView + ", xUp=" + xUp + ", yUp=" + yUp + ", zUp=" + zUp + "]";
	}

	public void Rotate(float rotViewerZ) {
		if (Float.isNaN(rotViewerZ)) {
			Log.w("Rot", "NaN");
			return;
		}
		float xLook = xView - xPos;
		float yLook = yView - yPos;
		float zLook = zView - zPos;
		float vlen = Matrix.length(xLook, yLook, zLook);
		xLook /= vlen;
		yLook /= vlen;
		zLook /= vlen;

		createRotationMatrixAroundVector(buffer, 24, rotViewerZ, xLook, yLook, zLook);
		float[] coordinates = new float[] { xPos, yPos, zPos, 1, xView, yView, zView, 1, xUp, yUp, zUp, 1 };
		multiplyMMV(buffer, 0, buffer, 24, coordinates, 0);

		xPos = buffer[0];
		yPos = buffer[1];
		zPos = buffer[2];
		xView = buffer[4 + 0];
		yView = buffer[4 + 1];
		zView = buffer[4 + 2];
		xUp = buffer[8 + 0];
		yUp = buffer[8 + 1];
		zUp = buffer[8 + 2];

		setChanged(true);
	}


}
