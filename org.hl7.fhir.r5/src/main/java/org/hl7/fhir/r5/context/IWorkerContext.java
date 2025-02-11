package org.hl7.fhir.r5.context;

import java.util.EnumSet;

/*-
 * #%L
 * org.hl7.fhir.r5
 * %%
 * Copyright (C) 2014 - 2019 Health Level 7
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fhir.ucum.UcumService;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.TerminologyServiceException;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.ParserType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureMap;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.terminologies.ValueSetExpander.TerminologyServiceErrorClass;
import org.hl7.fhir.r5.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.r5.utils.INarrativeGenerator;
import org.hl7.fhir.r5.utils.IResourceValidator;
import org.hl7.fhir.utilities.TerminologyServiceOptions;
import org.hl7.fhir.utilities.TranslationServices;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;


/**
 * This is the standard interface used for access to underlying FHIR
 * services through the tools and utilities provided by the reference
 * implementation. 
 * 
 * The functionality it provides is 
 *  - get access to parsers, validators, narrative builders etc
 *    (you can't create these directly because they need access 
 *    to the right context for their information)
 *    
 *  - find resources that the tools need to carry out their tasks
 *  
 *  - provide access to terminology services they need. 
 *    (typically, these terminology service requests are just
 *    passed through to the local implementation's terminology
 *    service)    
 *  
 * @author Grahame
 */
public interface IWorkerContext {

  /**
   * Get the versions of the definitions loaded in context
   * @return
   */
  public String getVersion();
  
  // get the UCUM service (might not be available)
  public UcumService getUcumService();
  
  // -- Parsers (read and write instances) ----------------------------------------


  /**
   * Get a parser to read/write instances. Use the defined type (will be extended 
   * as further types are added, though the only currently anticipate type is RDF)
   * 
   * XML/JSON - the standard renderers
   * XHTML - render the narrative only (generate it if necessary)
   * 
   * @param type
   * @return
   */
  public IParser getParser(ParserType type);

  /**
   * Get a parser to read/write instances. Determine the type 
   * from the stated type. Supported value for type:
   * - the recommended MIME types
   * - variants of application/xml and application/json
   * - _format values xml, json
   * 
   * @param type
   * @return
   */	
  public IParser getParser(String type);

  /**
   * Get a JSON parser
   * 
   * @return
   */
  public IParser newJsonParser();

  /**
   * Get an XML parser
   * 
   * @return
   */
  public IParser newXmlParser();

  /**
   * Get a generator that can generate narrative for the instance
   * 
   * @return a prepared generator
   */
  public INarrativeGenerator getNarrativeGenerator(String prefix, String basePath);

  /**
   * Get a validator that can check whether a resource is valid 
   * 
   * @return a prepared generator
   * @throws FHIRException 
   * @
   */
  public IResourceValidator newValidator() throws FHIRException;

  // -- resource fetchers ---------------------------------------------------

  /**
   * Find an identified resource. The most common use of this is to access the the 
   * standard conformance resources that are part of the standard - structure 
   * definitions, value sets, concept maps, etc.
   * 
   * Also, the narrative generator uses this, and may access any kind of resource
   * 
   * The URI is called speculatively for things that might exist, so not finding 
   * a matching resouce, return null, not an error
   * 
   * The URI can have one of 3 formats:
   *  - a full URL e.g. http://acme.org/fhir/ValueSet/[id]
   *  - a relative URL e.g. ValueSet/[id]
   *  - a logical id e.g. [id]
   *  
   * It's an error if the second form doesn't agree with class_. It's an 
   * error if class_ is null for the last form
   * 
   * @param resource
   * @param Reference
   * @return
   * @throws FHIRException 
   * @throws Exception
   */
  public <T extends Resource> T fetchResource(Class<T> class_, String uri);
  public <T extends Resource> T fetchResourceWithException(Class<T> class_, String uri) throws FHIRException;

  /**
   * Variation of fetchResource when you have a string type, and don't need the right class
   * 
   * The URI can have one of 3 formats:
   *  - a full URL e.g. http://acme.org/fhir/ValueSet/[id]
   *  - a relative URL e.g. ValueSet/[id]
   *  - a logical id e.g. [id]
   *  
   * if type == null, the URI can't be a simple logical id
   * 
   * @param type
   * @param uri
   * @return
   */
  public Resource fetchResourceById(String type, String uri);
  
  /**
   * find whether a resource is available. 
   * 
   * Implementations of the interface can assume that if hasResource ruturns 
   * true, the resource will usually be fetched subsequently
   * 
   * @param class_
   * @param uri
   * @return
   */
  public <T extends Resource> boolean hasResource(Class<T> class_, String uri);

