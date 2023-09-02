/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 */

package org.jogamp.java3d.utils.image;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import org.jogamp.java3d.CompressedImageComponent2D;
import org.jogamp.java3d.ImageComponent;
import org.jogamp.java3d.ImageComponent2D;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.Texture2D;

import compressedtexture.CompressedBufferedImage;
import compressedtexture.KTXImage;
import compressedtexture.dktxtools.ktx.KTXFormatException;
import etcpack.ETCPack;
import etcpack.ETCPack.FORMAT;
import javaawt.image.BufferedImage;
import javaawt.image.DataBuffer;
import javaawt.image.DataBufferByte;
import javaawt.image.DataBufferInt;
import javaawt.imageio.ImageIO;

/**
 * This class is used for loading a texture from an Image or BufferedImage.
 * The Image I/O API is used to load the images.  (If the JAI IIO Tools
 * package is available, a larger set of formats can be loaded, including
 * TIFF, JPEG2000, and so on.)
 *
 * Methods are provided to retrieve the Texture object and the associated
 * ImageComponent object or a scaled version of the ImageComponent object.
 *
 * Default format is RGBA. Other legal formats are: RGBA, RGBA4, RGB5_A1,
 * RGB, RGB4, RGB5, R3_G3_B2, LUM8_ALPHA8, LUM4_ALPHA4, LUMINANCE and ALPHA
 */
public class TextureLoader extends Object {

	/*
	 * Private declaration for BufferedImage allocation
	 */

	private Texture2D tex = null;
	private BufferedImage bufferedImage = null;
	private ImageComponent2D imageComponent = null;
	private int textureFormat = Texture.RGBA;
	private int imageComponentFormat = ImageComponent.FORMAT_RGBA;
	private boolean byRef = false;
	private boolean yUp = false;

	/**
	 * Constructs a TextureLoader object using the specified BufferedImage,
	 * format and option flags
	 * @param bImage The BufferedImage used for loading the texture
	 *
	 * @exception NullPointerException if bImage is null
	 */
	public TextureLoader(BufferedImage bImage) {
		//TODO: surely y up is much better less copying involved? but images are definitely upside down		
		this(bImage, false);
	}
	
	/**
	 * Constructs a TextureLoader object using the specified BufferedImage,
	 * format and option flags
	 * @param bImage The BufferedImage used for loading the texture
	 * @param yUp Is the image y up (true results in more performance)	 
	 *
	 * @exception NullPointerException if bImage is null
	 */
	public TextureLoader(BufferedImage bImage, boolean yUp) {
		if (bImage == null) {
			throw new NullPointerException();
		}

		bufferedImage = bImage;
		chooseFormat(bufferedImage);
		byRef = true;
		this.yUp = yUp;
	}

	/**
	 * Returns the associated Texture object.
	 *
	 * @return The associated Texture object
	 */
	public Texture getTexture() {
		ImageComponent2D[] scaledImageComponents = null;
		BufferedImage[] scaledBufferedImages = null;
		if (tex == null) {
			
			int width;
			int height;

			width = bufferedImage.getWidth();
			height = bufferedImage.getHeight();

			scaledImageComponents = new ImageComponent2D[1];
			scaledBufferedImages = new BufferedImage[1];
			
			// for phone to compress memory use down to 1/4
			if(CONVERT_TO_ETC2 && (width*height) > (16*16)) {
				tex = compressedToETC2(bufferedImage);
			} 

			// if no compression or compression failed load normally
			if(tex == null) {

				// Create texture from image
				scaledBufferedImages[0] = bufferedImage;
				scaledImageComponents[0] = new ImageComponent2D(imageComponentFormat, scaledBufferedImages[0], byRef, yUp);
	
				tex = new Texture2D(Texture.BASE_LEVEL, textureFormat, width, height);
	
				tex.setImage(0, scaledImageComponents[0]);
			}
		}
		tex.setMinFilter(Texture.NICEST);// will cause mip maps to be used if auto generation enabled on device
		tex.setMagFilter(Texture.NICEST);

		return tex;
	}

	/**
	* Choose the correct ImageComponent and Texture format for the given
	* image
	*/
	private void chooseFormat(BufferedImage image) {
		switch (image.getType()) {
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_INT_ARGB:
			imageComponentFormat = ImageComponent.FORMAT_RGBA;
			textureFormat = Texture.RGBA;
			break;
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_INT_BGR:
		case BufferedImage.TYPE_INT_RGB:
			imageComponentFormat = ImageComponent.FORMAT_RGB;
			textureFormat = Texture.RGB;
			break;
		case BufferedImage.TYPE_CUSTOM:
			throw new UnsupportedOperationException("BufferedImage.TYPE_CUSTOM!");

		default:
			// System.err.println("Unoptimized Image Type "+image.getType());
			imageComponentFormat = ImageComponent.FORMAT_RGBA;
			textureFormat = Texture.RGBA;
			break;
		}
	}
	
	
	public TextureLoader(final URL url,  boolean yUp) {
		this(url);
		this.yUp = yUp;
	}
	
