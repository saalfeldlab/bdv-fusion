package org.janelia.bdv.fusion;

import java.io.IOException;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.cache.CacheHints;
import bdv.cache.LoadingStrategy;
import bdv.img.SetCache;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.realtransform.AffineTransform3D;
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
	extends AbstractViewerSetupImgLoader< LongArrayType, VolatileLongArrayType >
	implements ViewerImgLoader, SetCache
{
	final protected double[] resolution;

	final protected long[] dimension;

	final protected int[] blockDimension;

	final protected int arrayLength;

	final protected AffineTransform3D mipmapTransform;

	protected VolatileGlobalCellCache cache;

	final protected CacheArrayLoader< VolatileLongArray > loader;

	final protected int setupId;

	final static protected double[] readResolution( final IHDF5Reader reader, final String dataset )
	{
		final double[] resolution;
		if ( reader.object().hasAttribute( dataset, "resolution" ) )
		{
			final double[] h5res = reader.float64().getArrayAttr( dataset, "resolution" );
			resolution = new double[] { h5res[ 2 ], h5res[ 1 ], h5res[ 0 ], };
		}
		else
			resolution = new double[] { 1, 1, 1 };

		return resolution;
	}

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
				createLongArrayType( reader, dataset ),
				createVolatileLongArrayType( reader, dataset ) );

		this.setupId = setupId;

		this.resolution = readResolution( reader, dataset );

		final long[] h5dim = reader.object().getDimensions( dataset );

		dimension = new long[]{
				h5dim[ 2 ],
				h5dim[ 1 ],
				h5dim[ 0 ] };

		arrayLength = ( int )h5dim[ 3 ];

		mipmapTransform = new AffineTransform3D();

		mipmapTransform.set( resolution[ 0 ], 0, 0 );
		mipmapTransform.set( resolution[ 1 ], 1, 1 );
		mipmapTransform.set( resolution[ 2 ], 2, 2 );

		this.blockDimension = blockDimension;

		this.loader = new H5LongArrayArrayLoader( reader, dataset );

		cache = new VolatileGlobalCellCache( 1, 10 );
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return new double[][]{ resolution };
	}

	@Override
	public int numMipmapLevels()
	{
		return 1;
	}

	protected < S extends NativeType< S > > CachedCellImg< S, VolatileLongArray > prepareCachedImage(
			final int timepointId,
			@SuppressWarnings( "hiding" ) final int setupId,
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
	public AffineTransform3D[] getMipmapTransforms()
	{
		return new AffineTransform3D[]{ mipmapTransform };
	}

	@Override
	public void setCache( final VolatileGlobalCellCache cache )
	{
		this.cache = cache;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return this;
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
