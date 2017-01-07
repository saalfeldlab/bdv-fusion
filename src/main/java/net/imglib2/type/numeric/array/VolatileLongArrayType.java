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
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileLongAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileLongArray;
import net.imglib2.type.volatiles.AbstractVolatileNativeNumericType;

/**
 * Volatile version of {@link LongArrayType}.
 *
 * @author Stephan Saalfeld
 */
public class VolatileLongArrayType extends AbstractVolatileNativeNumericType< LongArrayType, VolatileLongArrayType >
{
	final protected NativeImg< ?, ? extends VolatileLongAccess > img;

	private static class WrappedLongArrayType extends LongArrayType
	{
		public WrappedLongArrayType( final NativeImg<?, ? extends LongAccess > img, final int length )
		{
			super( img, length );
		}

		public WrappedLongArrayType( final LongAccess access, final int length )
		{
			super( access, length );
		}

		public void setAccess( final LongAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileLongArrayType( final NativeImg< ?, ? extends VolatileLongAccess > img, final int length )
	{
		super( new WrappedLongArrayType( img, length ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileLongArrayType( final VolatileLongAccess access, final int length )
	{
		super( new WrappedLongArrayType( access, length ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileLongArrayType( final long[] values )
	{
		this( new VolatileLongArray( values, true ), values.length );
	}

	// this is the constructor if you want it to be a variable
	public VolatileLongArrayType( final int length )
	{
		this( new long[ length ] );
	}

	public void set( final long value, final int index )
	{
		get().set( value, index );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileLongAccess a = img.update( c );
		( ( WrappedLongArrayType ) t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public VolatileLongArrayType duplicateTypeOnSameNativeImg()
	{
		return new VolatileLongArrayType( img, t.length );
	}

	@Override
	public VolatileLongArrayType createVariable()
	{
		return new VolatileLongArrayType( t.length );
	}

	@Override
	public VolatileLongArrayType copy()
	{
		final VolatileLongArrayType v = createVariable();
		v.set( this );
		return v;
	}

	@Override
	public NativeImg< VolatileLongArrayType, ? > createSuitableNativeImg( NativeImgFactory< VolatileLongArrayType > storageFactory, long[] dim )
	{
		// TODO implement (or not)
		throw new UnsupportedOperationException();
	}
}