  /**
   * cache a resource for later retrieval using fetchResource.
   * 
   * Note that various context implementations will have their own ways of loading
   * rseources, and not all need implement cacheResource 
   * @param res
   * @throws FHIRException 
   */
  public void cacheResource(Resource res) throws FHIRException;
  
  // -- profile services ---------------------------------------------------------
  
  /**
   * @return a list of the resource names defined for this version
   */
  public List<String> getResourceNames();
  /**
   * @return a set of the resource names defined for this version
   */
  public Set<String> getResourceNamesAsSet();

  /**
   * @return a list of the resource and type names defined for this version
   */
  public List<String> getTypeNames();
  
  /**
   * @return a list of all structure definitions, with snapshots generated (if possible)
   */
  public List<StructureDefinition> allStructures();
  
  /**
   * @return a list of all structure definitions, without trying to generate snapshots
   */
  public List<StructureDefinition> getStructures();
  
  /**
   * @return a list of all conformance resources
   */
  public List<MetadataResource> allConformanceResources();
  
  /**
   * Given a structure definition, generate a snapshot (or regenerate it)
   * @param p
   * @throws DefinitionException
   * @throws FHIRException
   */
  public void generateSnapshot(StructureDefinition p) throws DefinitionException, FHIRException;
  public void generateSnapshot(StructureDefinition mr, boolean ifLogical);
  
  // -- Terminology services ------------------------------------------------------

  /**
   * Set the expansion parameters passed through the terminology server when txServer calls are made
   * 
   * Note that the Validation Options override these when they are specified on validateCode
   */
  public Parameters getExpansionParameters();

  /**
   * Get the expansion parameters passed through the terminology server when txServer calls are made
   * 
   * Note that the Validation Options override these when they are specified on validateCode
   */
  public void setExpansionProfile(Parameters expParameters);

  // these are the terminology services used internally by the tools
  /**
   * Find the code system definition for the nominated system uri. 
   * return null if there isn't one (then the tool might try 
   * supportsSystem)
   * 
   * @param system
   * @return
   */
  public CodeSystem fetchCodeSystem(String system);

  /**
   * True if the underlying terminology service provider will do 
   * expansion and code validation for the terminology. Corresponds
   * to the extension 
   * 
   * http://hl7.org/fhir/StructureDefinition/capabilitystatement-supported-system
   * 
   * in the Conformance resource
   * 
   * @param system
   * @return
   * @throws Exception 
   */
  public boolean supportsSystem(String system) throws TerminologyServiceException;

  /**
   * find concept maps for a source
   * @param url
   * @return
   * @throws FHIRException 
   */
  public List<ConceptMap> findMapsForSource(String url) throws FHIRException;  

  /**
   * ValueSet Expansion - see $expand
   *  
   * @param source
   * @return
   */
  public ValueSetExpansionOutcome expandVS(ValueSet source, boolean cacheOk, boolean heiarchical);
  
  /**
   * ValueSet Expansion - see $expand, but resolves the binding first
   *  
   * @param source
   * @return
   * @throws FHIRException 
   */
  public ValueSetExpansionOutcome expandVS(ElementDefinitionBindingComponent binding, boolean cacheOk, boolean heiarchical) throws FHIRException;
  
  /**
   * Value set expanion inside the internal expansion engine - used 
   * for references to supported system (see "supportsSystem") for
   * which there is no value set. 
   * 
   * @param inc
   * @return
   * @throws FHIRException 
   */
  public ValueSetExpansionOutcome expandVS(ConceptSetComponent inc, boolean hierarchical) throws TerminologyServiceException;
  
  public class ValidationResult {
    private ConceptDefinitionComponent definition;
    private IssueSeverity severity;
    private String message;
    private TerminologyServiceErrorClass errorClass;
    private String txLink;
    
    public ValidationResult(IssueSeverity severity, String message) {
      this.severity = severity;
      this.message = message;
    }
    
    public ValidationResult(ConceptDefinitionComponent definition) {
      this.definition = definition;
    }

    public ValidationResult(IssueSeverity severity, String message, ConceptDefinitionComponent definition) {
      this.severity = severity;
      this.message = message;
      this.definition = definition;
    }
    
    public ValidationResult(IssueSeverity severity, String message, TerminologyServiceErrorClass errorClass) {
      this.severity = severity;
      this.message = message;
      this.errorClass = errorClass;
    }

    public boolean isOk() {
      return severity == null || severity == IssueSeverity.INFORMATION || severity == IssueSeverity.WARNING;
    }

    public String getDisplay() {
// We don't want to return question-marks because that prevents something more useful from being displayed (e.g. the code) if there's no display value
//      return definition == null ? "??" : definition.getDisplay();
      return definition == null ? null : definition.getDisplay();
    }

    public ConceptDefinitionComponent asConceptDefinition() {
      return definition;
    }

    public IssueSeverity getSeverity() {
      return severity;
    }

