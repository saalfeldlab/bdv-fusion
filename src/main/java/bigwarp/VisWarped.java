package bigwarp;

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

public class VisWarped
{

	final static ARGBType magenta =  new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ));
	final static ARGBType green = new ARGBType( ARGBType.rgba(   0, 255, 0, 255 ));
	
	public static void main(String[] args)
	{
		LinkedList<JsonXfmPair> pairList = genList();
		BigWarpData data = genData( pairList );

		try
		{
			BigWarp bw = new BigWarp( data, "VISALL", new ProgressWriterConsole() );
			
			// set everything to the same range
			for( MinMaxGroup mmGroup : bw.getSetupAssignments().getMinMaxGroups() )
			{
				mmGroup.setRange( 80, 250 );
			}
			
			// alternate colors
			boolean isGreen = false;
			for( ConverterSetup setup : bw.getSetupAssignments().getConverterSetups() )
			{
				if( isGreen )
				{
					setup.setColor( green );
				}
				else
				{
					setup.setColor( magenta );
				}
				isGreen = !isGreen;
			}
		} 
		catch (SpimDataException e)
		{
			e.printStackTrace();
		}

	}
	
	public static LinkedList<JsonXfmPair> genList()
	{
		LinkedList<JsonXfmPair> pairList = new LinkedList<JsonXfmPair>();
		
		// THE REFERENCE
		pairList.add( new JsonXfmPair(
					"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/12z-group1/ch0_pairwise_12z_group1-final-export.json",
					"none" ));

		// THE REFERENCE (PART 2)
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/12z-group0/ch0_pairwise_12z_group0-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_12z0-12z1_landmarks.csv" ));
		
		
		// INCREASING Z FROM THE REFERENCE
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/13z/ch0_pairwise_13z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_13-12.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/14z/ch0_pairwise_14z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_14-13.csv" ));

		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/15z/ch0_pairwise_15z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_15-14.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/16z/ch0_pairwise_16z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_16-15.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/17z/ch0_pairwise_17z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_17-16.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/18z/ch0_pairwise_18z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_18-17.csv" ));
		
		
		
		// DECREASING Z FROM THE REFERENCE
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/11z/ch0_pairwise_11z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_11-12.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/10z/ch0_pairwise_10z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_10-11.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/9z/ch0_pairwise_9z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_09-10.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/8z/ch0_pairwise_8z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_08-09.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/7z/ch0_pairwise_7z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_07-08-09.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/6z/ch0_pairwise_6z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_06.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/5z/ch0_pairwise_5z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_05.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/4z/ch0_pairwise_4z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_04.csv" ));

		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/3z/ch0_pairwise_3z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_03.csv" ));
		
		return pairList;
	}
	
	public static BigWarpData genData( LinkedList<JsonXfmPair> pairList )
	{
		int N = pairList.size();
		System.out.println( "N " + N );

//		LinkedList<AbstractSpimData<?>> dataList = new LinkedList<AbstractSpimData<?>>();
		AbstractSpimData<?>[] allData = new AbstractSpimData<?>[ pairList.size() ];

		AbstractSpimData<?>[] fixedData = new AbstractSpimData<?>[]
		{ 
			BigwarpCFV.spimDataFromJson( pairList.get( 0 ).json )
		};
		
		for( int i = 0; i < N; i++ )
		{
			JsonXfmPair pair = pairList.get( i );
//			dataList.add( BigwarpCFV.spimDataFromJson( pair.json ));
			allData[ i ] = BigwarpCFV.spimDataFromJson( pair.json );
		}

		
		System.out.println( "fixedData len: " + fixedData.length );
		System.out.println( "allData len: " + allData.length );
		
		BigWarpData datatmp = BigWarpInit.createBigWarpData( 
				allData, fixedData );
		
		ArrayList<SourceAndConverter<?>> sourcestmp = datatmp.sources;
		ArrayList<SourceAndConverter<?>> sources = new ArrayList<SourceAndConverter<?>>();
		
		for( int i = 0; i < sourcestmp.size(); i++ )
		{
			if( i >= pairList.size())
			{
				System.out.println( "adding unwarped" );
				sources.add( sourcestmp.get( i ));
				continue;
			}

			JsonXfmPair pair = pairList.get( i );
			if( !pair.xfmF.equals( "none" ))
			{
				System.out.println( "adding warped" );
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
