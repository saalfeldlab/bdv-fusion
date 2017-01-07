/**
 *
 */
package org.janelia.bdv.fusion;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bdv.img.h5.H5Utils;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.array.LongArrayType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 *
 */
public class H5LongArraySetupImageLoaderTest
{
	static private String testDirPath = System.getProperty( "user.home" ) + "/tmp/bdv-fusion-test/";

	static private String testH5Name = "test.h5";

	final static private long[] volumeDimensions = new long[]{ 32, 100, 20, 10 };

	final static private int[] cellDimensions = new int[]{ 32, 8, 8, 8 };

	final static private Random rnd = new Random( 0 );

	final static private long[] testArray =
			new long[ (int)(
					volumeDimensions[ 0 ] *
					volumeDimensions[ 1 ] *
					volumeDimensions[ 2 ] *
					volumeDimensions[ 3 ] ) ];

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		final File testDir = new File( testDirPath );
		testDir.mkdirs();
		if ( !( testDir.exists() && testDir.isDirectory() ) )
			throw new IOException( "Could not create test directory for HDF5Utils test." );

		for ( int i = 0; i < testArray.length; ++i )
			testArray[ i ] = rnd.nextLong();
//			testArray[ i ] = i;

		System.out.println( "Saving volume... " );
		final ArrayImg< LongType, ? > img = ArrayImgs.longs( testArray, volumeDimensions );

		H5Utils.saveLong( img, testDirPath + testH5Name, "/test", cellDimensions );
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void rampDownAfterClass() throws Exception
	{
		final File testDir = new File( testDirPath );
		final File testFile = new File( testDirPath + testH5Name );
		testFile.delete();
		testDir.delete();
	}



	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{}

	@Test
	public void testH5LongArraySetupImageLoader() throws IOException
	{
		final IHDF5Writer writer = HDF5Factory.open( testDirPath + testH5Name );
		final H5LongArraySetupImageLoader loader = new H5LongArraySetupImageLoader(
				writer,
				"/test",
				0,
				new int[]{ 8, 8, 8 } );

		final RandomAccessibleInterval< LongArrayType > img = loader.getImage( 0 );

		System.out.println( Arrays.toString( Intervals.dimensionsAsLongArray( img ) ) );
		int k = 0;
		for ( final LongArrayType t : Views.flatIterable( img ) )
			for ( int i = 0; i < t.size(); ++i, ++k )
				Assert.assertEquals( testArray[ k ], t.get( i ) );

		System.out.println( "Re-saving..." );
		loader.save( writer, "/save" );

		final H5LongArraySetupImageLoader loader2 = new H5LongArraySetupImageLoader(
				writer,
				"/save",
				0,
				new int[]{ 8, 8, 8 } );

		final RandomAccessibleInterval< LongArrayType > img2 = loader2.getImage( 0 );

		System.out.println( Arrays.toString( Intervals.dimensionsAsLongArray( img2 ) ) );
		k = 0;
		for ( final LongArrayType t : Views.flatIterable( img2 ) )
			for ( int i = 0; i < t.size(); ++i, ++k )
				Assert.assertEquals( testArray[ k ], t.get( i ) );

	}
}
