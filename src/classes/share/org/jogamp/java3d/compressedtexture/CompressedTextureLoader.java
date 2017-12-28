package org.jogamp.java3d.compressedtexture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jogamp.java3d.CompressedImageComponent2D;
import org.jogamp.java3d.ImageComponent;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.Texture2D;
import org.jogamp.java3d.TextureUnitState;

import compressedtexture.ASTCImage;
import compressedtexture.CompressedBufferedImage;
import compressedtexture.DDSImage;
import compressedtexture.KTXImage;
import javaawt.image.BufferedImage;

/**
 * DDS support onto the jogl pipeline.
 * 
 * The following changes/additions to core java3d(jogl2) classes
 * {@code JoglPipeline} has changes (to simply call glCompressedImage)
 * {@code TextureRetained}; has changes (to correctly deal with max mip map levels) 
 * {@code DDSImageComponent}; a new subclass for ImageComponent
 * {@code DDSImageComponentRetained}; a new subclass for ImageComponent2DRetained
 * 
 * The following DDS loading classes
 * {@code DDSTextureLoader}, {@code DDSBufferedImage}, {@code DDSImage} and {@code DxtFlipper}
 * 
 * Also some helpful utils for debug (not needed to use DXT textures)
 * {@code DSSTextureLoaderTester}, {@code DDSDecompressor}, {@code Color24} and {@code MiniFloat}
 */
public abstract class CompressedTextureLoader
{
	protected static int anisotropicFilterDegree = 0;

	public static void setAnisotropicFilterDegree(int d)
	{
		System.out.println("setAnisotropicFilterDegree=" + d);
		anisotropicFilterDegree = d;
	}

	/**
	 * A hashmap of the loaded {@code Texture2D} instances. Weak so that we can discard them if they are not
	 * in use by at least one Appearence node in the scene graph
	 * Note WeakValueHashMap are self expunging
	 */
	private static WeakValueHashMap<String, Texture2D> loadedTextures = new WeakValueHashMap<String, Texture2D>();
	private static WeakValueHashMap<String, TextureUnitState> loadedTextureUnitStates = new WeakValueHashMap<String, TextureUnitState>();

	//private static RequestStats requestStats = new RequestStats(loadedTextures);

	/**
	* Called to early out in case of cache hit, very likely to return null!
	* cached with no extension, so double up in type will be a problem
	* @param filename
	* @return Possibly a pre-loaded Texture, does not load if not found
	*/
	public static Texture checkCachedTexture(String filename)
	{
		//enable to test is caching is good
		//requestStats.request(filename);

		return loadedTextures.get(filename);
	}

	/**
	 * cached with no extension, so double up in type will be a problem
	 * @param filename
	 * @return
	 */
	public static TextureUnitState checkCachedTextureUnitState(String filename)
	{
		//enable to test is caching is good
		//requestStats.request(filename);

		return loadedTextureUnitStates.get(filename.replace(".dss", "").replace(".ktx", "").replace(".atc", ""));
	}

	public static void cacheTexture(String filename, Texture2D tex)
	{
		loadedTextures.put(filename.replace(".dss", "").replace(".ktx", "").replace(".atc", ""), tex);
	}

	public static void cacheTextureUnitState(String filename, TextureUnitState tus)
	{
		loadedTextureUnitStates.put(filename.replace(".dss", "").replace(".ktx", "").replace(".atc", ""), tus);
	}

	/**
	 * For debug purposes
	 */
	public static void clearCache()
	{
		loadedTextures.clear();
		loadedTextureUnitStates.clear();
	}

	protected static int computeLog(int value)
	{
		int i = 0;

		if (value == 0)
			return -1;
		for (;;)
		{
			if (value == 1)
				return i;
			value >>= 1;
			i++;
		}
	}

	private static int BUFSIZE = 16000;

