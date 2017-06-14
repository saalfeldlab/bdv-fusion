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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.InputTriggerAdder;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.viewer.ViewerPanel;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.Volatile;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class CropController
{
	final protected ViewerPanel viewer;

	private RealPoint lastClick = new RealPoint(3);
	private CellFileImageMetaData[] cellFileImageMetaDatas;

	static private int width = 1024;
	static private int height = 1024;
	static private int depth = 512;
	static private int scaleLevel = 0;
	static private boolean single4DStack = false;

	// for behavioUrs
	private final BehaviourMap behaviourMap = new BehaviourMap();
	private final InputTriggerMap inputTriggerMap = new InputTriggerMap();
	private final InputTriggerAdder inputAdder;

	// for keystroke actions
	private final ActionMap ksActionMap = new ActionMap();
	private final InputMap ksInputMap = new InputMap();
	private final KeyStrokeAdder ksKeyStrokeAdder;

	public CropController(
			final ViewerPanel viewer,
			final CellFileImageMetaData[] cellFileImageMetaDatas,
			final InputTriggerConfig config,
			final InputActionBindings inputActionBindings,
			final KeyStrokeAdder.Factory keyProperties )
	{
		this.viewer = viewer;
		this.cellFileImageMetaDatas = cellFileImageMetaDatas;

		inputAdder = config.inputTriggerAdder( inputTriggerMap, "crop" );
		ksKeyStrokeAdder = keyProperties.keyStrokeAdder( ksInputMap, "crop" );

		new Crop( "crop", "SPACE" ).register();

		inputActionBindings.addActionMap( "select", ksActionMap );
		inputActionBindings.addInputMap( "select", ksInputMap );
	}

	////////////////
	// behavioUrs //
	////////////////

	public BehaviourMap getBehaviourMap()
	{
		return behaviourMap;
	}

	public InputTriggerMap getInputTriggerMap()
	{
		return inputTriggerMap;
	}

	private abstract class SelfRegisteringBehaviour implements Behaviour
	{
		private final String name;

		private final String[] defaultTriggers;

		public SelfRegisteringBehaviour( final String name, final String... defaultTriggers )
		{
			this.name = name;
			this.defaultTriggers = defaultTriggers;
		}

		public void register()
		{
			behaviourMap.put( name, this );
			inputAdder.put( name, defaultTriggers );
		}
	}

	private abstract class SelfRegisteringAction extends AbstractNamedAction
	{
		private final String[] defaultTriggers;

		public SelfRegisteringAction( final String name, final String ... defaultTriggers )
		{
			super( name );
			this.defaultTriggers = defaultTriggers;
		}

		public void register()
		{
			put( ksActionMap );
			ksKeyStrokeAdder.put( name(), defaultTriggers );
		}
	}

	private class Crop extends SelfRegisteringBehaviour implements ClickBehaviour
	{
		public Crop( final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
		}

		@Override
		public void click( final int x, final int y )
		{
			viewer.displayToGlobalCoordinates(x, y, lastClick);
			final GenericDialog gd = new GenericDialog( "Crop" );
			gd.addNumericField( "width : ", width, 0, 5, "px" );
			gd.addNumericField( "height : ", height, 0, 5, "px" );
			gd.addNumericField( "depth : ", depth, 0, 5, "px" );
			gd.addNumericField( "scale_level : ", scaleLevel, 0 );
			gd.addCheckbox( "Single_4D_stack", single4DStack );

			gd.showDialog();

			if ( gd.wasCanceled() )
				return;

			width = ( int )gd.getNextNumber();
			height = ( int )gd.getNextNumber();
			depth = ( int )gd.getNextNumber();
			scaleLevel = ( int )gd.getNextNumber();
			single4DStack = gd.getNextBoolean();

			doCrop();
		}

		private < T extends NativeType< T > & RealType< T >, V extends Volatile< T > > void doCrop()
		{
			final int w = width;
			final int h = height;
			final int d = depth;
			final int s = scaleLevel;

			final List< RandomAccessibleInterval< T > > channelsImages = new ArrayList<>();
			long[] min = null;

			int channel = 0;
			for ( final CellFileImageMetaData metaData : cellFileImageMetaDatas )
			{
				final AbstractCellFileImageLoader< T, V > imgLoader = ( AbstractCellFileImageLoader< T, V > ) CellFileImageLoaderFactory.createImageLoader( metaData );
				if ( s < 0 || s >= imgLoader.numMipmapLevels() )
				{
					IJ.log( String.format( "Specified incorrect scale level %d. Valid range is [%d, %d]", s, 0, imgLoader.numMipmapLevels() - 1 ) );
					scaleLevel = imgLoader.numMipmapLevels() - 1;
					return;
				}
				final AffineTransform3D transform = imgLoader.getMipmapTransforms()[ s ].copy();
				transform.preConcatenate( metaData.getTransform() );
				final RealPoint center = new RealPoint( 3 );
				transform.applyInverse( center, lastClick );

				min = new long[] {
						Math.round( center.getDoublePosition( 0 ) - 0.5 * w ),
						Math.round( center.getDoublePosition( 1 ) - 0.5 * h ),
						Math.round( center.getDoublePosition( 2 ) - 0.5 * d ) };
				final long[] size = new long[] { w, h, d };

				IJ.log( String.format( "Cropping %s pixels at %s using scale level %d", Arrays.toString( size ), Arrays.toString( min ), s ) );

				final RandomAccessibleInterval< T > img = imgLoader.getImage( 0, s );
				final RandomAccessible< T > imgExtended = Views.extendZero( img );
				final IntervalView< T > crop = Views.offsetInterval( imgExtended, min, size );

				channelsImages.add( crop );

				if ( !single4DStack )
					show( crop, "channel " + channel + " " + Arrays.toString( min ) );

				System.out.println( metaData.getUrlFormat() + " " + Util.printCoordinates( center ) );

				++channel;
			}

			if ( single4DStack )
			{
				// FIXME: need to permute slices/channels. Swapping them in the resulting ImagePlus produces wrong output
				ImageJFunctions.show( Views.permute( Views.stack( channelsImages ), 2, 3 ), Arrays.toString( min ) );
			}

			System.out.println( Util.printCoordinates( lastClick ) );
			viewer.requestRepaint();
		}

		// Taken from ImageJFunctions. Modified to swap slices/channels for 3D image (by default they mistakenly are nSlices=1 and nChannels=depth)
		// TODO: pull request with this fix if appropriate in general case?
		private < T extends NumericType< T > > ImagePlus show( final RandomAccessibleInterval< T > img, final String title )
		{
			final ImagePlus imp = ImageJFunctions.wrap( img, title );
			if ( null == imp ) { return null; }

			// Make sure that nSlices>1 and nChannels=nFrames=1 for 3D image
			final int[] possible3rdDim = new int[] { imp.getNChannels(), imp.getNSlices(), imp.getNFrames() };
			Arrays.sort( possible3rdDim );
			if ( possible3rdDim[ 0 ] * possible3rdDim[ 1 ] == 1 )
				imp.setDimensions( 1, possible3rdDim[ 2 ], 1 );

			imp.show();
			imp.getProcessor().resetMinAndMax();
			imp.updateAndRepaintWindow();

			return imp;
		}
	}
}
