/*
 * Copyright (c) 2016 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the JogAmp Community.
 *
 */
package org.jogamp.java3d.utils.shader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.TriangleArray;

/**
 * Note Cube does not have normals and so cannot be used for lit geometries
 * It is designed as a general debug position indicator
 * If you want to add a Material and have lighting enabled please use a org.jogamp.java3d.utils.geometry.Box
 *
 */
public class Cube extends Shape3D
{

	private static final float[] verts = {
			// front face
			1.0f, -1.0f, 1.0f, //1
			1.0f, 1.0f, 1.0f, //2
			-1.0f, 1.0f, 1.0f, //3
			1.0f, -1.0f, 1.0f, //1
			-1.0f, 1.0f, 1.0f, //3
			-1.0f, -1.0f, 1.0f, //4
			// back face
			-1.0f, -1.0f, -1.0f, //1
			-1.0f, 1.0f, -1.0f, //2
			1.0f, 1.0f, -1.0f, //3
			-1.0f, -1.0f, -1.0f, //1
			1.0f, 1.0f, -1.0f, //3
			1.0f, -1.0f, -1.0f, //4
			// right face
			1.0f, -1.0f, -1.0f, //1
			1.0f, 1.0f, -1.0f, //2
			1.0f, 1.0f, 1.0f, //3
			1.0f, -1.0f, -1.0f, //1
			1.0f, 1.0f, 1.0f, //3
			1.0f, -1.0f, 1.0f, //4
			// left face
			-1.0f, -1.0f, 1.0f, //1
			-1.0f, 1.0f, 1.0f, //2
			-1.0f, 1.0f, -1.0f, //3
			-1.0f, -1.0f, 1.0f, //1
			-1.0f, 1.0f, -1.0f, //3
			-1.0f, -1.0f, -1.0f, //4
			// top face
			1.0f, 1.0f, 1.0f, //1
			1.0f, 1.0f, -1.0f, //2
			-1.0f, 1.0f, -1.0f, //3
			1.0f, 1.0f, 1.0f, //1
			-1.0f, 1.0f, -1.0f, //3
			-1.0f, 1.0f, 1.0f, //4			
			// bottom face
			-1.0f, -1.0f, 1.0f, //1
			-1.0f, -1.0f, -1.0f, //2
			1.0f, -1.0f, -1.0f, //3
			-1.0f, -1.0f, 1.0f, //1
			1.0f, -1.0f, -1.0f, //3
			1.0f, -1.0f, 1.0f, };//4

	private static final float[] colors = {
			// front face (red)
			1.0f, 0.0f, 0.0f, //1
			1.0f, 0.0f, 0.0f, //2
			1.0f, 0.0f, 0.0f, //3
			1.0f, 0.0f, 0.0f, //1
			1.0f, 0.0f, 0.0f, //3
			1.0f, 0.0f, 0.0f, //4
			// back face (green)
			0.0f, 1.0f, 0.0f, //1
			0.0f, 1.0f, 0.0f, //2
			0.0f, 1.0f, 0.0f, //3
			0.0f, 1.0f, 0.0f, //1
			0.0f, 1.0f, 0.0f, //3
			0.0f, 1.0f, 0.0f, //4			
			// right face (blue)
			0.0f, 0.0f, 1.0f, //1
			0.0f, 0.0f, 1.0f, //2
			0.0f, 0.0f, 1.0f, //3
			0.0f, 0.0f, 1.0f, //1
			0.0f, 0.0f, 1.0f, //3
			0.0f, 0.0f, 1.0f, //4
			// left face (yellow)
			1.0f, 1.0f, 0.0f, //1
			1.0f, 1.0f, 0.0f, //2
			1.0f, 1.0f, 0.0f, //3
			1.0f, 1.0f, 0.0f, //1
			1.0f, 1.0f, 0.0f, //3
			1.0f, 1.0f, 0.0f, //4
			// top face (magenta)
			1.0f, 0.0f, 1.0f, //1
			1.0f, 0.0f, 1.0f, //2
			1.0f, 0.0f, 1.0f, //3
			1.0f, 0.0f, 1.0f, //1
			1.0f, 0.0f, 1.0f, //3
			1.0f, 0.0f, 1.0f, //4
			// bottom face (cyan)
			0.0f, 1.0f, 1.0f, //1
			0.0f, 1.0f, 1.0f, //2
			0.0f, 1.0f, 1.0f, //3
			0.0f, 1.0f, 1.0f, //1
			0.0f, 1.0f, 1.0f, //3
			0.0f, 1.0f, 1.0f, };//4

