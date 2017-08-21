package haesleinhuepf.benchmarkingdog;

import clearcl.*;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.javacl.ClearCLBackendJavaCL;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import coremem.ContiguousMemoryInterface;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;
import coremem.offheap.OffHeapMemoryAccess;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLGaussianBlur;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLSum;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;
import java.util.Arrays;

@Plugin(type = Command.class, menuPath = "Plugins>DoG (CLearCL)")
public class DoGClearCL<T extends RealType<T>> implements Command
{
  private ClearCLContext mContext;

  @Parameter private Img currentData;

  @Parameter private UIService uiService;

  @Parameter private double sigma1;

  @Parameter private double sigma2;

  @Override public void run()
  {
    ClearCLBackendInterface
        lClearCLBackend =
        new ClearCLBackendJavaCL();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackend))
    {
      ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

      mContext = lBestGPUDevice.createContext();

      ClearCLImage image = convertCmgToClearCLImage(currentData);

      ClearCLGaussianBlur lClearCLGaussianBlur = new ClearCLGaussianBlur(mContext.getDefaultQueue());

      StopWatch watch = new StopWatch();
      watch.start();
      ClearCLImage filtered1 = lClearCLGaussianBlur.gausianBlur(image, (float)sigma1);
      watch.stop("CCL 1st Gaussian blur");
      watch.start();
      ClearCLImage filtered2 = lClearCLGaussianBlur.gausianBlur(image, (float)sigma2);
      watch.stop("CCL 2nd Gaussian blur");

      ClearCLSum lClearCLSum = new ClearCLSum(mContext.getDefaultQueue());
      watch.start();
      ClearCLImage result = lClearCLSum.subtract(filtered1, filtered2);
      watch.stop("CCL Subtracting images");

      uiService.show(convertClearClImageToImg(filtered1));
      uiService.show(convertClearClImageToImg(filtered2));
      uiService.show(convertClearClImageToImg(result));
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  private ClearCLImage convertCmgToClearCLImage(IterableInterval<T> iterable)
  {
    long[] dimensions = new long[iterable.numDimensions()];
    iterable.dimensions(dimensions);

    ClearCLImage
        lClearClImage =
        mContext.createImage(HostAccessType.ReadWrite,
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

  private Img<FloatType> convertClearClImageToImg(ClearCLImage image) {
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

    ClearCLBuffer buffer = mContext.createBuffer(NativeTypeEnum.Float, numberOfPixels);
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
}