	  public TextureLoader(final URL url, Object observer) {   
		  this(url);
	  }
	 /**
     * Constructs a TextureLoader object using the specified URL
     * and default format RGBA
     * @param url The URL that specifies an Image to load the texture with
     * @param observer The associated image observer
     *
     * @exception ImageException if there is a problem reading the image
     */
    public TextureLoader(final URL url) {                

	    bufferedImage = (BufferedImage)
	        java.security.AccessController.doPrivileged(
	        new java.security.PrivilegedAction() {
	                @Override
	                public Object run() {
	                    try {
	                        return ImageIO.read(url);
	                    } catch (IOException e) {
			    throw new ImageException(e);
	                    }
	                }
	            }
	        );

        if (bufferedImage==null) {
            throw new ImageException("Error loading image: " + url.toString());
        }

        imageComponentFormat = ImageComponent.FORMAT_RGBA;
        textureFormat = Texture.RGBA;
               
        chooseFormat(bufferedImage);
	
	    byRef = true;
	
	    yUp = true;
	    
	    id = url.toString();
		
    }
    
    public String id = "UnknownETC2Image"+System.currentTimeMillis();
    public static boolean CONVERT_TO_ETC2 = true; 
    
    /**
     * Returns the associated ImageComponent2D object
     *
     * @return The associated ImageComponent2D object
     */
    private ImageComponent2D getImage2() {
	if (imageComponent == null)
            imageComponent = new ImageComponent2D(imageComponentFormat,
						  bufferedImage, byRef, yUp);
        return imageComponent;
    }
    
 
    private Texture2D compressedToETC2(BufferedImage image) {
    	
    	
 
    	
    	
		FORMAT format = FORMAT.ETC2PACKAGE_RGBA;
    	
    	switch (image.getType()) {
    		case BufferedImage.TYPE_4BYTE_ABGR:
    		case BufferedImage.TYPE_INT_ARGB:
    			imageComponentFormat = ImageComponent.FORMAT_RGBA;
    			textureFormat = Texture.RGBA;
    			format = FORMAT.ETC2PACKAGE_RGBA;
    			break;
    		case BufferedImage.TYPE_3BYTE_BGR:
    		case BufferedImage.TYPE_INT_BGR:
    		case BufferedImage.TYPE_INT_RGB:
    			imageComponentFormat = ImageComponent.FORMAT_RGB;
    			textureFormat = Texture.RGB;
    			format = FORMAT.ETC2PACKAGE_RGB;
    			break;
    		case BufferedImage.TYPE_CUSTOM:
    			throw new UnsupportedOperationException("BufferedImage.TYPE_CUSTOM! " +id);

    		default:
    			// System.err.println("Unoptimized Image Type "+image.getType());
    			imageComponentFormat = ImageComponent.FORMAT_RGBA;
    			textureFormat = Texture.RGBA;
    			format = FORMAT.ETC2PACKAGE_RGBA;
    			break;
    		}
    	 
		
			Buffer b = null;
			DataBuffer db = image.getRaster().getDataBuffer();

			if (db instanceof DataBufferByte) {
				byte[] srcByteBuffer = ((DataBufferByte)db).getData();
				b = ByteBuffer.wrap(srcByteBuffer);
			} else if (db instanceof DataBufferInt) {
				int[] srcIntBuffer = ((DataBufferInt)db).getData();
				b = IntBuffer.wrap(srcIntBuffer);
			}
         
			byte[] img = null;
			byte[] imgalpha = null;
			
			
		if (b instanceof ByteBuffer) {
			//ok so now find the RGB or RGBA byte buffers
			ByteBuffer bb = (ByteBuffer)b;
			
			if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
				// just put the BGR data straight into the img byte array (RGB)
				img = new byte[bb.capacity()];
				for (int i = 0; i < bb.capacity()/3; i++) {
					img [i * 3 + 2] = bb.get();	//B				
					img [i * 3 + 1] = bb.get(); //G
					img [i * 3 + 0] = bb.get();	//R			
				}
			} else if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {	
				// copy RGB 3 sets out then 1 sets of alpha 
				img = new byte[(bb.capacity() / 4) * 3];
				imgalpha = new byte[(bb.capacity() / 4)];
				for (int i = 0; i < bb.capacity()/4; i++) {
					imgalpha [i] = bb.get(); //A
					img [i * 3 + 2] = bb.get();	//B				
					img [i * 3 + 1] = bb.get(); //G
					img [i * 3 + 0] = bb.get();	//R			
				}
			} else {
				System.err.println("Bad Image Type " + id);
				return null;
			}
		} else if (b instanceof IntBuffer) {
			
			IntBuffer ib = (IntBuffer)b;
			int[] temp = new int[ib.capacity()];
			ib.get(temp, 0, ib.capacity());
			img = new byte[ib.capacity()*3];
			
			if (image.getType() == BufferedImage.TYPE_INT_RGB) {
				// just put the RGB data straight into the img byte array 
				for (int i = 0; i < img.length / 3; i++) {
					img [i * 3 + 0] = (byte)(temp [i * 4 + 0] & 0x00ff0000);
					img [i * 3 + 1] = (byte)(temp [i * 4 + 1] & 0x0000ff00);
					img [i * 3 + 2] = (byte)(temp [i * 4 + 2] & 0x000000ff);
				}				 
			} else if (image.getType() == BufferedImage.TYPE_INT_ARGB) {				
				// copy RGB 3 sets out then 1 sets of alpha 				
				imgalpha = new byte[(ib.capacity())];
				for (int i = 0; i < img.length / 3; i++) {
					img [i * 3 + 0] = (byte)(temp [i * 4 + 0] & 0x00ff0000);
					img [i * 3 + 1] = (byte)(temp [i * 4 + 1] & 0x0000ff00);
					img [i * 3 + 2] = (byte)(temp [i * 4 + 2] & 0x000000ff);
					imgalpha [i] = (byte)(temp [i * 4 + 3] & 0xff000000);
				}
			} else {
				System.err.println("Bad Image Type " + id);
				return null;
			}
		} else {
			System.err.println("Not a ByteBuffer " + b);
			return null;
		}

