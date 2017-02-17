package bigwarp;

import static bdv.bigcat.CombinedImgLoader.SetupIdAndLoader.setupIdAndLoader;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.janelia.bdv.fusion.AbstractCellFileImageLoader;
import org.janelia.bdv.fusion.CellFileImageLoaderFactory;
import org.janelia.bdv.fusion.CellFileImageMetaData;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import bdv.BigDataViewer;
import bdv.bigcat.CombinedImgLoader;
import bdv.export.ProgressWriterConsole;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class BigwarpCFV
{

	public static BigWarpData genBigWarpData( final String jsonMoving, final String jsonFixed )
	{
		return BigWarpInit.createBigWarpData( 
				spimDataFromJson( jsonMoving, 0 ), 
				spimDataFromJson( jsonFixed, 512 ) );
	}
	
	public static AbstractSpimData< ? > spimDataFromJson( final String jsonPath )
	{
		return spimDataFromJson( jsonPath, 0 );
	}
	
	public static AbstractSpimData< ? > spimDataFromJson( final String jsonPath, int startingId )
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
			return null;
		}
		
		final ArrayList< AbstractCellFileImageLoader< ?, ? > > imgLoaders = new ArrayList<>();
		for ( final CellFileImageMetaData metaData : metaDatas )
		{
			imgLoaders.add( CellFileImageLoaderFactory.createImageLoader( metaData ) );
		}

		
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
			System.out.println( "loader setupid: " + loader.setupId );
			final VoxelDimensions voxelDimensions = metaDatas[ i ].getVoxelDimensions();
//			setups.put( loader.setupId, new BasicViewSetup( loader.setupId, null, null, voxelDimensions ) );
			setups.put( startingId + i, new BasicViewSetup( loader.setupId, null, null, voxelDimensions ) );
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
		

		return spimData;
	}
	
	public static void main(String[] args)
	{
	
			String fnP = "";
			String fnQ = "";
			String fnLandmarks = "";

			int i = 0;
			if ( args.length >= 2 )
			{
				fnP = args[ i++ ];
				fnQ = args[ i++ ];
			}
			else
			{
				System.err.println( "Must provide at least 2 inputs for moving and target image files" );
				System.exit( 1 );
			}

			if ( args.length > i )
				fnLandmarks = args[ i++ ];

			boolean doInverse = false;
			if ( args.length > i )
			{
				try
				{
					doInverse = Boolean.parseBoolean( args[ i++ ] );
					System.out.println( "parsed inverse param: " + doInverse );
				}
				catch ( final Exception e )
				{
					// no inverse param
					System.err.println( "Warning: Failed to parse inverse parameter." );
				}
			}

			try
			{
				System.setProperty( "apple.laf.useScreenMenuBar", "false" );

				BigWarp bw;
				if ( fnP.endsWith( "json" ) && fnQ.endsWith( "json" ) )
					bw = new BigWarp( genBigWarpData( fnP, fnQ ), new File( fnP ).getName(), new ProgressWriterConsole() );
				else
				{
					System.err.println( "Error reading images, must be json images" );
					return;
				}

				if ( !fnLandmarks.isEmpty() )
					bw.landmarkModel.load( new File( fnLandmarks ) );

				if ( doInverse )
					bw.invertPointCorrespondences();
				
				bw.getSetupAssignments().getMinMaxGroups().get( 0 ).setRange( 80, 200 );
				bw.getSetupAssignments().getMinMaxGroups().get( 1 ).setRange( 80, 200 );
				
				bw.getSetupAssignments().getConverterSetups().get( 0 ).setColor( new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) ));
				bw.getSetupAssignments().getConverterSetups().get( 1 ).setColor( new ARGBType( ARGBType.rgba(   0, 255, 0, 255 ) ));
				 

				bw.setSpotColor( new Color( 255, 255, 0, 255 ) ); // YELLOW
			}
			catch ( final Exception e )
			{

				e.printStackTrace();
			}

			System.out.println( "done" );
	
	}

}
