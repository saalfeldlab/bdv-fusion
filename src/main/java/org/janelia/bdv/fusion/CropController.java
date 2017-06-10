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

import bdv.ViewerSetupImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ViewerPanel;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.GenericDialog;
import net.imglib2.RealPoint;
import net.imglib2.Volatile;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
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

		new Crop( "crop", "SPACE button1" ).register();

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
			
			gd.showDialog();
			
			if ( gd.wasCanceled() )
				return;
			
			width = ( int )gd.getNextNumber();
			height = ( int )gd.getNextNumber();
			depth = ( int )gd.getNextNumber();
			scaleLevel = ( int )gd.getNextNumber();
			
			final int w = width;
			final int h = height;
			final int d = depth;
			final int s = scaleLevel;
			
			int channel = 0;
			for ( final CellFileImageMetaData metaData : cellFileImageMetaDatas )
			{
				AbstractCellFileImageLoader<? extends NativeType<?>, ? extends Volatile<?>> imgLoader = CellFileImageLoaderFactory.createImageLoader( metaData );
				AffineTransform3D transform = imgLoader.getMipmapTransforms()[ s ].copy();
				transform.preConcatenate( metaData.getTransform() );
				final RealPoint center = new RealPoint( 3 );
				transform.applyInverse( center, lastClick );
				
				final long[] min = new long[] {
						Math.round( center.getDoublePosition( 0 ) - 0.5 * w ),
						Math.round( center.getDoublePosition( 1 ) - 0.5 * h ),
						Math.round( center.getDoublePosition( 2 ) - 0.5 * d ) };
				final long[] size = new long[] { w, h, d };
				
				IJ.log( "cropping " + Arrays.toString( size ) + "pixels at " + Arrays.toString( min ) );
				
				IntervalView crop = Views.offsetInterval( imgLoader.getImage( 0, s ), min, size );
				
				ImageJFunctions.show( crop, "channel " + channel + " " + Arrays.toString( min ) );
				
				System.out.println( metaData.getUrlFormat() + " " + Util.printCoordinates( center ) );
				
				++channel;
			}
			
			System.out.println( Util.printCoordinates( lastClick ) );
			viewer.requestRepaint();
		}
	}
}
