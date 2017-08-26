package haesleinhuepf.benchmarkingdog.clearcl;

import clearcl.*;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import clearcl.ops.OpsBase;

import java.io.IOException;

public class ClearCLDifferenceOfGaussian extends OpsBase
{
  ImageCache mMinuendFilterKernelImageCache;
  ImageCache mSubtrahendFilterKernelImageCache;
  ImageCache mOutputImageCache;

  ClearCLContext mContext;
  private ClearCLKernel mSubtractionConvolvedKernelImage2F;

  public ClearCLDifferenceOfGaussian(ClearCLQueue pClearCLQueue) throws
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
    mSubtractionConvolvedKernelImage2F.setGlobalSizes(pInputImage.getDimensions());
    mSubtractionConvolvedKernelImage2F.run();

    return output;
  }
}
