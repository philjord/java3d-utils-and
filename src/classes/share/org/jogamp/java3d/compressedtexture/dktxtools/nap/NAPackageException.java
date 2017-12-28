package org.jogamp.java3d.compressedtexture.dktxtools.nap;

public class NAPackageException extends Exception {

    private static final long serialVersionUID = 1L;

    public NAPackageException(String message) {
        this(message, null);
    }

    public NAPackageException(String message, Throwable t) {
        super(message, t);
    }

}
