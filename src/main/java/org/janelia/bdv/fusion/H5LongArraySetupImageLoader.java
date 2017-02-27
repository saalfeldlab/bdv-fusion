package org.janelia.bdv.fusion;

import static bdv.img.hdf5.Util.reorder;

import java.io.File;
import java.io.IOException;

import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.cache.CacheHints;
import bdv.cache.LoadingStrategy;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.img.h5.AbstractH5SetupImageLoader;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5LongWriter;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.array.LongArrayType;
import net.imglib2.type.numeric.array.VolatileLongArrayType;
import net.imglib2.util.Fraction;
import net.imglib2.view.Views;

/**
 * {@link ViewerSetupImgLoader} for 3D volumes of LongArrayType stored as
 * 4D volumes (first/ slowest running dimension is the length of the array)
 * stored in HDF5 files.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
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

	@Override
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

	private void cropCellDimensions(
			final long[] offset,
			final long[] croppedCellDimensions )
	{
		for ( int d = 0; d < offset.length; ++d )
			croppedCellDimensions[ d ] = Math.min( blockDimension[ d ], dimension[ d ] - offset[ d ] );
	}

	/**
	 * Create an int64 dataset.
	 *
	 * @param writer
	 * @param dataset
	 */
	public void createDataset(
			final IHDF5Writer writer,
			final String dataset )
	{
		final IHDF5LongWriter int64Writer = writer.int64();

		if ( writer.exists( dataset ) )
			writer.delete( dataset );

		int64Writer.createMDArray(
				dataset,
				new long[]{ dimension[ 2 ], dimension[ 1 ], dimension[ 0 ], arrayLength },
				new int[]{ blockDimension[ 2 ], blockDimension[ 1 ], blockDimension[ 0 ], arrayLength },
				HDF5IntStorageFeatures.INT_AUTO_SCALING_DEFLATE );
	}


	/**
	 * Save as an int64 dataset.
	 *
	 * @param writer
	 * @param dataset
	 */
	public void save(
			final IHDF5Writer writer,
			final String dataset )
	{
		if ( !writer.exists( dataset ) )
			createDataset( writer, dataset );

		final long[] h5Dimensions = reorder( dimension );
		final int n = dimension.length;

		final IHDF5LongWriter int64Writer = writer.int64();

		final RandomAccessibleInterval< LongArrayType > img = getImage( 0 );

		final long[] offset = new long[ n ];
		final long[] offsetPlus = new long[ n + 1 ];
		final long[] sourceCellDimensions = new long[ n ];
		final long[] sourceCellDimensionsPlus = new long[ n + 1 ];
		sourceCellDimensionsPlus[ n ] = arrayLength;
		for ( int d = 0; d < n; )
		{
			cropCellDimensions( offset, sourceCellDimensions );
			final RandomAccessibleInterval< LongArrayType > sourceBlock = Views.offsetInterval( img, offset, sourceCellDimensions );

			System.arraycopy( reorder( sourceCellDimensions ), 0, sourceCellDimensionsPlus, 0, n );
			final MDLongArray targetCell = new MDLongArray( sourceCellDimensionsPlus );

			int i = 0;
			for ( final LongArrayType t : Views.flatIterable( sourceBlock ) )
				for ( int k = 0; k < arrayLength; ++k, ++i )
					targetCell.set( t.get( k ), i );

			int64Writer.writeMDArrayBlockWithOffset( dataset, targetCell, offsetPlus );

			for ( d = 0; d < n; ++d )
			{
				offset[ d ] += blockDimension[ d ];
				if ( offset[ d ] < dimension[ d ] )
					break;
				else
					offset[ d ] = 0;
			}

			System.arraycopy( reorder( offset ), 0, offsetPlus, 0, n );
		}
	}

	/**
	 * Save as an int64 dataset.
	 *
	 * @param file
	 * @param dataset
	 */
	public void save(
			final File file,
			final String dataset )
	{
		final IHDF5Writer writer = HDF5Factory.open( file );
		save( writer, dataset );
		writer.close();
	}

	/**
	 * Save as an int64 dataset.
	 *
	 * @param filePath
	 * @param dataset
	 */
	public void save(
			final String filePath,
			final String dataset )
	{
		save( new File( filePath ), dataset );
	}
}
