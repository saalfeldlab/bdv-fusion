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
	private long[] imageDimensions = new long[ 3 ];
	private int[] cellDimensions = new int[ 3 ];
	
	private double[][] transform = new double[][] {
		new double[] { 1, 0, 0, 0 },
		new double[] { 0, 1, 0, 0 },
		new double[] { 0, 0, 1, 0 }
	};

	private double displayRangeMin = 0, displayRangeMax = 0xffff;
	private double[] voxelSize = new double[]{ 80, 80, 150 };
	
	private String unit = "nm";
	
	
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
	
	public long[][] getDimensions()
	{
		final long[][] dimensions = new long[ numScales ][ imageDimensions.length ];
		for ( int i = 0; i < numScales; ++i )
			for ( int d = 0; d < imageDimensions.length; ++d )
				dimensions[ i ][ d ] = imageDimensions[ d ] >> i;
		
		return dimensions; 
	}
	
	public int[][] getCellDimensions()
	{
		final int[][] levelCellDimensions = new int[ numScales ][ cellDimensions.length ];
		for ( int i = 0; i < numScales; ++i )
			levelCellDimensions[ i ] = cellDimensions;
		
		return levelCellDimensions; 
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return new FinalVoxelDimensions( unit, voxelSize );
	}
	
	public AffineTransform3D getTransform()
	{
		final double[][] voxelTransform = new double[][] {
				transform[ 0 ].clone(),
				transform[ 1 ].clone(),
				transform[ 2 ].clone()
		};
		
		voxelTransform[ 1 ][ 1 ] *= voxelSize[ 1 ] / voxelSize[ 0 ];
		voxelTransform[ 2 ][ 2 ] *= voxelSize[ 2 ] / voxelSize[ 0 ];
		
		final AffineTransform3D ret = new AffineTransform3D();
		ret.set( voxelTransform );
		return ret;
	}
}