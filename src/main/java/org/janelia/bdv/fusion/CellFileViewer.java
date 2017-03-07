package org.janelia.bdv.fusion;

import static bdv.bigcat.CombinedImgLoader.SetupIdAndLoader.setupIdAndLoader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import bdv.BigDataViewer;
import bdv.bigcat.CombinedImgLoader;
import bdv.img.WarpedSource;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import fiji.util.gui.GenericDialogPlus;
import ij.plugin.PlugIn;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.type.numeric.ARGBType;

/**
 * {@link BigDataViewer}-based application for exploring large datasets that are cut into tiles.
 * Takes a path to a fusion export configuration file as a command line argument
 * or via Fiji's Plugins &gt; BigDataViewer &gt; Cell File Viewer.
 *
 * Blending is supported for multiple channels defined in the configuration file.
 *
 * @author Igor Pisarev
 */

public class CellFileViewer implements PlugIn
{
	protected static String jsonPath = "";

	static final int[] PREDEFINED_COLORS = new int[] {
			ARGBType.rgba( 0xff, 0, 0xff, 0xff ),
			ARGBType.rgba( 0, 0xff, 0, 0xff ),
			ARGBType.rgba( 0, 0, 0xff, 0xff ),
			ARGBType.rgba( 0xff, 0, 0, 0xff ),
			ARGBType.rgba( 0xff, 0xff, 0, 0xff ),
			ARGBType.rgba( 0, 0xff, 0xff, 0xff ),
	};

	final public static void main( final String... args )
	{
		exec( args[ 0 ] );
	}

	@Override
	public void run( final String args )
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Cell File Viewer" );
		gd.addFileField( "JSON_File: ", jsonPath);
		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		jsonPath = gd.getNextString();

