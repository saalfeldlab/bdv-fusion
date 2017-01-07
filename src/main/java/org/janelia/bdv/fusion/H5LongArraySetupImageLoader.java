package org.janelia.bdv.fusion;

import java.io.IOException;

import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.cache.CacheHints;
import bdv.cache.LoadingStrategy;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.img.h5.AbstractH5SetupImageLoader;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.array.LongArrayType;
import net.imglib2.type.numeric.array.VolatileLongArrayType;
import net.imglib2.util.Fraction;

/**
 * {@link ViewerSetupImgLoader} for 3D volumes of LongArrayType stored as
 * 4D volumes (first/ slowest running dimension is the length of the array)
 * stored in HDF5 files.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class H5LongArraySetupImageLoader
	extends AbstractH5SetupImageLoader< LongArrayType, VolatileLongArrayType, VolatileLongArray >
{
	final protected int arrayLength;

	private static final LongArrayType createLongArrayType(
			final IHDF5Reader reader,
			final String dataset )
	{
		final long[] dimensions = reader.object().getDimensions( dataset );
		return new LongArrayType( ( int )dimensions[ dimensions.length - 1 ] );
	}

	private static final VolatileLongArrayType createVolatileLongArrayType(
			final IHDF5Reader reader,
			final String dataset )
	{
		final long[] dimensions = reader.object().getDimensions( dataset );
		return new VolatileLongArrayType( ( int )dimensions[ dimensions.length - 1 ] );
	}

	public H5LongArraySetupImageLoader(
			final IHDF5Reader reader,
			final String dataset,
			final int setupId,
			final int[] blockDimension ) throws IOException
	{
		super(
				reader,
				dataset,
				setupId,
				blockDimension,
				createLongArrayType( reader, dataset ),
				createVolatileLongArrayType( reader, dataset ),
				new H5LongArrayArrayLoader( reader, dataset ) );

		final long[] h5dim = reader.object().getDimensions( dataset );

		arrayLength = ( int )h5dim[ 3 ];
	}

	protected < S extends NativeType< S > > CachedCellImg< S, VolatileLongArray > prepareCachedImage(
			final int timepointId,
			final int setupId,
			final int level,
			final LoadingStrategy loadingStrategy )
	{
		final int priority = 0;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileLongArray > c = cache.new VolatileCellCache< VolatileLongArray >( timepointId, setupId, level, cacheHints, loader );
		final VolatileImgCells< VolatileLongArray > cells = new VolatileImgCells< VolatileLongArray >( c, new Fraction(), dimension, blockDimension );
		final CachedCellImg< S, VolatileLongArray > img = new CachedCellImg< S, VolatileLongArray >( cells );
		return img;
	}

	@Override
	public RandomAccessibleInterval< LongArrayType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		final CachedCellImg< LongArrayType, VolatileLongArray > img = prepareCachedImage( timepointId, setupId, level, LoadingStrategy.BLOCKING );
		final LongArrayType linkedType = new LongArrayType( img, arrayLength );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileLongArrayType > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		final CachedCellImg< VolatileLongArrayType, VolatileLongArray > img = prepareCachedImage( timepointId, setupId, level, LoadingStrategy.VOLATILE );
		final VolatileLongArrayType linkedType = new VolatileLongArrayType( img, arrayLength );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public CacheControl getCacheControl()
	{
		return cache;
	}
}
