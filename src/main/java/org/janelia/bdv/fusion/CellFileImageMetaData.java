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

/**
 *
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class CellFileImageMetaData
{
	private String urlFormat = "";
	private String imageType = "";
	private int numScales = 0;
	private Map< Integer, long[] > levelImageDimensions = new TreeMap<>();
	private Map< Integer, int[] > levelCellDimensions = new TreeMap<>();

	private double[][] transform = new double[][] {
		new double[] { 1, 0, 0, 0 },
		new double[] { 0, 1, 0, 0 },
		new double[] { 0, 0, 1, 0 }
	};

	private double displayRangeMin = 0, displayRangeMax = 0xffff;

	private double[] voxelDimensions = new double[]{ 1, 1, 1 };
	private String voxelUnit = "nm";


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

	public long[][] getImageDimensions()
	{
		final long[][] imageDimensions = new long[ numScales ][];
		for ( final Entry< Integer, long[] > entry : levelImageDimensions.entrySet() )
			imageDimensions[ entry.getKey() ] = entry.getValue();
		return imageDimensions;
	}

	public int[][] getCellDimensions()
	{
		final int[][] cellDimensions = new int[ numScales ][];
		for ( final Entry< Integer, int[] > entry : levelCellDimensions.entrySet() )
			cellDimensions[ entry.getKey() ] = entry.getValue();
		return cellDimensions;
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return new FinalVoxelDimensions( voxelUnit, voxelDimensions );
	}

	public AffineTransform3D getTransform()
	{
		final double[][] voxelTransform = new double[][] {
				transform[ 0 ].clone(),
				transform[ 1 ].clone(),
				transform[ 2 ].clone()
		};

		voxelTransform[ 1 ][ 1 ] *= voxelDimensions[ 1 ] / voxelDimensions[ 0 ];
		voxelTransform[ 2 ][ 2 ] *= voxelDimensions[ 2 ] / voxelDimensions[ 0 ];

		final AffineTransform3D ret = new AffineTransform3D();
		ret.set( voxelTransform );
		return ret;
	}
}