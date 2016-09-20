package org.janelia.bdv.fusion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.janelia.stitching.Boundaries;
import org.janelia.stitching.TileInfo;
import org.janelia.stitching.TileOperations;
import org.janelia.stitching.Utils;

import bdv.img.cache.CacheArrayLoader;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class StitchingVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{
	private VolatileShortArray theEmptyArray;

	final private TileInfo[] tiles;

	final Set< Long > threads = new HashSet<>();

	public StitchingVolatileShortArrayLoader( final TileInfo[] tiles )
	{
		theEmptyArray = new VolatileShortArray( 1, false );
		this.tiles = tiles;
	}

	@Override
	public int getBytesPerElement()
	{
		return 2;
	}

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		synchronized ( threads )
		{
			if ( threads.add( Thread.currentThread().getId() ) )
				System.out.println( threads.size() + " active threads" );
		}

		//System.out.println( "Generating " + Arrays.toString( min ) );

		final long q = System.nanoTime();
		final ArrayList< TileInfo > boxTiles = TileOperations.findTilesWithinSubregion( tiles, min, dimensions );
		//System.out.println( "Searching tiles: " + ( ( System.nanoTime() - q ) / Math.pow( 10, 9 ) ) + "s" );

		final short[] data = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];

		final RandomAccessibleInterval< UnsignedShortType > cell =
				Views.translate(
						ArrayImgs.unsignedShorts(
								data,
								new long[] { dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ] } ),
						min );

		for ( final TileInfo tile : boxTiles )
		{
			//System.out.println( "Opening " + tile.getFilePath() );
			//q = System.nanoTime();
			final ImagePlus imp = IJ.openImage( tile.getFilePath() );
			Utils.workaroundImagePlusNSlices( imp );
			//System.out.println( "Loading tile: " + ( ( System.nanoTime() - q ) / Math.pow( 10, 9 ) ) + "s" );

			if ( imp != null )
			{
				final Boundaries tileBoundaries = tile.getBoundaries();
				final FinalInterval intersection = Intervals.intersect( new FinalInterval( tileBoundaries.getMin(), tileBoundaries.getMax() ), cell );

				@SuppressWarnings( "unchecked" )
				final RealRandomAccessible< UnsignedShortType > interpolatedTile = Views.interpolate( Views.extendBorder( ( RandomAccessibleInterval< UnsignedShortType > ) ImagePlusImgs.from( imp ) ), new NLinearInterpolatorFactory<>() );

				final Translation3D translation = new Translation3D( tile.getPosition() );
				final RandomAccessible< UnsignedShortType > translatedInterpolatedTile = RealViews.affine( interpolatedTile, translation );

				final IterableInterval< UnsignedShortType > tileSource = Views.flatIterable( Views.interval( translatedInterpolatedTile, intersection ) );

				final IterableInterval< UnsignedShortType > cellBox = Views.flatIterable( Views.interval( cell, intersection ) );

				final Cursor< UnsignedShortType > source = tileSource.cursor();
				final Cursor< UnsignedShortType > target = cellBox.cursor();

				while ( source.hasNext() )
					target.next().set( source.next() );
			}
		}

		//System.out.println( "Loading region took " + ( ( System.nanoTime() - q ) / Math.pow( 10, 9 ) ) + "s" );

		return new VolatileShortArray( data, true );
	}

	@Override
	public VolatileShortArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileShortArray( numEntities, false );
		return theEmptyArray;
	}
}
