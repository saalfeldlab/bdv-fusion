/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imglib2.type.numeric.array;

import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileFloatAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.type.volatiles.AbstractVolatileNativeNumericType;

/**
 * Volatile version of {@link DoubleArrayType}.
 *
 * @author Stephan Saalfeld
 */
public class VolatileFloatArrayType extends AbstractVolatileNativeNumericType< FloatArrayType, VolatileFloatArrayType >
{
	final protected NativeImg< ?, ? extends VolatileFloatAccess > img;

	private static class WrappedFloatArrayType extends FloatArrayType
	{
		public WrappedFloatArrayType( final NativeImg<?, ? extends FloatAccess > img, final int length )
		{
			super( img, length );
		}

		public WrappedFloatArrayType( final FloatAccess access, final int length )
		{
			super( access, length );
		}

		public void setAccess( final FloatAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileFloatArrayType( final NativeImg< ?, ? extends VolatileFloatAccess > img, final int length )
	{
		super( new WrappedFloatArrayType( img, length ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileFloatArrayType( final VolatileFloatAccess access, final int length )
	{
		super( new WrappedFloatArrayType( access, length ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileFloatArrayType( final float[] values )
	{
		this( new VolatileFloatArray( values, true ), values.length );
	}

	// this is the constructor if you want it to be a variable
	public VolatileFloatArrayType( final int length )
	{
		this( new float[ length ] );
	}

	public int size()
	{
		return t.size();
	}

	public void set( final float value, final int index )
	{
		get().set( value, index );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileFloatAccess a = img.update( c );
		( ( WrappedFloatArrayType ) t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public VolatileFloatArrayType duplicateTypeOnSameNativeImg()
	{
		return new VolatileFloatArrayType( img, t.length );
	}

	@Override
	public VolatileFloatArrayType createVariable()
	{
		return new VolatileFloatArrayType( t.length );
	}

	@Override
	public VolatileFloatArrayType copy()
	{
		final VolatileFloatArrayType v = createVariable();
		v.set( this );
		return v;
	}

	@Override
	public NativeImg< VolatileFloatArrayType, ? > createSuitableNativeImg( NativeImgFactory< VolatileFloatArrayType > storageFactory, long[] dim )
	{
		// TODO implement (or not)
		throw new UnsupportedOperationException();
	}
}
