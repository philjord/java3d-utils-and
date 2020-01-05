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


import java.io.IOException;
import java.net.URL;

import javaawt.imageio.ImageIO;

import org.jogamp.java3d.ImageComponent;
import org.jogamp.java3d.ImageComponent2D;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.Texture2D;

import javaawt.image.BufferedImage;

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
		//TODO: surely y up is much better less copying involved? but images  are definately upside down		
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

			// Create texture from image
			scaledBufferedImages[0] = bufferedImage;
			scaledImageComponents[0] = new ImageComponent2D(imageComponentFormat, scaledBufferedImages[0], byRef, yUp);

			tex = new Texture2D(Texture.BASE_LEVEL, textureFormat, width, height);

			tex.setImage(0, scaledImageComponents[0]);
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
		
    }
    
    /**
     * Returns the associated ImageComponent2D object
     *
     * @return The associated ImageComponent2D object
     */
    public ImageComponent2D getImage() {
	if (imageComponent == null)
            imageComponent = new ImageComponent2D(imageComponentFormat,
						  bufferedImage, byRef, yUp);
        return imageComponent;
    }
}
