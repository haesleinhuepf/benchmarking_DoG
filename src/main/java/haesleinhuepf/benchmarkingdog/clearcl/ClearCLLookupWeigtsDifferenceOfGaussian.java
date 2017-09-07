package haesleinhuepf.benchmarkingdog.clearcl;

import clearcl.*;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import clearcl.ops.OpsBase;

import java.io.IOException;
import java.util.Arrays;

public class ClearCLLookupWeigtsDifferenceOfGaussian extends OpsBase
{
  ImageCache mMinuendFilterKernelImageCache;
  ImageCache mSubtrahendFilterKernelImageCache;
  ImageCache mOutputImageCache;

  ClearCLContext mContext;
  private ClearCLKernel mSubtractionConvolvedKernelImage2F;

  public ClearCLLookupWeigtsDifferenceOfGaussian(ClearCLQueue pClearCLQueue) throws
                                                                 IOException
  {
    super(pClearCLQueue);
    mContext = getContext();
    mMinuendFilterKernelImageCache = new ImageCache(mContext);
    mSubtrahendFilterKernelImageCache = new ImageCache(mContext);
    mOutputImageCache = new ImageCache(mContext);

    ClearCLProgram
        lConvolutionProgram =
        getContext().createProgram(ClearCLGaussianBlur.class,
                                   "convolution.cl");

    lConvolutionProgram.addBuildOptionAllMathOpt();
    lConvolutionProgram.addDefine("FLOAT");
    lConvolutionProgram.buildAndLog();

    mSubtractionConvolvedKernelImage2F =
        lConvolutionProgram.createKernel(
            "subtract_convolved_images_2d");
  }

  public ClearCLImage differenceOfGaussian(ClearCLImage pInputImage,
                                           float pMinuendSigma,
                                           float pSubtrahendSigma)
  {
    int
        lRadius =
        (int) Math.ceil(3.0f * Math.max(pMinuendSigma,
                                        pSubtrahendSigma));
    ClearCLImage
        lMinuendFilterKernelImage =
        ClearCLGaussUtilities.createBlur2DFilterKernelImage(
            mMinuendFilterKernelImageCache,
            pMinuendSigma,
            lRadius);
    ClearCLImage
        lSubtrahendFilterKernelImage =
        ClearCLGaussUtilities.createBlur2DFilterKernelImage(
            mSubtrahendFilterKernelImageCache,
            pSubtrahendSigma,
            lRadius);

    ClearCLImage
        output =
        mOutputImageCache.get2DImage(HostAccessType.ReadWrite,
                                     KernelAccessType.ReadWrite,
                                     ImageChannelOrder.Intensity,
                                     ImageChannelDataType.Float,
                                     pInputImage.getWidth(),
                                     pInputImage.getHeight());

    long[] imageDimensions = pInputImage.getDimensions();
//    long[] workgroupDimensions = new long[imageDimensions.length];
//    for (int i = 0; i < imageDimensions.length; i++) {
//      workgroupDimensions[i] = 4;
//    }

    System.out.println("gl: " + Arrays.toString(imageDimensions));
    //System.out.println("ws: " + Arrays.toString(workgroupDimensions));

    mSubtractionConvolvedKernelImage2F.setArgument("input",
                                                   pInputImage);
    mSubtractionConvolvedKernelImage2F.setArgument(
        "filterkernel_minuend",
        lMinuendFilterKernelImage);
    mSubtractionConvolvedKernelImage2F.setArgument(
        "filterkernel_subtrahend",
        lSubtrahendFilterKernelImage);
    mSubtractionConvolvedKernelImage2F.setArgument("output", output);
    mSubtractionConvolvedKernelImage2F.setArgument("radius", lRadius);
    mSubtractionConvolvedKernelImage2F.setGlobalSizes(imageDimensions);
    //mSubtractionConvolvedKernelImage2F.setLocalSizes(workgroupDimensions);

    long[] sizes = new long[pInputImage.getDimensions().length];
    long[] offsets = new long[pInputImage.getDimensions().length];
    for (int i = 0; i < sizes.length; i++) {
      sizes[i] = pInputImage.getDimensions()[i];
      offsets[i] = 0;
    }

    long originalSize0 = sizes[0];
    int numberOfSplits = 16;
    for (int j = 0; j < numberOfSplits; j++) {
      if (j < numberOfSplits - 1) {
        sizes[0] = originalSize0 / numberOfSplits;
      } else {
        sizes[0] = originalSize0 - offsets[0];
      }

      System.out.println("s offset: " + Arrays.toString(offsets));
      System.out.println("s sizes: " + Arrays.toString(sizes));
      mSubtractionConvolvedKernelImage2F.setGlobalSizes(sizes);
      mSubtractionConvolvedKernelImage2F.setGlobalOffsets(offsets);
      mSubtractionConvolvedKernelImage2F.run();
      offsets[0] += sizes[0];
    }
    return output;
  }
}
