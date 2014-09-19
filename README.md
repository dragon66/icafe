ICAFE
=====

ICAFE is a Java library for reading, writing, converting and managing images.

Reading format support:
----------------------
- Windows BITMAP
- CompuServe GIF - In-house tree-based LZW decoder
- ZSoft's PCX
- Truevision TGA
- Portable Network Graphics (PNG) - support both 8 bit and 16 bit, all color depth; single color or alpha channel support; gamma support

Writing format support:
-----------------------
- Windows BITMAP - Non-compressed 256 color with the help color quantization utility or RGB
- CompuServe GIF - In-house tree-based LZW encoder; color quantization; inverse color map to map colors to indexes; transparency support
- JPEG - support 8 bit grayscale and full color; support both YCbCr and YCCK (CMYK) color spaces and embedded ICC_Profile, EXIF thumbnail; ANN fast forward DCT
- Adobe TIFF - compression types supported: LZW, Deflate, CCITTRLE, CCITTGROUP3, CCITTGROUP4, JPEG, PACKBITS; extra sample suport for CMYK and transparent RGB; Horizontal differencing predictor support to reduce image size 
- Portable Network Graphics (PNG) - support indexed, grayscale, and RGB colors; support configurable adaptive filter

Image convertion and management support:
----------------------------------------
- Image conversion to any of the supported format
- Multipage TIFF support
  * Create multiple page TIFF from a series of BufferedImages
  * Split multiple TIFF image to individual images in TIFF format without decompression
  * Insert and remove pages to and from multiple page TIFF etc.
- Animated GIF support
  * Create animated GIF from a series of BufferedImages
  * split animated GIF to individual images in any of the supported writing formats - keeping transparency etc.
- JPEG and TIFF thumbnail support
   * Insert thumbnails into JPEG image
   * Remove thumbnails from JPEG image
   * Extract thumbnails from JPEG (the extracted thumbnail is either in JPEG or TIFF format depending on whether or not it is in JPEG or Raw format and the inserted thumbnail could be completely different from the original image)
- JPEG EXIF data manipulation
   * Inserte EXIF data into JPEG
   * Extract EXIF data from JPEG
   * Remove EXIF data and other an significant APPn segments from JPEG
- JPEG ICC Profile support
   * Inserte ICC profile to JPEG
   * Extract ICC profile from JPEG
- PNG chunk manipulation
   * Remove chunks from or add chunks to existing PNG
   * Extract text chunk from PNG
   * Merge or split IDAT chunks


Go to the [wiki] page to see this library in action!

[wiki]:https://github.com/dragon66/icafe/wiki
