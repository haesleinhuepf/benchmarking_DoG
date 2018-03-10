package haesleinhuepf.benchmarkingdog;

import clearcl.*;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.backend.javacl.ClearCLBackendJavaCL;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLGaussianBlur;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLSum;
import haesleinhuepf.benchmarkingdog.clearcl.ClearCLUtilities;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>DoG (Dimension separated, ClearCL)")
public class DoGDimensionSeparatedClearCL<T extends RealType<T>> implements Command
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
            ClearCLBackends.getFunctionalBackend();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackend))
    {
      ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

      mContext = lBestGPUDevice.createContext();

      ClearCLImage image = ClearCLUtilities.convertImgToClearCLImage(mContext, currentData);

      ClearCLGaussianBlur lClearCLGaussianBlur = new ClearCLGaussianBlur(mContext.getDefaultQueue());

      StopWatch watch = new StopWatch();
      watch.start();
      ClearCLImage filtered1 = lClearCLGaussianBlur.gaussianBlur(image, (float)sigma1);
      watch.stop("Dimension separated CCL 1st Gaussian blur");
      watch.start();
      ClearCLImage filtered2 = lClearCLGaussianBlur.gaussianBlur(image, (float)sigma2);
      watch.stop("Dimension separated CCL 2nd Gaussian blur");

      ClearCLSum lClearCLSum = new ClearCLSum(mContext.getDefaultQueue());
      watch.start();
      ClearCLImage result = lClearCLSum.subtract(filtered1, filtered2);
      watch.stop("Dimension separated CCL Subtracting images");

      uiService.show(ClearCLUtilities.convertClearClImageToImg(mContext, filtered1));
      uiService.show(ClearCLUtilities.convertClearClImageToImg(mContext, filtered2));
      uiService.show(ClearCLUtilities.convertClearClImageToImg(mContext, result));
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }


}
