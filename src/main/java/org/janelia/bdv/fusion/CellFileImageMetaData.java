/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.bdv.fusion;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.io.SerializableTransform3D;

/**
 *
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 */
public class CellFileImageMetaData
{
	private String urlFormat = "";
	private String imageType = "";
	private long[] imageDimensions = new long[ 3 ];
	private Map< Integer, int[] > downsampleFactors = new TreeMap<>();
	private Map< Integer, int[] > cellDimensions = new TreeMap<>();

	private double[][] transform = new double[][] {
		new double[] { 1, 0, 0, 0 },
		new double[] { 0, 1, 0, 0 },
		new double[] { 0, 0, 1, 0 }
	};

	// an arbitrary transformation applied after the affine 'transform'
	private SerializableTransform3D postTransform;

	private double displayRangeMin = 0, displayRangeMax = 0xffff;

	private double[] voxelDimensions = new double[] { 1, 1, 1 };
	private String voxelUnit = "nm";

	public CellFileImageMetaData(
			final String urlFormat,
			final String imageType,
			final long[] imageDimensions,
			final Map< Integer, int[] > downsampleFactors,
			final Map< Integer, int[] > cellDimensions,
			final double[][] transform,
			final InvertibleRealTransform postTransform,
			final VoxelDimensions voxel )
	{
		this.urlFormat = urlFormat;
		this.imageType = imageType;
		this.imageDimensions = imageDimensions;
		this.downsampleFactors = downsampleFactors;
		this.cellDimensions = cellDimensions;

		setPostTransform( postTransform );
		this.transform = transform;

		voxel.dimensions( this.voxelDimensions );
		this.voxelUnit = voxel.unit();
	}

	public CellFileImageMetaData(
			final String urlFormat,
			final String imageType,
			final long[] imageDimensions,
			final Map< Integer, int[] > downsampleFactors,
			final Map< Integer, int[] > cellDimensions,
			final double[][] transform,
			final VoxelDimensions voxel )
	{
		this( urlFormat, imageType, imageDimensions, 
				downsampleFactors, cellDimensions, transform, 
				null, voxel );
	}

	protected CellFileImageMetaData()
	{
	}

	public String getUrlFormat()
	{
		return urlFormat;
	}
	public String getImageType()
	{
		return imageType;
	}

	public double getDisplayRangeMin()
	{
		return displayRangeMin;
	}
	public double getDisplayRangeMax()
	{
		return displayRangeMax;
	}

	public int getNumScales()
	{
		return downsampleFactors.size();
	}

	public long[][] getImageDimensions()
	{
		final long[][] ret = new long[ downsampleFactors.size() ][ 3 ];
		for ( final Entry< Integer, int[] > entry : downsampleFactors.entrySet() )
			for ( int d = 0; d < entry.getValue().length; d++ )
				ret[ entry.getKey() ][ d ] = imageDimensions[ d ] / entry.getValue()[ d ];
		return ret;
	}

	public int[][] getDownsampleFactors()
	{
		final int[][] ret = new int[ downsampleFactors.size() ][];
		for ( final Entry< Integer, int[] > entry : downsampleFactors.entrySet() )
			ret[ entry.getKey() ] = entry.getValue();
		return ret;
	}

	public int[][] getCellDimensions()
	{
		final int[][] ret = new int[ cellDimensions.size() ][];
		for ( final Entry< Integer, int[] > entry : cellDimensions.entrySet() )
			ret[ entry.getKey() ] = entry.getValue();
		return ret;
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return new FinalVoxelDimensions( voxelUnit, voxelDimensions );
	}

	public AffineTransform3D getTransform()
	{
		final double[][] voxelTransform = new double[ transform.length ][];
		for ( int d = 0; d < voxelTransform.length; d++ )
		{
			voxelTransform[ d ] = transform[ d ].clone();
			voxelTransform[ d ][ d ] *= voxelDimensions[ d ];
		}

		final AffineTransform3D ret = new AffineTransform3D();
		ret.set( voxelTransform );
		return ret;
	}

	public InvertibleRealTransform getPostTransform()
	{
		if( postTransform == null )
			return null;

		return postTransform.getTransform();
	}

	public void setPostTransform( InvertibleRealTransform postTransform )
	{
		this.postTransform = new SerializableTransform3D( postTransform );
	}
}