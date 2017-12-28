package org.jogamp.java3d.compressedtexture;

import java.io.ByteArrayInputStream;

/**
 * This a simple interface used by loader that want to allow direct access to the bytebuffer
 * It is used by TextureLoader to detect this allowance and for it to use the buf directly
 * @author Philip Jordan
 *
 */
public class FastByteArrayInputStream extends ByteArrayInputStream
{
	public FastByteArrayInputStream(byte[] buf, int offset, int length)
	{
		super(buf, offset, length);
	}

	public FastByteArrayInputStream(byte[] buf)
	{
		super(buf);
	}

	public byte[] getBuf()
	{
		return buf;
	}

}
