package org.jogamp.java3d.utils.geometry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jogamp.java3d.Geometry;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Shape3D;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Color4f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.TexCoord2f;
import org.jogamp.vecmath.Vector3f;

/**
 * Convenience for merging geometries for performance on the pipeline
 * Note live Shape3D will have trouble
 * @author phil
 *
 */
public class GeometryMerger
{

	public static GeometryInfo mergeShape3D(Shape3D shape3d)
	{
		ArrayList<GeometryInfo> gis = new ArrayList<GeometryInfo>();
		Iterator<Geometry> it = shape3d.getAllGeometries();
		while (it.hasNext())
			gis.add(new GeometryInfo((GeometryArray) it.next()));

		return mergeGeometryInfo(gis);
	}

	public static GeometryInfo mergeGeometryArray(List<GeometryArray> gas)
	{
		ArrayList<GeometryInfo> gis = new ArrayList<GeometryInfo>();
		for (GeometryArray ga : gas)
			if (ga != null)
				gis.add(new GeometryInfo(ga));

		return mergeGeometryInfo(gis);
	}

	public static GeometryInfo mergeGeometryArray(Geometry[] gas)
	{
		ArrayList<GeometryInfo> gis = new ArrayList<GeometryInfo>();
		for (Geometry g : gas)
			if (g != null)
				gis.add(new GeometryInfo((GeometryArray) g));

		return mergeGeometryInfo(gis);
	}

	public static GeometryInfo mergeGeometryInfo(List<GeometryInfo> gis)
	{
		if (gis.size() > 0)
		{
			int vertexCount = 0;
			int indexCount = 0;

			for (GeometryInfo srcGI : gis)
			{
				srcGI.convertToIndexedTriangles();// get rid of quads and madness
				srcGI.indexify(true);//coord indexes only

				vertexCount += srcGI.getCoordinates().length;
				indexCount += srcGI.getCoordinateIndices().length;
			}

			int[] destCoordIndexes = new int[indexCount];
			Point3f[] destCoords = new Point3f[vertexCount];
			Vector3f[] destNorms = new Vector3f[vertexCount];
			Color4f[] destColors = new Color4f[vertexCount];
			TexCoord2f[] destTexCoord = new TexCoord2f[vertexCount];

			int vertexOffset = 0;
			int indexOffset = 0;
			for (GeometryInfo srcGI : gis)
			{
				//coord index, don't forget to move them along to the new batch of coords! (+ vertexOffset)
				int[] srcCoordIndexes = srcGI.getCoordinateIndices();
				for (int i = 0; i < srcCoordIndexes.length; i++)
					destCoordIndexes[indexOffset + i] = srcCoordIndexes[i] + vertexOffset;

				//coords
				Point3f[] srcCoords = srcGI.getCoordinates();
				System.arraycopy(srcCoords, 0, destCoords, vertexOffset, srcCoords.length);
				//norms
				Vector3f[] srcNorms = srcGI.getNormals();
				if (srcNorms != null)
					System.arraycopy(srcNorms, 0, destNorms, vertexOffset, srcCoords.length);
				//colors
				Color4f[] srcColors = convertToColor4f(srcGI.getColors());
				if (srcColors != null)
					System.arraycopy(srcColors, 0, destColors, vertexOffset, srcCoords.length);
				//texcoord
				//TODO: multiple texcoord sets (texcoords3f probably just fail)
				TexCoord2f[] srcTexCoord = null;
				if (srcGI.getTexCoordSetCount() > 0)
					srcTexCoord = (TexCoord2f[]) srcGI.getTextureCoordinates(0);
				if (srcTexCoord != null)
					System.arraycopy(srcTexCoord, 0, destTexCoord, vertexOffset, srcCoords.length);

				//TODO: vertexattributes		

				indexOffset += srcCoordIndexes.length;
				vertexOffset += srcCoords.length;
			}

			GeometryInfo destGI = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
			if (!allNull(destTexCoord))
				destGI.setTextureCoordinateParams(1, 2);
			destGI.setUseCoordIndexOnly(true);
			destGI.setCoordinateIndices(destCoordIndexes);
			destGI.setCoordinates(destCoords);

			// fillInNulls needed because GeometryInfo fillIn uses the first element for typing
			if (!allNull(destNorms))
				destGI.setNormals((Vector3f[]) fillInNulls(destNorms, new Vector3f()));
			if (!allNull(destColors))
				destGI.setColors((Color3f[]) fillInNulls(destColors, new Color4f()));
			if (!allNull(destTexCoord))
				destGI.setTextureCoordinates(0, (TexCoord2f[]) fillInNulls(destTexCoord, new TexCoord2f()));

			return destGI;
		}
		return null;
	}

	private static Object[] fillInNulls(Object[] in, Object nonNull)
	{
		for (int i = 0; i < in.length; i++)
			if (in[i] == null)
				in[i] = nonNull;
		return in;
	}

	private static Color4f[] convertToColor4f(Object[] in)
	{
		if (in != null && in[0] instanceof Color4f)
			return (Color4f[]) in;
		else if (in != null && in[0] instanceof Color3f)
			return convertToColor4f((Color3f[]) in);
		else
			return null;
	}

	private static Color4f[] convertToColor4f(Color3f[] in)
	{
		Color4f[] out = new Color4f[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = new Color4f(in[i].x, in[i].y, in[i].z, 1.0f);
		return out;
	}

	private static boolean allNull(Object[] in)
	{
		for (int i = 0; i < in.length; i++)
			if (in[i] != null)
				return false;
		return true;
	}

}
