package org.janelia.bdv.fusion;

import static bdv.bigcat.CombinedImgLoader.SetupIdAndLoader.setupIdAndLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.janelia.stitching.Boundaries;
import org.janelia.stitching.TileInfo;
import org.janelia.stitching.TileInfoJSONProvider;
import org.janelia.stitching.TileOperations;

import bdv.BigDataViewer;
import bdv.ViewerSetupImgLoader;
import bdv.bigcat.CombinedImgLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

/**
 * {@link BigDataViewer}-based application for exploring large datasets that are cut into tiles.
 * Takes a path to a tile configuration file as a command line argument
 * and displays it performing on-the-fly fusion (simple copying).
 * It should be formatted as JSON array of {@link TileInfo} objects.
 *
 * For best performance, input tiles should have a size of 64x64x64 pixels and not overlap.
 *
 * You can pass an arbitrary number of input tile configurations and the images will be blended.
 * It has predefined colors for the first three images, and random colors will be generated for the rest in case you pass more.
 *
 * @author Igor Pisarev
 */

public class BDVFusion
{
	public static void main( final String... args ) throws FileNotFoundException, IOException
	{
		final double[] alignment = new double[] { -6, 1.5, -2 };

		final ArrayList< StitchingUnsignedShortLoader > loaders = new ArrayList<>();
		double[] offset = null;
		for ( final String arg : args )
		{
			final TileInfo[] tileInfos = TileInfoJSONProvider.loadTilesConfiguration( arg );
			double[] currOffset = null;

			if ( offset == null )
			{
				final Boundaries space = TileOperations.getCollectionBoundaries( tileInfos );
				System.out.println( "Space of the first configuration is " + Arrays.toString( space.getMin() ) );
				offset = new double[ space.numDimensions() ];
				for ( int d = 0; d < offset.length; d++ )
					offset[ d ] = -space.min( d );
				currOffset = offset;
			}
			else
			{
				currOffset = offset.clone();
				for ( int d = 0; d < offset.length; d++ )
					currOffset[ d ] += alignment[ d ];
			}

			TileOperations.translateTiles( tileInfos, currOffset );

			final StitchingUnsignedShortLoader loader = new StitchingUnsignedShortLoader( tileInfos, 0, new int[] { 64, 64, 64 } );
			loaders.add( loader );
		}

		/* this is how to save the whole thing into HDF5 */
		//			RandomAccessibleInterval< UnsignedShortType > img = loader.getImage( 0, ImgLoaderHints.LOAD_COMPLETELY );
		//			H5Utils.saveUnsignedLong( img, file, "/volumes/raw", new long[]{64, 64, 64} );

		final BigDataViewer bdv = createViewer( "StitchingViewer", loaders );

		bdv.getViewerFrame().setVisible( true );
	}

	public static < A extends ViewerSetupImgLoader< ? extends NumericType< ? >, ? > > BigDataViewer createViewer(
			final String windowTitle,
			final List< A > imgLoaders)
	{
		final ArrayList< CombinedImgLoader.SetupIdAndLoader > loaders = new ArrayList<>();
		for ( int i = 0; i < imgLoaders.size(); i++ )
			loaders.add( setupIdAndLoader( i, imgLoaders.get( i ) ) );
		final CombinedImgLoader combinedImgLoader = new CombinedImgLoader( loaders.toArray( new CombinedImgLoader.SetupIdAndLoader[ 0 ] ) );

		final ArrayList< TimePoint > timePointsList = new ArrayList< >();
		timePointsList.add( new TimePoint( 0 ) );

		final Map< Integer, BasicViewSetup > setups = new HashMap< >();
		for ( final CombinedImgLoader.SetupIdAndLoader loader : loaders )
			setups.put( loader.setupId, new BasicViewSetup( loader.setupId, null, null, null ) );

		final ArrayList< ViewRegistration > viewRegistrationsList = new ArrayList< >();
		for ( final CombinedImgLoader.SetupIdAndLoader loader : loaders )
			viewRegistrationsList.add( new ViewRegistration( 0, loader.setupId ) );

		final TimePoints timepoints = new TimePoints( timePointsList );
		final ViewRegistrations reg = new ViewRegistrations( viewRegistrationsList );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( timepoints, setups, combinedImgLoader, null );
		final SpimDataMinimal spimData = new SpimDataMinimal( null, seq, reg );

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< >();

		BigDataViewer.initSetups( spimData, converterSetups, sources );

		final int[] predefinedColors = new int[] {
				ARGBType.rgba( 1 << 7, 0, 0, 1 << 6 ),
				ARGBType.rgba( 0, 1 << 7, 0, 1 << 6 ),
				ARGBType.rgba( 0, 0, 1 << 7, 1 << 6 )
		};
		final Random rnd = new Random();
		for ( final ConverterSetup converterSetup : converterSetups )
		{
			final int i = converterSetup.getSetupId();
			converterSetup.setDisplayRange( 0, 1 << 8 );

			if ( converterSetups.size() > 1 )
			{
				if ( i < predefinedColors.length )
					converterSetup.setColor( new ARGBType( predefinedColors[ i ] ) );
				else
					converterSetup.setColor( new ARGBType( ARGBType.rgba( rnd.nextInt( 1 << 7) << 1, rnd.nextInt( 1 << 7) << 1, rnd.nextInt( 1 << 7) << 1, 1 << 6 ) ) );
			}
		}

		/* composites */
		/*final HashMap< Source< ? >, Composite< ARGBType, ARGBType > > sourceCompositesMap = new HashMap< >();
		for ( final SourceAndConverter< ? > source : sources )
			sourceCompositesMap.put( source.getSpimSource(), new CompositeCopy<>() );

		final AccumulateProjectorFactory< ARGBType > projectorFactory = new CompositeProjector.CompositeProjectorFactory< >( sourceCompositesMap );*/

		final ViewerOptions options = ViewerOptions.options()
				//.accumulateProjectorFactory( projectorFactory )
				.numRenderingThreads( 16 )
				.targetRenderNanos( 10000000 );

		final BigDataViewer bdv = new BigDataViewer( converterSetups, sources, null, timepoints.size(), combinedImgLoader.getCache(), windowTitle, null, options );

		bdv.getViewer().setDisplayMode( DisplayMode.FUSED );

		return bdv;
	}
}
