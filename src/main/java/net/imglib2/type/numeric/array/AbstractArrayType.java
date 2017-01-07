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
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * Abstract implementation of types that are arrays of primitive numbers.
 *
 *
 * @author Stephan Saalfeld
 */
abstract public class AbstractArrayType< T extends AbstractArrayType< T, A >, A > implements NativeType< T >
{
	protected int i = 0;
	protected int index = 0;

	final protected int length;

	final protected NativeImg< ?, ? extends A > img;

	protected A dataAccess;

	// this is the constructor if you want it to read from an array
	public AbstractArrayType( final NativeImg< ?, ? extends A > arrayStorage, final int length )
	{
		img = arrayStorage;
		this.length = length;
	}

	// this is the constructor if you want to specify the dataAccess
	public AbstractArrayType( final A access, final int length )
	{
		img = null;
		dataAccess = access;
		this.length = length;
	}

	@Override
	public Fraction getEntitiesPerPixel()
	{
		return new Fraction( length, 1 );
	}

	@Override
	public void updateContainer( Object c )
	{
		dataAccess = img.update( c );
	}

	@Override
	public void updateIndex( int i )
	{
		i = i * length;
	}

	@Override
	public int getIndex()
	{
		return index;
	}

	@Override
	public void incIndex()
	{
		i += length;
		++index;
	}

	@Override
	public void incIndex( int increment )
	{
		i += increment * length;
		index += increment;
	}

	@Override
	public void decIndex()
	{
		i -= length;
		--index;
	}

	@Override
	public void decIndex( int decrement )
	{
		i -= decrement * length;
		index -= decrement;
	}
}
