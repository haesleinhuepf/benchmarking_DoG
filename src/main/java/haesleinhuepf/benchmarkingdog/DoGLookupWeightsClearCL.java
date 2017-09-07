package haesleinhuepf.benchmarkingdog;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.javacl.ClearCLBackendJavaCL;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLLookupWeightsDifferenceOfGaussian;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLUtilities;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>DoG (Fast ClearCL)")
public class DoGLookupWeightsClearCL<T extends RealType<T>> implements Command
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

      ClearCLImage image = ClearCLUtilities.convertImgToClearCLImage(mContext, currentData);

      ClearCLLookupWeightsDifferenceOfGaussian
          lClearCLLookupWeigtsDifferenceOfGaussian = new ClearCLLookupWeightsDifferenceOfGaussian(mContext.getDefaultQueue());

      StopWatch watch = new StopWatch();
      watch.start();
      ClearCLImage result = lClearCLLookupWeigtsDifferenceOfGaussian.differenceOfGaussian(image, (float)sigma1, (float)sigma2);
      watch.stop("Lookup CCL Difference of Gaussian");

      uiService.show(ClearCLUtilities.convertClearClImageToImg(mContext, result));
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }

  }
}
