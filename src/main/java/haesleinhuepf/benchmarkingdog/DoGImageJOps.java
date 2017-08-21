/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package haesleinhuepf.benchmarkingdog;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This example illustrates how to create an ImageJ {@link Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>DoG (Ops)") public class DoGImageJOps<T extends RealType<T>> implements
                                                                                                               Command
{
  //
  // Feel free to add more parameters here...
  //

  @Parameter private Img currentData;

  @Parameter private UIService uiService;

  @Parameter private OpService opService;

  @Parameter private double sigma1;

  @Parameter private double sigma2;

  @Override public void run()
  {
    final Img<T> image = (Img<T>) currentData;

    List<RandomAccessibleInterval<T>> results = new ArrayList<>();

    StopWatch watch = new StopWatch();
    watch.start();
    RandomAccessibleInterval<T>
        filtered1 =
        opService.filter().gauss(image, sigma1);
    watch.stop("Ops 1st Gaussian blur");
    watch.start();
    RandomAccessibleInterval<T>
        filtered2 =
        opService.filter().gauss(image, sigma2);
    watch.stop("Ops 2nd Gaussian blur");

    long[] dimensions = new long[image.numDimensions()];
    image.dimensions(dimensions);
    Img<T>
        dogImage =
        image.factory().create(dimensions, image.cursor().next());

    watch.start();
    opService.math()
             .subtract(dogImage,
                       Views.iterable(filtered1),
                       Views.iterable(filtered2));
    watch.stop("Ops Subtracting images");

    uiService.show(filtered1);
    uiService.show(filtered2);
    uiService.show(dogImage);
  }

}
