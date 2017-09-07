package haesleinhuepf.benchmarkingdog;

import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import java.util.Random;

public class Main
{
  public static void main(final String... args) throws Exception
  {
    // Run ImageJ
    final ImageJ ij = new ImageJ();
    ij.ui().showUI();

    // Create test data
    int size = 2048;

    Img<FloatType> img = ArrayImgs.floats(new long[] { size, size });

    Cursor<FloatType> cursor = img.cursor();
    Random random = new Random();
    while (cursor.hasNext())
    {
      cursor.next().set(random.nextFloat() * 65536);
    }
    ImagePlus imp = ImageJFunctions.wrap(img, "temp");

    Object[]
        imglibParameters =
        new Object[] { "currentData",
                       img,
                       "sigma1",
                       10,
                       "sigma2",
                       20 };
    Object[]
        legacyParameters =
        new Object[] { "currentData",
                       imp,
                       "sigma1",
                       10,
                       "sigma2",
                       20 };

    ij.ui().show(img);
//
//    Thread.sleep(1000);
//
//    ij.command().run(DoGImageJOps.class, true, imglibParameters);
//
//    Thread.sleep(1000);
//
//    ij.command().run(DoGImageJLegacy.class, true, legacyParameters);
//
//    Thread.sleep(1000);
//
//    ij.command().run(DoGClearCL.class, true, imglibParameters);
//
//    Thread.sleep(1000);
//
    ij.command().run(DoGLookupWeightsClearCL.class, true, imglibParameters);

    Thread.sleep(1000);

    ij.command().run(DoGOnlineCalculateWeightsClearCL.class, true, imglibParameters);
    
  }
}
