package org.janelia.bdv.fusion;

import static bdv.bigcat.CombinedImgLoader.SetupIdAndLoader.setupIdAndLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		for ( final ConverterSetup converterSetup : converterSetups )
		{
			final int i = converterSetup.getSetupId();
			converterSetup.setDisplayRange( 0, 1 << 8 );

			if ( converterSetups.size() > 1 )
				converterSetup.setColor( new ARGBType( ARGBType.rgba( i << 7, ( ( i + 1 ) % 2 ) << 7, 0, 1 << 6 ) ) );
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
