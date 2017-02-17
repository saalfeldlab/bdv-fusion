package bigwarp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.jdom2.JDOMException;

import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.bigcat.composite.ARGBCompositeMax;
import bdv.bigcat.composite.Composite;
import bdv.bigcat.composite.CompositeCopy;
import bdv.bigcat.composite.CompositeProjector;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.render.AccumulateProjectorFactory;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarp.BigWarpViewerOptions;
import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.type.numeric.ARGBType;

public class VisWarped_ch1_MAX_alternateColorsBySection
{

	final static ARGBType magenta =  new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ));
	final static ARGBType green = new ARGBType( ARGBType.rgba(   0, 255, 0, 255 ));
	
	public static void main(String[] args) throws IOException, JDOMException
	{
		LinkedList<JsonXfmPair> pairListCh0 = genCh0List();
		//LinkedList<JsonXfmPair> pairListCh1 = genCh1List();
		
		BigWarpData data = genData( pairListCh0 );

		/* composites */
		final Composite<ARGBType, ARGBType> composite = new ARGBCompositeMax();
		final HashMap< Source< ? >, Composite< ARGBType, ARGBType > > sourceCompositesMap = new HashMap< Source< ? >, Composite< ARGBType, ARGBType > >();
		for ( int i = 0; i < data.sources.size(); ++i )
		{
			sourceCompositesMap.put( data.sources.get( i ).getSpimSource(), i == 0 ? new CompositeCopy<>() :  new ARGBCompositeMax() );
		}
		final AccumulateProjectorFactory< ARGBType > projectorFactory = new CompositeProjector.CompositeProjectorFactory< ARGBType >( sourceCompositesMap );

		ViewerOptions options = ViewerOptions.options()
				.accumulateProjectorFactory( projectorFactory )
				.numRenderingThreads( 16 )
				.targetRenderNanos( 300000 );
		
		final BigDataViewer bdv = new BigDataViewer( data.converterSetups, data.sources, null, 1, 
				( ( ViewerImgLoader ) data.seqP.getImgLoader() ).getCacheControl(), "VISALL", null, options );
		bdv.getViewer().setDisplayMode( DisplayMode.FUSED );

		
		
		// set everything to the same range
		for( MinMaxGroup mmGroup : bdv.getSetupAssignments().getMinMaxGroups() )
		{
			mmGroup.setRange( 80, 250 );
		}
		
		
		// assign channel 0 to green and channel 1 to magenta
		int j = 0;
		boolean isGreen = true;
		for( ConverterSetup setup : bdv.getSetupAssignments().getConverterSetups() )
		{

			if( isGreen )
			{
				//bdv.getSetupAssignments().getMinMaxGroups().get( 0 ).addSetup( setup );
				setup.setColor( green );
			}
			else
			{
				//bdv.getSetupAssignments().getMinMaxGroups().get( 0 ).addSetup( setup );
				setup.setColor( magenta );
			}
			isGreen = !isGreen;
			j++;
		}
		
		
		bdv.getViewerFrame().setVisible( true );

	}
	
	public static LinkedList<JsonXfmPair> genCh1List()
	{
		LinkedList<JsonXfmPair> pairList = new LinkedList<JsonXfmPair>();

		// DECREASING Z FROM THE REFERENCE
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/3z/ch1_pairwise_3z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_03.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/4z/ch1_pairwise_4z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_04.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/5z/ch1_pairwise_5z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_05.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/6z/ch1_pairwise_6z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_06.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/7z/ch1_pairwise_7z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_07-08-09.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/8z/ch1_pairwise_8z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_08-09.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/9z/ch1_pairwise_9z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_09-10.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/10z/ch1_pairwise_10z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_10-11.csv" ));
		
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/11z/ch1_pairwise_11z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_11-12.csv" ));
		
		// THE REFERENCE
		pairList.add( new JsonXfmPair(
					"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/12z-group1/ch1_pairwise_12z_group1-final-export.json",
					"none" ));

		// THE REFERENCE (PART 2)
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/12z-group0/ch1_pairwise_12z_group0-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_12z0-12z1_landmarks.csv" ));
		
		
		// INCREASING Z FROM THE REFERENCE
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/13z/ch1_pairwise_13z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_13-12.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/14z/ch1_pairwise_14z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_14-13.csv" ));

		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/15z/ch1_pairwise_15z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_15-14.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/16z/ch1_pairwise_16z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_16-15.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/17z/ch1_pairwise_17z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_17-16.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch1-xy/18z/ch1_pairwise_18z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_18-17.csv" ));

		return pairList;
	}
	
	public static LinkedList<JsonXfmPair> genCh0List()
	{
		LinkedList<JsonXfmPair> pairList = new LinkedList<JsonXfmPair>();
		
		// DECREASING Z FROM THE REFERENCE
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/3z/ch0_pairwise_3z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_03.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/4z/ch0_pairwise_4z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_04.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/5z/ch0_pairwise_5z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_05.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/6z/ch0_pairwise_6z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_06.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/7z/ch0_pairwise_7z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_07-08-09.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/8z/ch0_pairwise_8z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_08-09.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/9z/ch0_pairwise_9z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_09-10.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/10z/ch0_pairwise_10z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_10-11.csv" ));
		
		pairList.add( new JsonXfmPair(
				"/nrs/saalfeld/igor/illumination-correction/Sample1_C1/stitching/ch0-xy/11z/ch0_pairwise_11z-final-export.json",
				"/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/landmarks/ch0_11-12.csv" ));
		
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

		return pairList;
	}
	
	public static AbstractSpimData<?>[] jsonToData( final LinkedList<JsonXfmPair> pairList )
	{
		int N = pairList.size();
		AbstractSpimData<?>[] allData = new AbstractSpimData<?>[ N ];

		for( int i = 0; i < N; i++ )
		{
			JsonXfmPair pair = pairList.get( i );
			allData[ i ] = BigwarpCFV.spimDataFromJson( pair.json );
		}
		
		return allData;
	}

	public static BigWarpData genData( 
			final LinkedList<JsonXfmPair> pairListMoving )
	{
		AbstractSpimData<?>[] movingData = jsonToData( pairListMoving );
		//AbstractSpimData<?>[] fixedData = jsonToData( pairListFixed );

		
		System.out.println( "movingData len: " + movingData.length );

		BigWarpData datatmp = BigWarpInit.createBigWarpData( 
				movingData, new AbstractSpimData<?>[]{ movingData[0] } );

		ArrayList<SourceAndConverter<?>> sourcestmp = datatmp.sources;
		ArrayList<SourceAndConverter<?>> sources = new ArrayList<SourceAndConverter<?>>();
		

		int i = 0;
		
		// deal with the moving images
		System.out.println("Moving images");
		for( int j = 0; j < pairListMoving.size(); j++ )
		{
			JsonXfmPair pair = pairListMoving.get( j );
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
			i++;
		}
		
		// deal with the fixed images
		System.out.println("Fixed images");
		for( int j = 0; j < 1; j++ )
		{
			JsonXfmPair pair = pairListMoving.get( j );
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
			i++;
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