	public static ByteBuffer toByteBuffer(InputStream in) throws IOException
	{
		if (in instanceof FastByteArrayInputStream)
		{
			//NOTE there is no performance gain from this, but a definite copy time loss
			//ByteBuffer out = ByteBuffer.allocateDirect(((FastByteArrayInputStream) in).getBuf().length);
			//out.order(ByteOrder.nativeOrder());
			//out.put(((FastByteArrayInputStream) in).getBuf());
			//out.rewind();
			//return out;
			return ByteBuffer.wrap(((FastByteArrayInputStream) in).getBuf());
		}
		else
		{
			//note toByteArray trims to size
			ByteArrayOutputStream out = new ByteArrayOutputStream(BUFSIZE);
			byte[] tmp = new byte[BUFSIZE];
			while (true)
			{
				int r = in.read(tmp);
				if (r == -1)
					break;

				out.write(tmp, 0, r);
			}

			return ByteBuffer.wrap(out.toByteArray());
		}
	}

	public static class ASTC extends CompressedTextureLoader
	{
		public static TextureUnitState getTextureUnitState(File file)
		{
			String filename = file.getAbsolutePath();
			try
			{
				return getTextureUnitState(filename, new FileInputStream(file));
			}
			catch (IOException e)
			{
				System.out.println("" + ASTC.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
				return null;
			}
		}

		public static TextureUnitState getTextureUnitState(String filename, InputStream inputStream)
		{
			TextureUnitState ret_val = checkCachedTextureUnitState(filename);

			if (ret_val == null)
			{
				Texture tex = getTexture(filename, inputStream);
				//notice nulls are fine

				TextureUnitState tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(filename);
				cacheTextureUnitState(filename, tus);
				ret_val = tus;
			}
			return ret_val;
		}

		public static TextureUnitState getTextureUnitState(String filename, ByteBuffer inputBuffer)
		{
			TextureUnitState ret_val = checkCachedTextureUnitState(filename);

			if (ret_val == null)
			{
				Texture tex = getTexture(filename, inputBuffer);
				//notice nulls are fine

				TextureUnitState tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(filename);
				cacheTextureUnitState(filename, tus);
				ret_val = tus;
			}
			return ret_val;
		}

		/**
		* Returns the associated Texture object or null if the image failed to load
		* Note it may return a Texture loaded earlier
		* @param file a dds image file
		* @return A {@code Texture} with the associated DDS image or null if the image failed to load
		*/
		public static Texture getTexture(File file)
		{
			String filename = file.getAbsolutePath();
			try
			{
				return getTexture(filename, new FileInputStream(file));
			}
			catch (IOException e)
			{
				System.out.println("" + ASTC.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
				return null;
			}
		}

		/**
		 * Returns the associated Texture object or null if the image failed to load
		 * Note it may return a Texture loaded earlier
		 * @param filename just a useful name for teh inputstreams source
		 * @param inputStream which is fully read into a {@code ByteBuffer} and must contain a dds texture
		 * @return A {@code Texture} with the associated DDS image
		 */
		public static Texture getTexture(String filename, InputStream inputStream)
		{
			// Check the cache for an instance first
			Texture ret_val = checkCachedTexture(filename);

			if (ret_val == null)
			{
				try
				{
					ASTCImage astcImage = new ASTCImage(toByteBuffer(inputStream));

					Texture2D tex = createTexture(filename, astcImage);
					ret_val = tex;
				}
				catch (IOException e)
				{
					System.out.println("" + ASTC.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
					return null;
				}
			}
			return ret_val;
		}

		/**
		 * Note avoid mappedbytebufffers as that will push texture loading (disk activity) onto the j3d thread
		 * which is bad, pull everything into byte arrays on the current thread
		 * @param filename
		 * @param inputBuffer
		 * @return
		 */
		public static Texture getTexture(String filename, ByteBuffer inputBuffer)
		{
			// Check the cache for an instance first
			Texture ret_val = checkCachedTexture(filename);

			if (ret_val == null)
			{
				ASTCImage astcImage = new ASTCImage(inputBuffer);
				Texture2D tex = createTexture(filename, astcImage);
				ret_val = tex;
			}
			return ret_val;
		}

		private static Texture2D createTexture(String filename, ASTCImage astcImage)
		{
			Texture2D tex = new Texture2D(astcImage.getNumMipMaps() <= 1 ? Texture.BASE_LEVEL : Texture.MULTI_LEVEL_MIPMAP, Texture.RGBA,
					astcImage.getWidth(), astcImage.getHeight());

			tex.setName(filename);

			tex.setBaseLevel(0);
			tex.setMaximumLevel(0);

			tex.setBoundaryModeS(Texture.WRAP);
			tex.setBoundaryModeT(Texture.WRAP);

			// better to let machine decide
			tex.setMinFilter(Texture.NICEST);
			tex.setMagFilter(Texture.NICEST);

			//defaults to Texture.ANISOTROPIC_NONE
			if (anisotropicFilterDegree > 0)
			{
				tex.setAnisotropicFilterMode(Texture.ANISOTROPIC_SINGLE_VALUE);
				tex.setAnisotropicFilterDegree(anisotropicFilterDegree);
			}

			BufferedImage image = new CompressedBufferedImage.ASTC(astcImage, 0, filename);
			tex.setImage(0, new CompressedImageComponent2D(ImageComponent.FORMAT_RGBA, image));

			cacheTexture(filename, tex);

			return tex;
		}
	}

	public static class DDS extends CompressedTextureLoader
	{

		public static TextureUnitState getTextureUnitState(File file)
		{
			String filename = file.getAbsolutePath();
			try
			{
				return getTextureUnitState(filename, new FileInputStream(file));
			}
			catch (IOException e)
			{
				System.out.println("" + DDS.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
				return null;
			}
		}

		public static TextureUnitState getTextureUnitState(String filename, InputStream inputStream)
		{
			TextureUnitState ret_val = checkCachedTextureUnitState(filename);

			if (ret_val == null)
			{
				Texture tex = getTexture(filename, inputStream);
				//notice nulls are fine

				TextureUnitState tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(filename);
				cacheTextureUnitState(filename, tus);
				ret_val = tus;
			}
			return ret_val;
		}

		public static TextureUnitState getTextureUnitState(String filename, ByteBuffer inputBuffer)
		{
			TextureUnitState ret_val = checkCachedTextureUnitState(filename);

			if (ret_val == null)
			{
				Texture tex = getTexture(filename, inputBuffer);
				//notice nulls are fine

				TextureUnitState tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(filename);
				cacheTextureUnitState(filename, tus);
				ret_val = tus;
			}
			return ret_val;
		}

		/**
		 * Returns the associated Texture object or null if the image failed to load
		 * Note it may return a Texture loaded earlier
		 * @param file a dds image file
		 * @return A {@code Texture} with the associated DDS image or null if the image failed to load
		 */
		public static Texture getTexture(File file)
		{
			String filename = file.getAbsolutePath();
			try
			{
				return getTexture(filename, new FileInputStream(file));
			}
			catch (IOException e)
			{
				System.out.println("" + DDS.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
				return null;
			}
		}

		/**
		 * Returns the associated Texture object or null if the image failed to load
		 * Note it may return a Texture loaded earlier
		 * @param filename just a useful name for teh inputstreams source
		 * @param inputStream which is fully read into a {@code ByteBuffer} and must contain a dds texture
		 * @return A {@code Texture} with the associated DDS image
		 */
		public static Texture getTexture(String filename, InputStream inputStream)
		{
			// Check the cache for an instance first
			Texture ret_val = checkCachedTexture(filename);

			if (ret_val == null)
			{
				try
				{
					DDSImage ddsImage = DDSImage.read(toByteBuffer(inputStream));
					Texture2D tex = createTexture(filename, ddsImage);
					ret_val = tex;
				}
				catch (IOException e)
				{
					System.out.println("" + DDS.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
					return null;
				}
			}
			return ret_val;
		}

		/**
		 * Note avoid mappedbytebufffers as that will push texture loading (disk activity) onto the j3d thread
		 * which is bad, pull everything into byte arrays on the current thread
		 * @param filename
		 * @param inputBuffer
		 * @return
		 */

		public static Texture getTexture(String filename, ByteBuffer inputBuffer)
		{
			// Check the cache for an instance first
			Texture ret_val = checkCachedTexture(filename);

			if (ret_val == null)
			{
				try
				{
					DDSImage ddsImage = DDSImage.read(inputBuffer);
					Texture2D tex = createTexture(filename, ddsImage);
					ret_val = tex;
				}
				catch (IOException e)
				{
					System.out.println("" + DDS.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
					return null;
				}
			}
			return ret_val;
		}

		private static Texture2D createTexture(String filename, DDSImage ddsImage)
		{

			// return null for unsupproted types
			if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT2 //
					|| ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT4 //
					|| ddsImage.getPixelFormat() == DDSImage.D3DFMT_UNKNOWN)
			{
				System.out.println("Unsupported DDS format " + ddsImage.getPixelFormat() + " for file " + filename);
				return null;
			}

			int levels = ddsImage.getNumMipMaps();
			// now check how big it should be! sometime these things run out with 0 width or 0 height size images
			int levels2 = Math.min(computeLog(ddsImage.getWidth()), computeLog(ddsImage.getHeight())) + 1;
			// use the lower of the two, to avoid 0 sizes going to the driver
			levels = levels > levels2 ? levels2 : levels;

			// always 1 level
			levels = levels == 0 ? 1 : levels;

			int mipMapMode = ddsImage.getNumMipMaps() <= 1 ? Texture.BASE_LEVEL : Texture.MULTI_LEVEL_MIPMAP;
			//note Texture.RGBA is not used on the pipeline for compressed image, the buffered image holds that info
			Texture2D tex = new Texture2D(mipMapMode, Texture.RGBA, ddsImage.getWidth(), ddsImage.getHeight());

			tex.setName(filename);

			tex.setBaseLevel(0);
			tex.setMaximumLevel(levels - 1);

			tex.setBoundaryModeS(Texture.WRAP);
			tex.setBoundaryModeT(Texture.WRAP);

			// better to let machine decide
			tex.setMinFilter(Texture.NICEST);
			tex.setMagFilter(Texture.NICEST);

			//defaults to Texture.ANISOTROPIC_NONE
			if (anisotropicFilterDegree > 0)
			{
				tex.setAnisotropicFilterMode(Texture.ANISOTROPIC_SINGLE_VALUE);
				tex.setAnisotropicFilterDegree(anisotropicFilterDegree);
			}

			for (int i = 0; i < levels; i++)
			{
				BufferedImage image = new CompressedBufferedImage.DDS(ddsImage, i, filename);
				tex.setImage(i, new CompressedImageComponent2D(ImageComponent.FORMAT_RGBA, image));
			}

			cacheTexture(filename, tex);

			ddsImage.close();

			return tex;

		}

	}

	public static class KTX extends CompressedTextureLoader
	{
		public static TextureUnitState getTextureUnitState(File file)
		{
			String filename = file.getAbsolutePath();
			try
			{
				return getTextureUnitState(filename, new FileInputStream(file));
			}
			catch (IOException e)
			{
				System.out.println("" + KTX.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
				return null;
			}
		}

		public static TextureUnitState getTextureUnitState(String filename, InputStream inputStream)
		{
			TextureUnitState ret_val = checkCachedTextureUnitState(filename);

			if (ret_val == null)
			{
				Texture tex = getTexture(filename, inputStream);
				//notice nulls are fine

				TextureUnitState tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(filename);
				cacheTextureUnitState(filename, tus);
				ret_val = tus;
			}
			return ret_val;
		}

		public static TextureUnitState getTextureUnitState(String filename, ByteBuffer inputBuffer)
		{
			TextureUnitState ret_val = checkCachedTextureUnitState(filename);

			if (ret_val == null)
			{
				Texture tex = getTexture(filename, inputBuffer);
				//notice nulls are fine

				TextureUnitState tus = new TextureUnitState();
				tus.setTexture(tex);
				tus.setName(filename);
				cacheTextureUnitState(filename, tus);
				ret_val = tus;
			}
			return ret_val;
		}

		/**
		 * Returns the associated Texture object or null if the image failed to load
		 * Note it may return a Texture loaded earlier
		 * @param file a dds image file
		 * @return A {@code Texture} with the associated DDS image or null if the image failed to load
		 */
		public static Texture getTexture(File file)
		{
			String filename = file.getAbsolutePath();
			try
			{
				return getTexture(filename, new FileInputStream(file));
			}
			catch (IOException e)
			{
				System.out.println("" + KTX.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
				return null;
			}
		}

		/**
		 * Returns the associated Texture object or null if the image failed to load
		 * Note it may return a Texture loaded earlier
		 * @param filename just a useful name for teh inputstreams source
		 * @param inputStream which is fully read into a {@code ByteBuffer} and must contain a dds texture
		 * @return A {@code Texture} with the associated DDS image
		 */
		public static Texture getTexture(String filename, InputStream inputStream)
		{
			// Check the cache for an instance first
			Texture ret_val = checkCachedTexture(filename);

			if (ret_val == null)
			{
				try
				{
					KTXImage ktxImage = new KTXImage(toByteBuffer(inputStream));
					ret_val = createTexture(filename, ktxImage);
				}
				catch (IOException e)
				{
					System.out.println("" + KTX.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
					return null;
				}				
			}
			return ret_val;
		}

		/**
		 * Note avoid mappedbytebufffers as that will push texture loading (disk activity) onto the j3d thread
		 * which is bad, pull everything into byte arrays on the current thread
		 * @param filename
		 * @param inputBuffer
		 * @return
		 */

		public static Texture getTexture(String filename, ByteBuffer inputBuffer)
		{
			// Check the cache for an instance first
			Texture ret_val = checkCachedTexture(filename);

			if (ret_val == null)
			{
				try
				{
					KTXImage ktxImage = new KTXImage(inputBuffer);
					ret_val = createTexture(filename, ktxImage);
				}
				catch (IOException e)
				{
					System.out.println("" + KTX.class + " had a  IO problem with " + filename + " : " + e + " " + e.getStackTrace()[0]);
					return null;
				}				
			}
			return ret_val;

		}

		private static Texture2D createTexture(String filename, KTXImage ktxImage)
		{

			// unsupported type will have failed already		

			int levels = ktxImage.getNumMipMaps();
			// now check how big it should be! sometime these things run out with 0 width or 0 height size images
			int levels2 = Math.min(computeLog(ktxImage.getWidth()), computeLog(ktxImage.getHeight())) + 1;
			// use the lower of the two, to avoid 0 sizes going to the driver
			levels = levels > levels2 ? levels2 : levels;

			// always 1 level
			levels = levels == 0 ? 1 : levels;

			int mipMapMode = ktxImage.getNumMipMaps() <= 1 ? Texture.BASE_LEVEL : Texture.MULTI_LEVEL_MIPMAP;

			//note Texture.RGBA is not used on the pipeline for compressed image, the buffered image holds that info
			Texture2D tex = new Texture2D(mipMapMode, Texture.RGBA, ktxImage.getWidth(), ktxImage.getHeight());

			tex.setName(filename);

			tex.setBaseLevel(0);
			tex.setMaximumLevel(levels - 1);

			tex.setBoundaryModeS(Texture.WRAP);
			tex.setBoundaryModeT(Texture.WRAP);

			// better to let machine decide
			tex.setMinFilter(Texture.NICEST);
			tex.setMagFilter(Texture.NICEST);

			//defaults to Texture.ANISOTROPIC_NONE
			if (anisotropicFilterDegree > 0)
			{
				tex.setAnisotropicFilterMode(Texture.ANISOTROPIC_SINGLE_VALUE);
				tex.setAnisotropicFilterDegree(anisotropicFilterDegree);
			}

			for (int i = 0; i < levels; i++)
			{
				BufferedImage image = new CompressedBufferedImage.KTX(ktxImage, i, filename);
				tex.setImage(i, new CompressedImageComponent2D(ImageComponent.FORMAT_RGBA, image));
			}

			cacheTexture(filename, tex);

			return tex;

		}

	}
}
