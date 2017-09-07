package haesleinhuepf.benchmarkingdog.clearcl;

import clearcl.*;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import clearcl.ops.OpsBase;

import java.io.IOException;

public class ClearCLGaussianBlur extends OpsBase
{
  private ClearCLKernel mConvolutionKernelImage2F;
  private int mRadius;

  private ClearCLContext mContext;

  private ClearCLImage mKernelClearCLImage;

  private ImageCache mOutputImageCache;
  private ImageCache mKernelImageCache;

  public ClearCLGaussianBlur(ClearCLQueue pClearCLQueue) throws
                                                         IOException
  {
    super(pClearCLQueue);
    mContext = getContext();
    mOutputImageCache = new ImageCache(mContext);
    mKernelImageCache = new ImageCache(mContext);

    ClearCLProgram
        lConvolutionProgram =
        getContext().createProgram(ClearCLGaussianBlur.class,
                                   "convolution.cl");

    lConvolutionProgram.addBuildOptionAllMathOpt();
    lConvolutionProgram.addDefine("FLOAT");
    lConvolutionProgram.buildAndLog();

    mConvolutionKernelImage2F =
        lConvolutionProgram.createKernel("convolve_image_2d");
  }

  public ClearCLImage gaussianBlur(ClearCLImage input, float sigma)
  {
    createBlur2DFilterKernelImage(sigma);

    ClearCLImage
        output =
        mOutputImageCache.get2DImage(HostAccessType.ReadWrite,
                                     KernelAccessType.ReadWrite,
                                     ImageChannelOrder.Intensity,
                                     ImageChannelDataType.Float,
                                     input.getWidth(),
                                     input.getHeight());

    mConvolutionKernelImage2F.setArgument("input", input);
    mConvolutionKernelImage2F.setArgument("filterkernel",
                                          mKernelClearCLImage);
    mConvolutionKernelImage2F.setArgument("output", output);
    mConvolutionKernelImage2F.setArgument("radius", mRadius);
    mConvolutionKernelImage2F.setGlobalSizes(input.getDimensions());
    //mConvolutionKernelImage2F.run();

    long[] sizes = new long[input.getDimensions().length];
    long[] offsets = new long[input.getDimensions().length];
    for (int i = 0; i < sizes.length; i++) {
      sizes[i] = input.getDimensions()[i];
      offsets[i] = 0;
    }

    long originalSize0 = sizes[0];
    int numberOfSplits = 32;
    for (int j = 0; j < numberOfSplits; j++) {
      if (j < numberOfSplits - 1) {
        sizes[0] = originalSize0 / numberOfSplits;
      } else {
        sizes[0] = originalSize0 - offsets[0];
      }

      //System.out.println("s offset: " + Arrays.toString(offsets));
      //System.out.println("s sizes: " + Arrays.toString(sizes));
      mConvolutionKernelImage2F.setGlobalSizes(sizes);
      mConvolutionKernelImage2F.setGlobalOffsets(offsets);
      mConvolutionKernelImage2F.run();
      offsets[0] += sizes[0];
    }

    return output;
  }

  private void createBlur2DFilterKernelImage(float sigma)
  {
    int lRadius = (int) Math.ceil(3.0f * sigma);
    mKernelClearCLImage = ClearCLGaussUtilities.createBlur2DFilterKernelImage(mKernelImageCache, sigma, lRadius);
    mRadius = lRadius;
  }

}