		exec( jsonPath );
	}

	final public static void exec( final String jsonPath )
	{
		final Gson gson = new Gson();

		final CellFileImageMetaData[] metaDatas;
		try
		{
			metaDatas = gson.fromJson( new FileReader( jsonPath ), CellFileImageMetaData[].class );
		}
		catch ( JsonSyntaxException | JsonIOException | FileNotFoundException e )
		{
			e.printStackTrace();
			return;
		}

		final BigDataViewer bdv = createViewer( Paths.get( jsonPath ).getFileName() + " - Cell File Viewer", metaDatas );
		bdv.getViewerFrame().setVisible( true );
	}

	private static BigDataViewer createViewer(
			final String windowTitle,
			final CellFileImageMetaData[] metaDatas )
	{
		final ArrayList< AbstractCellFileImageLoader< ?, ? > > imgLoaders = new ArrayList<>();
		for ( final CellFileImageMetaData metaData : metaDatas )
			imgLoaders.add( CellFileImageLoaderFactory.createImageLoader( metaData ) );

		final ArrayList< CombinedImgLoader.SetupIdAndLoader > loaders = new ArrayList<>();
		for ( int i = 0; i < imgLoaders.size(); i++ )
			loaders.add( setupIdAndLoader( i, imgLoaders.get( i ) ) );
		final CombinedImgLoader combinedImgLoader = new CombinedImgLoader( loaders.toArray( new CombinedImgLoader.SetupIdAndLoader[ 0 ] ) );

		final ArrayList< TimePoint > timePointsList = new ArrayList< >();
		timePointsList.add( new TimePoint( 0 ) );

		final Map< Integer, BasicViewSetup > setups = new HashMap< >();
		for ( int i = 0; i < loaders.size(); i++ )
		{
			final CombinedImgLoader.SetupIdAndLoader loader  = loaders.get( i );
			final VoxelDimensions voxelDimensions = metaDatas[ i ].getVoxelDimensions();
			setups.put( loader.setupId, new BasicViewSetup( loader.setupId, null, null, voxelDimensions ) );
		}

		final ArrayList< ViewRegistration > viewRegistrationsList = new ArrayList< >();
		for ( int i = 0; i < loaders.size(); i++ )
		{
			final CombinedImgLoader.SetupIdAndLoader loader  = loaders.get( i );
			final AffineTransform3D calibrationTransform = metaDatas[ i ].getTransform();
			final ViewRegistration viewRegistration = new ViewRegistration( 0, loader.setupId );
			viewRegistration.preconcatenateTransform( new ViewTransformAffine( "calibration", calibrationTransform ));
			viewRegistrationsList.add( viewRegistration );
		}

		final TimePoints timepoints = new TimePoints( timePointsList );
		final ViewRegistrations reg = new ViewRegistrations( viewRegistrationsList );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( timepoints, setups, combinedImgLoader, null );
		final SpimDataMinimal spimData = new SpimDataMinimal( null, seq, reg );

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< >();
		final ArrayList< SourceAndConverter< ? > > sourcesTmp = new ArrayList< >();

		BigDataViewer.initSetups( spimData, converterSetups, sourcesTmp );
		ArrayList< SourceAndConverter< ? > > sources = addPostTransforms( metaDatas, sourcesTmp );


		final Random rnd = new Random();
		for ( final ConverterSetup converterSetup : converterSetups )
		{
			final int i = converterSetup.getSetupId();

			if ( converterSetups.size() > 1 )
			{
				if ( i < PREDEFINED_COLORS.length )
					converterSetup.setColor( new ARGBType( PREDEFINED_COLORS[ i ] ) );
				else
					converterSetup.setColor( new ARGBType( ARGBType.rgba( rnd.nextInt( 1 << 7) << 1, rnd.nextInt( 1 << 7) << 1, rnd.nextInt( 1 << 7) << 1, 0xff ) ) );
			}
		}

		final ViewerOptions options = ViewerOptions.options()
				.numRenderingThreads( 16 )
				.targetRenderNanos( 10000000 );

		final BigDataViewer bdv = new BigDataViewer(
				converterSetups,
				sources,
				null,
				timepoints.size(),
				combinedImgLoader.getCacheControl(),
				windowTitle,
				null,
				options );

		// FIXME: Create separate min-max group for every channel after BDV initialization
		// If specified before calling BDV constructor, it ignores this and uses display range of the first setup
		for ( final ConverterSetup converterSetup : converterSetups )
		{
			final int i = converterSetup.getSetupId();
			converterSetup.setDisplayRange( metaDatas[ i ].getDisplayRangeMin(), metaDatas[ i ].getDisplayRangeMax() );
			bdv.getSetupAssignments().removeSetupFromGroup( converterSetup, bdv.getSetupAssignments().getMinMaxGroups().get( 0 ) );
		}

		bdv.getViewer().setDisplayMode( DisplayMode.FUSED );

		return bdv;
	}

	public static ArrayList< SourceAndConverter< ? > > addPostTransforms( CellFileImageMetaData[] metalist, ArrayList< SourceAndConverter< ? > >  sources )
	{
		final ArrayList< SourceAndConverter< ? > > sourcesTransformed = new ArrayList< >();

		for( int i = 0; i < sources.size(); i++ )
		{
			CellFileImageMetaData meta = metalist[ i ];
			SourceAndConverter< ? > src = sources.get( i );

			if( meta.getPostTransform() != null )
			{
				InverseRealTransform iXfm = new InverseRealTransform(  meta.getPostTransform() );
				sourcesTransformed.add( wrapSourceAsTransformed( src, iXfm ));
			}
			else
			{
				sourcesTransformed.add ( src );
			}
		}

		return sourcesTransformed;
	}

	public static < T > SourceAndConverter< T > wrapSourceAsTransformed( 
			final SourceAndConverter< T > src,
			InverseRealTransform irXfm )
	{
		String warpedName = src.getSpimSource().getName() + "_warped";
		WarpedSource<T> warpedSource = new WarpedSource<T>( src.getSpimSource(), warpedName );
		warpedSource.updateTransform( irXfm );
		warpedSource.setIsTransformed( true );

		if ( src.asVolatile() == null )
		{
			return new SourceAndConverter< T >( warpedSource, src.getConverter(), null );
		}
		else
		{
			return new SourceAndConverter< T >( warpedSource, src.getConverter(), 
					wrapSourceAsTransformed( src.asVolatile(), irXfm ) );
		}
	}
}
