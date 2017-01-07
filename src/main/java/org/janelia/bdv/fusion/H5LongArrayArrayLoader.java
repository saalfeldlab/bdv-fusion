package org.janelia.bdv.fusion;

import java.util.Arrays;

import bdv.img.cache.CacheArrayLoader;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.hdf5.IHDF5LongReader;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;

/**
 * {@link CacheArrayLoader} for
 * Jan Funke's and other's h5 files
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class H5LongArrayArrayLoader implements CacheArrayLoader< VolatileLongArray >
{
	private VolatileLongArray theEmptyArray;

	final private IHDF5LongReader reader;

	final private String dataset;

	final int arrayLength;

	public H5LongArrayArrayLoader(
			final IHDF5Reader reader,
			final String dataset )
	{
		theEmptyArray = new VolatileLongArray( 1, false );
		this.reader = reader.int64();
		this.dataset = dataset;

		final long[] dimensions = reader.object().getDimensions( dataset );
		arrayLength = ( int )dimensions[ dimensions.length - 1 ];
	}

	@Override
	public int getBytesPerElement()
	{
		return 8 * arrayLength;
	}


	@Override
	public VolatileLongArray loadArray(
			final int timepoint,
			final int setup,
			final int level,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		long[] data = null;
		final MDLongArray slice = reader.readMDArrayBlockWithOffset(
				dataset,
				new int[]{ dimensions[ 2 ], dimensions[ 1 ], dimensions[ 0 ], arrayLength },
				new long[]{ min[ 2 ], min[ 1 ], min[ 0 ], 0 } );

		data = slice.getAsFlatArray();

		if ( data == null )
		{
			System.out.println(
					"H5 byte array loader failed loading min = " +
					Arrays.toString( min ) +
					", dimensions = " +
					Arrays.toString( dimensions ) );

			data = new long[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] * arrayLength ];
		}

		return new VolatileLongArray( data, true );
	}

	/**
	 * Reuses the existing empty array if it already has the desired size.
	 */
	@Override
	public VolatileLongArray emptyArray( final int[] dimensions )
	{
		int numEntities = arrayLength;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileLongArray( numEntities, false );
		return theEmptyArray;
	}
}
