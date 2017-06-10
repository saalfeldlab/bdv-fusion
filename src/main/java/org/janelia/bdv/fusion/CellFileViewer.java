package org.janelia.bdv.fusion;

import static bdv.bigcat.CombinedImgLoader.SetupIdAndLoader.setupIdAndLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import bdv.BigDataViewer;
import bdv.bigcat.CombinedImgLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import fiji.util.gui.GenericDialogPlus;
import ij.ImageJ;
import ij.plugin.PlugIn;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
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

	final public static void main( final String... args )
	{
		new ImageJ();
		
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
		
		final TriggerBehaviourBindings bindings = bdv.getViewerFrame().getTriggerbindings();
		final InputTriggerConfig config = getInputTriggerConfig();

		final CropController cropController = new CropController(
					bdv.getViewer(),
					metaDatas,
					config,
					bdv.getViewerFrame().getKeybindings(),
					config );
		
		bindings.addBehaviourMap( "crop", cropController.getBehaviourMap() );
		bindings.addInputTriggerMap( "crop", cropController.getInputTriggerMap() );
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
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< >();

		BigDataViewer.initSetups( spimData, converterSetups, sources );

		final int[] predefinedColors = new int[] {
				ARGBType.rgba( 0xff, 0, 0xff, 0xff ),
				ARGBType.rgba( 0, 0xff, 0, 0xff ),
				ARGBType.rgba( 0, 0, 0xff, 0xff ),
				ARGBType.rgba( 0xff, 0, 0, 0xff ),
				ARGBType.rgba( 0xff, 0xff, 0, 0xff ),
				ARGBType.rgba( 0, 0xff, 0xff, 0xff ),
		};

		final Random rnd = new Random();
		for ( final ConverterSetup converterSetup : converterSetups )
		{
			final int i = converterSetup.getSetupId();

			if ( converterSetups.size() > 1 )
			{
				if ( i < predefinedColors.length )
					converterSetup.setColor( new ARGBType( predefinedColors[ i ] ) );
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
			if ( ( int ) metaDatas[ i ].getDisplayRangeMin() == 0 && ( int ) metaDatas[ i ].getDisplayRangeMax() == 65535 )
			{
				final boolean isDeconExport = metaDatas[ i ].getUrlFormat().indexOf( "decon" ) != -1;
				converterSetup.setDisplayRange( 0, isDeconExport ? 3000 : 300 );
			}
			else
			{
				converterSetup.setDisplayRange( metaDatas[ i ].getDisplayRangeMin(), metaDatas[ i ].getDisplayRangeMax() );
			}
			bdv.getSetupAssignments().removeSetupFromGroup( converterSetup, bdv.getSetupAssignments().getMinMaxGroups().get( 0 ) );
		}

		// put the camera in the middle of the sample at a scale that captures the entire volume
		final long[] imageSize = metaDatas[ 0 ].getImageDimensions()[ 0 ];
		final int[] viewerFrameSize = new int[] { bdv.getViewer().getWidth(), bdv.getViewer().getHeight() };

		// translation to the middle
		final double[] normalizedVoxelDimensions = CellFileImageMetaData.normalizeVoxelDimensions( metaDatas[ 0 ].getVoxelDimensions() );
		final AffineTransform3D viewerTranslateTransform = new AffineTransform3D();
		final double[] viewerTranslation = new double[ viewerTranslateTransform.numDimensions() ];
		for ( int d = 0; d < viewerTranslateTransform.numDimensions(); ++d )
			viewerTranslation[ d ] = -imageSize[ d ] / 2. * normalizedVoxelDimensions[ d ];
		viewerTranslateTransform.setTranslation( viewerTranslation );

		// additional translation due to viewer frame size
		final AffineTransform3D viewerFrameTranslateTransform = new AffineTransform3D();
		viewerFrameTranslateTransform.setTranslation( new double[] { viewerFrameSize[ 0 ] / 2., viewerFrameSize[ 1 ] / 2., 0 } );

		// scaling
		final double scalingFactor = Math.min( ( double ) viewerFrameSize[ 0 ] / imageSize[ 0 ], ( double ) viewerFrameSize[ 1 ] / imageSize[ 1 ] );
		final AffineTransform3D viewerScaleTransform = new AffineTransform3D();
		for ( int d = 0; d < viewerScaleTransform.numDimensions(); ++d )
			viewerScaleTransform.set( scalingFactor, d, d );

		// apply the transform
		bdv.getViewer().setCurrentViewerTransform(  viewerTranslateTransform.preConcatenate( viewerScaleTransform.preConcatenate( viewerFrameTranslateTransform ) ) );

		bdv.getViewer().setDisplayMode( DisplayMode.FUSED );

		return bdv;
	}
	
	static protected InputTriggerConfig getInputTriggerConfig() throws IllegalArgumentException
	{
		final String[] filenames = { "bigcatkeyconfig.yaml", System.getProperty( "user.home" ) + "/.bdv/bigcatkeyconfig.yaml" };

		for ( final String filename : filenames )
		{
			try
			{
				if ( new File( filename ).isFile() )
				{
					System.out.println( "reading key config from file " + filename );
					return new InputTriggerConfig( YamlConfigIO.read( filename ) );
				}
			}
			catch ( final IOException e )
			{
				System.err.println( "Error reading " + filename );
			}
		}

		System.out.println( "creating default input trigger config" );

		// default input trigger config, disables "control button1" drag in bdv
		// (collides with default of "move annotation")
		final InputTriggerConfig config =
				new InputTriggerConfig(
						Arrays.asList(
								new InputTriggerDescription[] { new InputTriggerDescription( new String[] { "not mapped" }, "drag rotate slow", "bdv" ) } ) );

		return config;
	}

}
