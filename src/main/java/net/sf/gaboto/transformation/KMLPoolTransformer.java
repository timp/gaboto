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
package net.sf.gaboto.transformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.gaboto.node.GabotoEntity;
import net.sf.gaboto.node.pool.EntityPool;
import net.sf.gaboto.util.XMLUtils;
import net.sf.gaboto.vocabulary.DCVocab;
import net.sf.gaboto.vocabulary.FOAFVocab;
import net.sf.gaboto.vocabulary.GabotoKMLVocab;
import net.sf.gaboto.vocabulary.GeoVocab;
import net.sf.gaboto.vocabulary.OxPointsVocab;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hp.hpl.jena.ontology.ObjectProperty;


/**
 * Generic transformer to KML.
 * 
 * @author Arno Mittelbach
 * @version 0.1
 */
public class KMLPoolTransformer implements EntityPoolTransformer {

	/**
	 * The KML namespace.
	 */
	public static final String KML_NS = "http://www.opengis.net/kml/2.2";
	
	/**
	 * The KML extension namespace.
	 */
	public static final String KML_GX_NS = "http://www.google.com/kml/ext/2.2";
	
	private String orderBy = null;
	private boolean displayParentName = true;
	private Map<String, Collection<String>> entityFolderTypes = new HashMap<String, Collection<String>>();
  /**
   * The maximum allowed nesting level
   */
  public static final int MAX_NESTING = 8;
  
  private int nesting = 1;
	
	public String transform(EntityPool pool) {
		Document kmlDoc = getKMLDocumentTemplate();
		Element documentEl = (Element) kmlDoc.getElementsByTagName("Document").item(0);
		
		Collection<GabotoEntity> entities = null;
		if(orderBy == null)
			entities = pool.getEntities();
		else
			entities = pool.getEntitiesSorted(orderBy);
		
		addEntitiesToDocument(kmlDoc, documentEl, entities);
		
		return XMLUtils.getXMLNodeAsString(kmlDoc, "{" + KML_NS + "}description");
	}
	
	private void addEntitiesToDocument(Document kmlDoc, Element documentEl, Collection<GabotoEntity> entities){
		for(GabotoEntity entity : entities)
			transformEntity(kmlDoc, documentEl, entity);
	}
	
	private void transformEntity(Document kmlDoc, Element documentEl, GabotoEntity entity){
		if(! getEntityFolderTypes().containsKey(entity.getType()))
			addPlacemark(kmlDoc,documentEl, entity);
		else
			addFolder(kmlDoc, documentEl, entity);	
	}
	
	@SuppressWarnings("unchecked")
	private void addFolder(Document kmlDoc, Element documentEl, GabotoEntity entity) {
		// create folder
		Element folder = kmlDoc.createElementNS(KML_NS, "Folder");
		documentEl.appendChild(folder);
		
		// add properties
		addNameToElement(kmlDoc, folder, entity);
		addDescriptionToElement(kmlDoc, folder, entity);
		
		// loop over properties
		for(String property : entityFolderTypes.get(entity.getType())){
			Object obj = entity.getPropertyValue(property, true, true);
			if(obj instanceof GabotoEntity){
				addPlacemark(kmlDoc, folder, (GabotoEntity)obj);
			} else if(obj instanceof Collection){
        Collection<GabotoEntity> entityCollection = (Collection<GabotoEntity>) obj;
				for(GabotoEntity entityFromCollection : entityCollection){
					transformEntity(kmlDoc, folder, entityFromCollection);
				}
			}
		}
	}

	/**
	 * Turns an entity into a placemark.
	 * @param kmlDoc
	 * @param parentEl
	 * @param entity
	 */
	private void addPlacemark(Document kmlDoc, Element parentEl,
			GabotoEntity entity) {
		Element placemark = kmlDoc.createElementNS(KML_NS, "Placemark");

		Object idName = entity.getPropertyValue(OxPointsVocab.hasOUCSCode);
		if (idName != null)
			placemark.setAttribute("OUCSCode", idName.toString());
		idName = entity.getPropertyValue(OxPointsVocab.hasOLISCode);
		if (idName != null)
			placemark.setAttribute("OLISCode", idName.toString());
		placemark.setAttribute("id", entity.getUri());
		parentEl.appendChild(placemark);
		addNameToElement(kmlDoc, placemark, entity);
		addDescriptionToElement(kmlDoc, placemark, entity);
		addPointToElement(kmlDoc, placemark, entity);
	}

	/**
	 * Tries to find the location that should be added to the placemark.
	 * @param kmlDoc
	 * @param parentEl
	 * @param entity
	 */
	private void addPointToElement(Document kmlDoc, Element parentEl, GabotoEntity entity) {
	    Object long_ = entity.getPropertyValue(GeoVocab.long_);
	    Object lat = entity.getPropertyValue(GeoVocab.lat);
	    
		if(long_ != null && lat != null){
			// add Point to placemark
			Element pointEl = kmlDoc.createElementNS(KML_NS, "Point");
			parentEl.appendChild(pointEl);
			
			Element coordinatesEl = kmlDoc.createElementNS(KML_NS, "coordinates");
			pointEl.appendChild(coordinatesEl);
			
			String latlong = long_.toString() + "," + lat.toString();
			//add coordinates @todo .. take care of number format
			coordinatesEl.appendChild(kmlDoc.createTextNode(latlong));
		}
	}

