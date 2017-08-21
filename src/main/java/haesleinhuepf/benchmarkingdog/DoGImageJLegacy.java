package haesleinhuepf.benchmarkingdog;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>DoG (ImageJ-legacy)") public class DoGImageJLegacy implements
                                                                                                     Command
{
  @Parameter private ImagePlus currentData;

  @Parameter private double sigma1;

  @Parameter private double sigma2;

  @Override public void run()
  {
    ImagePlus filtered1 = new Duplicator().run(currentData);
    ImagePlus filtered2 = new Duplicator().run(currentData);

    StopWatch watch = new StopWatch();
    watch.start();
    IJ.run(filtered1, "Gaussian Blur...", "sigma=" + sigma1);
    watch.stop("Leg 1st Gaussian blur");
    watch.start();
    IJ.run(filtered2, "Gaussian Blur...", "sigma=" + sigma2);
    watch.stop("Leg 2nd Gaussian blur");

    watch.start();
    ImagePlus
        imp3 =
        new ImageCalculator().run("Subtract create",
                                  filtered1,
                                  filtered2);
    watch.stop("Leg Subtracting images");

    filtered1.show();
    filtered2.show();

    imp3.show();
  }
}
