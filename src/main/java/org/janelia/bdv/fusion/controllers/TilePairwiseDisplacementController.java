package org.janelia.bdv.fusion.controllers;

import java.util.List;
import java.util.Map;

import org.janelia.stitching.SerializablePairWiseStitchingResult;
import org.janelia.stitching.TileInfo;
import org.janelia.stitching.TileOperations;
import org.janelia.stitching.TilePair;
import org.janelia.stitching.Utils;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.viewer.ViewerPanel;
import ij.IJ;
import mpicbg.models.Point;
import net.imglib2.RealPoint;

public class TilePairwiseDisplacementController extends CustomBehaviourController
{
	private final ViewerPanel viewer;
	private final TileInfo[] tiles;
	private final Map< Integer, ? extends Map< Integer, SerializablePairWiseStitchingResult > > pairwiseShiftsMap;


	public TilePairwiseDisplacementController( final InputTriggerConfig config, final ViewerPanel viewer, final List< SerializablePairWiseStitchingResult > pairwiseShifts )
	{
		super( config );
		this.viewer = viewer;
		tiles = Utils.createTilesMap( pairwiseShifts, false ).values().toArray( new TileInfo[ 0 ] );
		pairwiseShiftsMap = Utils.createPairwiseShiftsMap( pairwiseShifts, false );
	}


	@Override public String getName() { return "tile pairwise displacement"; }
	@Override public String getTriggers() { return "SPACE"; }
	@Override public CustomBehaviour createBehaviour() { return new TilePairwiseDisplacementDragBehaviour(); }


	private class TilePairwiseDisplacementDragBehaviour extends CustomBehaviour implements DragBehaviour
	{
		private final RealPoint point = new RealPoint( 3 );
		private TilePair lastTilePair;

		@Override
		public void init( final int x, final int y ) { }

		@Override
		public void drag( final int x, final int y )
		{
			viewer.displayToGlobalCoordinates( x, y, point );

			final long[] elementaryBoxMin = new long[ 3 ];
			for ( int d = 0; d < 3; ++d )
				elementaryBoxMin[ d ] = ( long ) Math.floor( point.getDoublePosition( d ) );
			final int[] elementaryBoxSize = new int[] { 1, 1, 1 };

			final List< TileInfo > tilesAtPoint = TileOperations.findTilesWithinSubregion( tiles, elementaryBoxMin, elementaryBoxSize );
			if ( tilesAtPoint.size() == 2 && pairwiseShiftsMap.containsKey( tilesAtPoint.get( 0 ).getIndex() ) && pairwiseShiftsMap.get( tilesAtPoint.get( 0 ).getIndex() ).containsKey( tilesAtPoint.get( 1 ).getIndex() ) )
			{
				final SerializablePairWiseStitchingResult pairwiseShift = pairwiseShiftsMap.get( tilesAtPoint.get( 0 ).getIndex() ).get( tilesAtPoint.get( 1 ).getIndex() );
				final TilePair tilePair = pairwiseShift.getTilePair();
				if ( lastTilePair != tilePair )
				{
					lastTilePair = tilePair;
					final double[] tileOffsetPosition = new double[ 3 ];
					for ( int d = 0; d < 3; ++d )
						tileOffsetPosition[ d ] = tilePair.getA().getPosition( d ) + pairwiseShift.getOffset( d );
					IJ.log(
							String.format( "(%d,%d): displacement=%f, cross correlation=%f, variance=%f%s",
									tilePair.getA().getIndex(),
									tilePair.getB().getIndex(),
									Point.distance( new Point( tilePair.getB().getPosition() ), new Point( tileOffsetPosition ) ),
									pairwiseShift.getCrossCorrelation(),
									pairwiseShift.getVariance(),
									pairwiseShift.getIsValidOverlap() ? "" : " [rejected]" ) );
				}
			}
		}

		@Override
		public void end( final int x, final int y )
		{
			lastTilePair = null;
		}
	}
}
