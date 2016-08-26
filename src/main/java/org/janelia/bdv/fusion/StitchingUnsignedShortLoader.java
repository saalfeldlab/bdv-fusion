package org.janelia.bdv.fusion;

import java.util.Arrays;

import org.janelia.stitching.Boundaries;
import org.janelia.stitching.TileOperations;
import org.janelia.stitching.TileInfo;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;

public class StitchingUnsignedShortLoader extends AbstractViewerSetupImgLoader< UnsignedShortType, VolatileUnsignedShortType > implements ViewerImgLoader
{
	final protected int setupId;

	final TileInfo[] tiles;

	final private double[] mipmapResolution;

	final private AffineTransform3D mipmapTransform;

	final private long[] imageDimension;

	final private int[] blockSize;

	final private VolatileGlobalCellCache cache;

	final protected StitchingVolatileShortArrayLoader arrayLoader;

	public StitchingUnsignedShortLoader(
			final TileInfo[] tiles,
			final int setupId,
			final int[] blockSize )
	{
		super( new UnsignedShortType(), new VolatileUnsignedShortType() );

		this.tiles = tiles;
		this.setupId = setupId;
		this.blockSize = blockSize;

		final Boundaries boundaries = TileOperations.getCollectionBoundaries( tiles );
		imageDimension = boundaries.getDimensions();

		mipmapResolution = new double[ imageDimension.length ];
		Arrays.fill( mipmapResolution, 1.0 );

		mipmapTransform = new AffineTransform3D();

		cache = new VolatileGlobalCellCache( 1, 1, 1, 10 );

		arrayLoader = new StitchingVolatileShortArrayLoader( tiles, boundaries.getMin() );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		final CachedCellImg< UnsignedShortType, VolatileShortArray >  img = prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray >  img = prepareCachedImage( timepointId, level, LoadingStrategy.VOLATILE );
		final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return this;
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return new double[][]{ mipmapResolution };
	}

	@Override
	public int numMipmapLevels()
	{
		return 1;
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileShortArray > prepareCachedImage(
			final int timepointId,
			final int level,
			final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimension;

		final int priority = 0;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileShortArray > c = cache.new VolatileCellCache< >( timepointId, setupId, level, cacheHints, arrayLoader );
		final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< >( c, new Fraction(), dimensions, blockSize );
		final CachedCellImg< T, VolatileShortArray > img = new CachedCellImg< >( cells );
		return img;
	}

	@Override
	public VolatileGlobalCellCache getCache()
	{
		return cache;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return new AffineTransform3D[]{ mipmapTransform };
	}
}
