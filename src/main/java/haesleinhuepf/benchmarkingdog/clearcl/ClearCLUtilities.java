package haesleinhuepf.benchmarkingdog.clearcl;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import coremem.ContiguousMemoryInterface;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class ClearCLUtilities
{
  public static Img<FloatType> convertClearClImageToImg(ClearCLContext pContext,
                                                        ClearCLImage image) {
    Img<FloatType> img = ArrayImgs.floats(image.getDimensions());

    int bytesPerPixel = 4; //because we are talking about Java floats
    long numberOfPixels = image.getWidth()
                                  * image.getHeight()
                                  * image.getDepth();

    long numberOfBytesToAllocate = bytesPerPixel * numberOfPixels;

    ContiguousMemoryInterface contOut =
        new OffHeapMemory("memmm",
                          null,
                          OffHeapMemoryAccess
                              .allocateMemory(numberOfBytesToAllocate),
                          numberOfBytesToAllocate);

    ClearCLBuffer
        buffer = pContext.createBuffer(NativeTypeEnum.Float, numberOfPixels);
    image.copyTo(buffer, true);
    buffer.writeTo(contOut, true);

    int count = 0;
    Cursor<FloatType> cursor = img.cursor();
    while (cursor.hasNext())
    {
      cursor.next().set(contOut.getFloat(count));
      count+=bytesPerPixel;
    }
    return img;
  }


  public static <T extends RealType<T>> ClearCLImage convertImgToClearCLImage(ClearCLContext pContext,
                                                                              IterableInterval<T> iterable)
  {
    long[] dimensions = new long[iterable.numDimensions()];
    iterable.dimensions(dimensions);

    ClearCLImage
        lClearClImage =
        pContext.createImage(HostAccessType.ReadWrite,
                             KernelAccessType.ReadWrite,
                             ImageChannelOrder.Intensity,
                             ImageChannelDataType.Float,
                             dimensions);

    long sumDimensions = 1;
    for (int i = 0; i < dimensions.length; i++)
    {
      sumDimensions *= dimensions[i];
    }

    float[] inputArray = new float[(int) sumDimensions];

    int count = 0;
    Cursor<T> cursor = iterable.cursor();
    while (cursor.hasNext()) {
      inputArray[count] = cursor.next().getRealFloat();
      count++;
    }

    lClearClImage.readFrom(inputArray, true);
    return lClearClImage;
  }
}
