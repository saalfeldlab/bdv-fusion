package org.janelia.bdv.fusion;

import net.imglib2.Volatile;
import net.imglib2.type.NativeType;

public class CellFileImageLoaderFactory
{
	public static AbstractCellFileImageLoader< ? extends NativeType< ? >, ? extends Volatile< ? > > createImageLoader( final CellFileImageMetaData metaData )
	{
		switch ( metaData.getImageType() ) 
		{
		case "GRAY8":
			return new CellFileUnsignedByteImageLoader( 
					metaData.getUrlFormat(), 
					metaData.getDimensions(), 
					metaData.getCellDimensions() );
			
		case "GRAY16":
			return new CellFileUnsignedShortImageLoader( 
					metaData.getUrlFormat(), 
					metaData.getDimensions(), 
					metaData.getCellDimensions() );
			
		case "GRAY32":
		default:
			return new CellFileFloatImageLoader( 
					metaData.getUrlFormat(), 
					metaData.getDimensions(), 
					metaData.getCellDimensions() );
		}
	}
}
