package bigwarp;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import bdv.export.ProgressWriterConsole;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.type.numeric.ARGBType;

public class BigWarp_sec4
{

	final static ARGBType magenta =  new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ));
	final static ARGBType green = new ARGBType( ARGBType.rgba(   0, 255, 0, 255 ));
	final static ARGBType cyan = new ARGBType( ARGBType.rgba(   0, 255, 255, 255 ));
	
	public static void main(String[] args) throws IOException
	{
		LinkedList<JsonXfmPair> pairList = genFixedList();
		JsonXfmPair moving = genMoving();

//		String xfmF = "";
		String xfmF = "/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_04.csv";

		BigWarpData data = genData( moving, pairList );
		try
		{
			BigWarp bw = new BigWarp( data, "VISALL", new ProgressWriterConsole() );
			
			if ( !xfmF.isEmpty() )
				bw.landmarkModel.load( new File( xfmF ) );

			bw.setSpotColor( new Color( 255, 255, 0, 255 ) ); // YELLOW

			// set everything to the same range
			for( MinMaxGroup mmGroup : bw.getSetupAssignments().getMinMaxGroups() )
			{
				mmGroup.setRange( 80, 500 );
			}

			bw.getSetupAssignments().getConverterSetups().get( 0 ).setColor( magenta );
			bw.getSetupAssignments().getConverterSetups().get( 1 ).setColor( green );
			bw.getSetupAssignments().getConverterSetups().get( 2 ).setColor( cyan );
		} 
		catch (SpimDataException e)
		{
			e.printStackTrace();
		}

	}
	
	public static JsonXfmPair genMoving()
	{
		return new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/4z/ch0_pairwise_4z-final-export.json",
				"none" );	
	}

	public static LinkedList<JsonXfmPair> genFixedList()
	{
		LinkedList<JsonXfmPair> pairList = new LinkedList<JsonXfmPair>();

		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/5z/ch0_pairwise_5z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_05.csv" ));

		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/6z/ch0_pairwise_6z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_06.csv" ));

		return pairList;
	}
	
	public static BigWarpData genData( JsonXfmPair movingPair, LinkedList<JsonXfmPair> pairList )
	{
		int N = pairList.size();
		System.out.println( "N " + N );

//		LinkedList<AbstractSpimData<?>> dataList = new LinkedList<AbstractSpimData<?>>();


		AbstractSpimData<?>[] movingData = new AbstractSpimData<?>[]
		{ 
			BigwarpCFV.spimDataFromJson( movingPair.json )
		};


		AbstractSpimData<?>[] fixedData = new AbstractSpimData<?>[ pairList.size() ];
		for( int i = 0; i < N; i++ )
		{
			JsonXfmPair pair = pairList.get( i );
			fixedData[ i ] = BigwarpCFV.spimDataFromJson( pair.json );
		}

		
		System.out.println( "movingData len: " + movingData.length );
		System.out.println( "allData len: " + fixedData.length );
		
		BigWarpData datatmp = BigWarpInit.createBigWarpData( 
				movingData, fixedData);
		
		ArrayList<SourceAndConverter<?>> sourcestmp = datatmp.sources;
		ArrayList<SourceAndConverter<?>> sources = new ArrayList<SourceAndConverter<?>>();
		
		sources.add( sourcestmp.get( 0 ));

		for( int i = 1; i < sourcestmp.size(); i++ )
		{
			int pi = i - 1;
//			if( pi >= pairList.size())
//			{
//				System.out.println( "adding unwarped" );
//				sources.add( sourcestmp.get( i ));
//				continue;
//			}

			JsonXfmPair pair = pairList.get( pi );
			if( !pair.xfmF.equals( "none" ))
			{
				System.out.println( "adding warped: " );
				System.out.println( pair.xfmF );
				LandmarkTableModel ltm = new LandmarkTableModel( 3 );
				
				try
				{
					ltm.load( new File( pair.xfmF ));
				}
				catch (IOException e)
				{		
					e.printStackTrace();
					return null;
				}
				ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();
				sources.add( BigwarpCFV_DefTarget.wrapSourceAsTransformed( sourcestmp.get( i ), xfm ));
				
			}
			else
			{
				System.out.println( "adding unwarped" );
				sources.add( sourcestmp.get( i ));
			}
		}
		
		
		
		System.out.println( "sources len: " + sources.size() );
		
		BigWarpData dataout = new BigWarpData( 
				sources,
				datatmp.seqP, datatmp.seqQ, 
				datatmp.converterSetups, 
				datatmp.movingSourceIndices, datatmp.targetSourceIndices );

		return dataout;
	}
	
	public static class JsonXfmPair
	{
		final public String json;
		final public String xfmF;
		
		public JsonXfmPair( final String json, final String xfmF )
		{
			this.json = json;
			this.xfmF = xfmF;
		}
	}

}