	/**
	 * Constructs a color cube with unit scale.  The corners of the
	 * color cube are [-1,-1,-1] and [1,1,1].
	 */
	public Cube()
	{
		TriangleArray cube = new TriangleArray(36,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		cube.setCoordRefBuffer(new J3DBuffer(makeFloatBuffer(verts)));
		cube.setColorRefBuffer(new J3DBuffer(makeFloatBuffer(colors)));

		this.setGeometry(cube);
		this.setAppearance(new SimpleShaderAppearance());
	}

	/**
	 * Constructs a color cube with the specified scale.  The corners of the
	 * color cube are [-scale,-scale,-scale] and [scale,scale,scale].
	 * @param scale the scale of the cube
	 */
	public Cube(double scale)
	{
		TriangleArray cube = new TriangleArray(36,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		float scaledVerts[] = new float[verts.length];
		for (int i = 0; i < verts.length; i++)
			scaledVerts[i] = verts[i] * (float) scale;

		cube.setCoordRefBuffer(new J3DBuffer(makeFloatBuffer(scaledVerts)));
		cube.setColorRefBuffer(new J3DBuffer(makeFloatBuffer(colors)));

		this.setGeometry(cube);

		this.setAppearance(new SimpleShaderAppearance());
	}

	public Cube(double scale, float r, float g, float b)
	{
		TriangleArray cube = new TriangleArray(36,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		float scaledVerts[] = new float[verts.length];
		for (int i = 0; i < verts.length; i++)
			scaledVerts[i] = verts[i] * (float) scale;

		cube.setCoordRefBuffer(new J3DBuffer(makeFloatBuffer(scaledVerts)));

		float colorsSet[] = new float[36 * 3];
		for (int i = 0; i < 36; i++)
		{
			colorsSet[i * 3 + 0] = r;
			colorsSet[i * 3 + 1] = g;
			colorsSet[i * 3 + 2] = b;
		}

		cube.setColorRefBuffer(new J3DBuffer(makeFloatBuffer(colorsSet)));

		this.setGeometry(cube);
		this.setAppearance(new SimpleShaderAppearance());
	}

	/**
		 * Constructs a color cube with the specified scale.  The corners of the
		 * color cube are [-scale,-scale,-scale] and [scale,scale,scale].
		 * @param scale the scale of the cube
		 */
	public Cube(double xScale, double yScale, double zScale)
	{
		TriangleArray cube = new TriangleArray(36,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		float scaledVerts[] = new float[verts.length];
		for (int i = 0; i < verts.length; i += 3)
		{
			scaledVerts[i + 0] = verts[i + 0] * (float) xScale;
			scaledVerts[i + 1] = verts[i + 1] * (float) yScale;
			scaledVerts[i + 2] = verts[i + 2] * (float) zScale;
		}

		cube.setCoordRefBuffer(new J3DBuffer(makeFloatBuffer(scaledVerts)));
		cube.setColorRefBuffer(new J3DBuffer(makeFloatBuffer(colors)));

		this.setGeometry(cube);
		this.setAppearance(new SimpleShaderAppearance());
	}

	public Cube(double xScale, double yScale, double zScale, float r, float g, float b)
	{
		TriangleArray cube = new TriangleArray(36,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		float scaledVerts[] = new float[verts.length];
		for (int i = 0; i < verts.length; i += 3)
		{
			scaledVerts[i + 0] = verts[i + 0] * (float) xScale;
			scaledVerts[i + 1] = verts[i + 1] * (float) yScale;
			scaledVerts[i + 2] = verts[i + 2] * (float) zScale;
		}

		cube.setCoordRefBuffer(new J3DBuffer(makeFloatBuffer(scaledVerts)));

		float colorsSet[] = new float[36 * 3];
		for (int i = 0; i < 36; i++)
		{
			colorsSet[i * 3 + 0] = r;
			colorsSet[i * 3 + 1] = g;
			colorsSet[i * 3 + 2] = b;
		}

		cube.setColorRefBuffer(new J3DBuffer(makeFloatBuffer(colorsSet)));

		this.setGeometry(cube);
		this.setAppearance(new SimpleShaderAppearance());
	}

	public Cube(float xMin, float yMin, float zMin, float xMax, float yMax, float zMax)
	{
		TriangleArray cube = new TriangleArray(36,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		float scaledVerts[] = new float[] {
				// front face
				xMax, yMin, zMax, //1
				xMax, yMax, zMax, //2
				xMin, yMax, zMax, //3
				xMax, yMin, zMax, //1
				xMin, yMax, zMax, //3
				xMin, yMin, zMax, //4
				// back face
				xMin, yMin, zMin, //1
				xMin, yMax, zMin, //2
				xMax, yMax, zMin, //3				
				xMin, yMin, zMin, //1
				xMax, yMax, zMin, //3
				xMax, yMin, zMin, //4
				// right face
				xMax, yMin, zMin, //1
				xMax, yMax, zMin, //2
				xMax, yMax, zMax, //3
				xMax, yMin, zMin, //1
				xMax, yMax, zMax, //3
				xMax, yMin, zMax, //4
				// left face
				xMin, yMin, zMax, //1
				xMin, yMax, zMax, //2
				xMin, yMax, zMin, //3
				xMin, yMin, zMax, //1
				xMin, yMax, zMin, //3
				xMin, yMin, zMin, //4				
				// top face
				xMax, yMax, zMax, //1
				xMax, yMax, zMin, //2
				xMin, yMax, zMin, //3
				xMax, yMax, zMax, //1
				xMin, yMax, zMin, //3
				xMin, yMax, zMax, //4
				// bottom face
				xMin, yMin, zMax, //1
				xMin, yMin, zMin, //2
				xMax, yMin, zMin, //3
				xMin, yMin, zMax, //1
				xMax, yMin, zMin, //3
				xMax, yMin, zMax, };//4

		cube.setCoordRefBuffer(new J3DBuffer(makeFloatBuffer(scaledVerts)));
		cube.setColorRefBuffer(new J3DBuffer(makeFloatBuffer(colors)));

		this.setGeometry(cube);
		this.setAppearance(new SimpleShaderAppearance());
	}
	public static FloatBuffer makeFloatBuffer(float[] arr)
	{
		ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(arr);
		fb.position(0);
		return fb;
	}
}
