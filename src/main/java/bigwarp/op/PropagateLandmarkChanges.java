package bigwarp.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;

public class PropagateLandmarkChanges {

	public static void main(String[] args) throws IOException
	{
		String oldRefXfmF = "/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/test_propagation/ltm_shiftx.csv";
		String newRefXfmF = "/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/test_propagation/ltm_shifty.csv";
		String changeXfmF = "/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/test_propagation/ltm_identity2.csv";
		String outXfmF = "/groups/saalfeld/home/bogovicj/projects/igor_illumiation-correction/test_propagation/out.csv";

		int maxIters = 3000;
		double tolerance = 1e-4;

		LandmarkTableModel oldRefLtm = loadLtm( new File( oldRefXfmF ));
		LandmarkTableModel newRefLtm = loadLtm( new File( newRefXfmF ));
		LandmarkTableModel changeLtm = loadLtm( new File( changeXfmF ));
		LandmarkTableModel outLtm = new LandmarkTableModel( 3 );

		ThinPlateR2LogRSplineKernelTransform oldRefXfm = oldRefLtm.getTransform();
		ThinPlateR2LogRSplineKernelTransform newRefXfm = newRefLtm.getTransform();

		ArrayList<Double[]> fixedPointsToChange = changeLtm.getPoints( false ); // get fixed points to change
		int i = 0;
		for( Double[] pt : fixedPointsToChange )
		{
			double[] res = new double[ 3 ];
			double[] newFixed = new double[ 3 ];
			oldRefXfm.inverse( toPrimitive( pt ), res, tolerance, maxIters );
			newRefXfm.apply( res, newFixed );

			outLtm.add( toPrimitive( changeLtm.getPoint( true, i )), true );
			outLtm.add( newFixed, false );

			i++;
		}

		outLtm.save( new File( outXfmF ));
	}

	public static double[] toPrimitive( Double[] pt )
	{
		double[] out = new double[ pt.length ];
		for( int i = 0; i < pt.length; i++ )
			out[ i ] = pt[ i ].doubleValue();
		
		return out;
	}

	public static LandmarkTableModel loadLtm( File f ) throws IOException
	{
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		ltm.load( f );
		return ltm;
	}

}