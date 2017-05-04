package org.janelia.bdv.fusion.controllers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.stitching.TileInfo;
import org.janelia.stitching.TileOperations;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.viewer.ViewerPanel;
import ij.IJ;
import net.imglib2.RealPoint;

public class TilesAtPointController extends CustomBehaviourController
{
	private final ViewerPanel viewer;
	private final TileInfo[] tiles;

	public TilesAtPointController( final InputTriggerConfig config, final ViewerPanel viewer, final TileInfo[] tiles )
	{
		super( config );
		this.viewer = viewer;
		this.tiles = tiles;
		TileOperations.translateTilesToOriginReal( this.tiles );
	}

	@Override public String getName() { return "tiles at point"; }
	@Override public String getTriggers() { return "SPACE button1"; }
	@Override public CustomBehaviour createBehaviour() { return new TilesAtPointClickBehaviour(); }

	private class TilesAtPointClickBehaviour extends CustomBehaviour implements ClickBehaviour
	{
		private final RealPoint point = new RealPoint( 3 );

		@Override
		public void click( final int x, final int y )
		{
			viewer.displayToGlobalCoordinates( x, y, point );

			final long[] elementaryBoxMin = new long[ 3 ];
			for ( int d = 0; d < 3; ++d )
				elementaryBoxMin[ d ] = ( long ) Math.floor( point.getDoublePosition( d ) );
			final int[] elementaryBoxSize = new int[] { 1, 1, 1 };

			final List< TileInfo > tilesAtPoint = TileOperations.findTilesWithinSubregion( tiles, elementaryBoxMin, elementaryBoxSize );
			if ( !tilesAtPoint.isEmpty() )
			{
				final Set< Integer > tileIndexesAtPoint = new HashSet<>();
				for ( final TileInfo tile : tilesAtPoint )
					tileIndexesAtPoint.add( tile.getIndex() );

				final StringBuilder sb = new StringBuilder();
				sb.append( tilesAtPoint.size() == 1 ? "There is 1 tile" : String.format( "There are %d tiles", tilesAtPoint.size() ) );
				sb.append( String.format( " at %s:", Arrays.toString( elementaryBoxMin ) ) ).append( System.lineSeparator() );
				for ( final TileInfo tile : tilesAtPoint )
					sb.append( String.format( "  %d: %s", tile.getIndex(), tile.getFilePath() ) ).append( System.lineSeparator() );

				IJ.log( sb.toString() );
			}
		}
	}
}
