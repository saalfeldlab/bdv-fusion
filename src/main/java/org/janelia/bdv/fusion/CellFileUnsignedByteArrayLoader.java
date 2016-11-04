/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.janelia.bdv.fusion;

import java.io.File;
import java.io.IOException;

import bdv.img.cache.CacheArrayLoader;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class CellFileUnsignedByteArrayLoader implements CacheArrayLoader< VolatileByteArray >
{
	private VolatileByteArray theEmptyArray;

	private final String cellFormat;
	
	private int[][] cellSizes;

	/**
	 * <p>Create a {@link CacheArrayLoader} for a file per cell source.
	 * Cells are addressed, in this order, by their</p>
	 * <ol>
	 * <li>scale level,</li>
	 * <li>column (cell grid coordinates),</li>
	 * <li>row (cell grid coordinates),</li>
	 * <li>slice (cell grid coordinates),</li>
	 * <li>x (left pixel coordinates of the cell),</li>
	 * <li>y (top pixel coordinates of the cell),</li>
	 * <li>z (front pixel coordinates of the cell)</li>
	 * </ol>
	 * <p><code>urlFormat</code> specifies how these parameters are used
	 * to generate a URL referencing the tile.  Examples:</p>
	 *
	 * <dl>
	 * <dd>Stitching export version 0</dd>
	 * <dt>"/home/saalfeld/test/channel1/%1$d/%7$d/%6$d/%5$d.tif"</dt>
     * <dd>Stitching export version 1</dd>
	 * <dt>"/home/saalfeld/test/channel1/%1$d/%4$d/%3$d/%2$d.tif"</dt>
     * </dl>
	 *
	 * @param cellFormat
	 */
	public CellFileUnsignedByteArrayLoader( final String cellFormat, final int[][] cellSizes )
	{
		theEmptyArray = new VolatileByteArray( 1, false );
		this.cellFormat = cellFormat;
		this.cellSizes = cellSizes;
	}

	@Override
	public int getBytesPerElement()
	{
		return 1;
	}

	@Override
	final public VolatileByteArray loadArray(
			final int timepoint,
			final int setup,
			final int level,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		final int[] cellSize = cellSizes[ level ];
		
		final String cellString =
				String.format(
						cellFormat,
						level,
						min[ 0 ] / cellSize[ 0 ],
						min[ 1 ] / cellSize[ 1 ],
						min[ 2 ] / cellSize[ 2 ],
						min[ 0 ],
						min[ 1 ],
						min[ 2 ] );
		
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		final byte[] data = new byte[ numEntities ];
		
		try
		{
			final File file = new File( cellString );
			if ( !file.exists() )
				throw new IOException( "file does not exist" );
			
			final ImagePlus imp = IJ.openImage( cellString );
			
			if ( imp == null )
				throw new IOException( "imp == null" );
			
			if ( imp.getType() != ImagePlus.GRAY8 )
				new ImageConverter( imp ).convertToGray8();
			
			final long[] longDimensions = new long[]{
					dimensions[ 0 ],
					dimensions[ 1 ],
					dimensions[ 2 ] };
			
			final ArrayImg< UnsignedByteType, ByteArray > target =
					ArrayImgs.unsignedBytes(
							data,
							longDimensions );
			
			@SuppressWarnings( "unchecked" )
			final ImagePlusImg< UnsignedByteType, ByteArray > impSource =
					( ImagePlusImg< UnsignedByteType, ByteArray > )( Object )ImagePlusImgs.from( imp );
			
			final RandomAccessibleInterval< UnsignedByteType > source =
					Views.interval(
							Views.extendZero( impSource ),
							new FinalInterval( longDimensions ) );
			
			for ( final Pair< UnsignedByteType, UnsignedByteType > pair : Views.interval( Views.pair( source, target ), target ) )
				pair.getB().set( pair.getA() );
		}
		catch ( final IOException e ) {
//			System.out.println( "failed loading tile " + cellString );
		}
		
		return new VolatileByteArray( data, true );
	}


	@Override
	public VolatileByteArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileByteArray( numEntities, false );
		return theEmptyArray;
	}
}
