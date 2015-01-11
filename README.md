ICAFE
=====

ICAFE is a Java library for reading, writing, converting and managing images.

Read format support:
----------------------
- Windows BITMAP
- CompuServe GIF - Backed by a from-scratch tree-based LZW decoder (no hash, no string table, quick and clean); transparency support; Frame extraction for animated GIF. 
- ZSoft's PCX - Hard to find decoder for this dinosaur type of image.
- Truevision TGA - Support all kinds of scan line mode, bit depth, and even transparency (alpha channel)
- Portable Network Graphics (PNG) - Support both 8 bit and 16 bit, all color depth; single color transparency and alpha channel support; gamma support, ICC_Profile support.
- Adobe  TIFF - Support for RGB, Palette, CMYK, YCbCr Color image with LZW, Deflate, and Packbits compression. Support for both stripped and tiled format TIFF (From 1 bitPerPixel up to 64 bitPerPixel). Floating point samples are also supported (Half precision - 16 bit, single precision - 32 bit, double precision - 64 bit). Binary formats and JPEG compression are still under development.
- IJG JPEG - Currently delegate to Java ImageIO - under development.

Write format support:
-----------------------
- Windows BITMAP - Non-compressed RGB or 256 color image with the help of "icafe" color quantization utility.
- CompuServe GIF - In-house tree-based LZW encoder; color quantization and diffusion dithering; inverse color map for colors to indexes mapping; transparency support.
- IJG JPEG - Support 8 bit grayscale and full color; support both YCbCr and YCCK (CMYK) color spaces and embedded ICC_Profile, EXIF thumbnail; ANN fast forward DCT from scratch.
- Adobe TIFF - Compression types supported: LZW, Deflate, CCITTRLE, CCITTGROUP3, CCITTGROUP4, JPEG, PACKBITS; extra sample suport for CMYK and transparent RGB; Horizontal differencing predictor support to reduce image size; optional ICC_Profile support for CMYK color space. 
- Portable Network Graphics (PNG) - Support indexed, grayscale, and RGB colors; support configurable adaptive filter.

Image convertion and management:
----------------------------------------
- Image conversion to any of the supported format
- Multipage TIFF support
  * Create multiple page TIFF from a series of BufferedImages.
  * Split multipage TIFF image into individual TIFF images without decompression the images.
  * Insert into or remove pages from multipage TIFF images.
  * Merge multipage TIFF images togeter without decompressing (Support for any BitsPerSample TIFF).
- Animated GIF support
  * Create animated GIF from a series of BufferedImages.
  * split animated GIF to individual images in any of the supported writing formats - keeping transparency etc.
- JPEG and TIFF thumbnail support
   * Insert thumbnails into JPEG image.
   * Remove thumbnails from JPEG image.
   * Extract thumbnails from JPEG (the extracted thumbnail is either in JPEG or TIFF format depending on whether or not it is in JPEG or Raw format and the inserted thumbnail could be completely different from the original image).
   * Insert thumbnail to TIFF image.
   * Extract thumbnail(as normal TIFF page) from TIFF image.
   * Extract Photoshop thumbnail from Photoshop tag
- JPEG and TIFF EXIF data manipulation
   * Insert EXIF data into JPEG.
   * Extract EXIF data from JPEG.
   * Remove EXIF data and other insignificant APPn segments from JPEG.
   * Insert EXIF data into TIFF.
   * Read EXIF data embedded in TIFF.
- JPEG and TIFF ICC Profile support
   * Insert ICC profile to JPEG and TIFF.
   * Extract ICC profile from JPEG and TIFF.
- PNG chunk manipulation
   * Remove chunks from or add chunks to existing PNG.
   * Extract text chunk from PNG.
   * Insert text chunk to PNG.
   * Extract ICC profile from PNG.
   * Insert ICC_Profile to PNG.
   * Merge or split IDAT chunks.

Suggestions? custom requirements? email me: yuwen_66@yahoo.com

Go to the [wiki] page to see this library in action!

[wiki]:https://github.com/dragon66/icafe/wiki
