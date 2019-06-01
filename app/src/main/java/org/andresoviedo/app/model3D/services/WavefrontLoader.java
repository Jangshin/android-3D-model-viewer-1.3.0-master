// WavefrontLoader.java
// Andrew Davison, February 2007, ad@fivedots.coe.psu.ac.th

/* Load the OBJ model from MODEL_DIR, centering and scaling it.
 The scale comes from the sz argument in the constructor, and
 is implemented by changing the vertices of the loaded model.

 The model can have vertices, normals and tex coordinates, and
 refer to materials in a MTL file.

 The OpenGL commands for rendering the model are stored in 
 a display list (modelDispList), which is drawn by calls to
 draw().

 Information about the model is printed to stdout.

 Based on techniques used in the OBJ loading code in the
 JautOGL multiplayer racing game by Evangelos Pournaras 
 (http://today.java.net/pub/a/today/2006/10/10/
 development-of-3d-multiplayer-racing-game.html 
 and https://jautogl.dev.java.net/), and the 
 Asteroids tutorial by Kevin Glass 
 (http://www.cokeandcode.com/asteroidstutorial)

 CHANGES (Feb 2007)
 - a global flipTexCoords boolean
 - drawToList() sets and uses flipTexCoords
 */

package org.andresoviedo.app.model3D.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.util.Log;

public class WavefrontLoader {

	private static final float DUMMY_Z_TC = -5.0f;
	static final boolean INDEXES_START_AT_1 = true;
	private boolean hasTCs3D = false;

	private ArrayList<Tuple3> texCoords;

	private Faces faces;
	private FaceMaterials faceMats;
	private Materials materials; //  MTL file
	private ModelDimensions modelDims; //

	private String modelNm; // 路径
	private float maxSize; //

	// metadata
	int numVerts = 0;
	int numTextures = 0;
	int numNormals = 0;
	int numFaces = 0;
	int numVertsReferences = 0;


	private FloatBuffer vertsBuffer;
	private FloatBuffer normalsBuffer;
	// TODO: build texture data directly into this buffer
	private FloatBuffer textureCoordsBuffer;

	// flags
	private final int triangleMode = GLES20.GL_TRIANGLE_FAN;

	public WavefrontLoader(String nm) {
		modelNm = nm;
		maxSize = 1.0F;

		texCoords = new ArrayList<Tuple3>();


		faceMats = new FaceMaterials();
		modelDims = new ModelDimensions();
	}

	public FloatBuffer getVerts() {
		return vertsBuffer;
	}

	public FloatBuffer getNormals() {
		return normalsBuffer;
	}

	public ArrayList<Tuple3> getTexCoords() {
		return texCoords;
	}

	public Faces getFaces() {
		return faces;
	}

	public FaceMaterials getFaceMats() {
		return faceMats;
	}

	public Materials getMaterials() {
		return materials;
	}

	public ModelDimensions getDimensions() {
		return modelDims;
	}