    public String getMessage() {
      return message;
    }

    public boolean IsNoService() {
      return errorClass == TerminologyServiceErrorClass.NOSERVICE;
    }

    public TerminologyServiceErrorClass getErrorClass() {
      return errorClass;
    }

    public ValidationResult setSeverity(IssueSeverity severity) {
      this.severity = severity;
      return this;
    }

    public ValidationResult setMessage(String message) {
      this.message = message;
      return this;
    }

    public String getTxLink() {
      return txLink;
    }

    public ValidationResult setTxLink(String txLink) {
      this.txLink = txLink;
      return this;
    }
    
    
  }

  /**
   * Validation of a code - consult the terminology infrstructure and/or service 
   * to see whether it is known. If known, return a description of it
   * 
   * note: always return a result, with either an error or a code description
   *  
   * corresponds to 2 terminology service calls: $validate-code and $lookup
   * 
   * in this case, the system will be inferred from the value set. It's an error to call this one without the value set
   * 
   * @param options - validation options (required)
   * @param code he code to validate (required)
   * @param vs the applicable valueset (required)
   * @return
   */
  public ValidationResult validateCode(ValidationOptions options, String code, ValueSet vs);
  
  /**
   * Validation of a code - consult the terminology infrstructure and/or service 
   * to see whether it is known. If known, return a description of it
   * 
   * note: always return a result, with either an error or a code description
   *  
   * corresponds to 2 terminology service calls: $validate-code and $lookup
   * 
   * @param options - validation options (required)
   * @param system - equals Coding.system (required)
   * @param code - equals Coding.code (required)
   * @param display - equals Coding.display (optional)
   * @return
   */
  public ValidationResult validateCode(ValidationOptions options, String system, String code, String display);
  
  /**
   * Validation of a code - consult the terminology infrstructure and/or service 
   * to see whether it is known. If known, return a description of it
   * 
   * note: always return a result, with either an error or a code description
   *  
   * corresponds to 2 terminology service calls: $validate-code and $lookup
   * 
   * @param options - validation options (required)
   * @param system - equals Coding.system (required)
   * @param code - equals Coding.code (required)
   * @param display - equals Coding.display (optional)
   * @param vs the applicable valueset (optional)
   * @return
   */
  public ValidationResult validateCode(ValidationOptions options, String system, String code, String display, ValueSet vs);

  /**
   * Validation of a code - consult the terminology infrstructure and/or service 
   * to see whether it is known. If known, return a description of it
   * 
   * note: always return a result, with either an error or a code description
   *  
   * corresponds to 2 terminology service calls: $validate-code and $lookup
   * 
   * Note that this doesn't validate binding strength (e.g. is just text allowed?)
   * 
   * @param options - validation options (required)
   * @param code - CodeableConcept to validate
   * @param vs the applicable valueset (optional)
   * @return
   */
  public ValidationResult validateCode(ValidationOptions options, CodeableConcept code, ValueSet vs);

  /**
   * Validation of a code - consult the terminology infrstructure and/or service 
   * to see whether it is known. If known, return a description of it
   * 
   * note: always return a result, with either an error or a code description
   *  
   * corresponds to 2 terminology service calls: $validate-code and $lookup
   * 
   * in this case, the system will be inferred from the value set. It's an error to call this one without the value set
   * 
   * @param options - validation options (required)
   * @param code - Coding to validate
   * @param vs the applicable valueset (optional)
   * @return
   */
  public ValidationResult validateCode(ValidationOptions options, Coding code, ValueSet vs);
  
  /**
   * returns the recommended tla for the type  (from the structure definitions)
   * 
   * @param name
   * @return
   */
  public String getAbbreviation(String name);


  /**
   * translate an OID to a URI (look through known NamingSystems)
   * @param code
   * @return
   */
	public String oid2Uri(String code);

	/** 
	 * @return true if the contxt has a terminology caching service internally
	 */
  public boolean hasCache();

  public interface ILoggingService {
    public enum LogCategory {
      INIT, 
      PROGRESS,
      TX, 
      CONTEXT, 
      GENERATE,
      HTML 
    }
    public void logMessage(String message); // status messages, always display
    public void logDebugMessage(LogCategory category, String message); // verbose; only when debugging 
  }

  public void setLogger(ILoggingService logger);
  public ILoggingService getLogger();

  public boolean isNoTerminologyServer();

  public TranslationServices translator();
  public List<StructureMap> listTransforms();
  public StructureMap getTransform(String url);

  public String getOverrideVersionNs();
  public void setOverrideVersionNs(String value);

  public StructureDefinition fetchTypeDefinition(String typeName);

  public void setUcumService(UcumService ucumService);

  public String getLinkForUrl(String corePath, String s);
  public Map<String, byte[]> getBinaries();


}
