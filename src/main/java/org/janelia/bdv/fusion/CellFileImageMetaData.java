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

/**
 * 
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class CellFileImageMetaData
{
	public String urlFormat = "";
	public int numScales = 0;
	public long[] imageDimensions = new long[ 3 ];
	public int[] cellDimensions = new int[ 3 ];
	public double[] voxelSize = new double[]{ 80, 80, 150 };
	public String unit = "nm";
	
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
}