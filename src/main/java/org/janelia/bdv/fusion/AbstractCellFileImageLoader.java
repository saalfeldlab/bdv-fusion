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

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.AbstractVolatileArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.util.Fraction;

abstract public class AbstractCellFileImageLoader< T extends NativeType< T >, V extends Volatile< T > > extends AbstractViewerSetupImgLoader< T, V > implements ViewerImgLoader
{
	protected final double[][] mipmapResolutions;

	protected final AffineTransform3D[] mipmapTransforms;

	protected final long[][] dimensions;

	protected final int[][] cellDimensions;

	protected VolatileGlobalCellCache cache;

	public AbstractCellFileImageLoader(
			final String cellFormat,
			final long[][] dimensions,
			final int[][] cellDimensions,
			final T t,
			final V v )
	{
		super( t, v );
		
		this.dimensions = dimensions;
		this.cellDimensions = cellDimensions;
		
		final int numScales = dimensions.length;
		mipmapTransforms = new AffineTransform3D[ numScales ];
		mipmapResolutions = new double[ numScales ][];
		for ( int i = 0; i < numScales; ++i )
		{
			final int si = 1 << i;
			
			mipmapResolutions[ i ] = new double[] { si, si, si };
			
			final AffineTransform3D mipmapTransform = new AffineTransform3D();

			mipmapTransform.set( si, 0, 0 );
			mipmapTransform.set( si, 1, 1 );
			mipmapTransform.set( si, 2, 2 );

			final double mipmapOffset = 0.5 * ( si - 1 );
			mipmapTransform.set( mipmapOffset, 0, 3 );
			mipmapTransform.set( mipmapOffset, 1, 3 );
			mipmapTransform.set( mipmapOffset, 2, 3 );

			mipmapTransforms[ i ] = mipmapTransform;
		}

		cache = new VolatileGlobalCellCache( 1, 1, numScales, 10 );
	}


	final static public int getNumScales( long width, long height, final long tileWidth, final long tileHeight )
	{
		int i = 1;

		while ( ( width >>= 1 ) > tileWidth && ( height >>= 1 ) > tileHeight )
			++i;

		return i;
	}

	@Override
	public VolatileGlobalCellCache getCache()
	{
		return cache;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return this;
	}
	
	/**
	 * (Almost) create a {@link CachedCellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link FloatType} and {@link VolatileFloatType}.
	 */
	protected < N extends NativeType< N >, A extends AbstractVolatileArray< A > > CachedCellImg< N, A > prepareCachedImage(
			final CacheArrayLoader< A > loader,
			final int timepointId,
			final int setupId,
			final int level,
			final LoadingStrategy loadingStrategy )
	{
		final long[] levelDimensions = dimensions[ level ];

		final int priority = dimensions.length - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< A > c = cache.new VolatileCellCache< A >( timepointId, setupId, level, cacheHints, loader );
		final VolatileImgCells< A > cells = new VolatileImgCells<>( c, new Fraction(), levelDimensions, cellDimensions[ level ] );
		final CachedCellImg< N, A > img = new CachedCellImg<>( cells );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return mipmapResolutions;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return mipmapTransforms;
	}

	@Override
	public int numMipmapLevels()
	{
		return dimensions.length;
	}
	
	public void setCache( final VolatileGlobalCellCache cache )
	{
		this.cache = cache;
	}
}
