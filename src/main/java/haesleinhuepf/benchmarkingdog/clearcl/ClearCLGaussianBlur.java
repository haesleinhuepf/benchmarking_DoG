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
    mConvolutionKernelImage2F.run();

    return output;
  }

  private void createBlur2DFilterKernelImage(float sigma)
  {
    int lRadius = (int) Math.ceil(3.0f * sigma);
    mKernelClearCLImage = ClearCLGaussUtilities.createBlur2DFilterKernelImage(mKernelImageCache, sigma, lRadius);
    mRadius = lRadius;
  }

}