	/**
	 * Tries to find the name that should be added to the placemark
	 * @param kmlDoc
	 * @param parentEl
	 * @param entity
	 */
	private void addNameToElement(Document kmlDoc, Element parentEl, GabotoEntity entity) {
		String name = (String) entity.getPropertyValue(DCVocab.title);
		
		if(name != null){
			// traverse to parent
			if(isDisplayParentName()){
				name += getParentsNameRecursive(entity);
			}
			
			// add name to placemark
			Element nameEl = kmlDoc.createElementNS(KML_NS, "name");
			nameEl.appendChild(kmlDoc.createTextNode(name));
			parentEl.appendChild(nameEl);
			
		}
	}
	
	private String getParentsNameRecursive(GabotoEntity entity) {
		Object obj = entity.getPropertyValue(GabotoKMLVocab.parent_URI);
		
		if(obj instanceof GabotoEntity){
			GabotoEntity parent = (GabotoEntity) obj;
			
			String name = (String) parent.getPropertyValue(DCVocab.title);
			
			if(name != null){
				return ", " + name + getParentsNameRecursive(parent);
			}
		}
		
		return "";
	}

	private void addDescriptionToElement(Document kmlDoc, Element parentEl, GabotoEntity entity) {
		final ObjectProperty[] websiteProperties = {FOAFVocab.homepage, OxPointsVocab.hasLibraryHomepage, OxPointsVocab.hasITHomepage, OxPointsVocab.hasWeblearn};
		final String[] websiteNames = {"Homepage", "Library homepage", "IT homepage", "WebLearn"};

		String description = (String) entity.getPropertyValue(DCVocab.description);
		
		List<String> websites = new LinkedList<String>();
		for (int i = 0; i < websiteProperties.length; i++) {
			Object website = entity.getPropertyValue(websiteProperties[i]);
			if (website != null && website instanceof GabotoEntity) {
				String url = ((GabotoEntity) website).getUri();
				websites.add(websiteNames[i] + ": <a href=\"" + StringEscapeUtils.escapeXml(url) + "\">" + StringEscapeUtils.escapeHtml(url) + "</a>");
			}
		}
		
		description = (description == null) ? "" : ("<p>" + StringEscapeUtils.escapeHtml(description) + "</p>");
		description += (websites.size() == 0) ? "" : ("<p>" + StringUtils.join(websites, "<br/>") + "</p>");
		
		if(description.length() > 0){
			
			// add description to placemark
			Element descriptionEl = kmlDoc.createElementNS(KML_NS, "description");
			CDATASection cdata = kmlDoc.createCDATASection(description);
			descriptionEl.appendChild(cdata);
			parentEl.appendChild(descriptionEl);
		}
	}

	private Document getKMLDocumentTemplate(){
		Document kmlDoc = XMLUtils.getNewEmptyJAXPDoc();
		
		// create root element
		Element kml = kmlDoc.createElementNS(KML_NS, "kml");
		kmlDoc.appendChild(kml);
		
		// create document element
		Element documentEl = kmlDoc.createElementNS(KML_NS, "Document");
		kml.appendChild(documentEl);
		
		return kmlDoc;
	}

	
	/**
	 * Each entity type set here is transformed into a KML folder element.
	 * 
	 * <p>
	 * The collection is a collection of URIs describing the properties that
	 * return entities which are to be transformed.
	 * </p>
	 * 
	 * @param entityFolderTypes the entityFolderTypes to set
	 */
	public void setEntityFolderTypes(Map<String, Collection<String>> entityFolderTypes) {
		this.entityFolderTypes = entityFolderTypes;
	}

	/**
	 * Returns the entity types that are transformed into folder elements.
	 * @return the entityFolderTypes
	 */
	public Map<String, Collection<String>> getEntityFolderTypes() {
		return entityFolderTypes;
	}

	/**
	 * @param entityFolderType The entityFolderType to add.
	 * @param transformationProperties The properties to be transformed.
	 */
	public void addEntityFolderType(String entityFolderType, Collection<String> transformationProperties){
		this.entityFolderTypes.put(entityFolderType,transformationProperties);
	}

	/**
	 * 
	 * @param entityFolderType
	 * @param transformationProperty
	 */
	public void addEntityFolderType(String entityFolderType, String transformationProperty) {
		Collection<String> placemarkProperties = new HashSet<String>();
		placemarkProperties.add(transformationProperty);
		this.entityFolderTypes.put(entityFolderType,placemarkProperties);
	}

	/**
	 * Defines the output order 
	 * @param orderBy the orderBy to set
	 */
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	/**
	 * @return the orderBy
	 */
	public String getOrderBy() {
		return orderBy;
	}

	/**
	 * @param displayParentName the displayParentName to set
	 */
	public void setDisplayParentName(boolean displayParentName) {
		this.displayParentName = displayParentName;
	}

	/**
	 * @return the displayParentName
	 */
	public boolean isDisplayParentName() {
		return displayParentName;
	}

  /**
   * @param nesting the nesting level 
   */
  public void setNesting(int nesting) {
    if(nesting < 1 || nesting > MAX_NESTING)
      throw new IllegalArgumentException("Nesting has to be between 0 and " + MAX_NESTING);
    this.nesting = nesting;
  }

  /**
   * @return the nesting
   */
  public int getNesting() {
    return nesting;
  }
	
}
