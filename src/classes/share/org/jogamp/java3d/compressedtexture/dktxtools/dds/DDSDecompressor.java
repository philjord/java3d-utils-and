package org.jogamp.java3d.compressedtexture.dktxtools.dds;

import javaawt.Color;
import javaawt.image.BufferedImage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.jogamp.java3d.NioImageBuffer;
import org.jogamp.java3d.NioImageBuffer.ImageType;

import compressedtexture.DDSImage;

/**
 * Converts DDS files and streams into {@code BufferedImage} {@link http://en.wikipedia.org/wiki/S3_Texture_Compression}
 * Or NioImageBuffer
 */
public class DDSDecompressor {
	public static final int		BLOCK_SIZE	= 4;

	public DDSImage				ddsImage;

	private int					mipNumber;

	private DDSImage.ImageInfo	imageInfo;

	private ByteBuffer			buffer;

	private boolean				ignoreAlpha	= false;

	private int					width;

	private int					height;

	private String				imageName	= "";
	
	private boolean 			opaque = true;

	/**
	 * 
	 * @param ddsImage
	 * @param mipNumber
	 * @param imageName
	 */
	public DDSDecompressor(DDSImage ddsImage, int mipNumber, String imageName) {
		this.imageName = imageName;
		this.ddsImage = ddsImage;
		this.mipNumber = mipNumber;
		this.imageInfo = ddsImage.getAllMipMaps() [mipNumber];
		this.width = imageInfo.getWidth();
		this.height = imageInfo.getHeight();
		this.buffer = imageInfo.getData();
		this.opaque = true;
	}

	public String getImageName() {
		return imageName;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getType() {
		return BufferedImage.TYPE_INT_ARGB;
	}
	
	/**
	 * Can only be queried after decompression
	 * @return
	 */
	public boolean decompressedIsOpaque() {
		return opaque;
	}

	/**
	 * 
	 * @return a new non-cached {@code BufferedImage}
	 */
	public BufferedImage convertImage() {

		//can't use width or height as it's been corrected to 1 already
		if (imageInfo.getWidth() < 1 || imageInfo.getHeight() < 1) {
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}

		//prep the buffer
		buffer.rewind();
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT1) {
			//System.out.println("DXT1");
			if (!ddsImage.isPixelFormatFlagSet(DDSImage.DDPF_ALPHAPIXELS)) {
				//TODO: how do I discover no alpha flag? 
				//C:\game media\Black Prophecy\Textures\avatar_ai_pilot_f3_05.dds wants no alpha flag
				//possibly no mips maps indicates this?
				return decodeDxt1Buffer();
			} else {
				System.out.println("Alpha present in DXT1!; mip num = " + mipNumber);
				//return decompressRGBA_S3TC_DXT1_EXT(ddsImage.getMipMap(mipNumber));
			}
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT3) {
			return decodeDxt3Buffer();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT5) {
			return decompressRGBA_S3TC_DXT5_EXT();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_ATI2 || ddsImage.getPixelFormat() == DDSImage.D3DFMT_BC5U) {
			// NOT correct but it gives you the idea a bit
			return decompressRGBA_S3TC_DXT5_EXT();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_R8G8B8) {
			return decodeR8G8B8();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_A8B8G8R8) {
			return decodeA8B8G8R8();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_X8R8G8B8) {
			return decodeA8R8G8B8();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_A16B16G16R16F) {
			return decodeA16R16G16B16();
		}
		System.err.println("BAD DXT format!! " + ddsImage.getPixelFormat());
		return null;
	}

