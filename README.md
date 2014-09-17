icafe
=====

Java library for reading, writing, converting and managing images

Reading format support:
Windows BITMAP
GIF
PCX
TGA
PNG

Writing format support:
Windows BITMAP
GIF
JPEG
TIFF
PNG

Image conversion to any of the supported format

PNG meta data management (inserting, removing chunks)

Multiple TIFF page support (creating multiple page TIFF from a series of BufferedImages; spliting multiple TIFF image to 
individual images in TIFF format without decoding; inserting and removing pages to and from multiple page TIFF etc.)

Animated GIF support (Creating animated GIF from a series of BufferedImages; spliting animated GIF to individual images in any of the supported writing formats - keeping transparency etc.)

JPEG and TIFF thumbnail support (inserting, removing, extracting thumbnails to and from JPEG - the extracted thumbnail is either in JPEG or TIFF format depending on whether or not it is in JPEG or Raw format and the inserted thumbnail could be completely different from the original image)

JPEG EXIF data manipulation (inserting, extracting, deleting exif data)

JPEG ICC Profile support (inserting, extracting ICC profile)

Go to the wiki page to see this library in action!
