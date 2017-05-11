package net.imglib2.realtransform.io;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;

import bdv.img.TpsTransformWrapper;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.AbstractAffineTransform;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.Translation3D;

public class SerializableTransform3D //< T extends InvertibleRealTransform >
		implements Serializable
{
	private static final long serialVersionUID = -673560465134855871L;

//	private transient final Type TYPE = new TypeToken< SerializableTransform3D< T > >()
//	{}.getType();

	private transient InvertibleRealTransform transform;

	private String transformType;
	private String dataString;
	private double[] data;

	public SerializableTransform3D(){}

	public SerializableTransform3D( InvertibleRealTransform transform )
	{
		this.transform = transform;
		encode();
	}

	public InvertibleRealTransform getTransform()
	{
		if ( transform != null )
			return transform;
		else
			return buildTransform();
	}

	public InvertibleRealTransform buildTransform()
	{
		if( transformType.equals( "bdv.img.TpsTransformWrapper" ))
		{
			return new TpsTransformWrapper( 3, initTps( dataString ) );
		}
		else if( transformType.isEmpty() )
		{
			return new Translation3D(); // identity
		}
		else // we'd better have an affine of some kind
		{
			AffineTransform3D affine = new AffineTransform3D();
			affine.set( data );
			return affine;
		}
	}

	public void encode()
	{
		if( transform == null )
		{
			transformType = "";
			dataString = ""; 
		}

		transformType = transform.getClass().getName();
		if( transformType.equals( "bdv.img.TpsTransformWrapper" ))
		{
			StringBuilder builder = new StringBuilder();
			toDataString( builder, ((TpsTransformWrapper) transform).getTps() );
			dataString = builder.toString();
		}
		else
		{
			transformType = "affine";
			data = ((AffineGet)transform).getRowPackedCopy();
		}

	}

	/**
	 * Copy from trakem2-tps
	 */
	static private String encodeBase64( final double[] src )
	{
		final byte[] bytes = new byte[ src.length * 8 ];
		for ( int i = 0, j = -1; i < src.length; ++i )
		{
			final long bits = Double.doubleToLongBits( src[ i ] );
			bytes[ ++j ] = (byte) (bits >> 56);
			bytes[ ++j ] = (byte) ((bits >> 48) & 0xffL);
			bytes[ ++j ] = (byte) ((bits >> 40) & 0xffL);
			bytes[ ++j ] = (byte) ((bits >> 32) & 0xffL);
			bytes[ ++j ] = (byte) ((bits >> 24) & 0xffL);
			bytes[ ++j ] = (byte) ((bits >> 16) & 0xffL);
			bytes[ ++j ] = (byte) ((bits >> 8) & 0xffL);
			bytes[ ++j ] = (byte) (bits & 0xffL);
		}
		final Deflater deflater = new Deflater();
		deflater.setInput( bytes );
		deflater.finish();
		final byte[] zipped = new byte[ bytes.length ];
		final int n = deflater.deflate( zipped );
		if ( n == bytes.length )
			return '@' + Base64.encodeBase64String( bytes );
		else
			return Base64.encodeBase64String( Arrays.copyOf( zipped, n ) );
	}

	static private double[] decodeBase64( final String src, final int n )
			throws DataFormatException
	{
		final byte[] bytes;
		if ( src.charAt( 0 ) == '@' )
		{
			bytes = Base64.decodeBase64( src.substring( 1 ) );
		} else
		{
			bytes = new byte[ n * 8 ];
			final byte[] zipped = Base64.decodeBase64( src );
			final Inflater inflater = new Inflater();
			inflater.setInput( zipped, 0, zipped.length );

			inflater.inflate( bytes );
			inflater.end();
		}
		final double[] doubles = new double[ n ];
		for ( int i = 0, j = -1; i < n; ++i )
		{
			long bits = 0L;
			bits |= (bytes[ ++j ] & 0xffL) << 56;
			bits |= (bytes[ ++j ] & 0xffL) << 48;
			bits |= (bytes[ ++j ] & 0xffL) << 40;
			bits |= (bytes[ ++j ] & 0xffL) << 32;
			bits |= (bytes[ ++j ] & 0xffL) << 24;
			bits |= (bytes[ ++j ] & 0xffL) << 16;
			bits |= (bytes[ ++j ] & 0xffL) << 8;
			bits |= bytes[ ++j ] & 0xffL;
			doubles[ i ] = Double.longBitsToDouble( bits );
		}
		return doubles;
	}

	public ThinPlateR2LogRSplineKernelTransform initTps( final String data )
			throws NumberFormatException
	{

		final String[] fields = data.split( "\\s+" );

		int i = 0;

		final int ndims = Integer.parseInt( fields[ ++i ] );
		final int nLm = Integer.parseInt( fields[ ++i ] );

		double[][] aMtx = null;
		double[] bVec = null;
		if ( fields[ i + 1 ].equals( "null" ) )
		{
			// System.out.println(" No affines " );
			++i;
		} else
		{
			aMtx = new double[ ndims ][ ndims ];
			bVec = new double[ ndims ];

			final double[] values;
			try
			{
				values = decodeBase64( fields[ ++i ], ndims * ndims + ndims );
			} catch ( final DataFormatException e )
			{
				throw new NumberFormatException(
						"Failed decoding affine matrix." );
			}
			int l = -1;
			for ( int k = 0; k < ndims; k++ )
				for ( int j = 0; j < ndims; j++ )
				{
					aMtx[ k ][ j ] = values[ ++l ];
				}
			for ( int j = 0; j < ndims; j++ )
			{
				bVec[ j ] = values[ ++l ];
			}
		}

		final double[] values;
		try
		{
			values = decodeBase64( fields[ ++i ], 2 * nLm * ndims );
		} catch ( final DataFormatException e )
		{
			throw new NumberFormatException(
					"Failed decoding landmarks and weights." );
		}
		int k = -1;

		// parse control points
		final double[][] srcPts = new double[ ndims ][ nLm ];
		for ( int l = 0; l < nLm; l++ )
			for ( int d = 0; d < ndims; d++ )
			{
				srcPts[ d ][ l ] = values[ ++k ];
			}

		// parse control point coordinates
		int m = -1;
		final double[] dMtxDat = new double[ nLm * ndims ];
		for ( int l = 0; l < nLm; l++ )
			for ( int d = 0; d < ndims; d++ )
			{
				dMtxDat[ ++m ] = values[ ++k ];
			}

		return new ThinPlateR2LogRSplineKernelTransform( srcPts, aMtx, bVec,
				dMtxDat );
	}

	private final void toDataString(
			final StringBuilder data,
			final ThinPlateR2LogRSplineKernelTransform tps )
	{
		data.append("ThinPlateSplineR2LogR");

		final int ndims = tps.getNumDims();
		final int nLm = tps.getNumLandmarks();

		data.append(' ').append(ndims); // dimensions
		data.append(' ').append(nLm); // landmarks

		if (tps.getAffine() == null) {
			data.append(' ').append("null"); // aMatrix
		} else {
			final double[][] aMtx = tps.getAffine();
			final double[] bVec = tps.getTranslation();

			final double[] buffer = new double[ndims * ndims + ndims];
			int k = -1;
			for (int i = 0; i < ndims; i++)
				for (int j = 0; j < ndims; j++) {
					buffer[++k] = aMtx[i][j];
				}
			for (int i = 0; i < ndims; i++) {
				buffer[++k] = bVec[i];
			}

			data.append(' ').append(encodeBase64(buffer));
		}

		final double[][] srcPts = tps.getSourceLandmarks();
		final double[] dMtxDat = tps.getKnotWeights();

		final double[] buffer = new double[2 * nLm * ndims];
		int k = -1;
		for (int l = 0; l < nLm; l++)
			for (int d = 0; d < ndims; d++)
				buffer[++k] = srcPts[d][l];

		for (int i = 0; i < ndims * nLm; i++) {
			buffer[++k] = dMtxDat[i];
		}
		data.append(' ').append(encodeBase64(buffer));
}
}