	//分析模型
	public void analyzeModel(InputStream is) {
		int lineNum = 0;
		String line;


		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is));

			while ((line = br.readLine()) != null) {
				lineNum++;
				line = line.trim();
				if (line.length() > 0) {

					if (line.startsWith("v ")) { // vertex
						numVerts++;
					} else if (line.startsWith("vt")) { // tex coord
						numTextures++;
					} else if (line.startsWith("vn")) {// normal
						numNormals++;
					} else if (line.startsWith("f ")) { // face
						final int faceSize;
						if (line.contains("  ")) {
							faceSize = line.split(" +").length - 1;
						} else {
							faceSize = line.split(" ").length - 1;
						}
						numFaces += (faceSize - 2);
						// 将多边形转换为三角形
						numVertsReferences += (faceSize - 2) * 3;
					} else if (line.startsWith("mtllib ")) //
					{
						materials = new Materials(line.substring(7));
					} else if (line.startsWith("usemtl ")) {// 使用纹理
					} else if (line.charAt(0) == 'g') { //
						//不做计算
					} else if (line.charAt(0) == 's') { //

					} else if (line.charAt(0) == '#') //
						continue;
					else if (line.charAt(0) == 'o') //
						continue;
					else
						System.out.println("Ignoring line " + lineNum + " : " + line);
				}
			}
		} catch (IOException e) {
			Log.e("WavefrontLoader", "Problem reading line '" + (++lineNum) + "'");
			Log.e("WavefrontLoader", e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					Log.e("WavefrontLoader", e.getMessage(), e);
				}
			}
		}

		Log.i("OBJ","顶点数:"+numVerts);
		Log.i("OBJ","角面数:"+numFaces);
	}

	/**
	 * Allocate buffers for pushing the model data
	 * TODO: use textureCoordsBuffer
	 */
	public void allocateBuffers() {

		vertsBuffer = createNativeByteBuffer(numVerts*3*4).asFloatBuffer();
		normalsBuffer = createNativeByteBuffer(numNormals*3*4).asFloatBuffer();
		textureCoordsBuffer = createNativeByteBuffer(numTextures*3*4).asFloatBuffer();
		IntBuffer buffer = createNativeByteBuffer(numFaces*3*4).asIntBuffer();
		faces = new Faces(numFaces, buffer, vertsBuffer, normalsBuffer, texCoords);
	}

	public void loadModel(InputStream is) {
		// String fnm = MODEL_DIR + modelNm + ".obj";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			readModel(br);
		} finally{
			if (br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static ByteBuffer createNativeByteBuffer(int length) {
		// 初始化形状坐标的顶点字节缓冲区
		ByteBuffer bb = ByteBuffer.allocateDirect(length);
		//使用设备硬件的本机字节顺序
		bb.order(ByteOrder.nativeOrder());
		return bb;
	}

	private void readModel(BufferedReader br)
	// 逐行解析OBJ文件
	{
		boolean isLoaded = true; //

		int lineNum = 0;
		String line;
		boolean isFirstCoord = true;
		boolean isFirstTC = true;
		int numFaces = 0;

		int vertNumber = 0;
		int normalNumber = 0;


		try {
			while (((line = br.readLine()) != null)) {
				lineNum++;
				line = line.trim();
				if (line.length() > 0) {

					if (line.startsWith("v ")) { // 顶点数据
						isLoaded = addVert(vertsBuffer, vertNumber++ * 3, line, isFirstCoord, modelDims) && isLoaded;
						if (isFirstCoord)
							isFirstCoord = false;
					} else if (line.startsWith("vt")) { //
						isLoaded = addTexCoord(line, isFirstTC) && isLoaded;
						if (isFirstTC)
							isFirstTC = false;
					} else if (line.startsWith("vn")) // 法线
						isLoaded = addVert(normalsBuffer, normalNumber++ * 3,line, isFirstCoord, null) && isLoaded;
					else if (line.startsWith("f ")) { // 面
						isLoaded = faces.addFace(line) && isLoaded;
						numFaces++;
					} else if (line.startsWith("mtllib ")) //
					{

					} else if (line.startsWith("usemtl ")) // use material
						faceMats.addUse(numFaces, line.substring(7));
					else if (line.charAt(0) == 'g') { //

					} else if (line.charAt(0) == 's') { //

					} else if (line.charAt(0) == '#') //
						continue;
					else if (line.charAt(0) == 'o') //
						continue;
					else
						System.out.println("Ignoring line " + lineNum + " : " + line);
				}
			}
		} catch (IOException e) {
			Log.e("WavefrontLoader",e.getMessage(),e);
			throw new RuntimeException(e);
		}

		if (!isLoaded) {
			Log.e("WavefrontLoader","Error loading model");
			// throw new RuntimeException("Error loading model");
		}
	} // end of readModel()

	/**
	 * 解析顶点并将其添加到缓冲区。如果顶点不能被解析 默认（0,0,0）
	 *
	 *
	 * @param buffer 要添加顶点的缓冲区
	 * @param offset 缓冲区的偏移量
	 * @param line 要解析的顶点
	 * @param isFirstCoord 如果这是第一个被解析的顶点
	 * @param dimensions 模型显示，以便更新 (TODO move this out of this method)
	 * @return <
	 */
	private boolean addVert(FloatBuffer buffer, int offset, String line, boolean isFirstCoord, ModelDimensions dimensions)
	/*
	 * 将Vertex从行“V X Y Z”添加到VERT查询列表中，并更新模型维度的信息
	 */
	{
		float x=0,y=0,z=0;
		try{
			String[] tokens = null;
			if (line.contains("  ")){
				tokens = line.split(" +");
			}
			else{
				tokens = line.split(" ");
			}
			x = Float.parseFloat(tokens[1]);
			y = Float.parseFloat(tokens[2]);
			z = Float.parseFloat(tokens[3]);

			if (dimensions != null) {
				if (isFirstCoord)
					modelDims.set(x, y, z);
				else
					modelDims.update(x, y, z);
			}

			return true;

		}catch(NumberFormatException ex){
			Log.e("WavefrontLoader",ex.getMessage());
		} finally{

			buffer.put(offset, x).put(offset+1, y).put(offset+2, z);
		}

		return false;
	}

	private boolean addTexCoord(String line, boolean isFirstTC)
	/*
	 * 将“VT U V W”行中的纹理坐标添加到texcocorylist中.
	 * 在线上，这是通过查看第一条Tex coord线来确定的
	 */
	{
		if (isFirstTC) {
			hasTCs3D = checkTC3D(line);
			System.out.println("Using 3D tex coords: " + hasTCs3D);
		}

		Tuple3 texCoord = readTCTuple(line);
		if (texCoord != null) {
			texCoords.add(texCoord);
			return true;
		}

		return false;
	}

	private boolean checkTC3D(String line)
	/*
	 *检查模型数据
	 */
	{
		String[] tokens = line.split("\\s+");
		return (tokens.length == 4);
	}

	private Tuple3 readTCTuple(String line)
	/*
	 * 该行以“vt”OBJ字开头，两个或三个浮点数（x，y，z）用于由空格分隔的tex坐标
	 * 如果只有两个坐标，则为z值分配一个虚拟值DUMMY_Z_TC。
	 */
	{
		StringTokenizer tokens = new StringTokenizer(line, " ");
		tokens.nextToken(); //

		try {
			float x = Float.parseFloat(tokens.nextToken());
			float y = Float.parseFloat(tokens.nextToken());

			float z = DUMMY_Z_TC;
			if (hasTCs3D)
				z = Float.parseFloat(tokens.nextToken());
			return new Tuple3(x, y, z);
		} catch (NumberFormatException e) {
			System.out.println(e.getMessage());
		}

		return null; // means an error occurred
	}

	public void reportOnModel() {
		Log.i("WavefrontLoader","No. of vertices: " + vertsBuffer.capacity()/3);
		Log.i("WavefrontLoader","No. of normal coords: " + normalsBuffer.capacity()/3);
		Log.i("WavefrontLoader","No. of tex coords: " + texCoords.size());
		Log.i("WavefrontLoader","No. of faces: " + faces.getSize());

		modelDims.reportDimensions();
		// 模型尺寸（居中和缩放前）

		if (materials != null)
			materials.showMaterials(); // list defined materials
		faceMats.showUsedMaterials(); // show what materials have been used by
		// faces
	} //

	public static class Tuple3 {
		private float x, y, z;

		public Tuple3(float xc, float yc, float zc) {
			x = xc;
			y = yc;
			z = zc;
		}

		public String toString() {
			return "( " + x + ", " + y + ", " + z + " )";
		}

		public void setX(float xc) {
			x = xc;
		}

		public float getX() {
			return x;
		}

		public void setY(float yc) {
			y = yc;
		}

		public float getY() {
			return y;
		}

		public void setZ(float zc) {
			z = zc;
		}

		public float getZ() {
			return z;
		}

	} //

	public static class ModelDimensions {
		// edge coordinates
		private float leftPt, rightPt; // on x-axis
		private float topPt, bottomPt; // on y-axis
		private float farPt, nearPt; // on z-axis

		// for reporting
		private DecimalFormat df = new DecimalFormat("0.##"); // 2 dp

		public ModelDimensions() {
			leftPt = 0.0f;
			rightPt = 0.0f;
			topPt = 0.0f;
			bottomPt = 0.0f;
			farPt = 0.0f;
			nearPt = 0.0f;
		} //

		public void set(float x, float y, float z)
		// initialize the model's edge coordinates
		{
			rightPt = x;
			leftPt = x;

			topPt = y;
			bottomPt = y;

			nearPt = z;
			farPt = z;
		} // end of set()

		public void update(float x, float y, float z)
		//使用vert更新边缘坐标
		{
			if (x > rightPt)
				rightPt = x;
			if (x < leftPt)
				leftPt = x;

			if (y > topPt)
				topPt = y;
			if (y < bottomPt)
				bottomPt = y;

			if (z > nearPt)
				nearPt = z;
			if (z < farPt)
				farPt = z;
		} // end of update()


		public float getWidth() {
			return (rightPt - leftPt);
		}

		public float getHeight() {
			return (topPt - bottomPt);
		}

		public float getDepth() {
			return (nearPt - farPt);
		}

		public float getLargest() {
			float height = getHeight();
			float depth = getDepth();

			float largest = getWidth();
			if (height > largest)
				largest = height;
			if (depth > largest)
				largest = depth;

			return largest;
		}

		public Tuple3 getCenter() {
			float xc = (rightPt + leftPt) / 2.0f;
			float yc = (topPt + bottomPt) / 2.0f;
			float zc = (nearPt + farPt) / 2.0f;
			return new Tuple3(xc, yc, zc);
		} //

		public void reportDimensions() {
			Tuple3 center = getCenter();

			System.out.println("x Coords: " + df.format(leftPt) + " to " + df.format(rightPt));
			System.out.println("  Mid: " + df.format(center.getX()) + "; Width: " + df.format(getWidth()));

			System.out.println("y Coords: " + df.format(bottomPt) + " to " + df.format(topPt));
			System.out.println("  Mid: " + df.format(center.getY()) + "; Height: " + df.format(getHeight()));

			System.out.println("z Coords: " + df.format(nearPt) + " to " + df.format(farPt));
			System.out.println("  Mid: " + df.format(center.getZ()) + "; Depth: " + df.format(getDepth()));
		} //

	} //

	public static class Materials {

		public Map<String, Material> materials;
		// 存储从MTL文件数据构建的Material对象

		//
		private String mfnm;

		public Materials(String mtlFnm) {
			//
			//当支持多个纹理时，将其更改为简单的HashMap
			materials = new LinkedHashMap<String, Material>();

			this.mfnm = mtlFnm;
			// file = new File(mtlFnm);
		}

		public void readMaterials(File currentDir, String assetsDir, AssetManager am) {
			try {
				InputStream is;
				if (currentDir != null) {
					File file = new File(currentDir, mfnm);
					System.out.println("Loading material from " + file);
					is = new FileInputStream(file);
				} else {
					System.out.println("Loading material from " + mfnm);
					is = am.open(assetsDir + "/" + mfnm);
				}
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				readMaterials(br);
				br.close();
			} catch (FileNotFoundException ex){
				Log.w("WavefrontLoader", ex.getMessage());
			} catch (IOException e) {
				Log.e("WavefrontLoader", e.getMessage(), e);
			}

		} //

		private void readMaterials(BufferedReader br)
		/*
		 * 逐行解析MTL文件，构建材料ArrayList中收集的Material对象。
		 */
		{
			Log.v("materials", "Reading material...");
			try {
				String line;
				Material currMaterial = null; //

				while (((line = br.readLine()) != null)) {
					line = line.trim();
					if (line.length() == 0)
						continue;

					if (line.startsWith("newmtl ")) { //
						if (currMaterial != null) //
							materials.put(currMaterial.getName(), currMaterial);

						// 开始存储新材质的信息
						String name = line.substring(7);
						Log.d("Loader", "New material found: " + name);
						currMaterial = new Material(name);
					} else if (line.startsWith("map_Kd ")) { // texture filename
						//
						String textureFilename = line.substring(7);
						Log.d("Loader", "New texture found: " + textureFilename);
						currMaterial.setTexture(textureFilename);
					} else if (line.startsWith("Ka ")) //
						currMaterial.setKa(readTuple3(line));
					else if (line.startsWith("Kd ")) //
						currMaterial.setKd(readTuple3(line));
					else if (line.startsWith("Ks ")) //
						currMaterial.setKs(readTuple3(line));
					else if (line.startsWith("Ns ")) { //
						float val = Float.valueOf(line.substring(3)).floatValue();
						currMaterial.setNs(val);
					} else if (line.charAt(0) == 'd') { //
						float val = Float.valueOf(line.substring(2)).floatValue();
						currMaterial.setD(val);
					} else if (line.startsWith("Tr ")) { //
						float val = Float.valueOf(line.substring(3)).floatValue();
						currMaterial.setD(1 - val);
					} else if (line.startsWith("illum ")) { //

					} else if (line.charAt(0) == '#') // comment line
						continue;
					else
						System.out.println("Ignoring MTL line: " + line);

				}
				if (currMaterial != null) {
					materials.put(currMaterial.getName(), currMaterial);
				}
			} catch (Exception e) {
				Log.e("materials", e.getMessage(), e);
			}
		} //

		private Tuple3 readTuple3(String line)
		/*
		 * 该行以MTL字开头，例如Ka，Kd，Ks，以及由空格分隔的三个浮点数（x，y，z）
		 */
		{
			StringTokenizer tokens = new StringTokenizer(line, " ");
			tokens.nextToken(); // skip MTL word

			try {
				float x = Float.parseFloat(tokens.nextToken());
				float y = Float.parseFloat(tokens.nextToken());
				float z = Float.parseFloat(tokens.nextToken());

				return new Tuple3(x, y, z);
			} catch (NumberFormatException e) {
				System.out.println(e.getMessage());
			}

			return null; //
		} //

		public void showMaterials()
		// 列出所有Material对象
		{
			Log.i("WavefrontLoader","No. of materials: " + materials.size());
			Material m;
			for (int i = 0; i < materials.size(); i++) {
				m = (Material) materials.get(i);
				m.showMaterial();
				// System.out.println();
			}
		} //

		public Material getMaterial(String name) {
			return materials.get(name);
		}

	} //

	public static class Material {
		private String name;

		//
		private Tuple3 ka, kd, ks; //
		private float ns; //
		private float d; //

		//
		private String texFnm;
		private String texture;

		public Material(String nm) {
			name = nm;

			d = 1.0f;
			ns = 0.0f;
			ka = null;
			kd = null;
			ks = null;

			texFnm = null;
			texture = null;
		} //

		public void showMaterial() {
			System.out.println(name);
			if (ka != null)
				System.out.println("  Ka: " + ka.toString());
			if (kd != null)
				System.out.println("  Kd: " + kd.toString());
			if (ks != null)
				System.out.println("  Ks: " + ks.toString());
			if (ns != 0.0f)
				System.out.println("  Ns: " + ns);
			if (d != 1.0f)
				System.out.println("  d: " + d);
			if (texFnm != null)
				System.out.println("  Texture file: " + texFnm);
		} //

		public boolean hasName(String nm) {
			return name.equals(nm);
		}



		public void setD(float val) {
			d = val;
		}

		public float getD() {
			return d;
		}

		public void setNs(float val) {
			ns = val;
		}

		public float getNs() {
			return ns;
		}

		public void setKa(Tuple3 t) {
			ka = t;
		}

		public Tuple3 getKa() {
			return ka;
		}

		public void setKd(Tuple3 t) {
			kd = t;
		}

		public Tuple3 getKd() {
			return kd;
		}

		public float[] getKdColor() {
			if (kd == null) {
				return null;
			}
			return new float[] { kd.getX(), kd.getY(), kd.getZ(), getD() };
		}

		public void setKs(Tuple3 t) {
			ks = t;
		}

		public Tuple3 getKs() {
			return ks;
		}

		public void setMaterialColors(GLES20 gl)
		//使用此材质的颜色信息开始渲染
		{

		} //

		// ---------设置/获取纹理信息的方法 --------------

		public void setTexture(String t) {
			texture = t;
		}

		public String getTexture() {
			return texture;
		}

		String getName() {
			return name;
		}

	} //

	public class Faces {
		private static final float DUMMY_Z_TC = -5.0f;

		public final int totalFaces;
		/**
		 * 每个面使用的顶点的索引
		 */
		public IntBuffer facesVertIdxs;
		/**
		 * 每个面使用的tex坐标的索引
		 */
		public ArrayList<int[]> facesTexIdxs;
		/**
		 * 每个面使用的法线的索引
		 */
		public ArrayList<int[]> facesNormIdxs;

		private FloatBuffer normals;
		private ArrayList<Tuple3> texCoords;

		// 顶点引用的总数。也就是说，每个面引用3个或更多个向量。这是所有数据总和
		// faces
		private int facesLoadCounter;
		private int faceVertexLoadCounter = 0;
		private int verticesReferencesCount;

		//
		//

		Faces(int totalFaces, IntBuffer buffer, FloatBuffer vs, FloatBuffer ns, ArrayList<Tuple3> ts) {
			this.totalFaces = totalFaces;
			normals = ns;
			texCoords = ts;

			facesVertIdxs = buffer;
			facesTexIdxs = new ArrayList<int[]>();
			facesNormIdxs = new ArrayList<int[]>();
		} //
		public int getSize(){
			return totalFaces;
		}


		public boolean loaded(){
			return facesLoadCounter == totalFaces;
		}

		/**
		 * 从“f v / vt / vn ...”行得到这个面部的指示，其中vt或vn索引值可能不存在。
		 */
		public boolean addFace(String line) {
			try {
				line = line.substring(2); //
				String[] tokens = null;
				if (line.contains("  ")){
					tokens = line.split(" +");
				}
				else{
					tokens = line.split(" ");
				}
				int numTokens = tokens.length; //
				// 创建数组以保存v，vt，vn标记

				int vt[] = null;
				int vn[] = null;


				for (int i = 0, faceIndex = 0; i < numTokens; i++, faceIndex++) {

					// 所有多边形转换为三角形
					if (faceIndex > 2){
						// 将多边形转换为三角形
						faceIndex = 0;

						facesLoadCounter++;
						verticesReferencesCount += 3;
						if (vt != null)  facesTexIdxs.add(vt);
						if (vn != null) facesNormIdxs.add(vn);

						vt = null;
						vn = null;

						i -= 2;
					}

					// 将所有多边形转换为三角形
					String faceToken = null;
					if (WavefrontLoader.this.triangleMode == GLES20.GL_TRIANGLE_FAN) {
						if (faceIndex == 0){
							//在FAN模式下，所有面都共享初始顶点
							faceToken = tokens[0];//
						}else{
							faceToken = tokens[i]; //
						}
					}
					else {
						// GL.GL_TRIANGLES | GL.GL_TRIANGLE_STRIP
						faceToken = tokens[i]; //
					}


					String[] faceTokens = faceToken.split("/");
					int numSeps = faceTokens.length; //
					//

					int vertIdx = Integer.parseInt(faceTokens[0]);
					/*if (vertIdx > 65535){
						Log.e("WavefrontLoader","Ignoring face because its out of range (>65535)");
						continue;
					}*/
					if (numSeps > 1){
						if (vt == null)	vt = new int[3];
						try{
							vt[faceIndex] = Integer.parseInt(faceTokens[1]);
						}catch(NumberFormatException ex){
							vt[faceIndex] = 0;
						}
					}
					if (numSeps > 2){
						if (vn == null)	vn = new int[3];
						try{
							vn[faceIndex] = Integer.parseInt(faceTokens[2]);
						}catch(NumberFormatException ex){
							vn[faceIndex] = 0;
						}
					}
					//
					//如果缺少vt或vn索引值，则添加0;
					//实际指数从1开始

					if (WavefrontLoader.INDEXES_START_AT_1) {
						vertIdx--;
						if (vt != null)	vt[faceIndex] = vt[faceIndex] - 1;
						if (vn != null) vn[faceIndex] = vn[faceIndex] - 1;
					}
					// 存储面的索引
					facesVertIdxs.put(faceVertexLoadCounter++,vertIdx);
				}
				if (vt != null)  facesTexIdxs.add(vt);
				if (vn != null) facesNormIdxs.add(vn);

				facesLoadCounter++;
				verticesReferencesCount += 3;

			} catch (NumberFormatException e) {
				Log.e("WavefrontLoader",e.getMessage(),e);
				return false;
			}
			return true;
		}


		public int getVerticesReferencesCount() {
			// 只有三角形
			return getSize()*3;
		}

		public IntBuffer getIndexBuffer(){return facesVertIdxs;}

	} //

	public static class FaceMaterials {
		// 首先使用材料的面索引（整数）
		private HashMap<Integer, String> faceMats;

		//
		private HashMap<String, Integer> matCount;

		// 使用材料（字符串）的次数

		public FaceMaterials() {
			faceMats = new HashMap<Integer, String>();
			matCount = new HashMap<String, Integer>();
		} //

		public void addUse(int faceIdx, String matName) {
			// 存储面索引及其使用的材质
			if (faceMats.containsKey(faceIdx)) // 面的参数数已经存在
				System.out.println("Face index " + faceIdx + " changed to use material " + matName);
			faceMats.put(faceIdx, matName);

			// 存储face使用matName的次数
			if (matCount.containsKey(matName)) {
				int i = (Integer) matCount.get(matName) + 1;
				matCount.put(matName, i);
			} else
				matCount.put(matName, 1);
		} //

		public String findMaterial(int faceIdx) {
			return (String) faceMats.get(faceIdx);
		}

		public void showUsedMaterials()
		/*
		 * 列出面使用的所有材质以及使用它们的面数。
		 */
		{
			System.out.println("No. of materials used: " + faceMats.size());

			// 构建材质名称的迭代器
			Set<String> keys = matCount.keySet();
			Iterator<String> iter = keys.iterator();

			// 循环显示散列图，显示每种材料的计数
			String matName;
			int count;
			while (iter.hasNext()) {
				matName = iter.next();
				count = (Integer) matCount.get(matName);

				System.out.print(matName + ": " + count);
				System.out.println();
			}
		} //

		public boolean isEmpty() {
			return faceMats.isEmpty() || this.matCount.isEmpty();
		}

	}

}
