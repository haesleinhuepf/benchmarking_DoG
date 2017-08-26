package haesleinhuepf.benchmarkingdog.clearcl;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;

public class ImageCache
{
  private ClearCLContext mContext;
  private ClearCLImage mClearCLImage;

  public ImageCache(ClearCLContext pContext) {
    mContext = pContext;
  }

  public ClearCLImage get2DImage(HostAccessType pHostAccessType, KernelAccessType pKernelAccessType, ImageChannelOrder pImageChannelOrder, ImageChannelDataType pImageChannelDataType, long pWidth, long pHeight) {
    if (mClearCLImage == null ||
        mClearCLImage.getHostAccessType() != pHostAccessType ||
        mClearCLImage.getKernelAccessType() != pKernelAccessType ||
        mClearCLImage.getChannelOrder() != pImageChannelOrder ||
        mClearCLImage.getChannelDataType() != pImageChannelDataType ||
        mClearCLImage.getWidth() != pWidth ||
        mClearCLImage.getHeight() != pHeight
        ) {

      mContext.createImage(HostAccessType.WriteOnly,
                           KernelAccessType.ReadOnly,
                           ImageChannelOrder.Intensity,
                           ImageChannelDataType.Float,
                           pWidth,
                           pHeight);
    }
    return mClearCLImage;
  }

  public void invalidate() {
    mClearCLImage = null;
  }
}
