/**
 * Copyright 2009 University of Oxford
 *
 * Written by Arno Mittelbach for the Erewhon Project
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the University of Oxford nor the names of its 
 *    contributors may be used to endorse or promote products derived from this 
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.oucs.gaboto.entities.pool.filters;

import org.oucs.gaboto.entities.pool.GabotoEntityPool;
import org.oucs.gaboto.nodes.GabotoEntity;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * ResourceFilters can be used to guide the creation process of {@link GabotoEntityPool}s.
 * 
 * <p>
 * In contrast to {@link EntityFilter}s, ResourceFilters are applied before an entity is fully
 * loaded and therefore provide a better performance.
 * </p>
 * 
 * @author Arno Mittelbach
 * @version 0.1
 * @see GabotoEntityPool
 * @see EntityFilter
 */
public abstract class ResourceFilter {

	/**
	 * Defines to what entity type this filter applies to.
	 * 
	 * @see GabotoEntity#getType()
	 * @return To what entity type this filter applies to.
	 */
	public Class<? extends GabotoEntity> appliesTo(){
		return GabotoEntity.class;
	}
	
	/**
	 * Performs the actual filter operation.
	 * 
	 * <p>
	 * Returns false if the entity did not pass the filter criteria. True otherwise.
	 * </p>
	 * 
	 * @param res The resource.
	 * @return false|true (reject|pass). 
	 */
	abstract public boolean filterResource(Resource res);
}
