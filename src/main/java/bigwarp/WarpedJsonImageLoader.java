package bigwarp;

import java.util.HashMap;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

public class WarpedJsonImageLoader< T extends NumericType< T > & NativeType< T >, A extends ArrayDataAccess< A > > implements BasicImgLoader, TypedBasicImgLoader< T >
{

	private final T type;

	private final int numSetups;

	private final AbstractSpimData<?> data;

	private final HashMap< Integer, SetupImgLoader > setupImgLoaders;

	public WarpedJsonImageLoader( final T type, final AbstractSpimData<?> data, 
			ThinplateSplineTransform xfm,
			int[] setupIds )
	{
		this.type = type;
		this.data = data;
		numSetups = data.getSequenceDescription().getViewSetupsOrdered().size();
		setupImgLoaders = new HashMap< Integer, SetupImgLoader >();
		for ( int i = 0; i < numSetups; ++i )
			setupImgLoaders.put( setupIds[ i ], new SetupImgLoader( i ) );
	}

	public class SetupImgLoader implements BasicSetupImgLoader< T >
	{
		private final int channel;
	
		public SetupImgLoader( final int channel )
		{
			this.channel = channel;
		}
	
		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
//			WarpedSource<T> warpedSource = new WarpedSource<>(source, name)
			return null;
		}
	
		@Override
		public T getImageType()
		{
			return type;
		}
	}

	@Override
	public BasicSetupImgLoader<T> getSetupImgLoader(int setupId) {
		return null;
	}


}
