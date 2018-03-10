package haesleinhuepf.benchmarkingdog.clearcl;

import clearcl.*;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import clearcl.ops.OpsBase;

import java.io.IOException;

public class ClearCLSum extends OpsBase
{
  private ClearCLContext mContext;

  private ClearCLKernel mSumKernelImage2F;

  public ClearCLSum(ClearCLQueue pClearCLQueue) throws IOException
  {
    super(pClearCLQueue);
    mContext = getContext();

    ClearCLProgram
        lConvolutionProgram =
        getContext().createProgram(ClearCLSum.class, "sum.cl");

    lConvolutionProgram.addBuildOptionAllMathOpt();
    lConvolutionProgram.addDefine("FLOAT");
    lConvolutionProgram.buildAndLog();

    mSumKernelImage2F =
        lConvolutionProgram.createKernel("sum_image_2d");
  }

  public ClearCLImage add(ClearCLImage input1, ClearCLImage input2)
  {
    return add(input1, input2, 1, 1);
  }

  public ClearCLImage subtract(ClearCLImage input1,
                               ClearCLImage input2)
  {
    return add(input1, input2, 1, -1);
  }

  public ClearCLImage add(ClearCLImage input1,
                          ClearCLImage input2,
                          float factor1,
                          float factor2)
  {
    ClearCLImage
        output =
        mContext.createImage(HostAccessType.ReadWrite,
                             KernelAccessType.ReadWrite,
                             ImageChannelOrder.R,
                             ImageChannelDataType.Float,
                             input1.getDimensions());

    mSumKernelImage2F.setArgument("input1", input1);
    mSumKernelImage2F.setArgument("input2", input2);
    mSumKernelImage2F.setArgument("output", output);
    mSumKernelImage2F.setArgument("factor1", factor1);
    mSumKernelImage2F.setArgument("factor2", factor2);
    mSumKernelImage2F.setGlobalSizes(input1.getDimensions());
    mSumKernelImage2F.run();

    return output;
  }

}
