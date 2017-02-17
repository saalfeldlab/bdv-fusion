package bigwarp;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import bdv.export.ProgressWriterConsole;
import bdv.img.WarpedSource;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.type.numeric.ARGBType;

public class BigwarpCFV_DefTarget_to12z
{

	public static BigWarpData genBigWarpData( 
			final String jsonMoving, 
			final String jsonFixedTarget,
			final String jsonFixedMoving,
			final String csvLandmarks )
	{
		AbstractSpimData<?> dataMoving = BigwarpCFV.spimDataFromJson( jsonMoving );
		AbstractSpimData<?> dataFMoving= BigwarpCFV.spimDataFromJson( jsonFixedMoving );
		AbstractSpimData<?> dataFTarget = BigwarpCFV.spimDataFromJson( jsonFixedTarget );
		
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		
		try {
			ltm.load( new File( csvLandmarks ));
		} catch (IOException e) {		
			e.printStackTrace();
			return null;
		}

		ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();
		BigWarpData datatmp = BigWarpInit.createBigWarpData( 
				new AbstractSpimData<?>[]{ dataMoving },
				new AbstractSpimData<?>[]{ dataFMoving, dataFTarget });
		
		ArrayList<SourceAndConverter<?>> sourcestmp = datatmp.sources;
		ArrayList<SourceAndConverter<?>> sources = new ArrayList<SourceAndConverter<?>>();
		
		for( int i = 0; i < sourcestmp.size(); i++ )
		{
			System.out.println( "sources: " + sourcestmp.get( i ) );
			if( i == 1 )
			{
				sources.add( wrapSourceAsTransformed( sourcestmp.get( i ), xfm ));
			}
			else
			{
				sources.add( sourcestmp.get( i ));
			}
		}

		BigWarpData dataout = new BigWarpData( 
				sources,
				datatmp.seqP, datatmp.seqQ, 
				datatmp.converterSetups, 
				datatmp.movingSourceIndices, datatmp.targetSourceIndices );

		return dataout;
	}

	private static < T > SourceAndConverter< T > wrapSourceAsTransformed( 
			final SourceAndConverter< T > src,
			ThinPlateR2LogRSplineKernelTransform xfm )
	{
		String warpedName = src.getSpimSource().getName() + "_warped";
		WarpedSource<T> warpedSource = new WarpedSource<T>( src.getSpimSource(), warpedName );
		warpedSource.updateTransform( xfm );
		warpedSource.setIsTransformed( true );

		if ( src.asVolatile() == null )
		{
			return new SourceAndConverter< T >( warpedSource, src.getConverter(), null );
		}
		else
		{
			return new SourceAndConverter< T >( warpedSource, src.getConverter(), 
					wrapSourceAsTransformed( src.asVolatile(), xfm ) );
		}
	}

	public static void main(String[] args)
	{
	
			String fnP = "";
			String fnQ = "";
			String fnQm = "";
			String fnQmLM = "";
			String fnLandmarks = "";

			int i = 0;
			if ( args.length >= 4 )
			{
				fnP = args[ i++ ];
				fnQm = args[ i++ ];
				fnQmLM = args[ i++ ];
				fnQ = args[ i++ ];
			}
			else
			{
				System.err.println( "Must provide at least 2 inputs for moving and target image files" );
				System.exit( 1 );
			}
			
			System.out.println( "fnP " + fnP );
			System.out.println( "fnQm " + fnQm  );
			System.out.println( "fnQmLM " + fnQmLM );
			System.out.println( "fnQ " + fnQ );

			if ( args.length > i )
				fnLandmarks = args[ i++ ];

			System.out.println( "fnLandmarks " + fnLandmarks );

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
					bw = new BigWarp( genBigWarpData( fnP, fnQ, fnQm, fnQmLM ), new File( fnP ).getName(), new ProgressWriterConsole() );
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
				bw.getSetupAssignments().getMinMaxGroups().get( 2 ).setRange( 80, 200 );
				
				bw.getSetupAssignments().getConverterSetups().get( 0 ).setColor( new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) ));
				bw.getSetupAssignments().getConverterSetups().get( 1 ).setColor( new ARGBType( ARGBType.rgba(   0, 255, 0, 255 ) ));
				bw.getSetupAssignments().getConverterSetups().get( 2 ).setColor( new ARGBType( ARGBType.rgba(   0, 255, 0, 255 ) ));
				
//				bw.getSetupAssignments().getConverterSetups().get( 2 ).setColor( new ARGBType( ARGBType.rgba(   0,  0, 255, 255 ) ));

				bw.setSpotColor( new Color( 255, 255, 0, 255 ) ); // YELLOW
			}
			catch ( final Exception e )
			{

				e.printStackTrace();
			}

			System.out.println( "done" );
	
	}

}