		KTXImage ktxImage = null;
		ByteBuffer ktxBB = null;
		try {
			ETCPack ep = new ETCPack();
			// notice renovations wats only base level it makes many assumptions
			ktxBB = ep.compressImageToByteBuffer(img, imgalpha, image.getWidth(), image.getHeight(), format, false);

			ktxImage = new KTXImage(ktxBB);
		} catch (KTXFormatException e) {
			System.out.println("KTX image " + id);
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			System.out.println("KTX image" + id);
			e.printStackTrace();
			return null;
		} catch (BufferOverflowException e) {
			System.out.println("KTX image" + id);
			e.printStackTrace();
			return null;
		} catch (IllegalArgumentException e) {
			System.out.println("KTX image" + id);
			e.printStackTrace();
			return null;
		}
		

		int levels = ktxImage.getNumMipMaps();
		// now check how big it should be! sometime these things run out with 0 width or 0 height size images
		int levels2 = Math.min(computeLog(ktxImage.getWidth()), computeLog(ktxImage.getHeight())) + 1;
		// use the lower of the two, to avoid 0 sizes going to the driver
		levels = levels > levels2 ? levels2 : levels;

		// always 1 level
		if (levels == 0) {
			return null;
		}

		int mipMapMode = ktxImage.getNumMipMaps() <= 1 ? Texture.BASE_LEVEL : Texture.MULTI_LEVEL_MIPMAP;

		//note Texture.RGBA is not used on the pipeline for compressed image, the buffered image holds that info
		tex = new Texture2D(mipMapMode, Texture.RGBA, ktxImage.getWidth(), ktxImage.getHeight());

		tex.setName(id);

		tex.setBaseLevel(0);
		tex.setMaximumLevel(levels - 1);

		tex.setBoundaryModeS(Texture.WRAP);
		tex.setBoundaryModeT(Texture.WRAP);

		// better to let machine decide
		tex.setMinFilter(Texture.NICEST);
		tex.setMagFilter(Texture.NICEST);

		/*for (int i = 0; i < levels; i++) {
			BufferedImage cbi = new CompressedBufferedImage.KTX(ktxImage, i, id);
			tex.setImage(i, new CompressedImageComponent2D(ImageComponent.FORMAT_RGBA, cbi));
		}*/
		
		//cacheTexture(id, tex);
		
		bufferedImage = new CompressedBufferedImage.KTX(ktxImage, 0, id);
		imageComponent = new CompressedImageComponent2D(ImageComponent.FORMAT_RGBA, bufferedImage);
		tex.setImage(0, imageComponent);
		
		return tex;
    }
    protected static int computeLog(int value) {
		int i = 0;

		if (value == 0)
			return -1;
		for (;;) {
			if (value == 1)
				return i;
			value >>= 1;
			i++;
		}
	}
}