	private BufferedImage decodeR8G8B8() {
		//NOTE disagrees with fixed getType below
		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] pixels = new int[width * height];
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				pixels [(y * width) + x] = ((buffer.get() & 0xff) << 24 | (buffer.get() & 0xff) << 16
											| (buffer.get() & 0xff) << 8);
			}
		}
		delegate.setRGB(0, 0, width, height, pixels, 0, width);
		return delegate;
	}
	private BufferedImage decodeA8B8G8R8() {
		//NOTE disagrees with fixed getType below
		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] pixels = new int[width * height];
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				pixels [(y * width) + x] = ((buffer.get() & 0xff) << 8 | (buffer.get() & 0xff) << 16
											| (buffer.get() & 0xff) << 24);
			}
		}
		delegate.setRGB(0, 0, width, height, pixels, 0, width);
		return delegate;
	}
	private BufferedImage decodeA8R8G8B8() {
		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = new int[width * height];
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				pixels [(y * width) + x] = buffer.getInt();
			}
		}
		delegate.setRGB(0, 0, width, height, pixels, 0, width);
		return delegate;

	}

	private BufferedImage decodeA16R16G16B16() {
		//TODO: this is a dodgy layout here tested good on black prophecy images only		
		//2&4 good for smalls
		//2&64 for bigs

		int numBlocksWide = width / 2;
		int numBlocksHigh = 1;//height / 64;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, 2);
		int blockHeight = height;//Math.min(height, 64);

		int[] pixels = new int[blockWidth * blockHeight];

		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int col = 0; col < numBlocksWide; col++) {
			for (int row = 0; row < numBlocksHigh; row++) {
				float r = MiniFloat.toFloat((buffer.getShort()));
				float g = MiniFloat.toFloat((buffer.getShort()));
				float b = MiniFloat.toFloat((buffer.getShort()));
				float a = MiniFloat.toFloat((buffer.getShort()));
				Color c = new Color(r, g, b, a);
				for (int br = 0; br < blockHeight; br++) {
					for (int bc = 0; bc < blockWidth; bc++) {
						// for yUp must flip it so NOT pixels[br  * blockWidth + bc] = c.getRGB();
						pixels [((blockHeight - 1) - br) * blockWidth + bc] = c.getRGB();
					}
				}
				delegate.setRGB(col * blockWidth, (height - blockHeight) - (row * blockHeight), blockWidth, blockHeight,
						pixels, 0, blockWidth);
			}
		}
		return delegate;
	}

	private BufferedImage decodeDxt1Buffer() {
		int numBlocksWide = width / BLOCK_SIZE;
		int numBlocksHigh = height / BLOCK_SIZE;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, BLOCK_SIZE);
		int blockHeight = Math.min(height, BLOCK_SIZE);

		int[] pixels = new int[blockWidth * blockHeight];

		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		//One copy of table to minimises new calls
		Color24[] table = new Color24[4];
		table [0] = new Color24();
		table [1] = new Color24();
		table [2] = new Color24();
		table [3] = new Color24();

		for (int row = 0; row < numBlocksHigh; row++) {
			for (int col = 0; col < numBlocksWide; col++) {
				short c0 = buffer.getShort();
				short c1 = buffer.getShort();
				int colorIndexMask = buffer.getInt();

				//http://en.wikipedia.org/wiki/S3_Texture_Compression
				if (ignoreAlpha || !Color24.hasAlphaBit(c0, c1)) {
					Color24[] lookupTable = Color24.expandLookupTable(table, c0, c1);
					for (int br = 0; br < blockHeight; br++) {
						for (int bc = 0; bc < blockWidth; bc++) {
							int k = (br * blockWidth) + bc;
							int colorIndex = (colorIndexMask >>> k * 2) & 0x03;
							// for yUp must flip it so NOT pixels[br  * blockWidth + bc] = (0xFF << 24) | lookupTable[colorIndex].pix888;
							pixels [((blockHeight - 1) - br) * blockWidth + bc] = (0xFF << 24)
																					| lookupTable [colorIndex].pix888;
						}
					}
				} else {
					Color24[] lookupTable = Color24.expandLookupTableAlphable(table, c0, c1);
					for (int br = 0; br < blockHeight; br++) {
						for (int bc = 0; bc < blockWidth; bc++) {
							int k = (br * blockWidth) + bc;
							int colorIndex = (colorIndexMask >>> k * 2) & 0x03;
							int alpha = (colorIndex == 3) ? 0x00 : 0xFF;
														
							if( alpha != 255)
								opaque = false;
							
							// for yUp must flip it , so NOT pixels[br  * blockWidth + bc] = (alpha << 24) | lookupTable[colorIndex].pix888;
							pixels [((blockHeight - 1) - br) * blockWidth + bc] = (alpha << 24)
																					| lookupTable [colorIndex].pix888;
						}
					}
				}

				delegate.setRGB(col * blockWidth, (height - blockHeight) - (row * blockHeight), blockWidth, blockHeight,
						pixels, 0, blockWidth);
			}
		}
		return delegate;
	}

	private BufferedImage decodeDxt3Buffer() {
		int numBlocksWide = width / BLOCK_SIZE;
		int numBlocksHigh = height / BLOCK_SIZE;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, BLOCK_SIZE);
		int blockHeight = Math.min(height, BLOCK_SIZE);

		int[] pixels = new int[blockWidth * blockHeight];

		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		//One copy of table to minimises new calls
		Color24[] table = new Color24[4];
		table [0] = new Color24();
		table [1] = new Color24();
		table [2] = new Color24();
		table [3] = new Color24();

		for (int row = 0; row < numBlocksHigh; row++) {
			for (int col = 0; col < numBlocksWide; col++) {
				long alphaData = buffer.getLong();
				short minColor = buffer.getShort();
				short maxColor = buffer.getShort();
				int colorIndexMask = buffer.getInt();
				
				//-1 is all bits on which is 255 for all pixels or opaque
				if( alphaData != -1)
					opaque = false;

				Color24[] lookupTable = Color24.expandLookupTable(table, minColor, maxColor);

				for (int br = 0; br < blockHeight; br++) {
					for (int bc = 0; bc < blockWidth; bc++) {
						int k = (br * blockWidth) + bc;
						int alpha = (int)(alphaData >>> (k * 4)) & 0xF; // Alphas are just 4 bits per pixel
						// the original code is like *16 =>   alpha <<= 4;
						// but 0-15 needs *17 for 15==255
						alpha *= 17;

						int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

						Color24 color = lookupTable [colorIndex];
						int pixel8888 = (alpha << 24) | color.pix888;

						// for yUp must flip it , so NOT pixels[br  * blockWidth + bc] = pixel8888;
						pixels [((blockHeight - 1) - br) * blockWidth + bc] = pixel8888;
					}
				}
				//notice vertical flipping there
				delegate.setRGB(col * blockWidth, (height - blockHeight) - (row * blockHeight), blockWidth, blockHeight,
						pixels, 0, blockWidth);
			}

		}
		return delegate;
	}

	private BufferedImage decompressRGBA_S3TC_DXT5_EXT() {
		int numBlocksWide = width / BLOCK_SIZE;
		int numBlocksHigh = height / BLOCK_SIZE;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, BLOCK_SIZE);
		int blockHeight = Math.min(height, BLOCK_SIZE);

		int[] pixels = new int[blockWidth * blockHeight];

		BufferedImage delegate = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		//One copy of table to minimises new calls
		Color24[] table = new Color24[4];
		table [0] = new Color24();
		table [1] = new Color24();
		table [2] = new Color24();
		table [3] = new Color24();

		for (int row = 0; row < numBlocksHigh; row++) {
			for (int col = 0; col < numBlocksWide; col++) {
				int alpha0 = buffer.get() & 0xff; //unsigned byte
				int alpha1 = buffer.get() & 0xff; //unsigned byte

				// next 6 bytes are a look up list (note long casts, important!)
				long alphaBits = (buffer.get() & 0xffL) << 0L //
									| (buffer.get() & 0xffL) << 8L //
									| (buffer.get() & 0xffL) << 16L//
									| (buffer.get() & 0xffL) << 24L //
									| (buffer.get() & 0xffL) << 32L //
									| (buffer.get() & 0xffL) << 40L;//

				short minColor = buffer.getShort();
				short maxColor = buffer.getShort();
				int colorIndexMask = buffer.getInt();

				Color24[] lookupTable = Color24.expandLookupTable(table, minColor, maxColor);

				for (int br = 0; br < blockHeight; br++) {
					for (int bc = 0; bc < blockWidth; bc++) {
						int k = (br * blockWidth) + bc;

						int alphaCode = (int)(alphaBits >> (3 * k) & 0x07); // bottom 3 bits

						int alpha = 0;

						if (alphaCode == 0) {
							alpha = alpha0;
						} else if (alphaCode == 1) {
							alpha = alpha1;
						} else if (alpha0 > alpha1) {
							alpha = ((8 - alphaCode) * alpha0 + (alphaCode - 1) * alpha1) / 7;
						} else {
							if (alphaCode == 6)
								alpha = 0;
							else if (alphaCode == 7)
								alpha = 255;
							else
								alpha = ((6 - alphaCode) * alpha0 + (alphaCode - 1) * alpha1) / 5;
						}
						
						//255 pixel is opaque
						if( alpha != 255)
							opaque = false;

						int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

						Color24 color = lookupTable [colorIndex];
						int pixel8888 = (alpha << 24) | color.pix888;
						// for yUp must flip it , so NOT pixels[br  * blockWidth + bc] = pixel8888;
						pixels [((blockHeight - 1) - br) * blockWidth + bc] = pixel8888;
					}
				}
				//notice vertical flipping there
				delegate.setRGB(col * blockWidth, (height - blockHeight) - (row * blockHeight), blockWidth, blockHeight,
						pixels, 0, blockWidth);
			}

		}

		return delegate;
	}

	
	//COPY OF ABOVE BUT INTO A BYTEBUFFER INSTEAD
	
	/**
	 * 
	 * @return a new non-cached {@code BufferedImage}
	 */
	public NioImageBuffer convertImageNio() {

		//can't use width or height as it's been corrected to 1 already
		if (imageInfo.getWidth() < 1 || imageInfo.getHeight() < 1) {
			return new NioImageBuffer(1, 1, ImageType.TYPE_INT_ARGB);
		}

		//prep the buffer
		buffer.rewind();
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		int fmt = ddsImage.getPixelFormat();
		if (fmt == DDSImage.D3DFMT_DXT1) {
			return decodeDxt1BufferNio();
		} else if (fmt == DDSImage.D3DFMT_DXT3) {
			return decodeDxt3BufferNio();
		} else if (fmt == DDSImage.D3DFMT_DXT5) {
			return decompressRGBA_S3TC_DXT5_EXTNio();
		} else if (fmt == DDSImage.D3DFMT_ATI2 || ddsImage.getPixelFormat() == DDSImage.D3DFMT_BC5U) {
			// NOT correct but it gives you the idea a bit
			return decompressRGBA_S3TC_DXT5_EXTNio();
		} else if (fmt == DDSImage.D3DFMT_R8G8B8) {
			return decodeR8G8B8Nio();
		} else if (fmt == DDSImage.D3DFMT_L8) {
			return decodeL8Nio();
		}  else if (fmt == DDSImage.D3DFMT_A8L8) {
			return decodeA8L8Nio();
		}  else if (fmt == DDSImage.D3DFMT_A4R4G4B4) {
			return decodeA4R4G4B4Nio();
		} else if (fmt == DDSImage.D3DFMT_A8R8G8B8) {
			return decodeA8R8G8B8Nio();
		} else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_A8B8G8R8) {
			return decodeA8B8G8R8Nio();
		} else if (fmt == DDSImage.D3DFMT_X8R8G8B8) {
			return decodeA8R8G8B8Nio();
		} else if (fmt == DDSImage.D3DFMT_A16B16G16R16F) {
			return decodeA16R16G16B16Nio();
		} else if (fmt == DDSImage.D3DFMT_R5G6B5) {
			return decodeR5G6B5Nio();
		}
		
		//Possibly we have got an L8 format, 
		//D:\game_media\Oblivion\Oblivion - Textures - Compressed\textures\architecture\anvil\arcanesymbol01_g.dds
		
		//L8 seems to have a single channel that's repeat across RGB to show are a grey scale results
		//_g.dds must be glow
		
		
		//https://www.gamedev.net/forums/topic/575505-d3dfmt_l8-to-argb-color/575505/
		//https://learn.microsoft.com/en-us/windows/win32/direct3d9/d3dformat
		System.err.println("BAD DXT format!! " + ddsImage.getPixelFormat());
		ddsImage.debugPrint();
		
		return null;
	}
	
	
	private NioImageBuffer decodeR5G6B5Nio() {
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 3).order(ByteOrder.nativeOrder());
		 
 
		Color24 c = new Color24();
		
		// reverse to flip Y 
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {				
				//header.pfRBitMask == 0xF800 && header.pfGBitMask == 0x07E0&& header.pfBBitMask == 0x001F)
				Color24.fromShort565(c, buffer.getShort());					
				directBuffer.put((byte)c.r);
				directBuffer.put((byte)c.g);
				directBuffer.put((byte)c.b);
			}
		}
		
		return new NioImageBuffer(width, height, ImageType.TYPE_3BYTE_RGB, directBuffer);
	}
	
	private NioImageBuffer decodeR8G8B8Nio() {
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 3).order(ByteOrder.nativeOrder());
		 
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				directBuffer.put(buffer.get());
				directBuffer.put(buffer.get());
				directBuffer.put(buffer.get());
			}
		}
		//NOTE disagrees with fixed getType below
		return new NioImageBuffer(width, height, ImageType.TYPE_3BYTE_RGB, directBuffer);
	}
	
	private NioImageBuffer decodeL8Nio() {
		ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 1).order(ByteOrder.nativeOrder());
 
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				pixels.put(buffer.get());
			}
		}
		return new NioImageBuffer(width, height, ImageType.TYPE_BYTE_GRAY, pixels);
	}
		
	private NioImageBuffer decodeA8L8Nio() {
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		IntBuffer pixels = directBuffer.asIntBuffer();
 
		// reverse to flip Y - noting I'm undoing the reverse just below?? odd?
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				byte A = buffer.get();
				byte L = buffer.get();
				
				pixels.put((y * width) + x, (L & 0xff) << 24
						| (L & 0xff) << 16 | (L & 0xff) << 8 | (A & 0xff) << 0);
			}
		}
		
		//wastefully repeat L8 into the RGB slots
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, pixels);
	}
	
	
	private NioImageBuffer decodeA4R4G4B4Nio() {
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		IntBuffer pixels = directBuffer.asIntBuffer();
 
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				
				byte bg = buffer.get();
				int b = ((bg & 0xF0) >> 4) * 17;
				int g = (bg & 0x0F) * 17;
				byte ra = buffer.get();
				int r = ((ra & 0xF0) >> 4) * 17;
				int a  = (ra & 0x0F) * 17;
				
				pixels.put((r & 0xff) << 24
						| (g & 0xff) << 16 | (b & 0xff) << 8 | (a & 0xff) << 0);
			}
		}
		
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}

	private NioImageBuffer decodeA8R8G8B8Nio() {

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				byte a = buffer.get();	// swapped to end			
				directBuffer.put(buffer.get());
				directBuffer.put(buffer.get());
				directBuffer.put(buffer.get());
				directBuffer.put(a);
			}
		}
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}
	
	private NioImageBuffer decodeA8B8G8R8Nio() {

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		// reverse to flip Y
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				byte a = buffer.get();	// swapped to end			
				byte b = buffer.get();
				byte g = buffer.get();
				byte r = buffer.get();				
				directBuffer.put(r);
				directBuffer.put(g);
				directBuffer.put(b);				
				directBuffer.put(a);
			}
		}
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}
	 

	private NioImageBuffer decodeA16R16G16B16Nio() {
		//TODO: this is a dodgy layout here tested good on black prophecy images only		
		//2&4 good for smalls
		//2&64 for bigs

		int numBlocksWide = width / 2;
		int numBlocksHigh = 1;//height / 64;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, 2);
		int blockHeight = height;//Math.min(height, 64);

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4);
		IntBuffer pixelBuffer = directBuffer.asIntBuffer();
		
		for (int col = 0; col < numBlocksWide; col++) {
			for (int row = 0; row < numBlocksHigh; row++) {
				float r = MiniFloat.toFloat((buffer.getShort()));
				float g = MiniFloat.toFloat((buffer.getShort()));
				float b = MiniFloat.toFloat((buffer.getShort()));
				float a = MiniFloat.toFloat((buffer.getShort()));
				Color c = new Color(r, g, b, a);
				for (int br = 0; br < blockHeight; br++) {
					for (int bc = 0; bc < blockWidth; bc++) {
						
						pixelBuffer.put((col * blockWidth) *(numBlocksHigh*blockHeight)  // previous columns and row
								+  (col * (numBlocksHigh*blockHeight))
								+  (br * blockHeight) // current blocks rows 
								+ br //column thereof
								,c.getRGB());
					}
				}
			}
		}		
		
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}

	private NioImageBuffer decodeDxt1BufferNio() {
		int numBlocksWide = width / BLOCK_SIZE;
		int numBlocksHigh = height / BLOCK_SIZE;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, BLOCK_SIZE);
		int blockHeight = Math.min(height, BLOCK_SIZE);

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		IntBuffer pixels = directBuffer.asIntBuffer();

		

		//One copy of table to minimises new calls
		Color24[] table = new Color24[4];
		table [0] = new Color24();
		table [1] = new Color24();
		table [2] = new Color24();
		table [3] = new Color24();

		for (int row = 0; row < numBlocksHigh; row++) {
			for (int col = 0; col < numBlocksWide; col++) {
				short c0 = buffer.getShort();
				short c1 = buffer.getShort();
				int colorIndexMask = buffer.getInt();

				//http://en.wikipedia.org/wiki/S3_Texture_Compression
				if (ignoreAlpha || !Color24.hasAlphaBit(c0, c1)) {
					Color24[] lookupTable = Color24.expandLookupTable(table, c0, c1);
					for (int br = 0; br < blockHeight; br++) {
						for (int bc = 0; bc < blockWidth; bc++) {
							int k = (br * blockWidth) + bc;
							int colorIndex = (colorIndexMask >>> k * 2) & 0x03;
							
							Color24 color = lookupTable [colorIndex];	
							int pixel8888 = (0xFF << 24) | (color.b << 16) | (color.g << 8) | (color.r << 0);
							
							pixels.put(((row * blockHeight) * (numBlocksWide*blockWidth))  // previous columns and row
									+ (br * (numBlocksWide*blockWidth))
									+ (col * blockWidth)
									+ bc
									,pixel8888);
						}
					}
				} else {
					Color24[] lookupTable = Color24.expandLookupTableAlphable(table, c0, c1);
					for (int br = 0; br < blockHeight; br++) {
						for (int bc = 0; bc < blockWidth; bc++) {
							int k = (br * blockWidth) + bc;
							int colorIndex = (colorIndexMask >>> k * 2) & 0x03;
							int alpha = (colorIndex == 3) ? 0x00 : 0xFF;
							
							//255 pixel is opaque
							if( alpha != 255)
								opaque = false;

							Color24 color = lookupTable [colorIndex];	
							int pixel8888 = (alpha << 24) | (color.b << 16) | (color.g << 8) | (color.r << 0);
							
							pixels.put(((row * blockHeight) * (numBlocksWide*blockWidth))  // previous columns and row
									+ (br * (numBlocksWide*blockWidth))
									+ (col * blockWidth)
									+ bc
									,pixel8888);
						}
					}
				}
			}
		}
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}

	private NioImageBuffer decodeDxt3BufferNio() {
		int numBlocksWide = width / BLOCK_SIZE;
		int numBlocksHigh = height / BLOCK_SIZE;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, BLOCK_SIZE);
		int blockHeight = Math.min(height, BLOCK_SIZE);

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		IntBuffer pixels = directBuffer.asIntBuffer();

		//One copy of table to minimises new calls
		Color24[] table = new Color24[4];
		table [0] = new Color24();
		table [1] = new Color24();
		table [2] = new Color24();
		table [3] = new Color24();
		
		for (int row = 0; row < numBlocksHigh; row++) {
			for (int col = 0; col < numBlocksWide; col++) {
				long alphaData = buffer.getLong();
				short minColor = buffer.getShort();
				short maxColor = buffer.getShort();
				int colorIndexMask = buffer.getInt();
				
				//-1 is all bits on which is 255 for all pixels or opaque
				if( alphaData != -1)
					opaque = false;

				Color24[] lookupTable = Color24.expandLookupTable(table, minColor, maxColor);

				for (int br = 0; br < blockHeight; br++) {
					for (int bc = 0; bc < blockWidth; bc++) {
						int k = (br * blockWidth) + bc;
						int alpha = (int)(alphaData >>> (k * 4)) & 0xF; // Alphas are just 4 bits per pixel
						// the original code is like *16 =>   alpha <<= 4;
						// but 0-15 needs *17 for 15==255
						// Here, we should really multiply by 17 instead of 16. This can
						// be done by just copying the four lower bits to the upper ones
						// while keeping the lower bits.
						alpha = (byte)(alpha | (alpha <<4));

						int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

						Color24 color = lookupTable [colorIndex];						
						
						//0xFF0000FF = red
						//0xFF00FF00 = green						
						//0xFFFF0000 = blue
						//ABGR! so can't use the color24 ARGB system
						int pixel8888 = (alpha << 24) | (color.b << 16) | (color.g << 8) | (color.r << 0);

						pixels.put(((row * blockHeight) * (numBlocksWide*blockWidth))  // previous columns and row
								+ (br * (numBlocksWide*blockWidth))
								+ (col * blockWidth)
								+ bc
								,pixel8888);
					}
				}
				//notice notice no flipping for theNioImageBuffer version!
			}

		}
		
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}

	private NioImageBuffer decompressRGBA_S3TC_DXT5_EXTNio() {
		int numBlocksWide = width / BLOCK_SIZE;
		int numBlocksHigh = height / BLOCK_SIZE;

		// always at least 1x1 tile
		numBlocksWide = numBlocksWide < 1 ? 1 : numBlocksWide;
		numBlocksHigh = numBlocksHigh < 1 ? 1 : numBlocksHigh;

		int blockWidth = Math.min(width, BLOCK_SIZE);
		int blockHeight = Math.min(height, BLOCK_SIZE);

		ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		IntBuffer pixels = directBuffer.asIntBuffer();		

		//One copy of table to minimises new calls
		Color24[] table = new Color24[4];
		table [0] = new Color24();
		table [1] = new Color24();
		table [2] = new Color24();
		table [3] = new Color24();
		
		for (int row = 0; row < numBlocksHigh; row++) {
			for (int col = 0; col < numBlocksWide; col++) {
				int alpha0 = buffer.get() & 0xff; //unsigned byte
				int alpha1 = buffer.get() & 0xff; //unsigned byte

				// next 6 bytes are a look up list (note long casts, important!)
				long alphaBits = (buffer.get() & 0xffL) << 0L //
									| (buffer.get() & 0xffL) << 8L //
									| (buffer.get() & 0xffL) << 16L//
									| (buffer.get() & 0xffL) << 24L //
									| (buffer.get() & 0xffL) << 32L //
									| (buffer.get() & 0xffL) << 40L;//

				short minColor = buffer.getShort();
				short maxColor = buffer.getShort();
				int colorIndexMask = buffer.getInt();
				
				

				Color24[] lookupTable = Color24.expandLookupTable(table, minColor, maxColor);

				for (int br = 0; br < blockHeight; br++) {
					for (int bc = 0; bc < blockWidth; bc++) {
						int k = (br * blockWidth) + bc;

						int alphaCode = (int)(alphaBits >> (3 * k) & 0x07); // bottom 3 bits

						int alpha = 0;

						if (alphaCode == 0) {
							alpha = alpha0;
						} else if (alphaCode == 1) {
							alpha = alpha1;
						} else if (alpha0 > alpha1) {
							alpha = ((8 - alphaCode) * alpha0 + (alphaCode - 1) * alpha1) / 7;
						} else {
							if (alphaCode == 6)
								alpha = 0;
							else if (alphaCode == 7)
								alpha = 255;
							else
								alpha = ((6 - alphaCode) * alpha0 + (alphaCode - 1) * alpha1) / 5;

						}
						
						//255 pixel is opaque
						if( alpha != 255)
							opaque = false;
						
						int colorIndex = (colorIndexMask >>> k * 2) & 0x03;

						Color24 color = lookupTable [colorIndex];
						int pixel8888 = (alpha << 24) | (color.b << 16) | (color.g << 8) | (color.r << 0);
						
						pixels.put(((row * blockHeight) * (numBlocksWide*blockWidth))  // previous columns and row
								+ (br * (numBlocksWide*blockWidth))
								+ (col * blockWidth)
								+ bc
								,pixel8888);
					}
				}
			}

		}
		return new NioImageBuffer(width, height, ImageType.TYPE_4BYTE_RGBA, directBuffer);
	}

	
	
	/*	BELOW ARE MY CRAZY NOTES OVER LONG YEARS OF R&D, all of them redundant now the jogl pipeline supports DXT
	 * 
	 * This class does some crazy things to optomises memory versus speed
	 * The source data is compress at a 1:4 ratio of the required buffered image
	 * However the buffered image raster data is pulled out and sent to the GPU so we have at least a double 
	 * up of each image usage, bummer.
	 * Most images are called once and then their Texture reference is shared, so the majority (80%?) 
	 * need to uncompress once and discard the source data and in fact teh uncompressed data
	 * However if we have no data copy then if a second get raster is called we'd have to go to the 
	 * disk again and getRaster is in the render pipeline so it MUST be super fast.
	 * We need a system to work out a head of time if we will ever see getRaster called twice
	 * But I can't work that out, it's probably related to how many Appearances use the texture and what
	 * other attributes there are (like transparency for one).
	 * The constructor call is often on a  seperate thread form the renderer, so we want the first call to uncompress the
	 * image data at least, then on first getRaster we discard the uncompressed. On second get Raster we uncompress
	 * And keep the compressed data ready for the third etc call.
	 * If however I was to make the 3rd getRaster call the one to tenure the raster data, I'd save 200MB memory
	 * as there are more 3+ getRaster call textures 
	 * Only now I basically keep a weak reference to the handed out raster
	 * 
	 * getRasterCountForAll 1700
	 * Call Count 0 0
	 * Call Count 1 172
	 * Call Count 2 23
	 * Call Count 3 81
	 * 
	 * 
	 * 
	 * Note non BufferedIamge ARGB might end up not being treated by ref properly so there might be saving to be 
	 * had to make every this ARGB but 
	 * 
	 * private SoftReference<Object> ints;
	
	private Object firstTimeIntsRef = null;
	
	public Object getInts()
	{
		// oh my god!, After the first getRaster from J3d, it'll ask me for it again
		// only it turns out it's still holding the ref from the first time, so I can weakly hold it too
		// and hand it back whenever I'm asked for it! crazy. But sometimes it's let go of it, possibly soft?
	
		//FCUK!! possibly small gain from GC not clearing up fast, does work like I suggested, nobody holds a ref to 
		// returned raster at all
	
		// for first time only use constructors hard ref, and then drop it to weak
		if (firstTimeIntsRef != null)
		{
			Object ret = firstTimeIntsRef;
			ints = new SoftReference<Object>(ret);
			firstTimeIntsRef = null;
			return ret;
		}
		else
		{
			if (ints != null)
			{
				Object prevWr = ints.get();
				if (prevWr != null)
				{
					return prevWr;
				}
			}
	
			// don't have it any more so re-create it
			Object ret = ((DataBufferInt) convertImage().getRaster().getDataBuffer()).getData();
			ints = new SoftReference<Object>(ret);
	
			return ret;
		}
	}*/

	/*	public int getRasterCount = 0;
	
		private WritableRaster firstTimeRasterRef = null;
	
		private SoftReference<WritableRaster> weakRasterRef;
	
		//private WeakReference<WritableRaster> weakRasterRef;
	
		@Override
		public WritableRaster getRaster()
		{
			getRasterCount++;
	
			//Output some stats
			//dealWithStats();
	
			// oh my god!, After the first getRaster from J3d, it'll askme for it again
			// only it turns out it's still holding the ref from the first time, so I can weakly hold it too
			// and hand it back whenever I'm asked for it! crazy. But sometimes it's let go of it, possibly soft?
	
			//FCUK!! possibly small gain from GC not clearing up fast, does work like I suggested, nobody holds a ref to 
			// returned raster at all
	
			// for first time only use constructors hard ref, and then drop it to weak
			if (firstTimeRasterRef != null)
			{
				weakRasterRef = new SoftReference<WritableRaster>(firstTimeRasterRef);
				//weakRasterRef = new WeakReference<WritableRaster>(firstTimeRasterRef);
				WritableRaster ret = firstTimeRasterRef;
				firstTimeRasterRef = null;
				return ret;
			}
			else
			{
				if (weakRasterRef != null)
				{
					WritableRaster prevWr = weakRasterRef.get();
					if (prevWr != null)
					{
						System.out.println("prev raster hit!");
						return prevWr;
					}
				}
	
				// don't have it any more so re-create it
				//System.out.println("Had to re create raster!");
				WritableRaster wr = convertImage().getRaster();
				weakRasterRef = new SoftReference<WritableRaster>(wr);
				//weakRasterRef = new WeakReference<WritableRaster>(wr);
	
				return wr;
			}
		}
	
		private static HashSet<DDSBufferedImage> allDDSBufferedImage = new HashSet<DDSBufferedImage>();
	
		private static int getRasterCountForAll = 0;
	
		private void dealWithStats()
		{
			if (mipNumber == 0)
			{
				getRasterCountForAll++;
	
				allDDSBufferedImage.add(this);
	
				if (getRasterCountForAll % 100 == 0)
				{
					System.out.println("getRasterCountForAll " + getRasterCountForAll);
					int[] callCountCounts = new int[11];//10 is 10 and up
					int countOfTenured = 0;
					int countTenuredOnly = 0;
					System.out.println("allDDSBufferedImage.size() " + allDDSBufferedImage.size());
					for (DDSBufferedImage im : allDDSBufferedImage)
					{
						if (im.getRasterCount > 1)
							System.out.println("DDSBufferedImage " + im.getRasterCount + " " + im.getImageName());
	
						if (im.getRasterCount < 10)
						{
							callCountCounts[im.getRasterCount]++;
						}
						else
						{
							callCountCounts[10]++;
						}
	
						//if (im.tenuredImage != null)
						//	countOfTenured++;
	
						if (im.ddsImage == null)
							countTenuredOnly++;
					}
	
					for (int i = 0; i < callCountCounts.length; i++)
					{
						System.out.println("Call Count " + i + " " + callCountCounts[i]);
					}
					System.out.println("countOfTenured " + countOfTenured);
					System.out.println("countTenuredOnly " + countTenuredOnly);
				}
			}
	
		}
		
		
	*/

	public static class MiniFloat {
		// ignores the higher 16 bits
		public static float toFloat(int hbits) {
			int mant = hbits & 0x03ff; // 10 bits mantissa
			int exp = hbits & 0x7c00; // 5 bits exponent
			if (exp == 0x7c00) // NaN/Inf
				exp = 0x3fc00; // -> NaN/Inf
			else if (exp != 0) // normalized value
			{
				exp += 0x1c000; // exp - 15 + 127
				if (mant == 0 && exp > 0x1c400) // smooth transition
					return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
			} else if (mant != 0) // && exp==0 -> subnormal
			{
				exp = 0x1c400; // make it normal
				do {
					mant <<= 1; // mantissa * 2
					exp -= 0x400; // decrease exp by 1
				} while ((mant & 0x400) == 0); // while not normal
				mant &= 0x3ff; // discard subnormal bit
			} // else +/-0 -> +/-0
			return Float.intBitsToFloat( // combine all parts
					(hbits & 0x8000) << 16 // sign  << ( 31 - 15 )
										| (exp | mant) << 13); // value << ( 23 - 10 )
		}

		// returns all higher 16 bits as 0 for all results
		public static int fromFloat(float fval) {
			int fbits = Float.floatToIntBits(fval);
			int sign = fbits >>> 16 & 0x8000; // sign only
			int val = (fbits & 0x7fffffff) + 0x1000; // rounded value

			if (val >= 0x47800000) // might be or become NaN/Inf
			{ // avoid Inf due to rounding
				if ((fbits & 0x7fffffff) >= 0x47800000) { // is or must become NaN/Inf
					if (val < 0x7f800000) // was value but too large
						return sign | 0x7c00; // make it +/-Inf
					return sign | 0x7c00 | // remains +/-Inf or NaN
							(fbits & 0x007fffff) >>> 13; // keep NaN (and Inf) bits
				}
				return sign | 0x7bff; // unrounded not quite Inf
			}
			if (val >= 0x38800000) // remains normalized value
				return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
			if (val < 0x33000000) // too small for subnormal
				return sign; // becomes +/-0
			val = (fbits & 0x7fffffff) >>> 23; // tmp exp for subnormal calc
			return sign | ((fbits & 0x7fffff | 0x800000) // add subnormal bit
							+ (0x800000 >>> val - 102) // round depending on cut off
					>>> 126 - val); // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
		}
	}

	/**
	 * 24 bit 888 RGB color
	 *
	 * @author Lado Garakanidze
	 * @version $Id$
	 */

	public static class Color24 {
		/**
		 * The red color component.
		 */
		public int	r		= 0;

		/**
		 * The green color component.
		 */
		public int	g		= 0;

		/**
		 * The blue color component.
		 */
		public int	b		= 0;

		// as a regular pixel 888 format
		public int	pix888	= 0;

		/**
		 * Creates a 24 bit 888 RGB color with all values set to 0.
		 */
		public Color24() {

		}

		public void set(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
			pix888 = (r << 16 | g << 8 | b);
		}

		private static Color24 fromShort565(Color24 out, short pixel) {
			//out.set((int) (((long) pixel) & 0xf800) >>> 8, (int) (((long) pixel) & 0x07e0) >>> 3, (int) (((long) pixel) & 0x001f) << 3);
			out.set((pixel & 0xf800) >>> 8, (pixel & 0x07e0) >>> 3, (pixel & 0x001f) << 3);
			return out;
		}

		/**
		 * for DXT1 only the short need to be treated as unsigned and value compared c0 and c1 is a signed short, but we
		 * need to treat it as unsigned for comparision (it's 16 bits of info not a short at all)
		 * @param c0
		 * @param c1
		 * @return
		 */
		public static boolean hasAlphaBit(short c0, short c1) {
			// & 0xFFFF makes it an int of unsigned short value
			return (c0 & 0xFFFF) <= (c1 & 0xFFFF);
		}

		/**
		 * for DXT1 only If inC0 > inC1 c4 will be left as black to be used as full transparent
		 * http://en.wikipedia.org/wiki/S3_Texture_Compression
		 * @param inC0
		 * @param inC1
		 * @return
		 */
		public static Color24[] expandLookupTableAlphable(Color24[] out, short inC0, short inC1) {
			fromShort565(out [0], inC0);
			fromShort565(out [1], inC1);
			out [2].set((out [0].r + out [1].r) / 2, (out [0].g + out [1].g) / 2, (out [0].b + out [1].b) / 2);
			out [3].set(0, 0, 0);

			return out;
		}

		public static Color24[] expandLookupTable(Color24[] out, short inC0, short inC1) {
			fromShort565(out [0], inC0);
			fromShort565(out [1], inC1);
			out [2].set((2 * out [0].r + out [1].r) / 3, (2 * out [0].g + out [1].g) / 3,
					(2 * out [0].b + out [1].b) / 3);
			out [3].set((out [0].r + 2 * out [1].r) / 3, (out [0].g + 2 * out [1].g) / 3,
					(out [0].b + 2 * out [1].b) / 3);
			return out;

		}

	}
}
