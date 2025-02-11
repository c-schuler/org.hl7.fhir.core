package org.hl7.fhir.r5.validation;

/*-
 * #%L
 * org.hl7.fhir.validation
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.convertors.VersionConvertorConstants;
import org.hl7.fhir.convertors.VersionConvertor_10_50;
import org.hl7.fhir.convertors.VersionConvertor_14_50;
import org.hl7.fhir.convertors.VersionConvertor_30_50;
import org.hl7.fhir.convertors.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.PathEngineException;
import org.hl7.fhir.exceptions.TerminologyServiceException;
import org.hl7.fhir.r5.conformance.ProfileUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.IWorkerContext.ValidationResult;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Element.SpecialElement;
import org.hl7.fhir.r5.elementmodel.JsonParser;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.elementmodel.ObjectConverter;
import org.hl7.fhir.r5.elementmodel.ParserBase;
import org.hl7.fhir.r5.elementmodel.ParserBase.ValidationPolicy;
import org.hl7.fhir.r5.elementmodel.XmlParser;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.model.Address;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Constants;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.DateType;
import org.hl7.fhir.r5.model.DecimalType;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.ElementDefinition.AggregationMode;
import org.hl7.fhir.r5.model.ElementDefinition.ConstraintSeverity;
import org.hl7.fhir.r5.model.ElementDefinition.DiscriminatorType;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionConstraintComponent;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionMappingComponent;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionSlicingDiscriminatorComponent;
import org.hl7.fhir.r5.model.ElementDefinition.PropertyRepresentation;
import org.hl7.fhir.r5.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.Enumerations.BindingStrength;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.model.ExpressionNode;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.FhirPublication;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.ImplementationGuide.ImplementationGuideGlobalComponent;
import org.hl7.fhir.r5.model.InstantType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.Quantity;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r5.model.Range;
import org.hl7.fhir.r5.model.Ratio;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.SampledData;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionMappingComponent;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionSnapshotComponent;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.model.TimeType;
import org.hl7.fhir.r5.model.Timing;
import org.hl7.fhir.r5.model.Type;
import org.hl7.fhir.r5.model.TypeDetails;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.r5.terminologies.ValueSetUtilities;
import org.hl7.fhir.r5.utils.FHIRLexer.FHIRLexerException;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.r5.utils.FHIRPathEngine.IEvaluationContext;
import org.hl7.fhir.r5.utils.IResourceValidator;
import org.hl7.fhir.r5.utils.ToolingExtensions;
import org.hl7.fhir.r5.utils.ValidationProfileSet;
import org.hl7.fhir.r5.utils.ValidationProfileSet.ProfileRegistration;
import org.hl7.fhir.r5.validation.EnableWhenEvaluator.QStack;
import org.hl7.fhir.r5.validation.InstanceValidator.EntrySummary;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.TerminologyServiceOptions;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.Utilities.DecimalStatus;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ca.uhn.fhir.util.ObjectUtil;


/**
 * Thinking of using this in a java program? Don't! 
 * You should use one of the wrappers instead. Either in HAPI, or use ValidationEngine
 * 
 * @author Grahame Grieve
 *
 */
/* 
 * todo:
 * check urn's don't start oid: or uuid:
 * check MetadataResource.url is absolute 
 */

public class InstanceValidator extends BaseValidator implements IResourceValidator {


  public class ValidatorHostContext {
    private Object appContext;
    private Element container; // bundle, or parameters
    private Element resource;
    private Element rootResource;
    private StructureDefinition profile; // the profile that contains the content being validated
    public ValidatorHostContext(Object appContext) {
      this.appContext = appContext;
    }
    
    public ValidatorHostContext(Object appContext, Element element) {
      this.appContext = appContext;
      this.resource = element;
      this.rootResource = element;
    }
    
    public ValidatorHostContext forContained(Element element) {
      ValidatorHostContext res = new ValidatorHostContext(appContext);
      res.rootResource = resource;
      res.resource = element;
      res.container = resource;
      res.profile = profile;
      return res;
    }

    public ValidatorHostContext forEntry(Element element) {
      ValidatorHostContext res = new ValidatorHostContext(appContext);
      res.rootResource = element;
      res.resource = element;
      res.container = resource;
      res.profile = profile;
      return res;
    }

    public ValidatorHostContext forProfile(StructureDefinition profile) {
      ValidatorHostContext res = new ValidatorHostContext(appContext);
      res.resource = resource;
      res.rootResource = rootResource;
      res.container = container;
      res.profile = profile;
      return res;
    }
  }
  
  private class ValidatorHostServices implements IEvaluationContext {

    @Override
    public Base resolveConstant(Object appContext, String name, boolean beforeContext) throws PathEngineException {
      ValidatorHostContext c = (ValidatorHostContext) appContext;
      if (externalHostServices != null)
        return externalHostServices.resolveConstant(c.appContext, name, beforeContext);
      else
        return null;
    }

    @Override
    public TypeDetails resolveConstantType(Object appContext, String name) throws PathEngineException {
      ValidatorHostContext c = (ValidatorHostContext) appContext;
      if (externalHostServices != null)
        return externalHostServices.resolveConstantType(c.appContext, name);
      else
        return null;
    }

    @Override
    public boolean log(String argument, List<Base> focus) {
      if (externalHostServices != null)
        return externalHostServices.log(argument, focus);
      else 
        return false;
    }

    @Override
    public FunctionDetails resolveFunction(String functionName) {
      throw new Error("Not done yet (ValidatorHostServices.resolveFunction): "+functionName);
    }

    @Override
    public TypeDetails checkFunction(Object appContext, String functionName, List<TypeDetails> parameters) throws PathEngineException {
      throw new Error("Not done yet (ValidatorHostServices.checkFunction)");
    }

    @Override
    public List<Base> executeFunction(Object appContext, String functionName, List<List<Base>> parameters) {
      throw new Error("Not done yet (ValidatorHostServices.executeFunction)");
    }

    @Override
    public Base resolveReference(Object appContext, String url) throws FHIRException {
      ValidatorHostContext c = (ValidatorHostContext) appContext;
      
      if (c.appContext instanceof Element)  {
        Element bnd = (Element) c.appContext;
        Base res = resolveInBundle(url, bnd);
        if (res != null)
          return res;
      }
      Base res = resolveInBundle(url, c.resource);
      if (res != null)
        return res;
      res = resolveInBundle(url, c.container);
      if (res != null)
        return res;
      
      if (externalHostServices != null)
        return externalHostServices.resolveReference(c.appContext, url);
      else if (fetcher != null)
        try {
          return fetcher.fetch(c.appContext, url);
        } catch (IOException e) {
          throw new FHIRException(e);
        }
      else
        throw new Error("Not done yet - resolve "+url+" locally (2)");
          
    }

    public Base resolveInBundle(String url, Element bnd) {
      if (bnd == null)
        return null;
      if (bnd.fhirType().equals("Bundle")) {
        for (Element be : bnd.getChildrenByName("entry")) {
          Element res = be.getNamedChild("resource");
          if (res != null) { 
            String fullUrl = be.getChildValue("fullUrl");
            String rt = res.fhirType();
            String id = res.getChildValue("id");
            if (url.equals(fullUrl))
              return res;
            if (url.equals(rt+"/"+id))
              return res;
          }
        }
      }
      return null;
    }

    @Override
    public boolean conformsToProfile(Object appContext, Base item, String url) throws FHIRException {
      IResourceValidator val = new InstanceValidator(context, this);
      List<ValidationMessage> valerrors = new ArrayList<ValidationMessage>();
      if (item instanceof Resource) {
        val.validate(appContext, valerrors, (Resource) item, url);
      } else if (item instanceof Element) {
        val.validate(appContext, valerrors, (Element) item, url);
      } else
        throw new NotImplementedException("Not done yet (ValidatorHostServices.conformsToProfile), when item is element");
      boolean ok = true;
      for (ValidationMessage v : valerrors)
        ok = ok && !v.getLevel().isError();
      return ok;
    }

    @Override
    public ValueSet resolveValueSet(Object appContext, String url) {
      ValidatorHostContext c = (ValidatorHostContext) appContext;
      if (c.profile != null && url.startsWith("#")) {
        for (Resource r : c.profile.getContained()) {
          if (r.getId().equals(url.substring(1))) {
            if (r instanceof ValueSet)
              return (ValueSet) r;
            else
              throw new FHIRException("Reference "+url+" refers to a "+r.fhirType()+" not a ValueSet");
          }
        }
        return null;
      }
      return context.fetchResource(ValueSet.class, url);
    }

  }

  private IWorkerContext context;
  private FHIRPathEngine fpe;

  // configuration items
  private CheckDisplayOption checkDisplay;
  private boolean anyExtensionsAllowed;
  private boolean errorForUnknownProfiles;
  private boolean noInvariantChecks;
  private boolean noTerminologyChecks;
  private boolean hintAboutNonMustSupport;
  private BestPracticeWarningLevel bpWarnings;
  private String validationLanguage;

  private List<String> extensionDomains = new ArrayList<String>();

  private IdStatus resourceIdRule;
  private boolean allowXsiLocation;

  // used during the build process to keep the overall volume of messages down
  private boolean suppressLoincSnomedMessages;

  // time tracking
  private long overall = 0;
  private long txTime = 0;
  private long sdTime = 0;
  private long loadTime = 0;
  private long fpeTime = 0;

  private boolean noBindingMsgSuppressed;
  private boolean debug;
  private HashMap<Element, ResourceProfiles> resourceProfilesMap;
  private IValidatorResourceFetcher fetcher;
  long time = 0;
  private ValidationProfileSet providedProfiles;
  private IEvaluationContext externalHostServices;
  private boolean noExtensibleWarnings;
  private String serverBase;
  
  private EnableWhenEvaluator myEnableWhenEvaluator = new EnableWhenEvaluator();
  private String executionId;
  private XVerExtensionManager xverManager;

  /*
   * Keeps track of whether a particular profile has been checked or not yet
   */
  private class ProfileUsage {
    private StructureDefinition profile;
    private boolean checked;

    public ProfileUsage(StructureDefinition profile) {
      this.profile = profile;
      this.checked = false;
    }

    public boolean isChecked() {
      return checked;
    }

    public void setChecked() {
      this.checked = true;
    }

    public StructureDefinition getProfile() {
      return profile;
    }
  }

  /*
   * Keeps track of all profiles associated with a resource element and whether the resource has been checked against those profiles yet
   */
  public class ResourceProfiles {
    private Element resource;
    private Element owner;
    private NodeStack stack;
    private HashMap<StructureDefinition, ProfileUsage> profiles;
    private boolean processed;

    public ResourceProfiles(Element resource, NodeStack stack) {
      this.resource = resource;
      if (this.resource.getName().equals("contained") && stack.parent != null)
        this.owner = stack.parent.element;
      else
        this.owner = resource;
      this.stack = stack;
      this.profiles = new HashMap<StructureDefinition, ProfileUsage>();
      this.processed = false;
    }

    public boolean isProcessed() {
      return processed;
    }

    public void setProcessed() {
      processed = true;
    }

    public NodeStack getStack() {
      return stack;
    }

    public Element getOwner() {
      return owner;
    }

    public boolean hasProfiles() {
      return !profiles.isEmpty();
    }

    public void addProfiles(List<ValidationMessage> errors, ValidationProfileSet profiles, String path, Element element, boolean external) throws FHIRException {
      for (ProfileRegistration profile : profiles.getCanonical()) {
        StructureDefinition sd = profiles.fetch(profile.getProfile());
        if (sd == null)
          sd = context.fetchResource(StructureDefinition.class, profile.getProfile());
        if (sd == null) {
          ImplementationGuide ig = context.fetchResource(ImplementationGuide.class, profile.getProfile());
          if (ig != null) {
            for (ImplementationGuideGlobalComponent t : ig.getGlobal()) {
              if (t.getType().equals(element.fhirType())) {
                sd = context.fetchResource(StructureDefinition.class, t.getProfile());
                if (sd != null)
                  break;
              }
            }
          }
        }
        if (sd == null) {          
          errors.add(new ValidationMessage(Source.InstanceValidator, IssueType.UNKNOWN, path, "Unable to locate profile "+profile.getProfile(), external ? IssueSeverity.ERROR : IssueSeverity.WARNING));
        } else if (!sd.getType().equals(element.fhirType())) {
          boolean ok = false;
          if (element.fhirType().equals("Bundle")) { // special case: if the profile type isn't 'Bundle', then the profile applies to the first resource
            List<Element> entries = element.getChildren("entry");
            if (entries.size() > 0) {
              Element res = entries.get(0).getNamedChild("resource");
              if (res != null) {
                ok = true;
          //       addProfile(errors, profile.getProfile(), profile.isError(), path, res);  ggtodo: we need to go add this to a different profile          
              }
            }
          }
            
          if (!ok)
            errors.add(new ValidationMessage(Source.InstanceValidator, IssueType.UNKNOWN, path, "Profile mismatch on type for "+profile.getProfile()+": the profile constrains "+sd.getType()+" but the element is "+element.fhirType(), IssueSeverity.ERROR));
        } else 
          addProfile(errors, profile.getProfile(), profile.isErrorOnMissing(), path, element, sd);
      }
    }
    
    public boolean addProfile(List<ValidationMessage> errors, String profile, boolean errorOnMissing, String path, Element element, StructureDefinition containingProfile) {
      String effectiveProfile = profile;
      String version = null;
      if (profile.contains("|")) {
        effectiveProfile = profile.substring(0, profile.indexOf('|'));
        version = profile.substring(profile.indexOf('|')+1);
      }
      StructureDefinition sd = null;
      if (profile.startsWith("#")) {
        if (!rule(errors, IssueType.INVALID, element.line(), element.col(), path, containingProfile != null, "StructureDefinition reference \"{0}\" is local, but there is not local context", profile)) {
          return false;
        }
          
        if (containingProfile.hasUserData("container"))
          containingProfile = (StructureDefinition) containingProfile.getUserData("container");
        sd = (StructureDefinition) containingProfile.getContained(profile);
        if (sd != null)
          sd.setUserData("container", containingProfile);
      } else {
        if (providedProfiles != null)
          sd = providedProfiles.fetch(effectiveProfile);
        if (sd == null)
          sd = context.fetchResource(StructureDefinition.class, effectiveProfile);
      }
      
      if (warningOrError(errorOnMissing, errors, IssueType.INVALID, element.line(), element.col(), path, sd != null, "StructureDefinition reference \"{0}\" could not be resolved", profile)) {
        if (rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, version==null || (sd.getVersion()!=null && sd.getVersion().equals(version)), 
             "Referenced version " + version + " does not match found version " + sd.getVersion() + " for profile " + sd.getUrl(), profile)) {
          if (rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, sd.hasSnapshot(),
              "StructureDefinition has no snapshot - validation is against the snapshot, so it must be provided")) {
            if (!profiles.containsKey(sd)) {
              profiles.put(sd,  new ProfileUsage(sd));
              addAncestorProfiles(sd);
              return true;
            }
          }
        }
      }
      return false;
    }

    public void addAncestorProfiles(StructureDefinition sd) {
      if (sd.hasDerivation() && sd.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
        StructureDefinition parentSd = context.fetchResource(StructureDefinition.class, sd.getBaseDefinition());
        if (parentSd != null && !profiles.containsKey(parentSd)) {
          ProfileUsage pu = new ProfileUsage(parentSd);
          pu.setChecked(); // We're going to check the child, so no need to check the parent
          profiles.put(parentSd, pu);
        }
      }
    }

    public List<ProfileUsage> uncheckedProfiles() {
      List<ProfileUsage> uncheckedProfiles = new ArrayList<ProfileUsage>();
      for (ProfileUsage profileUsage : profiles.values()) {
        if (!profileUsage.isChecked())
          uncheckedProfiles.add(profileUsage);
      }
      return uncheckedProfiles;
    }

    public boolean hasUncheckedProfiles() {
      return !uncheckedProfiles().isEmpty();
    }

    public void checkProfile(StructureDefinition profile) {
      ProfileUsage profileUsage = profiles.get(profile);
      if (profileUsage==null)
        throw new Error("Can't check profile that hasn't been added: " + profile.getUrl());
      else
        profileUsage.setChecked();
    }
  }

  public InstanceValidator(IWorkerContext theContext, IEvaluationContext hostServices) {
    super();
    this.context = theContext;
    this.externalHostServices = hostServices;
    fpe = new FHIRPathEngine(context);
    fpe.setHostServices(new ValidatorHostServices());
    if (theContext.getVersion().startsWith("3.0") || theContext.getVersion().startsWith("1.0"))
      fpe.setLegacyMode(true);
    source = Source.InstanceValidator;
  }

  @Override
  public boolean isNoExtensibleWarnings() {
    return noExtensibleWarnings;
  }
  
  @Override
  public IResourceValidator setNoExtensibleWarnings(boolean noExtensibleWarnings) {
    this.noExtensibleWarnings = noExtensibleWarnings;
    return this;
  }

  @Override
  public boolean isNoInvariantChecks() {
    return noInvariantChecks;
  }

  @Override
  public IResourceValidator setNoInvariantChecks(boolean value) {
    this.noInvariantChecks = value;
    return this;
  }

  public IValidatorResourceFetcher getFetcher() {
    return this.fetcher;
  }

  public IResourceValidator setFetcher(IValidatorResourceFetcher value) {
    this.fetcher = value;
    return this;
  }

  
  public boolean isHintAboutNonMustSupport() {
    return hintAboutNonMustSupport;
  }

  public void setHintAboutNonMustSupport(boolean hintAboutNonMustSupport) {
    this.hintAboutNonMustSupport = hintAboutNonMustSupport;
  }

  private boolean allowUnknownExtension(String url) {
    if (url.contains("example.org") || url.contains("acme.com") || url.contains("nema.org") || url.startsWith("http://hl7.org/fhir/tools/StructureDefinition/") || url.equals("http://hl7.org/fhir/StructureDefinition/structuredefinition-expression"))
      // Added structuredefinition-expression explicitly because it wasn't defined in the version of the spec it needs to be used with
      return true;
    for (String s : extensionDomains)
      if (url.startsWith(s))
        return true;
    return anyExtensionsAllowed;
  }

  private boolean isKnownExtension(String url) {
    // Added structuredefinition-expression and following extensions explicitly because they weren't defined in the version of the spec they need to be used with
    if (url.contains("example.org") || url.contains("acme.com") || url.contains("nema.org") || url.startsWith("http://hl7.org/fhir/tools/StructureDefinition/") || url.equals("http://hl7.org/fhir/StructureDefinition/structuredefinition-expression") || url.equals(VersionConvertorConstants.IG_DEPENDSON_PACKAGE_EXTENSION))
      return true;
    for (String s : extensionDomains)
      if (url.startsWith(s))
        return true;
    return false;
  }

  private void bpCheck(List<ValidationMessage> errors, IssueType invalid, int line, int col, String literalPath, boolean test, String message) {
    if (bpWarnings != null) {
      switch (bpWarnings) {
      case Error:
        rule(errors, invalid, line, col, literalPath, test, message);
        break;
      case Warning:
        warning(errors, invalid, line, col, literalPath, test, message);
        break;
      case Hint:
        hint(errors, invalid, line, col, literalPath, test, message);
        break;
      default: // do nothing
			break;
      }
    }
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, InputStream stream, FhirFormat format) throws FHIRException {
    return validate(appContext, errors, stream, format, new ValidationProfileSet());
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, InputStream stream, FhirFormat format, String profile) throws FHIRException {
    return validate(appContext, errors, stream, format,  new ValidationProfileSet(profile, true));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, InputStream stream, FhirFormat format, StructureDefinition profile) throws FHIRException {
    return validate(appContext, errors, stream, format, new ValidationProfileSet(profile));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, InputStream stream, FhirFormat format, ValidationProfileSet profiles) throws FHIRException {
    ParserBase parser = Manager.makeParser(context, format);
    if (parser instanceof XmlParser)
      ((XmlParser) parser).setAllowXsiLocation(allowXsiLocation);
    parser.setupValidation(ValidationPolicy.EVERYTHING, errors);
    long t = System.nanoTime();
    Element e;
    try {
      e = parser.parse(stream);
    } catch (IOException e1) {
      throw new FHIRException(e1);
    }
    loadTime = System.nanoTime() - t;
    if (e != null)
      validate(appContext, errors, e, profiles);
    return e;
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Resource resource) throws FHIRException {
    return validate(appContext, errors, resource, new ValidationProfileSet());
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Resource resource, String profile) throws FHIRException {
    return validate(appContext, errors, resource, new ValidationProfileSet(profile, true));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Resource resource, StructureDefinition profile) throws FHIRException {
    return validate(appContext, errors, resource, new ValidationProfileSet(profile));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Resource resource, ValidationProfileSet profiles) throws FHIRException {
    long t = System.nanoTime();
    Element e;
    try {
      e = new ObjectConverter(context).convert(resource);
    } catch (IOException e1) {
      throw new FHIRException(e1);
    }
    loadTime = System.nanoTime() - t;
    validate(appContext, errors, e, profiles);
    return e;
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, org.w3c.dom.Element element) throws FHIRException {
    return validate(appContext, errors, element, new ValidationProfileSet());
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, org.w3c.dom.Element element, String profile) throws FHIRException {
    return validate(appContext, errors, element, new ValidationProfileSet(profile, true));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, org.w3c.dom.Element element, StructureDefinition profile) throws FHIRException {
    return validate(appContext, errors, element, new ValidationProfileSet(profile));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, org.w3c.dom.Element element, ValidationProfileSet profiles) throws FHIRException {
    XmlParser parser = new XmlParser(context);
    parser.setupValidation(ValidationPolicy.EVERYTHING, errors);
    long t = System.nanoTime();
    Element e;
    try {
      e = parser.parse(element);
    } catch (IOException e1) {
      throw new FHIRException(e1);
    }
    loadTime = System.nanoTime() - t;
    if (e != null)
      validate(appContext, errors, e, profiles);
    return e;
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Document document) throws FHIRException {
    return validate(appContext, errors, document, new ValidationProfileSet());
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Document document, String profile) throws FHIRException {
    return validate(appContext, errors, document, new ValidationProfileSet(profile, true));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Document document, StructureDefinition profile) throws FHIRException {
    return validate(appContext, errors, document, new ValidationProfileSet(profile));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, Document document, ValidationProfileSet profiles) throws FHIRException {
    XmlParser parser = new XmlParser(context);
    parser.setupValidation(ValidationPolicy.EVERYTHING, errors);
    long t = System.nanoTime();
    Element e;
    try {
      e = parser.parse(document);
    } catch (IOException e1) {
      throw new FHIRException(e1);
    }
    loadTime = System.nanoTime() - t;
    if (e != null)
      validate(appContext, errors, e, profiles);
    return e;
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, JsonObject object) throws FHIRException {
    return validate(appContext, errors, object, new ValidationProfileSet());
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, JsonObject object, String profile) throws FHIRException {
    return validate(appContext, errors, object, new ValidationProfileSet(profile, true));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, JsonObject object, StructureDefinition profile) throws FHIRException {
    return validate(appContext, errors, object, new ValidationProfileSet(profile));
  }

  @Override
  public org.hl7.fhir.r5.elementmodel.Element validate(Object appContext, List<ValidationMessage> errors, JsonObject object, ValidationProfileSet profiles) throws FHIRException {
    JsonParser parser = new JsonParser(context);
    parser.setupValidation(ValidationPolicy.EVERYTHING, errors);
    long t = System.nanoTime();
    Element e = parser.parse(object);
    loadTime = System.nanoTime() - t;
    if (e != null)
      validate(appContext, errors, e, profiles);
    return e;
  }

  @Override
  public void validate(Object appContext, List<ValidationMessage> errors, Element element) throws FHIRException {
    ValidationProfileSet profileSet = new ValidationProfileSet();
    validate(appContext, errors, element, profileSet);
  }

  private void validateRemainder(Object appContext, List<ValidationMessage> errors) throws IOException, FHIRException {
    boolean processedResource;
    do {
      processedResource = false;
      Set<Element> keys = new HashSet<Element>();
      keys.addAll(resourceProfilesMap.keySet());
      for (Element resource : keys) {
        ResourceProfiles rp = resourceProfilesMap.get(resource);
        if (rp.hasUncheckedProfiles()) {
          processedResource = true;
          start(new ValidatorHostContext(appContext), errors, rp.getOwner(), resource, null, rp.getStack());
        }
      }
    } while (processedResource);
  }

  @Override
  public void validate(Object appContext, List<ValidationMessage> errors, Element element, String profile) throws FHIRException {
    validate(appContext, errors, element, new ValidationProfileSet(profile, true));
  }

  @Override
  public void validate(Object appContext, List<ValidationMessage> errors, Element element, StructureDefinition profile) throws FHIRException {
    validate(appContext, errors, element, new ValidationProfileSet(profile));
  }

  @Override
  public void validate(Object appContext, List<ValidationMessage> errors, Element element, ValidationProfileSet profiles) throws FHIRException {
    // this is the main entry point; all the other entry points end up here coming here...
    providedProfiles = profiles;
    long t = System.nanoTime();
    boolean isRoot = false;
    if (resourceProfilesMap == null) {
      resourceProfilesMap = new HashMap<Element, ResourceProfiles>();
      isRoot = true;
    }
    try {
      validateResource(new ValidatorHostContext(appContext, element), errors, element, element, null, profiles, resourceIdRule, new NodeStack(element), true);
      if (isRoot) {
        validateRemainder(appContext, errors);
        resourceProfilesMap = null;
        if (hintAboutNonMustSupport)
          checkElementUsage(errors, element, new NodeStack(element));
      }
    } catch (IOException e) {
      throw new FHIRException(e);
    }
    overall = System.nanoTime() - t;
  }

  private void checkElementUsage(List<ValidationMessage> errors, Element element, NodeStack stack) {
     String elementUsage = element.getUserString("elementSupported");
    hint(errors, IssueType.INFORMATIONAL, element.line(),element.col(), stack.getLiteralPath(), elementUsage==null || elementUsage.equals("Y"),
        String.format("The element %s is not marked as 'mustSupport' in the profile %s. Consider not using the element, or marking the element as must-Support in the profile", element.getName(), element.getProperty().getStructure().getUrl()));	  
	  
    if (element.hasChildren()) {
      String prevName = "";
      int elementCount = 0;
      for (Element ce : element.getChildren()) {
        if (ce.getName().equals(prevName))
          elementCount++;
        else {
          elementCount=1;
          prevName = ce.getName();
        }
        checkElementUsage(errors, ce, stack.push(ce, elementCount, null, null));
      }
    }
  }

  private boolean check(String v1, String v2) {
    return v1 == null ? Utilities.noString(v1) : v1.equals(v2);
  }

  private void checkAddress(List<ValidationMessage> errors, String path, Element focus, Address fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".use", focus.getNamedChild("use"), fixed.getUseElement(), fixedSource, "use", focus, pattern);
    checkFixedValue(errors, path + ".text", focus.getNamedChild("text"), fixed.getTextElement(), fixedSource, "text", focus, pattern);
    checkFixedValue(errors, path + ".city", focus.getNamedChild("city"), fixed.getCityElement(), fixedSource, "city", focus, pattern);
    checkFixedValue(errors, path + ".state", focus.getNamedChild("state"), fixed.getStateElement(), fixedSource, "state", focus, pattern);
    checkFixedValue(errors, path + ".country", focus.getNamedChild("country"), fixed.getCountryElement(), fixedSource, "country", focus, pattern);
    checkFixedValue(errors, path + ".zip", focus.getNamedChild("zip"), fixed.getPostalCodeElement(), fixedSource, "postalCode", focus, pattern);

    List<Element> lines = new ArrayList<Element>();
    focus.getNamedChildren("line", lines);
    if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, lines.size() == fixed.getLine().size(),
        "Expected " + Integer.toString(fixed.getLine().size()) + " but found " + Integer.toString(lines.size()) + " line elements")) {
      for (int i = 0; i < lines.size(); i++)
        checkFixedValue(errors, path + ".coding", lines.get(i), fixed.getLine().get(i), fixedSource, "coding", focus, pattern);
    }
  }

  private void checkAttachment(List<ValidationMessage> errors, String path, Element focus, Attachment fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".contentType", focus.getNamedChild("contentType"), fixed.getContentTypeElement(), fixedSource, "contentType", focus, pattern);
    checkFixedValue(errors, path + ".language", focus.getNamedChild("language"), fixed.getLanguageElement(), fixedSource, "language", focus, pattern);
    checkFixedValue(errors, path + ".data", focus.getNamedChild("data"), fixed.getDataElement(), fixedSource, "data", focus, pattern);
    checkFixedValue(errors, path + ".url", focus.getNamedChild("url"), fixed.getUrlElement(), fixedSource, "url", focus, pattern);
    checkFixedValue(errors, path + ".size", focus.getNamedChild("size"), fixed.getSizeElement(), fixedSource, "size", focus, pattern);
    checkFixedValue(errors, path + ".hash", focus.getNamedChild("hash"), fixed.getHashElement(), fixedSource, "hash", focus, pattern);
    checkFixedValue(errors, path + ".title", focus.getNamedChild("title"), fixed.getTitleElement(), fixedSource, "title", focus, pattern);
  }

  // public API
  private boolean checkCode(List<ValidationMessage> errors, Element element, String path, String code, String system, String display, boolean checkDisplay, NodeStack stack) throws TerminologyServiceException {
    long t = System.nanoTime();
    boolean ss = context.supportsSystem(system);
    txTime = txTime + (System.nanoTime() - t);
    if (ss) {
      t = System.nanoTime();
      ValidationResult s = context.validateCode(new ValidationOptions(stack.workingLang), system, code, checkDisplay ? display : null);
      txTime = txTime + (System.nanoTime() - t);
      if (s == null)
        return true;
      if (s.isOk()) {
        if (s.getMessage() != null)
          txWarning(errors, s.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, s == null, s.getMessage());
        return true;
      }
      if (s.getErrorClass() != null && s.getErrorClass().isInfrastructure())
        txWarning(errors, s.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, s == null, s.getMessage());
      else if (s.getSeverity() == IssueSeverity.INFORMATION)
        txHint(errors, s.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, s == null, s.getMessage());
      else if (s.getSeverity() == IssueSeverity.WARNING)
        txWarning(errors, s.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, s == null, s.getMessage());
      else
        return txRule(errors, s.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, s == null, s.getMessage()+" for '"+system+"#"+code+"'");
      return true;
    } else if (system.startsWith("http://hl7.org/fhir")) {
      if (Utilities.existsInList(system, "http://hl7.org/fhir/sid/icd-10", "http://hl7.org/fhir/sid/cvx", "http://hl7.org/fhir/sid/icd-10-cm","http://hl7.org/fhir/sid/icd-9","http://hl7.org/fhir/sid/ndc","http://hl7.org/fhir/sid/srt"))
        return true; // else don't check these (for now)
      else if (system.startsWith("http://hl7.org/fhir/test"))
        return true; // we don't validate these
      else {
        CodeSystem cs = getCodeSystem(system);
        if (rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, cs != null, "Unknown Code System " + system)) {
          ConceptDefinitionComponent def = getCodeDefinition(cs, code);
          if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, def != null, "Unknown Code (" + system + "#" + code + ")"))
            return warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, display == null || display.equals(def.getDisplay()), "Display should be '" + def.getDisplay() + "'");
        }
        return false;
      }
    } else if (context.isNoTerminologyServer() &&  Utilities.existsInList(system, "http://loinc.org", "http://unitsofmeasure.org", "http://snomed.info/sct", "http://www.nlm.nih.gov/research/umls/rxnorm")) {
      return true; // no checks in this case
    } else if (startsWithButIsNot(system, "http://snomed.info/sct", "http://loinc.org", "http://unitsofmeasure.org", "http://www.nlm.nih.gov/research/umls/rxnorm")) {
      rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Invalid System URI: "+system);
      return false;
    } else {
      try {
        if (context.fetchResourceWithException(ValueSet.class, system) != null) {
          rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Invalid System URI: "+system+" - cannot use a value set URI as a system");
          // Lloyd: This error used to prohibit checking for downstream issues, but there are some cases where that checking needs to occur.  Please talk to me before changing the code back.
        }
        return true;
      }
      catch (Exception e) {
        return true;
      }
    }
  }

  private boolean startsWithButIsNot(String system, String... uri) {
    for (String s : uri)
      if (!system.equals(s) && system.startsWith(s))
        return true;
    return false;
  }
  
  
  private boolean hasErrors(List<ValidationMessage> errors) {
    if (errors!=null) {
      for (ValidationMessage vm : errors) {
        if (vm.getLevel() == IssueSeverity.FATAL || vm.getLevel() == IssueSeverity.ERROR) {
          return true;
        }
      }
    }
    return false;
  }
  
  private void checkCodeableConcept(List<ValidationMessage> errors, String path, Element focus, CodeableConcept fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".text", focus.getNamedChild("text"), fixed.getTextElement(), fixedSource, "text", focus, pattern);
    List<Element> codings = new ArrayList<Element>();
    focus.getNamedChildren("coding", codings);
    if (pattern) {
      if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, codings.size() >= fixed.getCoding().size(),
          "Expected " + Integer.toString(fixed.getCoding().size()) + " but found " + Integer.toString(codings.size())
              + " coding elements")) {
        for (int i = 0; i < fixed.getCoding().size(); i++) {
          Coding fixedCoding = fixed.getCoding().get(i);
          boolean found = false;
          List<ValidationMessage> allErrorsFixed = new ArrayList<>();
          List<ValidationMessage> errorsFixed;
          for (int j = 0; j < codings.size() && !found; ++j) {
            errorsFixed = new ArrayList<>();
            checkFixedValue(errorsFixed, path + ".coding", codings.get(j), fixedCoding, fixedSource, "coding", focus, pattern);
            if (!hasErrors(errorsFixed)) {
              found = true;
            } else {
              errorsFixed
                .stream()
                .filter(t->t.getLevel().ordinal() >= IssueSeverity.ERROR.ordinal())
                .forEach(t->allErrorsFixed.add(t));
            }
          }
          if (!found) {
            // The argonaut DSTU2 labs profile requires userSelected=false on the category.coding and this
            // needs to produce an understandable error message
            String message = "Expected CodeableConcept "+(pattern ? "pattern" : "fixed value")+" not found for" +
              " system: " + fixedCoding.getSystemElement().asStringValue() +
              " code: " + fixedCoding.getCodeElement().asStringValue() +
              " display: " + fixedCoding.getDisplayElement().asStringValue();
            if (fixedCoding.hasUserSelected()) {
              message += " userSelected: " + fixedCoding.getUserSelected();
            }
            message += " - Issues: " + allErrorsFixed;
            rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, false, message);
          }
        }
      }
    } else {
      if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, codings.size() == fixed.getCoding().size(),
          "Expected " + Integer.toString(fixed.getCoding().size()) + " but found " + Integer.toString(codings.size())
              + " coding elements")) {
        for (int i = 0; i < codings.size(); i++)
          checkFixedValue(errors, path + ".coding", codings.get(i), fixed.getCoding().get(i), fixedSource, "coding", focus);
      }
    }
  }

  private boolean checkCodeableConcept(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, ElementDefinition theElementCntext, NodeStack stack)  {
    boolean res = true;
    if (!noTerminologyChecks && theElementCntext != null && theElementCntext.hasBinding()) {
      ElementDefinitionBindingComponent binding = theElementCntext.getBinding();
      if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, binding != null, "Binding for " + path + " missing (cc)")) {
        if (binding.hasValueSet()) {
          ValueSet valueset = resolveBindingReference(profile, binding.getValueSet(), profile.getUrl());
          if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(binding.getValueSet()) + " not found by validator")) {
            try {
              CodeableConcept cc = ObjectConverter.readAsCodeableConcept(element);
              if (!cc.hasCoding()) {
                if (binding.getStrength() == BindingStrength.REQUIRED)
                  rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "No code provided, and a code is required from the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl());
                else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                  if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                    rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "No code provided, and a code must be provided from the value set " + describeReference(ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet")) + " (max value set " + valueset.getUrl()+")");
                  else
                    warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "No code provided, and a code should be provided from the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl()+")");
                }
              } else {
                long t = System.nanoTime();

                // Check whether the codes are appropriate for the type of binding we have
                boolean bindingsOk = true;
                if (binding.getStrength() != BindingStrength.EXAMPLE) {
                  boolean atLeastOneSystemIsSupported = false;
                  for (Coding nextCoding : cc.getCoding()) {
                    String nextSystem = nextCoding.getSystem();
                    if (isNotBlank(nextSystem) && context.supportsSystem(nextSystem)) {
                      atLeastOneSystemIsSupported = true;
                      break;
                    }
                  }

                  if (!atLeastOneSystemIsSupported && binding.getStrength() == BindingStrength.EXAMPLE) {
                    // ignore this since we can't validate but it doesn't matter..
                  } else {
                    ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), cc, valueset); // we're going to validate the codings directly
                    if (!vr.isOk()) {
                      bindingsOk = false;
                      if (vr.getErrorClass() != null && vr.getErrorClass().isInfrastructure()) {
                        if (binding.getStrength() == BindingStrength.REQUIRED)
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " and a code from this value set is required (class = "+vr.getErrorClass().toString()+")");
                        else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                          if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                            checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), cc, stack);
                          else if (!noExtensibleWarnings)
                            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " and a code should come from this value set unless it has no suitable code (class = "+vr.getErrorClass().toString()+")");
                        } else if (binding.getStrength() == BindingStrength.PREFERRED)
                          txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " and a code is recommended to come from this value set (class = "+vr.getErrorClass().toString()+")");
                      } else {
                        if (binding.getStrength() == BindingStrength.REQUIRED)
                          txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "None of the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl()+", and a code from this value set is required) (codes = "+ccSummary(cc)+")");
                        else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                          if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                            checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), cc, stack);
                          if (!noExtensibleWarnings)
                            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "None of the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code should come from this value set unless it has no suitable code) (codes = "+ccSummary(cc)+")");
                        } else if (binding.getStrength() == BindingStrength.PREFERRED) {
                          txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "None of the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is recommended to come from this value set) (codes = "+ccSummary(cc)+")");
                        }
                      }
                    } else if (vr.getMessage()!=null) {
                      res = false;
                      txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, vr.getMessage());
                    } else {
                      res = false;
                    }
                  }
                  // Then, for any codes that are in code systems we are able
                  // to validate, we'll validate that the codes actually exist
                  if (bindingsOk) {
                    for (Coding nextCoding : cc.getCoding()) {
                      String nextCode = nextCoding.getCode();
                      String nextSystem = nextCoding.getSystem();
                      if (isNotBlank(nextCode) && isNotBlank(nextSystem) && context.supportsSystem(nextSystem)) {
                        ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), nextSystem, nextCode, null);
                        if (!vr.isOk()) {
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Code {0} is not a valid code in code system {1}", nextCode, nextSystem);
                        }
                      }
                    }
                  }
                  txTime = txTime + (System.nanoTime() - t);
                }
              }
            } catch (Exception e) {
              warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating CodeableConcept");
            }
          }
        } else if (binding.hasValueSet()) {
          hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding by URI reference cannot be checked");
        } else if (!noBindingMsgSuppressed) {
          hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding for path " + path + " has no source, so can't be checked");
        }
      }
    }
    return res;
  }

  private boolean checkTerminologyCodeableConcept(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, ElementDefinition theElementCntext, NodeStack stack, StructureDefinition logical)  {
    boolean res = true;
    if (!noTerminologyChecks && theElementCntext != null && theElementCntext.hasBinding()) {
      ElementDefinitionBindingComponent binding = theElementCntext.getBinding();
      if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, binding != null, "Binding for " + path + " missing (cc)")) {
        if (binding.hasValueSet()) {
          ValueSet valueset = resolveBindingReference(profile, binding.getValueSet(), profile.getUrl());
          if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(binding.getValueSet()) + " not found by validator")) {
            try {
              CodeableConcept cc = convertToCodeableConcept(element, logical);
              if (!cc.hasCoding()) {
                if (binding.getStrength() == BindingStrength.REQUIRED)
                  rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "No code provided, and a code is required from the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl());
                else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                  if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                    rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "No code provided, and a code must be provided from the value set " + describeReference(ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet")) + " (max value set " + valueset.getUrl()+")");
                  else
                    warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "No code provided, and a code should be provided from the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl()+")");
                }
              } else {
                long t = System.nanoTime();

                // Check whether the codes are appropriate for the type of binding we have
                boolean bindingsOk = true;
                if (binding.getStrength() != BindingStrength.EXAMPLE) {
                  boolean atLeastOneSystemIsSupported = false;
                  for (Coding nextCoding : cc.getCoding()) {
                    String nextSystem = nextCoding.getSystem();
                    if (isNotBlank(nextSystem) && context.supportsSystem(nextSystem)) {
                      atLeastOneSystemIsSupported = true;
                      break;
                    }
                  }

                  if (!atLeastOneSystemIsSupported && binding.getStrength() == BindingStrength.EXAMPLE) {
                    // ignore this since we can't validate but it doesn't matter..
                  } else {
                    ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), cc, valueset); // we're going to validate the codings directly
                    if (!vr.isOk()) {
                      bindingsOk = false;
                      if (vr.getErrorClass() != null && vr.getErrorClass().isInfrastructure()) {
                        if (binding.getStrength() == BindingStrength.REQUIRED)
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " and a code from this value set is required (class = "+vr.getErrorClass().toString()+")");
                        else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                          if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                            checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), cc, stack);
                          else if (!noExtensibleWarnings)
                            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " and a code should come from this value set unless it has no suitable code (class = "+vr.getErrorClass().toString()+")");
                        } else if (binding.getStrength() == BindingStrength.PREFERRED)
                          txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " and a code is recommended to come from this value set (class = "+vr.getErrorClass().toString()+")");
                      } else {
                        if (binding.getStrength() == BindingStrength.REQUIRED)
                          txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "None of the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl()+", and a code from this value set is required) (codes = "+ccSummary(cc)+")");
                        else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                          if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                            checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), cc, stack);
                          if (!noExtensibleWarnings)
                            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "None of the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code should come from this value set unless it has no suitable code) (codes = "+ccSummary(cc)+")");
                        } else if (binding.getStrength() == BindingStrength.PREFERRED) {
                          txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "None of the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is recommended to come from this value set) (codes = "+ccSummary(cc)+")");
                        }
                      }
                    } else if (vr.getMessage()!=null) {
                      res = false;
                      txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, vr.getMessage());
                    } else {
                      res = false;
                    }
                  }
                  // Then, for any codes that are in code systems we are able
                  // to validate, we'll validate that the codes actually exist
                  if (bindingsOk) {
                    for (Coding nextCoding : cc.getCoding()) {
                      String nextCode = nextCoding.getCode();
                      String nextSystem = nextCoding.getSystem();
                      if (isNotBlank(nextCode) && isNotBlank(nextSystem) && context.supportsSystem(nextSystem)) {
                        ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), nextSystem, nextCode, null);
                        if (!vr.isOk()) {
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Code {0} is not a valid code in code system {1}", nextCode, nextSystem);
                        }
                      }
                    }
                  }
                  txTime = txTime + (System.nanoTime() - t);
                }
              }
            } catch (Exception e) {
              warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating CodeableConcept");
            }
            // special case: if the logical model has both CodeableConcept and Coding mappings, we'll also check the first coding.
            if (getMapping("http://hl7.org/fhir/terminology-pattern", logical, logical.getSnapshot().getElementFirstRep()).contains("Coding")) {
              checkTerminologyCoding(errors, path, element, profile, theElementCntext, true, true, stack, logical);
            }
          }
        } else if (binding.hasValueSet()) {
          hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding by URI reference cannot be checked");
        } else if (!noBindingMsgSuppressed) {
          hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding for path " + path + " has no source, so can't be checked");
        }
      }
    }
    return res;
  }

  private void checkTerminologyCoding(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, ElementDefinition theElementCntext, boolean inCodeableConcept, boolean checkDisplay, NodeStack stack, StructureDefinition logical)  {
    Coding c = convertToCoding(element, logical);
    String code = c.getCode();
    String system = c.getSystem();
    String display = c.getDisplay();
    rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, isAbsolute(system), "Coding.system must be an absolute reference, not a local reference");

    if (system != null && code != null && !noTerminologyChecks) {
      rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, !isValueSet(system), "The Coding references a value set, not a code system (\""+system+"\")");
      try {
        if (checkCode(errors, element, path, code, system, display, checkDisplay, stack))
          if (theElementCntext != null && theElementCntext.hasBinding()) {
            ElementDefinitionBindingComponent binding = theElementCntext.getBinding();
            if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, binding != null, "Binding for " + path + " missing")) {
              if (binding.hasValueSet()) {
                ValueSet valueset = resolveBindingReference(profile, binding.getValueSet(), profile.getUrl());
                if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(binding.getValueSet()) + " not found by validator")) {
                  try {
                    long t = System.nanoTime();
                    ValidationResult vr = null;
                    if (binding.getStrength() != BindingStrength.EXAMPLE) {
                      vr = context.validateCode(new ValidationOptions(stack.workingLang), c, valueset);
                    }
                    txTime = txTime + (System.nanoTime() - t);
                    if (vr != null && !vr.isOk()) {
                      if (vr.IsNoService())
                        txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "The value provided could not be validated in the absence of a terminology server");
                      else if (vr.getErrorClass() != null && !vr.getErrorClass().isInfrastructure()) {
                        if (binding.getStrength() == BindingStrength.REQUIRED)
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl()+", and a code from this value set is required)");
                        else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                          if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                            checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), c, stack);
                          else if (!noExtensibleWarnings)
                            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code should come from this value set unless it has no suitable code)");
                        } else if (binding.getStrength() == BindingStrength.PREFERRED)
                          txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is recommended to come from this value set)");
                      } else if (binding.getStrength() == BindingStrength.REQUIRED)
                        txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The Coding provided is not in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is required from this value set)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
                      else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                        if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                          checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), c, stack);
                        else
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The Coding provided is not in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code should come from this value set unless it has no suitable code)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
                      } else if (binding.getStrength() == BindingStrength.PREFERRED)
                        txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "The Coding provided is not in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is recommended to come from this value set)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
                    }
                  } catch (Exception e) {
                    warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating Coding");
                  }
                }
              } else if (binding.hasValueSet()) {
                hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding by URI reference cannot be checked");
              } else if (!inCodeableConcept && !noBindingMsgSuppressed) {
                hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding for path " + path + " has no source, so can't be checked");
              }
            }
          }
      } catch (Exception e) {
        rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating Coding: " + e.toString());
      }
    }
  }

  private CodeableConcept convertToCodeableConcept(Element element, StructureDefinition logical) {
    CodeableConcept res = new CodeableConcept();
    for (ElementDefinition ed : logical.getSnapshot().getElement()) {
      if (Utilities.charCount(ed.getPath(), '.') == 1) {
        List<String> maps = getMapping("http://hl7.org/fhir/terminology-pattern", logical, ed);
        for (String m : maps) {
          String name = tail(ed.getPath());
          List<Element> list = new ArrayList<>();
          element.getNamedChildren(name, list);
          if (!list.isEmpty()) {
            if ("Coding.code".equals(m)) {
              res.getCodingFirstRep().setCode(list.get(0).primitiveValue());
            } else if ("Coding.system[fmt:OID]".equals(m)) {
              String oid = list.get(0).primitiveValue();
              String url = context.oid2Uri(oid);
              if (url != null) {
                res.getCodingFirstRep().setSystem(url);                
              } else {
                res.getCodingFirstRep().setSystem("urn:oid:"+oid);
              }
            } else if ("Coding.version".equals(m)) {
              res.getCodingFirstRep().setVersion(list.get(0).primitiveValue());
            } else if ("Coding.display".equals(m)) {
              res.getCodingFirstRep().setDisplay(list.get(0).primitiveValue());
            } else if ("CodeableConcept.text".equals(m)) {
              res.setText(list.get(0).primitiveValue());
            } else if ("CodeableConcept.coding".equals(m)) {
              StructureDefinition c = context.fetchTypeDefinition(ed.getTypeFirstRep().getCode());
              for (Element e : list) {
                res.addCoding(convertToCoding(e, c));
              }
            }
          }
        }
      }
    }
    return res;
  }

  private Coding convertToCoding(Element element, StructureDefinition logical) {
    Coding res = new Coding();
    for (ElementDefinition ed : logical.getSnapshot().getElement()) {
      if (Utilities.charCount(ed.getPath(), '.') == 1) {
        List<String> maps = getMapping("http://hl7.org/fhir/terminology-pattern", logical, ed);
        for (String m : maps) {
          String name = tail(ed.getPath());
          List<Element> list = new ArrayList<>();
          element.getNamedChildren(name, list);
          if (!list.isEmpty()) {
            if ("Coding.code".equals(m)) {
              res.setCode(list.get(0).primitiveValue());
            } else if ("Coding.system[fmt:OID]".equals(m)) {
              String oid = list.get(0).primitiveValue();
              String url = context.oid2Uri(oid);
              if (url != null) {
                res.setSystem(url);                
              } else {
                res.setSystem("urn:oid:"+oid);
              }
            } else if ("Coding.version".equals(m)) {
              res.setVersion(list.get(0).primitiveValue());
            } else if ("Coding.display".equals(m)) {
              res.setDisplay(list.get(0).primitiveValue());
            }
          }
        }
      }
    }
    return res;
  }

  private void checkMaxValueSet(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, String maxVSUrl, CodeableConcept cc, NodeStack stack) {
    // TODO Auto-generated method stub
    ValueSet valueset = resolveBindingReference(profile, maxVSUrl, profile.getUrl());
    if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(maxVSUrl) + " not found by validator")) {
      try {
        long t = System.nanoTime();
        ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), cc, valueset);
        txTime = txTime + (System.nanoTime() - t);
        if (!vr.isOk()) {
          if (vr.getErrorClass() != null && vr.getErrorClass().isInfrastructure())
            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "None of the codes provided could be validated against the maximum value set " + describeReference(maxVSUrl) + " (" + valueset.getUrl()+"), (error = "+vr.getMessage()+")");
          else
            txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "None of the codes provided are in the maximum value set " + describeReference(maxVSUrl) + " (" + valueset.getUrl()+", and a code from this value set is required) (codes = "+ccSummary(cc)+")");
        }
      } catch (Exception e) {
        warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating CodeableConcept using maxValueSet");
      }
    }
  }

  private void checkMaxValueSet(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, String maxVSUrl, Coding c, NodeStack stack) {
    // TODO Auto-generated method stub
    ValueSet valueset = resolveBindingReference(profile, maxVSUrl, profile.getUrl());
    if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(maxVSUrl) + " not found by validator")) {
      try {
        long t = System.nanoTime();
        ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), c, valueset);
        txTime = txTime + (System.nanoTime() - t);
        if (!vr.isOk()) {
          if (vr.getErrorClass() != null && vr.getErrorClass().isInfrastructure())
            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The code provided could not be validated against the maximum value set " + describeReference(maxVSUrl) + " (" + valueset.getUrl()+"), (error = "+vr.getMessage()+")");
          else
            txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The code provided is not in the maximum value set " + describeReference(maxVSUrl) + " (" + valueset.getUrl()+", and a code from this value set is required) (code = "+c.getSystem()+"#"+c.getCode()+")");
        }
      } catch (Exception e) {
        warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating CodeableConcept using maxValueSet");
      }
    }
  }

  private void checkMaxValueSet(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, String maxVSUrl, String value, NodeStack stack) {
    // TODO Auto-generated method stub
    ValueSet valueset = resolveBindingReference(profile, maxVSUrl, profile.getUrl());
    if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(maxVSUrl) + " not found by validator")) {
      try {
        long t = System.nanoTime();
        ValidationResult vr = context.validateCode(new ValidationOptions(stack.workingLang), value, valueset);
        txTime = txTime + (System.nanoTime() - t);
        if (!vr.isOk()) {
          if (vr.getErrorClass() != null && vr.getErrorClass().isInfrastructure())
            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The code provided could not be validated against the maximum value set " + describeReference(maxVSUrl) + " (" + valueset.getUrl()+"), (error = "+vr.getMessage()+")");
          else
            txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The code provided is not in the maximum value set " + describeReference(maxVSUrl) + " (" + valueset.getUrl()+"), and a code from this value set is required) (code = "+value+"), (error = "+vr.getMessage()+")");
        }
      } catch (Exception e) {
        warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating CodeableConcept using maxValueSet");
      }
    }
  }

  private String ccSummary(CodeableConcept cc) {
    CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
    for (Coding c : cc.getCoding())
      b.append(c.getSystem()+"#"+c.getCode());
    return b.toString();
  }

  private void checkCoding(List<ValidationMessage> errors, String path, Element focus, Coding fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".system", focus.getNamedChild("system"), fixed.getSystemElement(), fixedSource, "system", focus, pattern);
    checkFixedValue(errors, path + ".version", focus.getNamedChild("version"), fixed.getVersionElement(), fixedSource, "version", focus, pattern);
    checkFixedValue(errors, path + ".code", focus.getNamedChild("code"), fixed.getCodeElement(), fixedSource, "code", focus, pattern);
    checkFixedValue(errors, path + ".display", focus.getNamedChild("display"), fixed.getDisplayElement(), fixedSource, "display", focus, pattern);
    checkFixedValue(errors, path + ".userSelected", focus.getNamedChild("userSelected"), fixed.getUserSelectedElement(), fixedSource, "userSelected", focus, pattern);
  }

  private void checkCoding(List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, ElementDefinition theElementCntext, boolean inCodeableConcept, boolean checkDisplay, NodeStack stack)  {
    String code = element.getNamedChildValue("code");
    String system = element.getNamedChildValue("system");
    String display = element.getNamedChildValue("display");
    rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, isAbsolute(system), "Coding.system must be an absolute reference, not a local reference");

    if (system != null && code != null && !noTerminologyChecks) {
      rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, !isValueSet(system), "The Coding references a value set, not a code system (\""+system+"\")");
      try {
        if (checkCode(errors, element, path, code, system, display, checkDisplay, stack))
          if (theElementCntext != null && theElementCntext.hasBinding()) {
            ElementDefinitionBindingComponent binding = theElementCntext.getBinding();
            if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, binding != null, "Binding for " + path + " missing")) {
              if (binding.hasValueSet()) {
                ValueSet valueset = resolveBindingReference(profile, binding.getValueSet(), profile.getUrl());
                if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, valueset != null, "ValueSet " + describeReference(binding.getValueSet()) + " not found by validator")) {
                  try {
                    Coding c = ObjectConverter.readAsCoding(element);
                    long t = System.nanoTime();
                    ValidationResult vr = null;
                    if (binding.getStrength() != BindingStrength.EXAMPLE) {
                      vr = context.validateCode(new ValidationOptions(stack.workingLang), c, valueset);
						        }
                    txTime = txTime + (System.nanoTime() - t);
                    if (vr != null && !vr.isOk()) {
                      if (vr.IsNoService())
                        txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "The value provided could not be validated in the absence of a terminology server");
                      else if (vr.getErrorClass() != null && !vr.getErrorClass().isInfrastructure()) {
                        if (binding.getStrength() == BindingStrength.REQUIRED)
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl()+", and a code from this value set is required)");
                        else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                          if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                            checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), c, stack);
                          else if (!noExtensibleWarnings)
                            txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code should come from this value set unless it has no suitable code)");
                        } else if (binding.getStrength() == BindingStrength.PREFERRED)
                          txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "Could not confirm that the codes provided are in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is recommended to come from this value set)");
                      } else if (binding.getStrength() == BindingStrength.REQUIRED)
                        txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The Coding provided is not in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is required from this value set)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
                      else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
                        if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
                          checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), c, stack);
                        else
                          txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The Coding provided is not in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code should come from this value set unless it has no suitable code)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
                      } else if (binding.getStrength() == BindingStrength.PREFERRED)
                        txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "The Coding provided is not in the value set " + describeReference(binding.getValueSet()) + " (" + valueset.getUrl() + ", and a code is recommended to come from this value set)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
                    }
                  } catch (Exception e) {
                    warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating Coding");
                  }
                }
              } else if (binding.hasValueSet()) {
                hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding by URI reference cannot be checked");
              } else if (!inCodeableConcept && !noBindingMsgSuppressed) {
                hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Binding for path " + path + " has no source, so can't be checked");
              }
            }
          }
      } catch (Exception e) {
        rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, false, "Error "+e.getMessage()+" validating Coding: " + e.toString());
      }
    }
  }

  private boolean isValueSet(String url) {
    try {
      ValueSet vs = context.fetchResourceWithException(ValueSet.class, url);
      return vs != null;
    } catch (Exception e) {
      return false;
    }
  }

  private void checkContactPoint(List<ValidationMessage> errors, String path, Element focus, ContactPoint fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".system", focus.getNamedChild("system"), fixed.getSystemElement(), fixedSource, "system", focus, pattern);
    checkFixedValue(errors, path + ".value", focus.getNamedChild("value"), fixed.getValueElement(), fixedSource, "value", focus, pattern);
    checkFixedValue(errors, path + ".use", focus.getNamedChild("use"), fixed.getUseElement(), fixedSource, "use", focus, pattern);
    checkFixedValue(errors, path + ".period", focus.getNamedChild("period"), fixed.getPeriod(), fixedSource, "period", focus, pattern);

  }

  protected void checkDeclaredProfiles(ResourceProfiles resourceProfiles, List<ValidationMessage> errors, Element resource, Element element, NodeStack stack) throws FHIRException {
    Element meta = element.getNamedChild("meta");
    if (meta != null) {
      List<Element> profiles = new ArrayList<Element>();
      meta.getNamedChildren("profile", profiles);
      int i = 0;
      for (Element profile : profiles) {
        String ref = profile.primitiveValue();
        String p = stack.addToLiteralPath("meta", "profile", ":" + Integer.toString(i));
        if (rule(errors, IssueType.INVALID, element.line(), element.col(), p, !Utilities.noString(ref), "StructureDefinition reference invalid")) {
          long t = System.nanoTime();
          resourceProfiles.addProfile(errors, ref, errorForUnknownProfiles, p, element, null);
          i++;
        }
      }
    }
  }

  private StructureDefinition checkExtension(ValidatorHostContext hostContext, List<ValidationMessage> errors, String path, Element resource, Element element, ElementDefinition def, StructureDefinition profile, NodeStack stack, String extensionUrl) throws FHIRException, IOException {
    String url = element.getNamedChildValue("url");
    boolean isModifier = element.getName().equals("modifierExtension");

    long t = System.nanoTime();
    StructureDefinition ex = Utilities.isAbsoluteUrl(url) ? context.fetchResource(StructureDefinition.class, url) : null;
    sdTime = sdTime + (System.nanoTime() - t);
    if (ex == null) {
      if (xverManager == null) {
        xverManager = new XVerExtensionManager(context);
      }
      if (xverManager.matchingUrl(url)) {
        switch (xverManager.status(url)) {
        case BadVersion:
          rule(errors, IssueType.INVALID, element.line(), element.col(), path + "[url='" + url + "']", false, "Extension url '" + url + "' evaluation state is not valid (invalidVersion \""+xverManager.getVersion(url)+"\")");
          break;
        case Unknown:
          rule(errors, IssueType.INVALID, element.line(), element.col(), path + "[url='" + url + "']", false, "Extension url '" + url + "' evaluation state is not valid (unknown Element id \""+xverManager.getElementId(url)+"\")");
          break;
        case Invalid:
          rule(errors, IssueType.INVALID, element.line(), element.col(), path + "[url='" + url + "']", false, "Extension url '" + url + "' evaluation state is not valid (Element id \""+xverManager.getElementId(url)+"\" is valid, but cannot be used in a cross-version paradigm because there has been no changes across the relevant versions)");
          break;
        case Valid:
          ex = xverManager.makeDefinition(url);
          context.generateSnapshot(ex);
          context.cacheResource(ex);
          break;
        default:
          rule(errors, IssueType.INVALID, element.line(), element.col(), path + "[url='" + url + "']", false, "Extension url '" + url + "' evaluation state illegal");
          break;
        }
      } else if (extensionUrl != null && !isAbsolute(url)) {
        if (extensionUrl.equals(profile.getUrl())) {
          rule(errors, IssueType.INVALID, element.line(), element.col(), path + "[url='" + url + "']", hasExtensionSlice(profile, url), "Sub-extension url '" + url + "' is not defined by the Extension "+profile.getUrl());
        }
      } else if (rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, allowUnknownExtension(url), "The extension " + url + " is unknown, and not allowed here")) {
        hint(errors, IssueType.STRUCTURE, element.line(), element.col(), path, isKnownExtension(url), "Unknown extension " + url);
      }
    }
    if (ex != null) {
      if (def.getIsModifier()) {
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path + "[url='" + url + "']", ex.getSnapshot().getElement().get(0).getIsModifier(),
            "Extension modifier mismatch: the extension element is labelled as a modifier, but the underlying extension is not");
      } else {
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path + "[url='" + url + "']", !ex.getSnapshot().getElement().get(0).getIsModifier(),
            "Extension modifier mismatch: the extension element is not labelled as a modifier, but the underlying extension is");
      }
      // two questions
      // 1. can this extension be used here?
      checkExtensionContext(errors, element, /* path+"[url='"+url+"']", */ ex, stack, ex.getUrl());

      if (isModifier)
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path + "[url='" + url + "']", ex.getSnapshot().getElement().get(0).getIsModifier(),
            "The Extension '" + url + "' must be used as a modifierExtension");
      else
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path + "[url='" + url + "']", !ex.getSnapshot().getElement().get(0).getIsModifier(),
            "The Extension '" + url + "' must not be used as an extension (it's a modifierExtension)");

      // check the type of the extension:
      Set<String> allowedTypes = listExtensionTypes(ex);
      String actualType = getExtensionType(element);
      if (actualType == null)
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path + "[url='" + url + "']", allowedTypes.isEmpty(), "The Extension '" + url + "' definition is for a simple extension, so it must contain a value, not extensions");
      else
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path + "[url='" + url + "']", allowedTypes.contains(actualType), "The Extension '" + url + "' definition allows for the types "+allowedTypes.toString()+" but found type "+actualType);

      // 3. is the content of the extension valid?
      validateElement(hostContext, errors, ex, ex.getSnapshot().getElement().get(0), null, null, resource, element, "Extension", stack, false, true, url);

    }
    return ex;
  }

  private boolean hasExtensionSlice(StructureDefinition profile, String sliceName) {
    for (ElementDefinition ed : profile.getSnapshot().getElement()) {
      if (ed.getPath().equals("Extension.extension.url") && ed.hasFixed() && sliceName.equals(ed.getFixed().primitiveValue())) {
        return true;        
      }
    }
    return false;
  }

  private String getExtensionType(Element element) {
    for (Element e : element.getChildren()) {
      if (e.getName().startsWith("value")) {
        String tn = e.getName().substring(5);
        String ltn = Utilities.uncapitalize(tn);
        if (isPrimitiveType(ltn))
          return ltn;
        else
          return tn;
      }
    }
    return null;
  }

  private Set<String> listExtensionTypes(StructureDefinition ex) {
    ElementDefinition vd = null;
    for (ElementDefinition ed : ex.getSnapshot().getElement()) {
      if (ed.getPath().startsWith("Extension.value")) {
        vd = ed;
        break;
      }
    }
    Set<String> res = new HashSet<String>();
    if (vd != null && !"0".equals(vd.getMax())) {
      for (TypeRefComponent tr : vd.getType()) {
        res.add(tr.getWorkingCode());
      }
    }
    return res;
  }

  private boolean checkExtensionContext(List<ValidationMessage> errors, Element element, StructureDefinition definition, NodeStack stack, String extensionParent) {
//    String extUrl = definition.getUrl();
//    CommaSeparatedStringBuilder p = new CommaSeparatedStringBuilder();
//    for (String lp : stack.getLogicalPaths())
//      p.append(lp);
//    if (definition.getContextType() == ExtensionContext.DATATYPE) {
//      boolean ok = false;
//      CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
//      for (StringType ct : definition.getContext()) {
//        b.append(ct.getValue());
//        if (ct.getValue().equals("*") || stack.getLogicalPaths().contains(ct.getValue() + ".extension"))
//          ok = true;
//      }
//      return rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), ok,
//          "The extension " + extUrl + " is not allowed to be used on the logical path set [" + p.toString() + "] (allowed: datatype=" + b.toString() + ")");
//    } else if (definition.getContextType() == ExtensionContext.EXTENSION) {
//      boolean ok = false;
//      for (StringType ct : definition.getContext())
//        if (ct.getValue().equals("*") || ct.getValue().equals(extensionParent))
//          ok = true;
//      return rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), ok,
//          "The extension " + extUrl + " is not allowed to be used with the extension '" + extensionParent + "'");
//    } else if (definition.getContextType() == ExtensionContext.RESOURCE) {
//      boolean ok = false;
//      // String simplePath = container.getPath();
//      // System.out.println(simplePath);
//      // if (effetive.endsWith(".extension") || simplePath.endsWith(".modifierExtension"))
//      // simplePath = simplePath.substring(0, simplePath.lastIndexOf('.'));
//      CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
//      for (StringType ct : definition.getContext()) {
//        String c = ct.getValue();
//        b.append(c);
//        if (c.equals("*") || stack.getLogicalPaths().contains(c + ".extension") || (c.startsWith("@") && stack.getLogicalPaths().contains(c.substring(1) + ".extension")))
//          ;
//        ok = true;
//      }
//      return rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), ok,
//          "The extension " + extUrl + " is not allowed to be used on the logical path set " + p.toString() + " (allowed: resource=" + b.toString() + ")");
//    } else
//      throw new Error("Unsupported context type");
    return true;
  }
  //
  // private String simplifyPath(String path) {
  // String s = path.replace("/f:", ".");
  // while (s.contains("["))
  // s = s.substring(0, s.indexOf("["))+s.substring(s.indexOf("]")+1);
  // String[] parts = s.split("\\.");
  // int i = 0;
  // while (i < parts.length && !context.getProfiles().containsKey(parts[i].toLowerCase()))
  // i++;
  // if (i >= parts.length)
  // throw new Error("Unable to process part "+path);
  // int j = parts.length - 1;
  // while (j > 0 && (parts[j].equals("extension") || parts[j].equals("modifierExtension")))
  // j--;
  // StringBuilder b = new StringBuilder();
  // boolean first = true;
  // for (int k = i; k <= j; k++) {
  // if (k == j || !parts[k].equals(parts[k+1])) {
  // if (first)
  // first = false;
  // else
  // b.append(".");
  // b.append(parts[k]);
  // }
  // }
  // return b.toString();
  // }
  //
  
  private void checkFixedValue(List<ValidationMessage> errors, String path, Element focus, org.hl7.fhir.r5.model.Element fixed, String fixedSource, String propName, Element parent) {
	  checkFixedValue(errors, path, focus, fixed, fixedSource, propName, parent, false);
  }

  @SuppressWarnings("rawtypes")
  private void checkFixedValue(List<ValidationMessage> errors, String path, Element focus, org.hl7.fhir.r5.model.Element fixed, String fixedSource, String propName, Element parent, boolean pattern) {
    if ((fixed == null || fixed.isEmpty()) && focus == null) {
      ; // this is all good
    } else if ((fixed == null || fixed.isEmpty()) && focus != null) {
      rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, pattern, "The element " + focus.getName()+" is present in the instance but not allowed in the applicable "+(pattern ? "pattern" : "fixed value")+" specified in profile");
    } else if (fixed != null && !fixed.isEmpty() && focus == null) {
      rule(errors, IssueType.VALUE, parent == null ? -1 : parent.line(), parent == null ? -1 : parent.col(), path, false, "Missing element '" + propName+"' - required by fixed value assigned in profile "+fixedSource);
    } else {
      String value = focus.primitiveValue();
      if (fixed instanceof org.hl7.fhir.r5.model.BooleanType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.BooleanType) fixed).asStringValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.BooleanType) fixed).asStringValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.IntegerType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.IntegerType) fixed).asStringValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.IntegerType) fixed).asStringValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.DecimalType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.DecimalType) fixed).asStringValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.DecimalType) fixed).asStringValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.Base64BinaryType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.Base64BinaryType) fixed).asStringValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.Base64BinaryType) fixed).asStringValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.InstantType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.InstantType) fixed).getValue().toString(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.InstantType) fixed).asStringValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.CodeType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.CodeType) fixed).getValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.CodeType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.Enumeration)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.Enumeration) fixed).asStringValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.Enumeration) fixed).asStringValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.StringType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.StringType) fixed).getValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.StringType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.UriType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.UriType) fixed).getValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.UriType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.DateType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.DateType) fixed).getValue().toString(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.DateType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.DateTimeType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.DateTimeType) fixed).getValue().toString(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.DateTimeType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.OidType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.OidType) fixed).getValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.OidType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.UuidType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.UuidType) fixed).getValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.UuidType) fixed).getValue() + "'");
      else if (fixed instanceof org.hl7.fhir.r5.model.IdType)
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, check(((org.hl7.fhir.r5.model.IdType) fixed).getValue(), value),
            "Value is '" + value + "' but must be '" + ((org.hl7.fhir.r5.model.IdType) fixed).getValue() + "'");
      else if (fixed instanceof Quantity)
        checkQuantity(errors, path, focus, (Quantity) fixed, fixedSource, pattern);
      else if (fixed instanceof Address)
        checkAddress(errors, path, focus, (Address) fixed, fixedSource, pattern);
      else if (fixed instanceof ContactPoint)
        checkContactPoint(errors, path, focus, (ContactPoint) fixed, fixedSource, pattern);
      else if (fixed instanceof Attachment)
        checkAttachment(errors, path, focus, (Attachment) fixed, fixedSource, pattern);
      else if (fixed instanceof Identifier)
        checkIdentifier(errors, path, focus, (Identifier) fixed, fixedSource, pattern);
      else if (fixed instanceof Coding)
        checkCoding(errors, path, focus, (Coding) fixed, fixedSource, pattern);
      else if (fixed instanceof HumanName)
        checkHumanName(errors, path, focus, (HumanName) fixed, fixedSource, pattern);
      else if (fixed instanceof CodeableConcept)
        checkCodeableConcept(errors, path, focus, (CodeableConcept) fixed, fixedSource, pattern);
      else if (fixed instanceof Timing)
        checkTiming(errors, path, focus, (Timing) fixed, fixedSource, pattern);
      else if (fixed instanceof Period)
        checkPeriod(errors, path, focus, (Period) fixed, fixedSource, pattern);
      else if (fixed instanceof Range)
        checkRange(errors, path, focus, (Range) fixed, fixedSource, pattern);
      else if (fixed instanceof Ratio)
        checkRatio(errors, path, focus, (Ratio) fixed, fixedSource, pattern);
      else if (fixed instanceof SampledData)
        checkSampledData(errors, path, focus, (SampledData) fixed, fixedSource, pattern);

      else
        rule(errors, IssueType.EXCEPTION, focus.line(), focus.col(), path, false, "Unhandled fixed value type " + fixed.getClass().getName());
      List<Element> extensions = new ArrayList<Element>();
      focus.getNamedChildren("extension", extensions);
      if (fixed.getExtension().size() == 0) {
        rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, extensions.size() == 0, "No extensions allowed, as the specified fixed value doesn't contain any extensions");
      } else if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, extensions.size() == fixed.getExtension().size(),
          "Extensions count mismatch: expected " + Integer.toString(fixed.getExtension().size()) + " but found " + Integer.toString(extensions.size()))) {
        for (Extension e : fixed.getExtension()) {
          Element ex = getExtensionByUrl(extensions, e.getUrl());
          if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, ex != null, "Extension count mismatch: unable to find extension: " + e.getUrl())) {
            checkFixedValue(errors, path, ex.getNamedChild("extension").getNamedChild("value"), e.getValue(), fixedSource, "extension.value", ex.getNamedChild("extension"));
          }
        }
      }
    }
  }

  private void checkHumanName(List<ValidationMessage> errors, String path, Element focus, HumanName fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".use", focus.getNamedChild("use"), fixed.getUseElement(), fixedSource, "use", focus, pattern);
    checkFixedValue(errors, path + ".text", focus.getNamedChild("text"), fixed.getTextElement(), fixedSource, "text", focus, pattern);
    checkFixedValue(errors, path + ".period", focus.getNamedChild("period"), fixed.getPeriod(), fixedSource, "period", focus, pattern);

    List<Element> parts = new ArrayList<Element>();
    focus.getNamedChildren("family", parts);
    if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, parts.size() > 0 == fixed.hasFamily(),
        "Expected " + (fixed.hasFamily() ? "1" : "0") + " but found " + Integer.toString(parts.size()) + " family elements")) {
      for (int i = 0; i < parts.size(); i++)
        checkFixedValue(errors, path + ".family", parts.get(i), fixed.getFamilyElement(), fixedSource, "family", focus, pattern);
    }
    focus.getNamedChildren("given", parts);
    if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, parts.size() == fixed.getGiven().size(),
        "Expected " + Integer.toString(fixed.getGiven().size()) + " but found " + Integer.toString(parts.size()) + " given elements")) {
      for (int i = 0; i < parts.size(); i++)
        checkFixedValue(errors, path + ".given", parts.get(i), fixed.getGiven().get(i), fixedSource, "given", focus, pattern);
    }
    focus.getNamedChildren("prefix", parts);
    if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, parts.size() == fixed.getPrefix().size(),
        "Expected " + Integer.toString(fixed.getPrefix().size()) + " but found " + Integer.toString(parts.size()) + " prefix elements")) {
      for (int i = 0; i < parts.size(); i++)
        checkFixedValue(errors, path + ".prefix", parts.get(i), fixed.getPrefix().get(i), fixedSource, "prefix", focus, pattern);
    }
    focus.getNamedChildren("suffix", parts);
    if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, parts.size() == fixed.getSuffix().size(),
        "Expected " + Integer.toString(fixed.getSuffix().size()) + " but found " + Integer.toString(parts.size()) + " suffix elements")) {
      for (int i = 0; i < parts.size(); i++)
        checkFixedValue(errors, path + ".suffix", parts.get(i), fixed.getSuffix().get(i), fixedSource, "suffix", focus, pattern);
    }
  }

  private void checkIdentifier(List<ValidationMessage> errors, String path, Element element, ElementDefinition context) {
    String system = element.getNamedChildValue("system");
    rule(errors, IssueType.CODEINVALID, element.line(), element.col(), path, isAbsolute(system), "Identifier.system must be an absolute reference, not a local reference");
  }

  private void checkIdentifier(List<ValidationMessage> errors, String path, Element focus, Identifier fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".use", focus.getNamedChild("use"), fixed.getUseElement(), fixedSource, "use", focus, pattern);
    checkFixedValue(errors, path + ".type", focus.getNamedChild("type"), fixed.getType(), fixedSource, "type", focus, pattern);
    checkFixedValue(errors, path + ".system", focus.getNamedChild("system"), fixed.getSystemElement(), fixedSource, "system", focus,  pattern);
    checkFixedValue(errors, path + ".value", focus.getNamedChild("value"), fixed.getValueElement(), fixedSource, "value", focus, pattern);
    checkFixedValue(errors, path + ".period", focus.getNamedChild("period"), fixed.getPeriod(), fixedSource, "period", focus, pattern);
    checkFixedValue(errors, path + ".assigner", focus.getNamedChild("assigner"), fixed.getAssigner(), fixedSource, "assigner", focus, pattern);
  }

  private void checkPeriod(List<ValidationMessage> errors, String path, Element focus, Period fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".start", focus.getNamedChild("start"), fixed.getStartElement(), fixedSource, "start", focus, pattern);
    checkFixedValue(errors, path + ".end", focus.getNamedChild("end"), fixed.getEndElement(), fixedSource, "end", focus, pattern);
  }

  private void checkPrimitive(Object appContext, List<ValidationMessage> errors, String path, String type, ElementDefinition context, Element e, StructureDefinition profile, NodeStack node) throws FHIRException, IOException {
    if (isBlank(e.primitiveValue())) {
      if (e.primitiveValue() == null)
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.hasChildren(), "Primitive types must have a value or must have child extensions");
      else if (e.primitiveValue().length() == 0)
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.hasChildren(), "Primitive types must have a value that is not empty");
      else if (StringUtils.isWhitespace(e.primitiveValue()))
        warning(errors, IssueType.INVALID, e.line(), e.col(), path, e.hasChildren(), "Primitive types should not only be whitespace");
      return;
    }
    String regex = context.getExtensionString(ToolingExtensions.EXT_REGEX);
    if (regex!=null)
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue().matches(regex), "Element value '" + e.primitiveValue() + "' does not meet regex '" + regex + "'");

    if (type.equals("boolean")) {
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, "true".equals(e.primitiveValue()) || "false".equals(e.primitiveValue()), "boolean values must be 'true' or 'false'");
    }
    if (type.equals("uri") || type.equals("oid") || type.equals("uuid")  || type.equals("url") || type.equals("canonical")) {
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, !e.primitiveValue().startsWith("oid:"), "URI values cannot start with oid:");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, !e.primitiveValue().startsWith("uuid:"), "URI values cannot start with uuid:");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue().equals(e.primitiveValue().trim().replace(" ", "")), "URI values cannot have whitespace");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxLength() || context.getMaxLength()==0 ||  e.primitiveValue().length() <= context.getMaxLength(), "value is longer than permitted maximum length of " + context.getMaxLength());


      if (type.equals("oid")) {
        if (rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue().startsWith("urn:oid:"), "OIDs must start with urn:oid:"))
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, Utilities.isOid(e.primitiveValue().substring(8)), "OIDs must be valid");
      }
      if (type.equals("uuid")) {
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue().startsWith("urn:uuid:"), "UUIDs must start with urn:uuid:");
        try {
          UUID.fromString(e.primitiveValue().substring(8));
        } catch (Exception ex) {
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "UUIDs must be valid ("+ex.getMessage()+")");
        }
      }

      // now, do we check the URI target?
      if (fetcher != null) {
        boolean found = fetcher.resolveURL(appContext, path, e.primitiveValue());
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, found, "URL value '"+e.primitiveValue()+"' does not resolve");
      }
    }
    if (type.equals("id")) {
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, FormatUtilities.isValidId(e.primitiveValue()), "id value '"+e.primitiveValue()+"' is not valid");
    }
    if (type.equalsIgnoreCase("string") && e.hasPrimitiveValue()) {
      if (rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue() == null || e.primitiveValue().length() > 0, "@value cannot be empty")) {
        warning(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue() == null || e.primitiveValue().trim().equals(e.primitiveValue()), "value should not start or finish with whitespace");
        if (rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue().length() <= 1048576, "value is longer than permitted maximum length of 1 MB (1048576 bytes)")) {
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxLength() || context.getMaxLength()==0 ||  e.primitiveValue().length() <= context.getMaxLength(), "value is longer than permitted maximum length of " + context.getMaxLength());
        }
      }
    }
    if (type.equals("dateTime")) {
      warning(errors, IssueType.INVALID, e.line(), e.col(), path, yearIsValid(e.primitiveValue()), "The value '" + e.primitiveValue() + "' is outside the range of reasonable years - check for data entry error");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path,
          e.primitiveValue()
          .matches("([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?)?)?)?"),
          "Not a valid date time");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, !hasTime(e.primitiveValue()) || hasTimeZone(e.primitiveValue()), "if a date has a time, it must have a timezone");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxLength() || context.getMaxLength()==0 ||  e.primitiveValue().length() <= context.getMaxLength(), "value is longer than permitted maximum length of " + context.getMaxLength());
      try {
        DateTimeType dt = new DateTimeType(e.primitiveValue());
      } catch (Exception ex) {
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "Not a valid date/time ("+ex.getMessage()+")");
      }
    }
    if (type.equals("time")) {
      rule(errors, IssueType.INVALID, e.line(), e.col(), path,
          e.primitiveValue()
          .matches("([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)"),
          "Not a valid time");
      try {
        TimeType dt = new TimeType(e.primitiveValue());
      } catch (Exception ex) {
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "Not a valid time ("+ex.getMessage()+")");
      }
    }
    if (type.equals("date")) {
      warning(errors, IssueType.INVALID, e.line(), e.col(), path, yearIsValid(e.primitiveValue()), "The value '" + e.primitiveValue() + "' is outside the range of reasonable years - check for data entry error");
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, e.primitiveValue().matches("([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1]))?)?"),
            "Not a valid date");
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxLength() || context.getMaxLength()==0 ||  e.primitiveValue().length() <= context.getMaxLength(), "value is longer than permitted maximum value of " + context.getMaxLength());
        try {
          DateType dt = new DateType(e.primitiveValue());
        } catch (Exception ex) {
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "Not a valid date ("+ex.getMessage()+")");
        }
    }
    if (type.equals("base64Binary")) {
		String encoded = e.primitiveValue();
		if (isNotBlank(encoded)) {
			/*
			 * Technically this is not bulletproof as some invalid base64 won't be caught,
			 * but I think it's good enough. The original code used Java8 Base64 decoder
			 * but I've replaced it with a regex for 2 reasons:
			 * 1. This code will run on any version of Java
			 * 2. This code doesn't actually decode, which is much easier on memory use for big payloads
			 */
			int charCount = 0;
			for (int i = 0; i < encoded.length(); i++) {
				char nextChar = encoded.charAt(i);
				if (Character.isWhitespace(nextChar)) {
					continue;
				}
				if (Character.isLetterOrDigit(nextChar)) {
					charCount++;
				}
				if (nextChar == '/' || nextChar == '=' || nextChar == '+') {
					charCount++;
				}
			}

			if (charCount > 0 && charCount % 4 != 0) {
				String value = encoded.length() < 100 ? encoded : "(snip)";
				rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "The value \"{0}\" is not a valid Base64 value", value);
			}
		}
    }
    if (type.equals("integer") || type.equals("unsignedInt") || type.equals("positiveInt")) {
      if (rule(errors, IssueType.INVALID, e.line(), e.col(), path, Utilities.isInteger(e.primitiveValue()), "The value '" + e.primitiveValue() + "' is not a valid integer")) {
        Integer v = new Integer(e.getValue()).intValue();
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxValueIntegerType() || !context.getMaxValueIntegerType().hasValue() || (context.getMaxValueIntegerType().getValue() >= v), "value is greater than permitted maximum value of " + (context.hasMaxValueIntegerType() ? context.getMaxValueIntegerType() : ""));
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMinValueIntegerType() || !context.getMinValueIntegerType().hasValue() || (context.getMinValueIntegerType().getValue() <= v), "value is less than permitted minimum value of " + (context.hasMinValueIntegerType() ? context.getMinValueIntegerType() : ""));
        if (type.equals("unsignedInt"))
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, v >= 0, "value is less than permitted minimum value of 0");
        if (type.equals("positiveInt"))
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, v > 0, "value is less than permitted minimum value of 1");
      }
    }
    if (type.equals("integer64")) {
      if (rule(errors, IssueType.INVALID, e.line(), e.col(), path, Utilities.isLong(e.primitiveValue()), "The value '" + e.primitiveValue() + "' is not a valid integer64")) {
        Long v = new Long(e.getValue()).longValue();
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxValueInteger64Type() || !context.getMaxValueInteger64Type().hasValue() || (context.getMaxValueInteger64Type().getValue() >= v), "value is greater than permitted maximum value of " + (context.hasMaxValueInteger64Type() ? context.getMaxValueInteger64Type() : ""));
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMinValueInteger64Type() || !context.getMinValueInteger64Type().hasValue() || (context.getMinValueInteger64Type().getValue() <= v), "value is less than permitted minimum value of " + (context.hasMinValueInteger64Type() ? context.getMinValueInteger64Type() : ""));
        if (type.equals("unsignedInt"))
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, v >= 0, "value is less than permitted minimum value of 0");
        if (type.equals("positiveInt"))
          rule(errors, IssueType.INVALID, e.line(), e.col(), path, v > 0, "value is less than permitted minimum value of 1");
      }
    }
    if (type.equals("decimal")) {
      if (e.primitiveValue() != null) {
        DecimalStatus ds = Utilities.checkDecimal(e.primitiveValue(), true, false);
        if (rule(errors, IssueType.INVALID, e.line(), e.col(), path, ds == DecimalStatus.OK || ds == DecimalStatus.RANGE, "The value '" + e.primitiveValue() + "' is not a valid decimal")) 
          warning(errors, IssueType.VALUE, e.line(), e.col(), path, ds != DecimalStatus.RANGE, "The value '" + e.primitiveValue() + "' is outside the range of commonly/reasonably supported decimals");
      }
    }
    if (type.equals("instant")) {
      rule(errors, IssueType.INVALID, e.line(), e.col(), path,
          e.primitiveValue().matches("-?[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))"),
          "The instant '" + e.primitiveValue() + "' is not valid (by regex)");
      warning(errors, IssueType.INVALID, e.line(), e.col(), path, yearIsValid(e.primitiveValue()), "The value '" + e.primitiveValue() + "' is outside the range of reasonable years - check for data entry error");
      try {
        InstantType dt = new InstantType(e.primitiveValue());
      } catch (Exception ex) {
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "Not a valid instant ("+ex.getMessage()+")");
      }
    }

    if (type.equals("code") && e.primitiveValue() != null) {
      // Technically, a code is restricted to string which has at least one character and no leading or trailing whitespace, and where there is no whitespace
      // other than single spaces in the contents
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, passesCodeWhitespaceRules(e.primitiveValue()), "The code '" + e.primitiveValue() + "' is not valid (whitespace rules)");
      rule(errors, IssueType.INVALID, e.line(), e.col(), path, !context.hasMaxLength() || context.getMaxLength()==0 ||  e.primitiveValue().length() <= context.getMaxLength(), "value is longer than permitted maximum length of " + context.getMaxLength());
    }

    if (context.hasBinding() && e.primitiveValue() != null) {
      checkPrimitiveBinding(errors, path, type, context, e, profile, node);
    }

    if (type.equals("xhtml")) {
      XhtmlNode xhtml = e.getXhtml();
      if (xhtml != null) { // if it is null, this is an error already noted in the parsers
        // check that the namespace is there and correct.
        String ns = xhtml.getNsDecl();
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, FormatUtilities.XHTML_NS.equals(ns), "Wrong namespace on the XHTML ('"+ns+"', should be '"+FormatUtilities.XHTML_NS+"')");
        // check that inner namespaces are all correct
        checkInnerNS(errors, e, path, xhtml.getChildNodes());
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, "div".equals(xhtml.getName()), "Wrong name on the XHTML ('"+ns+"') - must start with div");
        // check that no illegal elements and attributes have been used
        checkInnerNames(errors, e, path, xhtml.getChildNodes());
      }
    }

    if (context.hasFixed()) {
      checkFixedValue(errors,path,e, context.getFixed(), profile.getUrl(), context.getSliceName(), null, false);
    } 
    if (context.hasPattern()) {
      checkFixedValue(errors, path, e, context.getPattern(), profile.getUrl(), context.getSliceName(), null, true);
    }

    // for nothing to check
  }

  private void checkInnerNames(List<ValidationMessage> errors, Element e, String path, List<XhtmlNode> list) {
    for (XhtmlNode node : list) {
      if (node.getNodeType() == NodeType.Element) {
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, Utilities.existsInList(node.getName(),
            "p", "br", "div", "h1", "h2", "h3", "h4", "h5", "h6", "a", "span", "b", "em", "i", "strong",
            "small", "big", "tt", "small", "dfn", "q", "var", "abbr", "acronym", "cite", "blockquote", "hr", "address", "bdo", "kbd", "q", "sub", "sup",
            "ul", "ol", "li", "dl", "dt", "dd", "pre", "table", "caption", "colgroup", "col", "thead", "tr", "tfoot", "tbody", "th", "td",
            "code", "samp", "img", "map", "area"

            ), "Illegal element name in the XHTML ('"+node.getName()+"')");
        for (String an : node.getAttributes().keySet()) {
          boolean ok = an.startsWith("xmlns") || Utilities.existsInList(an,
              "title", "style", "class", "id", "lang", "xml:lang", "dir", "accesskey", "tabindex",
              // tables
              "span", "width", "align", "valign", "char", "charoff", "abbr", "axis", "headers", "scope", "rowspan", "colspan") ||

              Utilities.existsInList(node.getName()+"."+an, "a.href", "a.name", "img.src", "img.border", "div.xmlns", "blockquote.cite", "q.cite",
                  "a.charset", "a.type", "a.name", "a.href", "a.hreflang", "a.rel", "a.rev", "a.shape", "a.coords", "img.src",
                  "img.alt", "img.longdesc", "img.height", "img.width", "img.usemap", "img.ismap", "map.name", "area.shape",
                  "area.coords", "area.href", "area.nohref", "area.alt", "table.summary", "table.width", "table.border",
                  "table.frame", "table.rules", "table.cellspacing", "table.cellpadding", "pre.space", "td.nowrap"
                  );
          if (!ok)
            rule(errors, IssueType.INVALID, e.line(), e.col(), path, false, "Illegal attribute name in the XHTML ('"+an+"' on '"+node.getName()+"')");
        }
        checkInnerNames(errors, e, path, node.getChildNodes());
      }
    }
  }

  private void checkInnerNS(List<ValidationMessage> errors, Element e, String path, List<XhtmlNode> list) {
    for (XhtmlNode node : list) {
      if (node.getNodeType() == NodeType.Element) {
        String ns = node.getNsDecl();
        rule(errors, IssueType.INVALID, e.line(), e.col(), path, ns == null || FormatUtilities.XHTML_NS.equals(ns), "Wrong namespace on the XHTML ('"+ns+"', should be '"+FormatUtilities.XHTML_NS+"')");
        checkInnerNS(errors, e, path, node.getChildNodes());
      }
    }
  }

  private void checkPrimitiveBinding(List<ValidationMessage> errors, String path, String type, ElementDefinition elementContext, Element element, StructureDefinition profile, NodeStack stack) {
    // We ignore bindings that aren't on string, uri or code
    if (!element.hasPrimitiveValue() || !("code".equals(type) || "string".equals(type) || "uri".equals(type) || "url".equals(type) || "canonical".equals(type))) {
      return;
    }
    if (noTerminologyChecks)
      return;

    String value = element.primitiveValue();
    // System.out.println("check "+value+" in "+path);

    // firstly, resolve the value set
    ElementDefinitionBindingComponent binding = elementContext.getBinding();
    if (binding.hasValueSet()) {
      ValueSet vs = resolveBindingReference(profile, binding.getValueSet(), profile.getUrl());
      if (warning(errors, IssueType.CODEINVALID, element.line(), element.col(), path, vs != null, "ValueSet {0} not found by validator", describeReference(binding.getValueSet()))) {
        long t = System.nanoTime();
        ValidationResult vr = null;
		  if (binding.getStrength() != BindingStrength.EXAMPLE) {
          vr = context.validateCode(new ValidationOptions(stack.workingLang), value, vs);
		  }
        txTime = txTime + (System.nanoTime() - t);
        if (vr != null && !vr.isOk()) {
          if (vr.IsNoService())
            txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "The value provided ('"+value+"') could not be validated in the absence of a terminology server");
          else if (binding.getStrength() == BindingStrength.REQUIRED)
            txRule(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The value provided ('"+value+"') is not in the value set " + describeReference(binding.getValueSet()) + " (" + vs.getUrl() + ", and a code is required from this value set)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
          else if (binding.getStrength() == BindingStrength.EXTENSIBLE) {
            if (binding.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"))
              checkMaxValueSet(errors, path, element, profile, ToolingExtensions.readStringExtension(binding, "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxValueSet"), value, stack);
            else if (!noExtensibleWarnings) 
              txWarning(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false, "The value provided ('"+value+"') is not in the value set " + describeReference(binding.getValueSet()) + " (" + vs.getUrl() + ", and a code should come from this value set unless it has no suitable code)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
          } else if (binding.getStrength() == BindingStrength.PREFERRED)
            txHint(errors, vr.getTxLink(), IssueType.CODEINVALID, element.line(), element.col(), path, false,  "The value provided ('"+value+"') is not in the value set " + describeReference(binding.getValueSet()) + " (" + vs.getUrl() + ", and a code is recommended to come from this value set)"+(vr.getMessage() != null ? " (error message = "+vr.getMessage()+")" : ""));
        }
      }
    } else if (!noBindingMsgSuppressed)
      hint(errors, IssueType.CODEINVALID, element.line(), element.col(), path, !type.equals("code"), "Binding has no source, so can't be checked");
  }

  private void checkQuantity(List<ValidationMessage> errors, String path, Element focus, Quantity fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".value", focus.getNamedChild("value"), fixed.getValueElement(), fixedSource, "value", focus, pattern);
    checkFixedValue(errors, path + ".comparator", focus.getNamedChild("comparator"), fixed.getComparatorElement(), fixedSource, "comparator", focus, pattern);
    checkFixedValue(errors, path + ".units", focus.getNamedChild("unit"), fixed.getUnitElement(), fixedSource, "units", focus, pattern);
    checkFixedValue(errors, path + ".system", focus.getNamedChild("system"), fixed.getSystemElement(), fixedSource, "system", focus, pattern);
    checkFixedValue(errors, path + ".code", focus.getNamedChild("code"), fixed.getCodeElement(), fixedSource, "code", focus, pattern);
  }

  // implementation

  private void checkRange(List<ValidationMessage> errors, String path, Element focus, Range fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".low", focus.getNamedChild("low"), fixed.getLow(), fixedSource, "low", focus, pattern);
    checkFixedValue(errors, path + ".high", focus.getNamedChild("high"), fixed.getHigh(), fixedSource, "high", focus, pattern);

  }

  private void checkRatio(List<ValidationMessage> errors, String path, Element focus, Ratio fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".numerator", focus.getNamedChild("numerator"), fixed.getNumerator(), fixedSource, "numerator", focus, pattern);
    checkFixedValue(errors, path + ".denominator", focus.getNamedChild("denominator"), fixed.getDenominator(), fixedSource, "denominator", focus, pattern);
  }

  private void checkReference(ValidatorHostContext hostContext, List<ValidationMessage> errors, String path, Element element, StructureDefinition profile, ElementDefinition container, String parentType, NodeStack stack) throws FHIRException, IOException {
    Reference reference = ObjectConverter.readAsReference(element);

    String ref = reference.getReference();
    if (Utilities.noString(ref)) {
      if (Utilities.noString(reference.getIdentifier().getSystem()) && Utilities.noString(reference.getIdentifier().getValue())) {
        warning(errors, IssueType.STRUCTURE, element.line(), element.col(), path, !Utilities.noString(element.getNamedChildValue("display")), "A Reference without an actual reference or identifier should have a display");
      }
      return;
    }

    Element we = localResolve(ref, stack, errors, path);
    String refType;
    if (ref.startsWith("#")) {
      refType = "contained";
    } else {
      if (we == null) {
        refType = "remote";
      } else {
        refType = "bundled";
      }
    }
    ReferenceValidationPolicy pol = refType.equals("contained") || refType.equals("bundled") ? ReferenceValidationPolicy.CHECK_VALID : fetcher == null ? ReferenceValidationPolicy.IGNORE : fetcher.validationPolicy(hostContext.appContext, path, ref);

    if (pol.checkExists()) {
      if (we == null) {
        if (fetcher == null) {
          if (!refType.equals("contained"))
            throw new FHIRException("Resource resolution services not provided");
        } else {
          we = fetcher.fetch(hostContext.appContext, ref);
        }
      }
      rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, we != null || pol == ReferenceValidationPolicy.CHECK_TYPE_IF_EXISTS, "Unable to resolve resource '"+ref+"'");
    }

    String ft;
    if (we != null)
      ft = we.getType();
    else
      ft = tryParse(ref);

    if (reference.hasType()) {
      // the type has to match the specified
      String tu = isAbsolute(reference.getType()) ? reference.getType() : "http://hl7.org/fhir/StructureDefinition/"+reference.getType();
      TypeRefComponent containerType = container.getType("Reference");
      if (!containerType.hasTargetProfile(tu) && !containerType.hasTargetProfile("http://hl7.org/fhir/StructureDefinition/Resource")) {
        boolean matchingResource = false;
        for (CanonicalType target: containerType.getTargetProfile()) {
          StructureDefinition sd = (StructureDefinition)context.fetchResource(StructureDefinition.class, target.asStringValue());
          if (("http://hl7.org/fhir/StructureDefinition/" + sd.getType()).equals(tu)) {
            matchingResource = true;
            break;
          }
        }
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, matchingResource, 
            "The type '"+reference.getType()+"' is not a valid Target for this element (must be one of "+container.getType("Reference").getTargetProfile()+")");
        
      }
      // the type has to match the actual
      rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, ft==null || ft.equals(reference.getType()), "The specified type '"+reference.getType()+"' does not match the found type '"+ft+"'");      
    }
    
    if (we != null && pol.checkType()) {
      if (warning(errors, IssueType.STRUCTURE, element.line(), element.col(), path, ft!=null, "Unable to determine type of target resource")) {
        boolean ok = false;
        CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
        for (TypeRefComponent type : container.getType()) {
          if (!ok && type.getWorkingCode().equals("Reference")) {
            // we validate as much as we can. First, can we infer a type from the profile?
            if (!type.hasTargetProfile() || type.hasTargetProfile("http://hl7.org/fhir/StructureDefinition/Resource"))
              ok = true;
            else {
              List<String> candidateProfiles = new ArrayList<String>();
              for (UriType u : type.getTargetProfile()) {              
                String pr = u.getValue();
  
                String bt = getBaseType(profile, pr);
                StructureDefinition sd = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/" + bt);
                if (rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, bt != null, "Unable to resolve the profile reference '" + pr + "'")) {
                  b.append(bt);
                  if (bt.equals(ft)) {
                    ok = true;
                    if (we!=null && pol.checkValid())
                      candidateProfiles.add(pr);
                  }
                }
              }
              HashMap<String, List<ValidationMessage>> goodProfiles = new HashMap<String, List<ValidationMessage>>();
              List<List<ValidationMessage>> badProfiles = new ArrayList<List<ValidationMessage>>();
              List<String> profiles = new ArrayList<String>();
              if (!candidateProfiles.isEmpty()) {
                for (String pr: candidateProfiles) {
                  profiles.add(pr);
                  List<ValidationMessage> profileErrors = new ArrayList<ValidationMessage>();
                  doResourceProfile(hostContext, we, pr, profileErrors, stack.push(we, -1, null, null), path, element, profile);
  
                  if (hasErrors(profileErrors))
                    badProfiles.add(profileErrors);
                  else
                    goodProfiles.put(pr, profileErrors);
                    if (type.hasAggregation()) {
                      boolean modeOk = false;
                      for (Enumeration<AggregationMode> mode : type.getAggregation()) {
                        if (mode.getValue().equals(AggregationMode.CONTAINED) && refType.equals("contained"))
                          modeOk = true;
                        else if (mode.getValue().equals(AggregationMode.BUNDLED) && refType.equals("bundled"))
                          modeOk = true;
                        else if (mode.getValue().equals(AggregationMode.REFERENCED) && (refType.equals("bundled")||refType.equals("remote")))
                          modeOk = true;
                      }
                      rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, modeOk, "Reference is " + refType + " which isn't supported by the specified aggregation mode(s) for the reference");
                    }
                }
                if (goodProfiles.size()==1) {
                  errors.addAll(goodProfiles.values().iterator().next());
                } else if (goodProfiles.size()==0) {
                  rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, profiles.size()==1, "Unable to find matching profile among choices: " + StringUtils.join("; ", profiles));
                  for (List<ValidationMessage> messages : badProfiles) {
                    errors.addAll(messages);
                  }
                } else {
                  warning(errors, IssueType.STRUCTURE, element.line(), element.col(), path, false, "Found multiple matching profiles among choices: " + StringUtils.join("; ", goodProfiles.keySet()));
                  for (List<ValidationMessage> messages : goodProfiles.values()) {
                    errors.addAll(messages);
                  }                    
                }
              }
            }
          }
          if (!ok && type.getCode().equals("*")) {
            ok = true; // can refer to anything
          }
        }
        rule(errors, IssueType.STRUCTURE, element.line(), element.col(), path, ok, "Invalid Resource target type. Found " + ft + ", but expected one of (" + b.toString() + ")");
      }
    }
    if (we == null) {
    	// Ensure that reference was not defined as being "bundled" or "contained"
        boolean missingRef = false;
        for (TypeRefComponent type : container.getType()) {
        	if (!missingRef && type.getCode().equals("Reference")) {
        		if (type.hasAggregation()) {
        			for (Enumeration<AggregationMode> mode : type.getAggregation()) {
        				if (mode.getValue().equals(AggregationMode.CONTAINED) || mode.getValue().equals(AggregationMode.BUNDLED)) {
        					missingRef = true;
        					break;
        				}
        			}
        		}
        	}
        }
        rule(errors, IssueType.REQUIRED, -1, -1, path, !missingRef, "Bundled or contained reference not found within the bundle/resource " + ref);
    }

    if (pol == ReferenceValidationPolicy.CHECK_VALID) {
      // todo....
    }
  }

  private void doResourceProfile(ValidatorHostContext hostContext, Element resource, String profile, List<ValidationMessage> errors, NodeStack stack, String path, Element element, StructureDefinition containingProfile) throws FHIRException, IOException {
    ResourceProfiles resourceProfiles = addResourceProfile(errors, resource, profile, path, element, stack, containingProfile);
    if (resourceProfiles.isProcessed()) {
      start(hostContext, errors, resource, resource, null, stack);
    }
  }

  private ResourceProfiles getResourceProfiles(Element resource, NodeStack stack) {
    ResourceProfiles resourceProfiles = resourceProfilesMap.get(resource);
    if (resourceProfiles==null) {
      resourceProfiles = new ResourceProfiles(resource, stack);
      resourceProfilesMap.put(resource, resourceProfiles);
    }
    return resourceProfiles;
  }

  private ResourceProfiles addResourceProfile(List<ValidationMessage> errors, Element resource, String profile, String path, Element element, NodeStack stack, StructureDefinition containingProfile) {
    ResourceProfiles resourceProfiles = getResourceProfiles(resource, stack);
    resourceProfiles.addProfile(errors, profile, errorForUnknownProfiles, path, element, containingProfile);
    return resourceProfiles;
  }

  private String checkResourceType(String type)  {
    long t = System.nanoTime();
    try {
      if (context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/" + type) != null)
        return type;
      else
        return null;
    } finally {
      sdTime = sdTime + (System.nanoTime() - t);
    }
  }

  private void checkSampledData(List<ValidationMessage> errors, String path, Element focus, SampledData fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".origin", focus.getNamedChild("origin"), fixed.getOrigin(), fixedSource, "origin", focus, pattern);
    checkFixedValue(errors, path + ".period", focus.getNamedChild("period"), fixed.getPeriodElement(), fixedSource, "period", focus, pattern);
    checkFixedValue(errors, path + ".factor", focus.getNamedChild("factor"), fixed.getFactorElement(), fixedSource, "factor", focus, pattern);
    checkFixedValue(errors, path + ".lowerLimit", focus.getNamedChild("lowerLimit"), fixed.getLowerLimitElement(), fixedSource, "lowerLimit", focus, pattern);
    checkFixedValue(errors, path + ".upperLimit", focus.getNamedChild("upperLimit"), fixed.getUpperLimitElement(), fixedSource, "upperLimit", focus, pattern);
    checkFixedValue(errors, path + ".dimensions", focus.getNamedChild("dimensions"), fixed.getDimensionsElement(), fixedSource, "dimensions", focus, pattern);
    checkFixedValue(errors, path + ".data", focus.getNamedChild("data"), fixed.getDataElement(), fixedSource, "data", focus, pattern);
  }

  private void checkTiming(List<ValidationMessage> errors, String path, Element focus, Timing fixed, String fixedSource, boolean pattern) {
    checkFixedValue(errors, path + ".repeat", focus.getNamedChild("repeat"), fixed.getRepeat(), fixedSource, "value", focus, pattern);

    List<Element> events = new ArrayList<Element>();
    focus.getNamedChildren("event", events);
    if (rule(errors, IssueType.VALUE, focus.line(), focus.col(), path, events.size() == fixed.getEvent().size(),
        "Expected " + Integer.toString(fixed.getEvent().size()) + " but found " + Integer.toString(events.size()) + " event elements")) {
      for (int i = 0; i < events.size(); i++)
        checkFixedValue(errors, path + ".event", events.get(i), fixed.getEvent().get(i), fixedSource, "event", focus, pattern);
    }
  }

  private boolean codeinExpansion(ValueSetExpansionContainsComponent cnt, String system, String code) {
    for (ValueSetExpansionContainsComponent c : cnt.getContains()) {
      if (code.equals(c.getCode()) && system.equals(c.getSystem().toString()))
        return true;
      if (codeinExpansion(c, system, code))
        return true;
    }
    return false;
  }

  private boolean codeInExpansion(ValueSet vs, String system, String code) {
    for (ValueSetExpansionContainsComponent c : vs.getExpansion().getContains()) {
      if (code.equals(c.getCode()) && (system == null || system.equals(c.getSystem())))
        return true;
      if (codeinExpansion(c, system, code))
        return true;
    }
    return false;
  }

  private String describeReference(String reference) {
    if (reference == null)
      return "null";
    return reference;
  }

  private String describeTypes(List<TypeRefComponent> types) {
    CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
    for (TypeRefComponent t : types) {
      b.append(t.getWorkingCode());
    }
    return b.toString();
  }

  protected ElementDefinition findElement(StructureDefinition profile, String name) {
    for (ElementDefinition c : profile.getSnapshot().getElement()) {
      if (c.getPath().equals(name)) {
        return c;
      }
    }
    return null;
  }

  public BestPracticeWarningLevel getBestPracticeWarningLevel() {
    return bpWarnings;
  }

  private String getBaseType(StructureDefinition profile, String pr)  {
    StructureDefinition p = resolveProfile(profile, pr);
    if (p == null)
      return null;
    else
      return p.getType();
  }

  @Override
  public CheckDisplayOption getCheckDisplay() {
    return checkDisplay;
  }

  //	private String findProfileTag(Element element) {
  //  	String uri = null;
  //	  List<Element> list = new ArrayList<Element>();
  //	  element.getNamedChildren("category", list);
  //	  for (Element c : list) {
  //	  	if ("http://hl7.org/fhir/tag/profile".equals(c.getAttribute("scheme"))) {
  //	  		uri = c.getAttribute("term");
  //	  	}
  //	  }
  //	  return uri;
  //  }

  private ConceptDefinitionComponent getCodeDefinition(ConceptDefinitionComponent c, String code) {
    if (code.equals(c.getCode()))
      return c;
    for (ConceptDefinitionComponent g : c.getConcept()) {
      ConceptDefinitionComponent r = getCodeDefinition(g, code);
      if (r != null)
        return r;
    }
    return null;
  }

  private ConceptDefinitionComponent getCodeDefinition(CodeSystem cs, String code) {
    for (ConceptDefinitionComponent c : cs.getConcept()) {
      ConceptDefinitionComponent r = getCodeDefinition(c, code);
      if (r != null)
        return r;
    }
    return null;
  }

  private Element getContainedById(Element container, String id) {
    List<Element> contained = new ArrayList<Element>();
    container.getNamedChildren("contained", contained);
    for (Element we : contained) {
      if (id.equals(we.getNamedChildValue("id")))
        return we;
    }
    return null;
  }

  public IWorkerContext getContext() {
    return context;
  }

  private List<ElementDefinition> getCriteriaForDiscriminator(String path, ElementDefinition element, String discriminator, StructureDefinition profile, boolean removeResolve) throws FHIRException {
    List<ElementDefinition> elements = new ArrayList<ElementDefinition>();
    if ("value".equals(discriminator) && element.hasFixed()) {
      elements.add(element);
      return elements;
    }

    if (removeResolve) {  // if we're doing profile slicing, we don't want to walk into the last resolve.. we need the profile on the source not the target
      if (discriminator.equals("resolve()")) {
        elements.add(element);
        return elements;
      }
      if (discriminator.endsWith(".resolve()"))
        discriminator = discriminator.substring(0, discriminator.length() - 10);
    }

    ElementDefinition ed = null;
    ExpressionNode expr = fpe.parse(fixExpr(discriminator));
    long t2 = System.nanoTime();
    ed = fpe.evaluateDefinition(expr, profile, element);
    sdTime = sdTime + (System.nanoTime() - t2);
    if (ed!= null)
      elements.add(ed);

    for (TypeRefComponent type: element.getType()) {
      for (CanonicalType p: type.getProfile()) {
        String id = p.hasExtension(ToolingExtensions.EXT_PROFILE_ELEMENT) ? p.getExtensionString(ToolingExtensions.EXT_PROFILE_ELEMENT) : null;
        StructureDefinition sd = context.fetchResource(StructureDefinition.class, p.getValue());
        if (sd == null)
          throw new DefinitionException("Unable to resolve profile "+p);
        profile = sd;
        if (id == null)
          element = sd.getSnapshot().getElementFirstRep();
        else {
          element = null;
          for (ElementDefinition t : sd.getSnapshot().getElement()) {
            if (id.equals(t.getId()))
              element = t;
          }
          if (element == null)
            throw new DefinitionException("Unable to resolve element "+id+" in profile "+p);
        }
        expr = fpe.parse(fixExpr(discriminator));
        t2 = System.nanoTime();
        ed = fpe.evaluateDefinition(expr, profile, element);
        sdTime = sdTime + (System.nanoTime() - t2);
        if (ed != null)
          elements.add(ed);
      }
    }
    return elements;
  }


  private Element getExtensionByUrl(List<Element> extensions, String urlSimple) {
    for (Element e : extensions) {
      if (urlSimple.equals(e.getNamedChildValue("url")))
        return e;
    }
    return null;
  }

  public List<String> getExtensionDomains() {
    return extensionDomains;
  }

  private Element getFromBundle(Element bundle, String ref, String fullUrl, List<ValidationMessage> errors, String path, String type) {
    String targetUrl = null;
    String version = "";
    String resourceType = null;
    if (ref.startsWith("http") || ref.startsWith("urn")) {
      // We've got an absolute reference, no need to calculate
      if (ref.contains("/_history/")) {
        targetUrl = ref.substring(0, ref.indexOf("/_history/") - 1);
        version = ref.substring(ref.indexOf("/_history/") + 10);
      }  else
        targetUrl = ref;

    } else if (fullUrl == null) {
      //This isn't a problem for signatures - if it's a signature, we won't have a resolution for a relative reference.  For anything else, this is an error
      // but this rule doesn't apply for batches or transactions
      rule(errors, IssueType.REQUIRED, -1, -1, path, Utilities.existsInList(type, "batch-response", "transaction-response") || path.startsWith("Bundle.signature"), "Relative Reference appears inside Bundle whose entry is missing a fullUrl");
      return null;

    } else if (ref.split("/").length!=2 && ref.split("/").length!=4) {
      rule(errors, IssueType.INVALID, -1, -1, path, false, "Relative URLs must be of the format [ResourceName]/[id].  Encountered " + ref);
      return null;

    } else {
      String base = "";
      if (fullUrl.startsWith("urn")) {
        String[] parts = fullUrl.split("\\:");
        for (int i=0; i < parts.length-1; i++) {
          base = base + parts[i] + ":";
        }
      } else {
        String[] parts;
        parts = fullUrl.split("/");
        for (int i=0; i < parts.length-2; i++) {
          base = base + parts[i] + "/";
        }
      }

      String id = null;
      if (ref.contains("/_history/")) {
        version = ref.substring(ref.indexOf("/_history/") + 10);
        String[] refBaseParts = ref.substring(0, ref.indexOf("/_history/")).split("/"); 
        resourceType = refBaseParts[0];
        id = refBaseParts[1];
      } else if (base.startsWith("urn")) {
        resourceType = ref.split("/")[0];
        id = ref.split("/")[1];
      } else
        id = ref;

      targetUrl = base + id;
    }

    List<Element> entries = new ArrayList<Element>();
    bundle.getNamedChildren("entry", entries);
    Element match = null;
    for (Element we : entries) {
      if (targetUrl.equals(we.getChildValue("fullUrl"))) {
        Element r = we.getNamedChild("resource");
        if (version.isEmpty()) {
          rule(errors, IssueType.FORBIDDEN, -1, -1, path, match==null, "Multiple matches in bundle for reference " + ref);
          match = r;
        } else {
          try {
            if (version.equals(r.getChildren("meta").get(0).getChildValue("versionId"))) {
              rule(errors, IssueType.FORBIDDEN, -1, -1, path, match==null, "Multiple matches in bundle for reference " + ref);
              match = r;
            }
          } catch (Exception e) {
            warning(errors, IssueType.REQUIRED, -1, -1, path, r.getChildren("meta").size()==1 && r.getChildren("meta").get(0).getChildValue("versionId")!=null, "Entries matching fullURL " + targetUrl + " should declare meta/versionId because there are version-specific references");
            // If one of these things is null
          }
        }
      }
    }

    if (match!=null && resourceType!=null)
      rule(errors, IssueType.REQUIRED, -1, -1, path, match.getType().equals(resourceType), "Matching reference for reference " + ref + " has resourceType " + match.getType());
    if (match == null)
      warning(errors, IssueType.REQUIRED, -1, -1, path, !ref.startsWith("urn"), "URN reference is not locally contained within the bundle " + ref);
    return match;
  }

  private StructureDefinition getProfileForType(String type, List<TypeRefComponent> list) {
    for (TypeRefComponent tr : list) {
      String url = tr.getWorkingCode();
      if (!Utilities.isAbsoluteUrl(url))
        url = "http://hl7.org/fhir/StructureDefinition/" + url;
      long t = System.nanoTime();
      StructureDefinition sd = context.fetchResource(StructureDefinition.class, url);
      sdTime = sdTime + (System.nanoTime() - t);
      if (sd != null && (sd.getType().equals(type) || sd.getUrl().equals(type)) && sd.hasSnapshot())
        return sd;
    }
    return null;
  }

  private Element getValueForDiscriminator(Object appContext, List<ValidationMessage> errors, Element element, String discriminator, ElementDefinition criteria, NodeStack stack) throws FHIRException, IOException  {
    String p = stack.getLiteralPath()+"."+element.getName();
    Element focus = element;
    String[] dlist = discriminator.split("\\.");
    for (String d : dlist) {
      if (focus.fhirType().equals("Reference") && d.equals("reference")) {
        String url = focus.getChildValue("reference");
        if (Utilities.noString(url))
          throw new FHIRException("No reference resolving discriminator "+discriminator+" from "+element.getProperty().getName());
        // Note that we use the passed in stack here. This might be a problem if the discriminator is deep enough?
        Element target = resolve(appContext, url, stack, errors, p);
        if (target == null)
          throw new FHIRException("Unable to find resource "+url+" at "+d+" resolving discriminator "+discriminator+" from "+element.getProperty().getName());
        focus = target;
      } else if (d.equals("value") && focus.isPrimitive()) {
        return focus;
      } else {
        List<Element> children = focus.getChildren(d);
        if (children.isEmpty())
          throw new FHIRException("Unable to find "+d+" resolving discriminator "+discriminator+" from "+element.getProperty().getName());
        if (children.size() > 1)
          throw new FHIRException("Found "+Integer.toString(children.size())+" items for "+d+" resolving discriminator "+discriminator+" from "+element.getProperty().getName());
        focus = children.get(0);
        p = p + "."+d;
      }
    }
    return focus;
  }

  private CodeSystem getCodeSystem(String system) {
    long t = System.nanoTime();
    try {
      return context.fetchCodeSystem(system);
    } finally {
      txTime = txTime + (System.nanoTime() - t);
    }
  }

  private boolean hasTime(String fmt) {
    return fmt.contains("T");
  }

  private boolean hasTimeZone(String fmt) {
    return fmt.length() > 10 && (fmt.substring(10).contains("-") || fmt.substring(10).contains("+") || fmt.substring(10).contains("Z"));
  }

  private boolean isAbsolute(String uri) {
    return Utilities.noString(uri) || uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("urn:uuid:") || uri.startsWith("urn:oid:") || uri.startsWith("urn:ietf:")
        || uri.startsWith("urn:iso:") || uri.startsWith("urn:iso-astm:") || isValidFHIRUrn(uri);
  }

  private boolean isValidFHIRUrn(String uri) {
    return (uri.equals("urn:x-fhir:uk:id:nhs-number")) || uri.startsWith("urn:"); // Anyone can invent a URN, so why should we complain?
  }

  public boolean isAnyExtensionsAllowed() {
    return anyExtensionsAllowed;
  }

  public boolean isErrorForUnknownProfiles() {
    return errorForUnknownProfiles;
  }

  public void setErrorForUnknownProfiles(boolean errorForUnknownProfiles) {
    this.errorForUnknownProfiles = errorForUnknownProfiles;
  }

  private boolean isParametersEntry(String path) {
    String[] parts = path.split("\\.");
    return parts.length > 2 && parts[parts.length - 1].equals("resource") && (pathEntryHasName(parts[parts.length - 2], "parameter") || pathEntryHasName(parts[parts.length - 2], "part"));
  }

  private boolean isBundleEntry(String path) {
    String[] parts = path.split("\\.");
    return parts.length > 2 && parts[parts.length - 1].equals("resource") && pathEntryHasName(parts[parts.length - 2], "entry");
  }

  private boolean isBundleOutcome(String path) {
    String[] parts = path.split("\\.");
    return parts.length > 2 && parts[parts.length - 1].equals("outcome") && pathEntryHasName(parts[parts.length - 2], "response");
  }


  private static boolean pathEntryHasName(String thePathEntry, String theName) {
    if (thePathEntry.equals(theName)) {
      return true;
    }
    if (thePathEntry.length() >= theName.length() + 3) {
      if (thePathEntry.startsWith(theName)) {
        if (thePathEntry.charAt(theName.length()) == '[') {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isPrimitiveType(String code) {
    StructureDefinition sd = context.fetchTypeDefinition(code);
    return sd != null && sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE;
  }



  public boolean isSuppressLoincSnomedMessages() {
    return suppressLoincSnomedMessages;
  }

  private boolean nameMatches(String name, String tail) {
    if (tail.endsWith("[x]"))
      return name.startsWith(tail.substring(0, tail.length() - 3));
    else
      return (name.equals(tail));
  }

  // private String mergePath(String path1, String path2) {
  // // path1 is xpath path
  // // path2 is dotted path
  // String[] parts = path2.split("\\.");
  // StringBuilder b = new StringBuilder(path1);
  // for (int i = 1; i < parts.length -1; i++)
  // b.append("/f:"+parts[i]);
  // return b.toString();
  // }

  private boolean passesCodeWhitespaceRules(String v) {
    if (!v.trim().equals(v))
      return false;
    boolean lastWasSpace = true;
    for (char c : v.toCharArray()) {
      if (c == ' ') {
        if (lastWasSpace)
          return false;
        else
          lastWasSpace = true;
      } else if (Character.isWhitespace(c))
        return false;
      else
        lastWasSpace = false;
    }
    return true;
  }

  private Element localResolve(String ref, NodeStack stack, List<ValidationMessage> errors, String path) {
    if (ref.startsWith("#")) {
      // work back through the contained list.
      // really, there should only be one level for this (contained resources cannot contain
      // contained resources), but we'll leave that to some other code to worry about
      while (stack != null && stack.getElement() != null) {
        if (stack.getElement().getProperty().isResource()) {
          // ok, we'll try to find the contained reference
          Element res = getContainedById(stack.getElement(), ref.substring(1));
          if (res != null)
            return res;
        }
        if (stack.getElement().getSpecial() == SpecialElement.BUNDLE_ENTRY) {
          return null; // we don't try to resolve contained references across this boundary
        }
        stack = stack.parent;
      }
      return null;
    } else {
      // work back through the contained list - if any of them are bundles, try to resolve
      // the resource in the bundle
      String fullUrl = null; // we're going to try to work this out as we go up
      while (stack != null && stack.getElement() != null) {
        if (stack.getElement().getSpecial() == SpecialElement.BUNDLE_ENTRY && fullUrl==null && stack.parent != null && stack.parent.getElement().getName().equals("entry")) {
          String type = stack.parent.parent.element.getChildValue("type");
          fullUrl = stack.parent.getElement().getChildValue("fullUrl"); // we don't try to resolve contained references across this boundary
          if (fullUrl==null) 
            rule(errors, IssueType.REQUIRED, stack.parent.getElement().line(), stack.parent.getElement().col(), stack.parent.getLiteralPath(), 
                Utilities.existsInList(type, "batch-response", "transaction-response") || fullUrl!=null, "Bundle entry missing fullUrl");
        }
        if ("Bundle".equals(stack.getElement().getType())) {
          String type = stack.getElement().getChildValue("type");
          Element res = getFromBundle(stack.getElement(), ref, fullUrl, errors, path, type);
          return res;
        }
        stack = stack.parent;
      }
    }
    return null;
  }

  private Element resolve(Object appContext, String ref, NodeStack stack, List<ValidationMessage> errors, String path) throws IOException, FHIRException {
    Element local = localResolve(ref, stack, errors, path);
    if (local!=null)
      return local;
    if (fetcher == null)
      return null;
    return fetcher.fetch(appContext, ref);
  }

  private ValueSet resolveBindingReference(DomainResource ctxt, String reference, String uri) {
    if (reference != null) {
      if (reference.startsWith("#")) {
        for (Resource c : ctxt.getContained()) {
          if (c.getId().equals(reference.substring(1)) && (c instanceof ValueSet))
            return (ValueSet) c;
        }
        return null;
      } else {
        long t = System.nanoTime();
			ValueSet fr = context.fetchResource(ValueSet.class, reference);
			if (fr == null) {
				if (!Utilities.isAbsoluteUrl(reference)) {
					reference = resolve(uri, reference);
					fr = context.fetchResource(ValueSet.class, reference);
				}
			}
			if (fr == null)
			  fr = ValueSetUtilities.generateImplicitValueSet(reference);
			txTime = txTime + (System.nanoTime() - t);
        return fr;
      }
    } else
      return null;
  }

  private String resolve(String uri, String ref) {
    if (isBlank(uri)) {
      return ref;
    }
    String[] up = uri.split("\\/");
    String[] rp = ref.split("\\/");
    if (context.getResourceNames().contains(up[up.length-2]) && context.getResourceNames().contains(rp[0])) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < up.length-2; i++) {
        b.append(up[i]);
        b.append("/");
      }
      b.append(ref);
      return b.toString();
    } else
      return ref;
  }

  private Element resolveInBundle(List<Element> entries, String ref, String fullUrl, String type, String id) {
    if (Utilities.isAbsoluteUrl(ref)) {
      // if the reference is absolute, then you resolve by fullUrl. No other thinking is required.
      for (Element entry : entries) {
        String fu = entry.getNamedChildValue("fullUrl");
        if (ref.equals(fu))
          return entry;
      }
      return null;
    } else {
      // split into base, type, and id
      String u = null;
      if (fullUrl != null && fullUrl.endsWith(type+"/"+id))
        // fullUrl = complex
        u = fullUrl.substring(0, fullUrl.length() - (type+"/"+id).length())+ref;
//        u = fullUrl.substring((type+"/"+id).length())+ref;
      String[] parts = ref.split("\\/");
      if (parts.length >= 2) {
        String t = parts[0];
        String i = parts[1];
        for (Element entry : entries) {
          String fu = entry.getNamedChildValue("fullUrl");
          if (u != null && fu.equals(u))
            return entry;
          if (u == null) {
            Element resource = entry.getNamedChild("resource");
            String et = resource.getType();
            String eid = resource.getNamedChildValue("id");
            if (t.equals(et) && i.equals(eid))
              return entry;
          }
        }
      }
      return null;
    }
  }

  private ElementDefinition resolveNameReference(StructureDefinitionSnapshotComponent snapshot, String contentReference) {
    for (ElementDefinition ed : snapshot.getElement())
      if (contentReference.equals("#"+ed.getId()))
        return ed;
    return null;
  }

  private StructureDefinition resolveProfile(StructureDefinition profile, String pr)  {
    if (pr.startsWith("#")) {
      for (Resource r : profile.getContained()) {
        if (r.getId().equals(pr.substring(1)) && r instanceof StructureDefinition)
          return (StructureDefinition) r;
      }
      return null;
    } else {
      long t = System.nanoTime();
      StructureDefinition fr = context.fetchResource(StructureDefinition.class, pr);
      sdTime = sdTime + (System.nanoTime() - t);
      return fr;
    }
  }

  private ElementDefinition resolveType(String type, List<TypeRefComponent> list)  {
    for (TypeRefComponent tr : list) {
      String url = tr.getWorkingCode();
      if (!Utilities.isAbsoluteUrl(url))
        url = "http://hl7.org/fhir/StructureDefinition/" + url;
      long t = System.nanoTime();
      StructureDefinition sd = context.fetchResource(StructureDefinition.class, url);
      sdTime = sdTime + (System.nanoTime() - t);
      if (sd != null && (sd.getType().equals(type) || sd.getUrl().equals(type)) && sd.hasSnapshot())
        return sd.getSnapshot().getElement().get(0);
    }
    return null;
  }

  public void setAnyExtensionsAllowed(boolean anyExtensionsAllowed) {
    this.anyExtensionsAllowed = anyExtensionsAllowed;
  }

  public IResourceValidator setBestPracticeWarningLevel(BestPracticeWarningLevel value) {
    bpWarnings = value;
    return this;
  }

  @Override
  public void setCheckDisplay(CheckDisplayOption checkDisplay) {
    this.checkDisplay = checkDisplay;
  }

  public void setSuppressLoincSnomedMessages(boolean suppressLoincSnomedMessages) {
    this.suppressLoincSnomedMessages = suppressLoincSnomedMessages;
  }

  public IdStatus getResourceIdRule() {
    return resourceIdRule;
  }

  public void setResourceIdRule(IdStatus resourceIdRule) {
    this.resourceIdRule = resourceIdRule;
  }


  public boolean isAllowXsiLocation() {
    return allowXsiLocation;
  }

  public void setAllowXsiLocation(boolean allowXsiLocation) {
    this.allowXsiLocation = allowXsiLocation;
  }
  
  /**
   *
   * @param element
   *          - the candidate that might be in the slice
   * @param path
   *          - for reporting any errors. the XPath for the element
   * @param slicer
   *          - the definition of how slicing is determined
   * @param ed
   *          - the slice for which to test membership
   * @param errors
   * @param stack
   * @return
   * @throws DefinitionException
   * @throws DefinitionException
   * @throws IOException
   * @throws FHIRException
   */
  private boolean sliceMatches(ValidatorHostContext hostContext, Element element, String path, ElementDefinition slicer, ElementDefinition ed, StructureDefinition profile, List<ValidationMessage> errors, NodeStack stack) throws DefinitionException, FHIRException, IOException {
    if (!slicer.getSlicing().hasDiscriminator())
      return false; // cannot validate in this case

    ExpressionNode n = (ExpressionNode) ed.getUserData("slice.expression.cache");
    if (n == null) {
      long t = System.nanoTime();
      // GG: this approach is flawed because it treats discriminators individually rather than collectively
      StringBuilder expression = new StringBuilder("true");
      boolean anyFound = false;
      Set<String> discriminators = new HashSet<>();
      for (ElementDefinitionSlicingDiscriminatorComponent s : slicer.getSlicing().getDiscriminator()) {
        String discriminator = s.getPath();
        discriminators.add(discriminator);
        
        List<ElementDefinition> criteriaElements = getCriteriaForDiscriminator(path, ed, discriminator, profile, s.getType() == DiscriminatorType.PROFILE);
        boolean found = false;
        for (ElementDefinition criteriaElement : criteriaElements) {
          found = true;
          if (s.getType() == DiscriminatorType.TYPE) {
            String type = null;
            if (!criteriaElement.getPath().contains("[") && discriminator.contains("[")) {
              discriminator = discriminator.substring(0, discriminator.indexOf('['));
              String lastNode = tail(discriminator);
              type = tail(criteriaElement.getPath()).substring(lastNode.length());
              type = type.substring(0,1).toLowerCase() + type.substring(1);
            } else if (!criteriaElement.hasType() || criteriaElement.getType().size()==1) {
              if (discriminator.contains("["))
                discriminator = discriminator.substring(0, discriminator.indexOf('['));
              type = criteriaElement.getType().get(0).getWorkingCode();
            } else if (criteriaElement.getType().size() > 1) {
              throw new DefinitionException("Discriminator (" + discriminator + ") is based on type, but slice " + ed.getId() + " in "+profile.getUrl()+" has multiple types: "+criteriaElement.typeSummary());
            } else
              throw new DefinitionException("Discriminator (" + discriminator + ") is based on type, but slice " + ed.getId() + " in "+profile.getUrl()+" has no types");
            if (discriminator.isEmpty())
              expression.append(" and this is " + type);
            else
              expression.append(" and " + discriminator + " is " + type);
          } else if (s.getType() == DiscriminatorType.PROFILE) {
            if (criteriaElement.getType().size() == 0)
              throw new DefinitionException("Profile based discriminators must have a type ("+criteriaElement.getId()+")");
            if (criteriaElement.getType().size() != 1)
              throw new DefinitionException("Profile based discriminators must have only one type ("+criteriaElement.getId()+")");
            List<CanonicalType> list = discriminator.endsWith(".resolve()") || discriminator.equals("resolve()") ? criteriaElement.getType().get(0).getTargetProfile() : criteriaElement.getType().get(0).getProfile();
            if (list.size() == 0)
              throw new DefinitionException("Profile based discriminators must have a type with a profile ("+criteriaElement.getId()+")");
            if (list.size() > 1)
              throw new DefinitionException("Profile based discriminators must have a type with only one profile ("+criteriaElement.getId()+")");
            expression.append(" and "+discriminator+".conformsTo('"+list.get(0).getValue()+"')");
          } else if (s.getType() == DiscriminatorType.EXISTS) {
            if (criteriaElement.hasMin() && criteriaElement.getMin()>=1)
              expression.append(" and (" + discriminator + ".exists())");
            else if (criteriaElement.hasMax() && criteriaElement.getMax().equals("0"))
              expression.append(" and (" + discriminator + ".exists().not())");
            else
              throw new FHIRException("Discriminator (" + discriminator + ") is based on element existence, but slice " + ed.getId() + " neither sets min>=1 or max=0");
          } else if (criteriaElement.hasFixed()) {
            buildFixedExpression(ed, expression, discriminator, criteriaElement);
          } else if (criteriaElement.hasPattern()) {
            buildPattternExpression(ed, expression, discriminator, criteriaElement);
          } else if (criteriaElement.hasBinding() && criteriaElement.getBinding().hasStrength() && criteriaElement.getBinding().getStrength().equals(BindingStrength.REQUIRED) && criteriaElement.getBinding().hasValueSet()) {
            expression.append(" and (" + discriminator + " memberOf '" + criteriaElement.getBinding().getValueSet() + "')");
          } else {
            found = false;
          }
          if (found)
            break;
        }
        if (found)
          anyFound = true;
      }
      if (!anyFound) {
        if (slicer.getSlicing().getDiscriminator().size() > 1)
          throw new DefinitionException("Could not match any discriminators (" + discriminators + ") for slice " + ed.getId() + " in profile " + profile.getUrl() + " - None of the discriminator " + discriminators + " have fixed value, binding or existence assertions");
        else 
          throw new DefinitionException("Could not match discriminator (" + discriminators + ") for slice " + ed.getId() + " in profile " + profile.getUrl() + " - the discriminator " + discriminators + " does not have fixed value, binding or existence assertions");
      }

      try {
        n = fpe.parse(fixExpr(expression.toString()));
      } catch (FHIRLexerException e) {
        throw new FHIRException("Problem processing expression "+expression +" in profile " + profile.getUrl() + " path " + path + ": " + e.getMessage());
      }
      fpeTime = fpeTime + (System.nanoTime() - t);
      ed.setUserData("slice.expression.cache", n);
    }

    return evaluateSlicingExpression(hostContext, element, path, profile, n);
  }

  public boolean evaluateSlicingExpression(ValidatorHostContext hostContext, Element element, String path, StructureDefinition profile, ExpressionNode n)  throws FHIRException {
    String msg;
    boolean ok;
    try {
      long t = System.nanoTime();
      ok = fpe.evaluateToBoolean(hostContext.forProfile(profile), hostContext.resource, hostContext.rootResource, element, n);
      fpeTime = fpeTime + (System.nanoTime() - t);
      msg = fpe.forLog();
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new FHIRException("Problem evaluating slicing expression for element in profile " + profile.getUrl() + " path " + path + " (fhirPath = "+n+"): " + ex.getMessage());
    }
    return ok;
  }

  private void buildPattternExpression(ElementDefinition ed, StringBuilder expression, String discriminator, ElementDefinition criteriaElement) throws DefinitionException {
    Type pattern = criteriaElement.getPattern();
    if (pattern instanceof CodeableConcept) {
      CodeableConcept cc = (CodeableConcept) pattern;
      expression.append(" and ");
      buildCodeableConceptExpression(ed, expression, discriminator, cc);
    } else if (pattern instanceof Identifier) {
    	Identifier ii = (Identifier) pattern;
    	expression.append(" and ");
    	buildIdentifierExpression(ed, expression, discriminator, ii);
    } else
      throw new DefinitionException("Unsupported fixed pattern type for discriminator(" + discriminator + ") for slice " + ed.getId() + ": " + pattern.getClass().getName());
  }
  
  private void buildIdentifierExpression(ElementDefinition ed, StringBuilder expression, String discriminator, Identifier ii)
      throws DefinitionException {
    if (ii.hasExtension())
      throw new DefinitionException("Unsupported Identifier pattern - extensions are not allowed - for discriminator(" + discriminator + ") for slice " + ed.getId());
    boolean first = true;
    expression.append(discriminator + ".where(");
    if (ii.hasSystem()) {
      first = false;
      expression.append("system = '"+ii.getSystem()+"'");
    }
    if (ii.hasValue()) {
      if (first) 
        first = false; 
      else 
        expression.append(" and ");
      expression.append("value = '"+ii.getValue()+"'");
    }
    if (ii.hasUse()) {
      if (first) 
        first = false; 
       else 
         expression.append(" and ");
      expression.append("use = '"+ii.getUse()+"'");
    }
    if (ii.hasType()) {
      if (first) 
        first = false;
      else 
        expression.append(" and ");
      buildCodeableConceptExpression(ed, expression, "type", ii.getType());
    }
    expression.append(").exists()");
  }

  private void buildCodeableConceptExpression(ElementDefinition ed, StringBuilder expression, String discriminator, CodeableConcept cc)
      throws DefinitionException {
    if (cc.hasText())
      throw new DefinitionException("Unsupported CodeableConcept pattern - using text - for discriminator(" + discriminator + ") for slice " + ed.getId());
    if (!cc.hasCoding())
      throw new DefinitionException("Unsupported CodeableConcept pattern - must have at least one coding - for discriminator(" + discriminator + ") for slice " + ed.getId());
    if (cc.hasExtension())
      throw new DefinitionException("Unsupported CodeableConcept pattern - extensions are not allowed - for discriminator(" + discriminator + ") for slice " + ed.getId());
    boolean firstCoding = true;
    for(Coding c : cc.getCoding()) {
      if (c.hasExtension())
        throw new DefinitionException("Unsupported CodeableConcept pattern - extensions are not allowed - for discriminator(" + discriminator + ") for slice " + ed.getId());
      if (firstCoding) firstCoding = false; else expression.append(" and ");
      expression.append(discriminator + ".coding.where(");
      boolean first = true;
      if (c.hasSystem()) {
        first = false;
        expression.append("system = '"+c.getSystem()+"'");
      }
      if (c.hasVersion()) {
        if (first) first = false; else expression.append(" and ");
        expression.append("version = '"+c.getVersion()+"'");
      }
      if (c.hasCode()) {
        if (first) first = false; else expression.append(" and ");
        expression.append("code = '"+c.getCode()+"'");
      }
      if (c.hasDisplay()) {
        if (first) first = false; else expression.append(" and ");
        expression.append("display = '"+c.getDisplay()+"'");
      }
      expression.append(").exists()");
    }
  }

  private void buildFixedExpression(ElementDefinition ed, StringBuilder expression, String discriminator, ElementDefinition criteriaElement) throws DefinitionException {
    Type fixed = criteriaElement.getFixed();
    if (fixed instanceof CodeableConcept) {
      CodeableConcept cc = (CodeableConcept) fixed;
      expression.append(" and ");
      buildCodeableConceptExpression(ed, expression, discriminator, cc);
    } else if (fixed instanceof Identifier) {
      Identifier ii = (Identifier) fixed;
      expression.append(" and ");
      buildIdentifierExpression(ed, expression, discriminator, ii);
    } else {
      expression.append(" and (");
      if (fixed instanceof StringType) {
        Gson gson = new Gson();
        String json = gson.toJson((StringType)fixed);
        String escapedString = json.substring(json.indexOf(":")+2);
        escapedString = escapedString.substring(0, escapedString.indexOf(",\"myStringValue")-1);
        expression.append("'" + escapedString + "'");
      } else if (fixed instanceof UriType) {
        expression.append("'" + ((UriType)fixed).asStringValue() + "'");
      } else if (fixed instanceof IntegerType) {
        expression.append(((IntegerType)fixed).asStringValue());
      } else if (fixed instanceof DecimalType) {
        expression.append(((IntegerType)fixed).asStringValue());
      } else if (fixed instanceof BooleanType) {
        expression.append(((BooleanType)fixed).asStringValue());
      } else
        throw new DefinitionException("Unsupported fixed value type for discriminator(" + discriminator + ") for slice " + ed.getId() + ": " + fixed.getClass().getName());
      expression.append(" in " + discriminator +")");
    }
  }

  // we assume that the following things are true:
  // the instance at root is valid against the schema and schematron
  // the instance validator had no issues against the base resource profile
  private void start(ValidatorHostContext hostContext, List<ValidationMessage> errors, Element resource, Element element, StructureDefinition defn, NodeStack stack) throws FHIRException, FHIRException, IOException {
    checkLang(resource, stack);
    
    // profile is valid, and matches the resource name
    ResourceProfiles resourceProfiles = getResourceProfiles(element, stack);
    if (!resourceProfiles.isProcessed())
      checkDeclaredProfiles(resourceProfiles, errors, resource, element, stack);

    if (!resourceProfiles.isProcessed()) {
      resourceProfiles.setProcessed();
      if (!resourceProfiles.hasProfiles() &&
          (rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), defn.hasSnapshot(),
              "StructureDefinition has no snapshot - validation is against the snapshot, so it must be provided"))) {
        // Don't need to validate against the resource if there's a profile because the profile snapshot will include the relevant parts of the resources
        validateElement(hostContext, errors, defn, defn.getSnapshot().getElement().get(0), null, null, resource, element, element.getName(), stack, false, true, null);
      }

      // specific known special validations
      if (element.getType().equals("Bundle"))
        validateBundle(errors, element, stack);
      else if (element.getType().equals("Observation"))
        validateObservation(errors, element, stack);
      else if (element.getType().equals("Questionnaire"))
        validateQuestionannaire(errors, element, stack);
      else if (element.getType().equals("QuestionnaireResponse"))
        validateQuestionannaireResponse(hostContext, errors, element, stack);
      else if (element.getType().equals("CodeSystem"))
        validateCodeSystem(errors, element, stack);
      validateResourceRules(errors, element, stack);
    }
    for (ProfileUsage profileUsage : resourceProfiles.uncheckedProfiles()) {
      profileUsage.setChecked();
// todo: re-enable this when I can deal with the impact...   (GG)
//      if (!profileUsage.getProfile().getType().equals(resource.fhirType()))
//        throw new FHIRException("Profile type mismatch - resource is "+resource.fhirType()+", and profile is for "+profileUsage.getProfile().getType());
      validateElement(hostContext, errors, profileUsage.getProfile(), profileUsage.getProfile().getSnapshot().getElement().get(0), null, null, resource, element, element.getName(), stack, false, true, null);
    }
  }

  private void validateQuestionannaire(List<ValidationMessage> errors, Element element, NodeStack stack) {
    List<Element> list = getItems(element);
    for (int i = 0; i < list.size(); i++) {
      Element e = list.get(i);
      NodeStack ns = stack.push(element, i, e.getProperty().getDefinition(), e.getProperty().getDefinition());
      validateQuestionnaireElement(errors, ns, element, e, new ArrayList<>());
    }
    
  }

  private void validateQuestionnaireElement(List<ValidationMessage> errors, NodeStack ns, Element questionnaire, Element item, List<Element> parents) {
    // R4+
    if (FHIRVersion.isR4Plus(context.getVersion())) {
      if (item.hasChild("enableWhen")) {
        Element ew = item.getNamedChild("enableWhen");
        String ql = ew.getNamedChildValue("question");
        if (rule(errors, IssueType.BUSINESSRULE, ns.literalPath, ql != null, "Questions with an enableWhen must have a value for the question link")) {
          Element tgt = getQuestionById(item, ql);
          if (rule(errors, IssueType.BUSINESSRULE, ns.literalPath, tgt == null, "Questions with an enableWhen cannot refer to an inner question for it's enableWhen condition")) {
            tgt = getQuestionById(questionnaire, ql);
            if (rule(errors, IssueType.BUSINESSRULE, ns.literalPath, tgt != null, "Unable to find "+ql+" target for this question enableWhen")) {
              if (rule(errors, IssueType.BUSINESSRULE, ns.literalPath, tgt != item, "Target for this question enableWhen can't reference itself")) {
                warning(errors, IssueType.BUSINESSRULE, ns.literalPath, isBefore(item, tgt, parents), "The target of this enableWhen rule ("+ql+") comes after the question itself");
              }
            }
          }  
        }
      }
    }
  }

  private boolean isBefore(Element item, Element tgt, List<Element> parents) {
    // we work up the list, looking for tgt in the children of the parents 
    for (Element p : parents) {
      int i = findIndex(p, item);
      int t = findIndex(p, tgt);
      if (i > -1 && t > -1) {
        return i > t;
      }
    }
    return false; // unsure... shouldn't ever get to this point;
  }
  

  private int findIndex(Element parent, Element descendant) {
    for (int i = 0; i < parent.getChildren().size(); i++) {
      if (parent.getChildren().get(i) == descendant || isChild(parent.getChildren().get(i), descendant))
        return i;
    }
    return -1;
  }

  private boolean isChild(Element element, Element descendant) {
    for (Element e : element.getChildren()) {
      if (e == descendant)
        return true;
      if (isChild(element, descendant))
        return true;
    }
    return false;
  }

  private Element getQuestionById(Element focus, String ql) {
    List<Element> list = getItems(focus);
    for (Element item : list) {
      String v = item.getNamedChildValue("linkId");
      if (ql.equals(v))
        return item;
      Element tgt = getQuestionById(item, ql);
      if (tgt != null)
        return tgt;
    }
    return null;
    
  }

  private List<Element> getItems(Element element) {
    List<Element> list = new ArrayList<>();
    element.getNamedChildren("item", list);
    return list;
  }

  private void checkLang(Element resource, NodeStack stack) {
    String lang = resource.getNamedChildValue("language");
    if (!Utilities.noString(lang))
      stack.workingLang = lang;
  }

  private void validateResourceRules(List<ValidationMessage> errors, Element element, NodeStack stack) {
    String lang = element.getNamedChildValue("language");
    Element text = element.getNamedChild("text");
    if (text != null) {
      Element div = text.getNamedChild("div");
      if (lang != null && div != null) {
        XhtmlNode xhtml = div.getXhtml();
        String xl = xhtml.getAttribute("lang");
        if (xl == null) {
          warning(errors, IssueType.BUSINESSRULE, div.line(), div.col(), stack.getLiteralPath(), false, "Resource has a language, but the XHTML does not have a language tag");           
        } else if (!xl.equals(lang)) {
          warning(errors, IssueType.BUSINESSRULE, div.line(), div.col(), stack.getLiteralPath(), false, "Resource has a language ("+lang+"), and the XHTML has a language ("+xl+"), but they differ ");           
        }
      }
    }
    // security tags are a set (system|code)
    Element meta = element.getNamedChild("meta");
    if (meta != null) {
      Set<String> tags = new HashSet<>();
      List<Element> list = new ArrayList<>();
      meta.getNamedChildren("security", list);
      int i = 0;
      for (Element e : list) {
        String s = e.getNamedChildValue("system") + "#" + e.getNamedChildValue("code");
        rule(errors, IssueType.BUSINESSRULE, e.line(), e.col(), stack.getLiteralPath()+".meta.profile["+Integer.toString(i)+"]", !tags.contains(s), "Duplicate Security Label "+s);
        tags.add(s);
        i++;
      }
    }
  }

  private void validateCodeSystem(List<ValidationMessage> errors, Element cs, NodeStack stack) {
    String url = cs.getNamedChildValue("url");
    String vsu = cs.getNamedChildValue("valueSet");
    if (!Utilities.noString(vsu)) {
      ValueSet vs;
      try {
        vs = context.fetchResourceWithException(ValueSet.class, vsu);
      } catch (FHIRException e) {
        vs = null;
      }
      if (vs != null) {
        if (rule(errors, IssueType.BUSINESSRULE, stack.getLiteralPath(), vs.hasCompose() && !vs.hasExpansion(), "CodeSystem "+url+" has a 'all system' value set of "+vsu+", but it is an expansion"))
          if (rule(errors, IssueType.BUSINESSRULE, stack.getLiteralPath(), vs.getCompose().getInclude().size() == 1, "CodeSystem "+url+" has a 'all system' value set of "+vsu+", but doesn't have a single include"))
            if (rule(errors, IssueType.BUSINESSRULE, stack.getLiteralPath(), vs.getCompose().getInclude().get(0).getSystem().equals(url), "CodeSystem "+url+" has a 'all system' value set of "+vsu+", but doesn't have a matching system ("+vs.getCompose().getInclude().get(0).getSystem()+")")) {
              rule(errors, IssueType.BUSINESSRULE, stack.getLiteralPath(), !vs.getCompose().getInclude().get(0).hasValueSet()
                   && !vs.getCompose().getInclude().get(0).hasConcept() && !vs.getCompose().getInclude().get(0).hasFilter(), "CodeSystem "+url+" has a 'all system' value set of "+vsu+", but the include has extra details");
            }
      } 
    } // todo... try getting the value set the other way...
  }

  private void validateQuestionannaireResponse(ValidatorHostContext hostContext, List<ValidationMessage> errors, Element element, NodeStack stack) throws FHIRException, IOException {
    Element q = element.getNamedChild("questionnaire");
    String questionnaire = null;
    if (q != null) {
    	/*
    	 * q.getValue() is correct for R4 content, but we'll also accept the second
    	 * option just in case we're validating raw STU3 content. Being lenient here
    	 * isn't the end of the world since if someone is actually doing the reference
    	 * wrong in R4 content it'll get flagged elsewhere by the validator too
    	 */
    	if (isNotBlank(q.getValue())) {
    		questionnaire = q.getValue();
		} else if (isNotBlank(q.getChildValue("reference"))) {
    		questionnaire = q.getChildValue("reference");
		}
	 }
    if (hint(errors, IssueType.REQUIRED, element.line(), element.col(), stack.getLiteralPath(), questionnaire != null, "No questionnaire is identified, so no validation can be performed against the base questionnaire")) {
      long t = System.nanoTime();
      Questionnaire qsrc = questionnaire.startsWith("#") ? loadQuestionnaire(element, questionnaire.substring(1)) : context.fetchResource(Questionnaire.class, questionnaire);
      sdTime = sdTime + (System.nanoTime() - t);
      if (warning(errors, IssueType.REQUIRED, q.line(), q.col(), stack.getLiteralPath(), qsrc != null, "The questionnaire \""+questionnaire+"\" could not be resolved, so no validation can be performed against the base questionnaire")) {
        boolean inProgress = "in-progress".equals(element.getNamedChildValue("status"));
        validateQuestionannaireResponseItems(hostContext, qsrc, qsrc.getItem(), errors, element, stack, inProgress, element, new QStack(qsrc, element));
      }
    }
  }

  private Questionnaire loadQuestionnaire(Element resource, String id) throws FHIRException, IOException {
    for (Element contained : resource.getChildren("contained")) {
      if (contained.getIdBase().equals(id)) {
        FhirPublication v = FhirPublication.fromCode(context.getVersion());
        ByteArrayOutputStream bs = new  ByteArrayOutputStream();
        new JsonParser(context).compose(contained, bs, OutputStyle.NORMAL, id);
        byte[] json = bs.toByteArray();         
        switch (v) {
        case DSTU1: throw new FHIRException("Unsupported version R1");
        case DSTU2:
          org.hl7.fhir.dstu2.model.Resource r2 = new org.hl7.fhir.dstu2.formats.JsonParser().parse(json);
          Resource r5 = new VersionConvertor_10_50(null).convertResource(r2);
          if (r5 instanceof Questionnaire)
            return (Questionnaire) r5;
          else 
            return null;
        case DSTU2016May: 
          org.hl7.fhir.dstu2016may.model.Resource r2a = new org.hl7.fhir.dstu2016may.formats.JsonParser().parse(json);
          r5 = VersionConvertor_14_50.convertResource(r2a);
          if (r5 instanceof Questionnaire)
            return (Questionnaire) r5;
          else 
            return null;
        case STU3:
          org.hl7.fhir.dstu3.model.Resource r3 = new org.hl7.fhir.dstu3.formats.JsonParser().parse(json);
          r5 = VersionConvertor_30_50.convertResource(r3, false);
          if (r5 instanceof Questionnaire)
            return (Questionnaire) r5;
          else 
            return null;
        case R4:
          org.hl7.fhir.r4.model.Resource r4 = new org.hl7.fhir.r4.formats.JsonParser().parse(json);
          r5 = VersionConvertor_40_50.convertResource(r4);
          if (r5 instanceof Questionnaire)
            return (Questionnaire) r5;
          else 
            return null;
        case R5:
          r5 = new org.hl7.fhir.r5.formats.JsonParser().parse(json);
          if (r5 instanceof Questionnaire)
            return (Questionnaire) r5;
          else 
            return null;
        }
      }
    }
    return null;
  }

  private void validateQuestionnaireResponseItem(ValidatorHostContext hostContext, Questionnaire qsrc, QuestionnaireItemComponent qItem, List<ValidationMessage> errors, Element element, NodeStack stack, boolean inProgress, Element questionnaireResponseRoot, QStack qstack) {
    String text = element.getNamedChildValue("text");
    rule(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), Utilities.noString(text) || text.equals(qItem.getText()), "If text exists, it must match the questionnaire definition for linkId "+qItem.getLinkId());

    List<Element> answers = new ArrayList<Element>();
    element.getNamedChildren("answer", answers);
    if (inProgress)
      warning(errors, IssueType.REQUIRED, element.line(), element.col(), stack.getLiteralPath(), isAnswerRequirementFulfilled(qItem, answers), "No response answer found for required item "+qItem.getLinkId());
    else if (myEnableWhenEvaluator.isQuestionEnabled(hostContext, qItem, qstack, fpe)) {
       rule(errors, IssueType.REQUIRED, element.line(), element.col(), stack.getLiteralPath(), isAnswerRequirementFulfilled(qItem, answers), "No response answer found for required item "+qItem.getLinkId());
    } else if (!answers.isEmpty()) { // items without answers should be allowed, but not items with answers to questions that are disabled
      // it appears that this is always a duplicate error - it will always already have beeb reported, so no need to report it again?
      // GDG 2019-07-13
//      rule(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), !isAnswerRequirementFulfilled(qItem, answers), "Item has answer (2), even though it is not enabled "+qItem.getLinkId());
    }

    if (answers.size() > 1)
      rule(errors, IssueType.INVALID, answers.get(1).line(), answers.get(1).col(), stack.getLiteralPath(), qItem.getRepeats(), "Only one response answer item with this linkId allowed");

    for (Element answer : answers) {
      NodeStack ns = stack.push(answer, -1, null, null);
      if (qItem.getType() != null) {
        switch (qItem.getType()) {
          case GROUP:
            rule(errors, IssueType.STRUCTURE, answer.line(), answer.col(), stack.getLiteralPath(), false, "Items of type group should not have answers");
            break;
          case DISPLAY:  // nothing
            break;
          case BOOLEAN:
            validateQuestionnaireResponseItemType(errors, answer, ns, "boolean");
            break;
          case DECIMAL:
            validateQuestionnaireResponseItemType(errors, answer, ns, "decimal");
            break;
          case INTEGER:
            validateQuestionnaireResponseItemType(errors, answer, ns, "integer");
            break;
          case DATE:
            validateQuestionnaireResponseItemType(errors, answer, ns, "date");
            break;
          case DATETIME:
            validateQuestionnaireResponseItemType(errors, answer, ns, "dateTime");
            break;
          case TIME:
            validateQuestionnaireResponseItemType(errors, answer, ns, "time");
            break;
          case STRING:
            validateQuestionnaireResponseItemType(errors, answer, ns, "string");
            break;
          case TEXT:
            validateQuestionnaireResponseItemType(errors, answer, ns, "text");
            break;
          case URL:
            validateQuestionnaireResponseItemType(errors, answer, ns, "uri");
            break;
          case ATTACHMENT:
            validateQuestionnaireResponseItemType(errors, answer, ns, "Attachment");
            break;
          case REFERENCE:
            validateQuestionnaireResponseItemType(errors, answer, ns, "Reference");
            break;
          case QUANTITY:
            if ("Quantity".equals(validateQuestionnaireResponseItemType(errors, answer, ns, "Quantity")))
              if (qItem.hasExtension("???"))
                validateQuestionnaireResponseItemQuantity(errors, answer, ns);
            break;
          case CHOICE:
            String itemType = validateQuestionnaireResponseItemType(errors, answer, ns, "Coding", "date", "time", "integer", "string");
            if (itemType != null) {
              if (itemType.equals("Coding")) validateAnswerCode(errors, answer, ns, qsrc, qItem, false);
              else if (itemType.equals("date")) checkOption(errors, answer, ns, qsrc, qItem, "date");
              else if (itemType.equals("time")) checkOption(errors, answer, ns, qsrc, qItem, "time");
              else if (itemType.equals("integer")) checkOption(errors, answer, ns, qsrc, qItem, "integer");
              else if (itemType.equals("string")) checkOption(errors, answer, ns, qsrc, qItem, "string");
            }
            break;
          case OPENCHOICE:
            itemType = validateQuestionnaireResponseItemType(errors, answer, ns, "Coding", "date", "time", "integer", "string");
            if (itemType != null) {
              if (itemType.equals("Coding")) validateAnswerCode(errors, answer, ns, qsrc, qItem, true);
              else if (itemType.equals("date")) checkOption(errors, answer, ns, qsrc, qItem, "date");
              else if (itemType.equals("time")) checkOption(errors, answer, ns, qsrc, qItem, "time");
              else if (itemType.equals("integer")) checkOption(errors, answer, ns, qsrc, qItem, "integer");
              else if (itemType.equals("string")) checkOption(errors, answer, ns, qsrc, qItem, "string", true);
            }
            break;
          case QUESTION:
          case NULL:
            // no validation
            break;
        }
      }
      validateQuestionannaireResponseItems(hostContext, qsrc, qItem.getItem(), errors, answer, stack, inProgress, questionnaireResponseRoot, qstack);
    }
    if (qItem.getType() == null) {
      fail(errors, IssueType.REQUIRED, element.line(), element.col(), stack.getLiteralPath(), false, "Definition for item "+qItem.getLinkId() + " does not contain a type");
    } else if (qItem.getType() == QuestionnaireItemType.DISPLAY) {
      List<Element> items = new ArrayList<Element>();
      element.getNamedChildren("item", items);
      rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), items.isEmpty(), "Items not of type DISPLAY should not have items - linkId {0}", qItem.getLinkId());
    } else {
      validateQuestionannaireResponseItems(hostContext, qsrc, qItem.getItem(), errors, element, stack, inProgress, questionnaireResponseRoot, qstack);
    }
  }

private boolean isAnswerRequirementFulfilled(QuestionnaireItemComponent qItem, List<Element> answers) {
	return !answers.isEmpty() || !qItem.getRequired() || qItem.getType() == QuestionnaireItemType.GROUP;
}

  private void validateQuestionnaireResponseItem(ValidatorHostContext hostcontext, Questionnaire qsrc, QuestionnaireItemComponent qItem, List<ValidationMessage> errors, List<Element> elements, NodeStack stack, boolean inProgress, Element questionnaireResponseRoot, QStack qstack) {
    if (elements.size() > 1)
      rule(errors, IssueType.INVALID, elements.get(1).line(), elements.get(1).col(), stack.getLiteralPath(), qItem.getRepeats(), "Only one response item with this linkId allowed - " + qItem.getLinkId());
    int i = 0;
    for (Element element : elements) {
      NodeStack ns = stack.push(element, i, null, null);
      validateQuestionnaireResponseItem(hostcontext, qsrc, qItem, errors, element, ns, inProgress, questionnaireResponseRoot, qstack.push(qItem, element));
      i++;
    }
  }

  private int getLinkIdIndex(List<QuestionnaireItemComponent> qItems, String linkId) {
    for (int i = 0; i < qItems.size(); i++) {
      if (linkId.equals(qItems.get(i).getLinkId()))
        return i;
    }
    return -1;
  }
  
  private void validateQuestionannaireResponseItems(ValidatorHostContext hostContext, Questionnaire qsrc, List<QuestionnaireItemComponent> qItems, List<ValidationMessage> errors, Element element, NodeStack stack, boolean inProgress, Element questionnaireResponseRoot, QStack qstack) {
    List<Element> items = new ArrayList<Element>();
    element.getNamedChildren("item", items);
    // now, sort into stacks
    Map<String, List<Element>> map = new HashMap<String, List<Element>>();
    int lastIndex = -1;
    for (Element item : items) {
      String linkId = item.getNamedChildValue("linkId");
      if (rule(errors, IssueType.REQUIRED, item.line(), item.col(), stack.getLiteralPath(), !Utilities.noString(linkId), "No LinkId, so can't be validated")) {
        int index = getLinkIdIndex(qItems, linkId);
        if (index == -1) {
          QuestionnaireItemComponent qItem = findQuestionnaireItem(qsrc, linkId);
          if (qItem != null) {
            rule(errors, IssueType.STRUCTURE, item.line(), item.col(), stack.getLiteralPath(), index > -1, misplacedItemError(qItem));
            NodeStack ns = stack.push(item, -1, null, null);
            validateQuestionnaireResponseItem(hostContext, qsrc, qItem, errors, item, ns, inProgress, questionnaireResponseRoot, qstack.push(qItem, item));
          }
          else
            rule(errors, IssueType.NOTFOUND, item.line(), item.col(), stack.getLiteralPath(), index > -1, "LinkId \""+linkId+"\" not found in questionnaire");
        }
        else
        {
          rule(errors, IssueType.STRUCTURE, item.line(), item.col(), stack.getLiteralPath(), index >= lastIndex, "Structural Error: items are out of order");
          lastIndex = index;

          // If an item has a child called "linkId" but no child called "answer",
          // we'll treat it as not existing for the purposes of enableWhen validation
          if (item.hasChildren("answer") || item.hasChildren("item")) {
            List<Element> mapItem = map.computeIfAbsent(linkId, key -> new ArrayList<>());
            mapItem.add(item);
          }
        }
      }
    }

    // ok, now we have a list of known items, grouped by linkId. We've made an error for anything out of order
    for (QuestionnaireItemComponent qItem : qItems) {
      List<Element> mapItem = map.get(qItem.getLinkId());
      validateQuestionnaireResponseItem(hostContext, qsrc, errors, element, stack, inProgress, questionnaireResponseRoot, qItem, mapItem, qstack);
    }
  }

  public void validateQuestionnaireResponseItem(ValidatorHostContext hostContext, Questionnaire qsrc, List<ValidationMessage> errors, Element element, NodeStack stack, boolean inProgress, Element questionnaireResponseRoot, QuestionnaireItemComponent qItem, List<Element> mapItem, QStack qstack) {
    boolean enabled = myEnableWhenEvaluator.isQuestionEnabled(hostContext, qItem, qstack, fpe);
    if (mapItem != null){
      if (!enabled) {
        int i = 0;
        for (Element e : mapItem) {
          NodeStack ns = stack.push(e, i, e.getProperty().getDefinition(), e.getProperty().getDefinition());
          rule(errors, IssueType.INVALID, e.line(), e.col(), ns.getLiteralPath(), enabled, "Item has answer, even though it is not enabled (item id = '"+qItem.getLinkId()+"')");
          i++;
        }
      }

      // Recursively validate child items
      validateQuestionnaireResponseItem(hostContext, qsrc, qItem, errors, mapItem, stack, inProgress, questionnaireResponseRoot, qstack);

    } else {

      // item is missing, is the question enabled?
      if (enabled && qItem.getRequired()) {
        String message = "No response found for required item with id = '" + qItem.getLinkId() + "'";
        if (inProgress) {
          warning(errors, IssueType.REQUIRED, element.line(), element.col(), stack.getLiteralPath(), false, message);
        } else {
          rule(errors, IssueType.REQUIRED, element.line(), element.col(), stack.getLiteralPath(), false, message);
        }
      }

    }

  }

  private String misplacedItemError(QuestionnaireItemComponent qItem) {
  	return qItem.hasLinkId() ? String.format("Structural Error: item with linkid %s is in the wrong place", qItem.getLinkId()) : "Structural Error: item is in the wrong place";
  }

  private void validateQuestionnaireResponseItemQuantity( List<ValidationMessage> errors, Element answer, NodeStack stack)	{

  }

  private String validateQuestionnaireResponseItemType(List<ValidationMessage> errors, Element element, NodeStack stack, String... types) {
    List<Element> values = new ArrayList<Element>();
    element.getNamedChildrenWithWildcard("value[x]", values);
    for (int i = 0; i < types.length; i++) {
      if (types[i].equals("text")) {
        types[i] = "string";
      }
    }
    if (values.size() > 0) {
      NodeStack ns = stack.push(values.get(0), -1, null, null);
      CommaSeparatedStringBuilder l = new CommaSeparatedStringBuilder();
      for (String s : types)  {
        l.append(s);
        if (values.get(0).getName().equals("value"+Utilities.capitalize(s)))
          return(s);
      }
      if (types.length == 1)
        rule(errors, IssueType.STRUCTURE, values.get(0).line(), values.get(0).col(), ns.getLiteralPath(), false, "Answer value must be of type "+types[0]);
      else
        rule(errors, IssueType.STRUCTURE, values.get(0).line(), values.get(0).col(), ns.getLiteralPath(), false, "Answer value must be one of the types "+l.toString());
    }
    return null;
  }

  private QuestionnaireItemComponent findQuestionnaireItem(Questionnaire qSrc, String linkId) {
    return findItem(qSrc.getItem(), linkId);
  }

  private QuestionnaireItemComponent findItem(List<QuestionnaireItemComponent> list, String linkId) {
    for (QuestionnaireItemComponent item : list) {
      if (linkId.equals(item.getLinkId()))
        return item;
      QuestionnaireItemComponent result = findItem(item.getItem(), linkId);
      if (result != null)
        return result;
    }
    return null;
  }

  /*	private void validateAnswerCode(List<ValidationMessage> errors, Element value, NodeStack stack, List<Coding> optionList) {
	  String system = value.getNamedChildValue("system");
	  String code = value.getNamedChildValue("code");
	  boolean found = false;
	  for (Coding c : optionList) {
      if (ObjectUtil.equals(c.getSystem(), system) && ObjectUtil.equals(c.getCode(), code)) {
	      found = true;
	      break;
	    }
	  }
	  rule(errors, IssueType.STRUCTURE, value.line(), value.col(), stack.getLiteralPath(), found, "The code "+system+"::"+code+" is not a valid option");
	}*/

  private void validateAnswerCode(List<ValidationMessage> errors, Element value, NodeStack stack, Questionnaire qSrc, String ref, boolean theOpenChoice) {
    ValueSet vs = resolveBindingReference(qSrc, ref, qSrc.getUrl());
    if (warning(errors, IssueType.CODEINVALID, value.line(), value.col(), stack.getLiteralPath(), vs != null, "ValueSet " + describeReference(ref) + " not found by validator"))  {
      try {
        Coding c = ObjectConverter.readAsCoding(value);
        if (isBlank(c.getCode()) && isBlank(c.getSystem()) && isNotBlank(c.getDisplay())) {
          if (theOpenChoice) {
            return;
          }
        }

        long t = System.nanoTime();
        ValidationResult res = context.validateCode(new ValidationOptions(stack.workingLang), c, vs);
        txTime = txTime + (System.nanoTime() - t);
        if (!res.isOk()) {
			  txRule(errors, res.getTxLink(), IssueType.CODEINVALID, value.line(), value.col(), stack.getLiteralPath(), false, "The value provided (" + c.getSystem() + "::" + c.getCode() + ") is not in the options value set in the questionnaire");
		  } else if (res.getSeverity() != null) {
        	  super.addValidationMessage(errors, IssueType.CODEINVALID, value.line(), value.col(), stack.getLiteralPath(), res.getMessage(), res.getSeverity(), Source.TerminologyEngine);
		  }
      } catch (Exception e) {
        warning(errors, IssueType.CODEINVALID, value.line(), value.col(), stack.getLiteralPath(), false, "Error " + e.getMessage() + " validating Coding against Questionnaire Options");
      }
    }
  }

  private void validateAnswerCode( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, boolean theOpenChoice) {
    Element v = answer.getNamedChild("valueCoding");
    NodeStack ns = stack.push(v, -1, null, null);
    if (qItem.getAnswerOption().size() > 0)
      checkCodingOption(errors, answer, stack, qSrc, qItem, theOpenChoice);
    //	    validateAnswerCode(errors, v, stack, qItem.getOption());
    else if (qItem.hasAnswerValueSet())
      validateAnswerCode(errors, v, stack, qSrc, qItem.getAnswerValueSet(), theOpenChoice);
    else
      hint(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Cannot validate options because no option or options are provided");
  }

  private void checkOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, String type) {
    checkOption(errors, answer, stack, qSrc,  qItem, type, false);
  }

  private void checkOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, String type, boolean openChoice) {
    if (type.equals("integer"))     checkIntegerOption(errors, answer, stack, qSrc, qItem, openChoice);
    else if (type.equals("date"))   checkDateOption(errors, answer, stack, qSrc, qItem, openChoice);
    else if (type.equals("time"))   checkTimeOption(errors, answer, stack, qSrc, qItem, openChoice);
    else if (type.equals("string")) checkStringOption(errors, answer, stack, qSrc, qItem, openChoice);
    else if (type.equals("Coding")) checkCodingOption(errors, answer, stack, qSrc, qItem, openChoice);
  }

  private void checkIntegerOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, boolean openChoice) {
    Element v = answer.getNamedChild("valueInteger");
    NodeStack ns = stack.push(v, -1, null, null);
    if (qItem.getAnswerOption().size() > 0) {
      List<IntegerType> list = new ArrayList<IntegerType>();
      for (QuestionnaireItemAnswerOptionComponent components : qItem.getAnswerOption())  {
        try {
          list.add(components.getValueIntegerType());
        } catch (FHIRException e) {
          // If it's the wrong type, just keep going
        }
      }
      if (list.isEmpty() && !openChoice) {
        rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Option list has no option values of type integer");
      } else {
        boolean found = false;
        for (IntegerType item : list) {
          if (item.getValue() == Integer.parseInt(v.primitiveValue())) {
            found = true;
            break;
          }
        }
        if (!found) {
          rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), found, "The integer "+v.primitiveValue()+" is not a valid option");
        }
      }
    } else
      hint(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Cannot validate integer answer option because no option list is provided");
  }

  private void checkDateOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, boolean openChoice) {
    Element v = answer.getNamedChild("valueDate");
    NodeStack ns = stack.push(v, -1, null, null);
    if (qItem.getAnswerOption().size() > 0) {
      List<DateType> list = new ArrayList<DateType>();
      for (QuestionnaireItemAnswerOptionComponent components : qItem.getAnswerOption())  {
        try {
          list.add(components.getValueDateType());
        } catch (FHIRException e) {
          // If it's the wrong type, just keep going
        }
      }
      if (list.isEmpty() && !openChoice) {
        rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Option list has no option values of type date");
      } else {
        boolean found = false;
        for (DateType item : list) {
          if (item.getValue().equals(v.primitiveValue())) {
            found = true;
            break;
          }
        }
        if (!found) {
          rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), found, "The date "+v.primitiveValue()+" is not a valid option");
        }
      }
    } else
      hint(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Cannot validate date answer option because no option list is provided");
  }

  private void checkTimeOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, boolean openChoice) {
    Element v = answer.getNamedChild("valueTime");
    NodeStack ns = stack.push(v, -1, null, null);
    if (qItem.getAnswerOption().size() > 0) {
      List<TimeType> list = new ArrayList<TimeType>();
      for (QuestionnaireItemAnswerOptionComponent components : qItem.getAnswerOption())  {
        try {
          list.add(components.getValueTimeType());
        } catch (FHIRException e) {
          // If it's the wrong type, just keep going
        }
      }
      if (list.isEmpty() && !openChoice) {
        rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Option list has no option values of type time");
      } else {
        boolean found = false;
        for (TimeType item : list) {
          if (item.getValue().equals(v.primitiveValue())) {
            found = true;
            break;
          }
        }
        if (!found) {
          rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), found, "The time "+v.primitiveValue()+" is not a valid option");
        }
      }
    } else
      hint(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Cannot validate time answer option because no option list is provided");
  }

  private void checkStringOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, boolean openChoice) {
    Element v = answer.getNamedChild("valueString");
    NodeStack ns = stack.push(v, -1, null, null);
    if (qItem.getAnswerOption().size() > 0) {
      List<StringType> list = new ArrayList<StringType>();
      for (QuestionnaireItemAnswerOptionComponent components : qItem.getAnswerOption())  {
        try {
          if (components.getValue() != null) {
            list.add(components.getValueStringType());
          }
        } catch (FHIRException e) {
          // If it's the wrong type, just keep going
        }
      }
      if (!openChoice) {
        if (list.isEmpty()) {
          rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Option list has no option values of type string");
        } else {
          boolean found = false;
          for (StringType item : list) {
            if (item.getValue().equals((v.primitiveValue()))) {
              found = true;
              break;
            }
          }
          if (!found) {
            rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), found, "The string " + v.primitiveValue() + " is not a valid option");
          }
        }
      }
    } else {
      hint(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Cannot validate string answer option because no option list is provided");
    }
  }

  private void checkCodingOption( List<ValidationMessage> errors, Element answer, NodeStack stack, Questionnaire qSrc, QuestionnaireItemComponent qItem, boolean openChoice) {
    Element v = answer.getNamedChild("valueCoding");
    String system = v.getNamedChildValue("system");
    String code = v.getNamedChildValue("code");
    NodeStack ns = stack.push(v, -1, null, null);
    if (qItem.getAnswerOption().size() > 0) {
      List<Coding> list = new ArrayList<Coding>();
      for (QuestionnaireItemAnswerOptionComponent components : qItem.getAnswerOption())  {
        try {
          if (components.getValue() != null) {
            list.add(components.getValueCoding());
          }
        } catch (FHIRException e) {
          // If it's the wrong type, just keep going
        }
      }
      if (list.isEmpty() && !openChoice) {
        rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Option list has no option values of type coding");
      } else {
        boolean found = false;
        for (Coding item : list) {
          if (ObjectUtil.equals(item.getSystem(), system) && ObjectUtil.equals(item.getCode(), code)) {
            found = true;
            break;
          }
        }
        if (!found) {
          rule(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), found, "The code "+system+"::"+code+" is not a valid option");
        }
      }
    } else
      hint(errors, IssueType.STRUCTURE, v.line(), v.col(), stack.getLiteralPath(), false, "Cannot validate Coding option because no option list is provided");
  }

  private String tail(String path) {
    return path.substring(path.lastIndexOf(".") + 1);
  }

  private String tryParse(String ref)  {
    String[] parts = ref.split("\\/");
    switch (parts.length) {
    case 1:
      return null;
    case 2:
      return checkResourceType(parts[0]);
    default:
      if (parts[parts.length - 2].equals("_history"))
        return checkResourceType(parts[parts.length - 4]);
      else
        return checkResourceType(parts[parts.length - 2]);
    }
  }

  private boolean typesAreAllReference(List<TypeRefComponent> theType) {
    for (TypeRefComponent typeRefComponent : theType) {
      if (typeRefComponent.getCode().equals("Reference") == false) {
        return false;
      }
    }
    return true;
  }

  private void validateBundle(List<ValidationMessage> errors, Element bundle, NodeStack stack) {
    List<Element> entries = new ArrayList<Element>();
    bundle.getNamedChildren("entry", entries);
    String type = bundle.getNamedChildValue("type");
    type = StringUtils.defaultString(type);

    if (entries.size() == 0) {
      rule(errors, IssueType.INVALID, stack.getLiteralPath(), !(type.equals("document") || type.equals("message")), "Documents or Messages must contain at least one entry");
    } else {
      // Get the first entry, the MessageHeader
      Element firstEntry = entries.get(0);
      // Get the stack of the first entry
      NodeStack firstStack = stack.push(firstEntry, 1, null, null);

      String fullUrl = firstEntry.getNamedChildValue("fullUrl");

      if (type.equals("document")) {
        Element resource = firstEntry.getNamedChild("resource");
        String id = resource.getNamedChildValue("id");
        if (rule(errors, IssueType.INVALID, firstEntry.line(), firstEntry.col(), stack.addToLiteralPath("entry", ":0"), resource != null, "No resource on first entry")) {
          validateDocument(errors, entries, resource, firstStack.push(resource, -1, null, null), fullUrl, id);
        }
        checkAllInterlinked(errors, entries, stack, bundle, true);
      }
      if (type.equals("message")) {
        Element resource = firstEntry.getNamedChild("resource");
        String id = resource.getNamedChildValue("id");
        if (rule(errors, IssueType.INVALID, firstEntry.line(), firstEntry.col(), stack.addToLiteralPath("entry", ":0"), resource != null, "No resource on first entry")) {
          validateMessage(errors, entries, resource, firstStack.push(resource, -1, null, null), fullUrl, id);
        }
        checkAllInterlinked(errors, entries, stack, bundle, VersionUtilities.isR5Ver(context.getVersion()));
      }
      // We do not yet have rules requiring that the id and fullUrl match when dealing with messaging Bundles
      //      validateResourceIds(errors, entries, stack);
    }
    for (Element entry : entries) {
      String fullUrl = entry.getNamedChildValue("fullUrl");
      String url = getCanonicalURLForEntry(entry);
      String id = getIdForEntry(entry);
      if (url != null) {
        if (!(!url.equals(fullUrl) || (url.matches(uriRegexForVersion()) && url.endsWith("/"+id))) && !isV3orV2Url(url))
          rule(errors, IssueType.INVALID, entry.line(), entry.col(), stack.addToLiteralPath("entry", ":0"), false, "The canonical URL ("+url+") cannot match the fullUrl ("+fullUrl+") unless the resource id ("+id+") also matches");
        rule(errors, IssueType.INVALID, entry.line(), entry.col(), stack.addToLiteralPath("entry", ":0"), !url.equals(fullUrl) || serverBase == null || (url.equals(Utilities.pathURL(serverBase, entry.getNamedChild("resource").fhirType(), id))), "The canonical URL ("+url+") cannot match the fullUrl ("+fullUrl+") unless on the canonical server itself");
      }
    }
  }

  // hack for pre-UTG v2/v3
  private boolean isV3orV2Url(String url) {
    return url.startsWith("http://hl7.org/fhir/v3/") || url.startsWith("http://hl7.org/fhir/v2/");
  }

  public final static String URI_REGEX3 = "((http|https)://([A-Za-z0-9\\\\\\.\\:\\%\\$]*\\/)*)?(Account|ActivityDefinition|AllergyIntolerance|AdverseEvent|Appointment|AppointmentResponse|AuditEvent|Basic|Binary|BodySite|Bundle|CapabilityStatement|CarePlan|CareTeam|ChargeItem|Claim|ClaimResponse|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition|Composition|ConceptMap|Condition (aka Problem)|Consent|Contract|Coverage|DataElement|DetectedIssue|Device|DeviceComponent|DeviceMetric|DeviceRequest|DeviceUseStatement|DiagnosticReport|DocumentManifest|DocumentReference|EligibilityRequest|EligibilityResponse|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse|EpisodeOfCare|ExpansionProfile|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal|GraphDefinition|Group|GuidanceResponse|HealthcareService|ImagingManifest|ImagingStudy|Immunization|ImmunizationRecommendation|ImplementationGuide|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration|MedicationDispense|MedicationRequest|MedicationStatement|MessageDefinition|MessageHeader|NamingSystem|NutritionOrder|Observation|OperationDefinition|OperationOutcome|Organization|Parameters|Patient|PaymentNotice|PaymentReconciliation|Person|PlanDefinition|Practitioner|PractitionerRole|Procedure|ProcedureRequest|ProcessRequest|ProcessResponse|Provenance|Questionnaire|QuestionnaireResponse|ReferralRequest|RelatedPerson|RequestGroup|ResearchStudy|ResearchSubject|RiskAssessment|Schedule|SearchParameter|Sequence|ServiceDefinition|Slot|Specimen|StructureDefinition|StructureMap|Subscription|Substance|SupplyDelivery|SupplyRequest|Task|TestScript|TestReport|ValueSet|VisionPrescription)\\/[A-Za-z0-9\\-\\.]{1,64}(\\/_history\\/[A-Za-z0-9\\-\\.]{1,64})?";
  private static final String EXECUTED_CONSTRAINT_LIST = "validator.executed.invariant.list";
  private static final String EXECUTION_ID = "validator.execution.id";

  private String uriRegexForVersion() {
    if (VersionUtilities.isR3Ver(context.getVersion()))
      return URI_REGEX3;
    else
      return Constants.URI_REGEX;
  }

  private String getCanonicalURLForEntry(Element entry) {
    Element e = entry.getNamedChild("resource");
    if (e == null)
      return null;
    return e.getNamedChildValue("url");
  }

  private String getIdForEntry(Element entry) {
    Element e = entry.getNamedChild("resource");
    if (e == null)
      return null;
    return e.getNamedChildValue("id");
  }

  /**
   * Check each resource entry to ensure that the entry's fullURL includes the resource's id
   * value. Adds an ERROR ValidationMessge to errors List for a given entry if it references
   * a resource and fullURL does not include the resource's id.
   * @param errors List of ValidationMessage objects that new errors will be added to. 
   * @param entries List of entry Element objects to be checked.
   * @param stack Current NodeStack used to create path names in error detail messages.
   */
  private void validateResourceIds(List<ValidationMessage> errors, List<Element> entries, NodeStack stack) {
    // TODO: Need to handle _version
   int i = 1;
   for(Element entry : entries) {
     String fullUrl = entry.getNamedChildValue("fullUrl");
     Element resource = entry.getNamedChild("resource");
     String id = resource != null?resource.getNamedChildValue("id"):null;
     if (id != null && fullUrl != null) {
       String urlId = null;
       if (fullUrl.startsWith("https://") || fullUrl.startsWith("http://")) {
         urlId = fullUrl.substring(fullUrl.lastIndexOf('/')+1);
       } else if (fullUrl.startsWith("urn:uuid") || fullUrl.startsWith("urn:oid")) {
         urlId = fullUrl.substring(fullUrl.lastIndexOf(':')+1);
       }
       rule(errors, IssueType.INVALID, entry.line(), entry.col(), stack.addToLiteralPath("entry["+i+"]"), urlId.equals(id),
           "Resource ID does not match the ID in the entry full URL ('"+id+"' vs '"+fullUrl+"') ");
     }
     i++;
   }
  }

  public class EntrySummary {
    Element entry;
    Element resource;
    List<EntrySummary> targets = new ArrayList<>();
    public EntrySummary(Element entry, Element resource) {
      this.entry = entry;
      this.resource = resource;
    }

  }

  private void checkAllInterlinked(List<ValidationMessage> errors, List<Element> entries, NodeStack stack, Element bundle, boolean isError) {
    List<EntrySummary> entryList = new ArrayList<>();
    for (Element entry: entries) {
      Element r = entry.getNamedChild("resource");
      if (r != null) {
        entryList.add(new EntrySummary(entry, r));
      }
    }
    for (EntrySummary e : entryList) {
      Set<String> references = findReferences(e.entry);
      for (String ref : references) {
        Element tgt = resolveInBundle(entries, ref, e.entry.getChildValue("fullUrl"), e.resource.fhirType(), e.resource.getIdBase());
        if (tgt != null) {
          EntrySummary t = entryForTarget(entryList, tgt);
          if (t != null) {
            e.targets.add(t);
          }
        }
      }
    }

    Set<EntrySummary> visited = new HashSet<>();
    visitLinked(visited, entryList.get(0));
    boolean foundRevLinks;
    do {
      foundRevLinks = false;
      for (EntrySummary e : entryList) {
        if (!visited.contains(e)) {
          boolean add = false;
          for (EntrySummary t : e.targets) {
            if (visited.contains(t)) {
              add = true;
            }
          }
          if (add) {
            foundRevLinks = true;
            visitLinked(visited, e);              
          }
        }
      }
    } while (foundRevLinks);
    
    int i = 0;
    for (EntrySummary e : entryList) {
      Element entry = e.entry;
      if (isError) {
        rule(errors, IssueType.INFORMATIONAL, entry.line(), entry.col(), stack.addToLiteralPath("entry" + '[' + (i+1) + ']'), visited.contains(e), "Entry "+(entry.getChildValue("fullUrl") != null ? "'"+entry.getChildValue("fullUrl")+"'" : "")+" isn't reachable by traversing from first Bundle entry");
      } else {
        warning(errors, IssueType.INFORMATIONAL, entry.line(), entry.col(), stack.addToLiteralPath("entry" + '[' + (i+1) + ']'), visited.contains(e), "Entry "+(entry.getChildValue("fullUrl") != null ? "'"+entry.getChildValue("fullUrl")+"'" : "")+" isn't reachable by traversing from first Bundle entry");
      }
      i++;
    }
  }

  private EntrySummary entryForTarget(List<EntrySummary> entryList, Element tgt) {
    for (EntrySummary e : entryList) {
      if (e.entry == tgt) {
        return e;
      }
    }
    return null;
  }

  private void visitLinked(Set<EntrySummary> visited, EntrySummary t) {
    if (!visited.contains(t)) {
      visited.add(t);
      for (EntrySummary e : t.targets) {
        visitLinked(visited, e);
      }
    }
  }

  private void followResourceLinks(Element entry, Map<String, Element> visitedResources, Map<Element, Element> candidateEntries, List<Element> candidateResources, List<ValidationMessage> errors, NodeStack stack) {
    followResourceLinks(entry, visitedResources, candidateEntries, candidateResources, errors, stack, 0);
  }

  private void followResourceLinks(Element entry, Map<String, Element> visitedResources, Map<Element, Element> candidateEntries, List<Element> candidateResources, List<ValidationMessage> errors, NodeStack stack, int depth) {
    Element resource = entry.getNamedChild("resource");
    if (visitedResources.containsValue(resource))
      return;

    visitedResources.put(entry.getNamedChildValue("fullUrl"), resource);

    String type = null;
    Set<String> references = findReferences(resource);
    for (String reference: references) {
      // We don't want errors when just retrieving the element as they will be caught (with better path info) in subsequent processing
      Element r = getFromBundle(stack.getElement(), reference, entry.getChildValue("fullUrl"), new ArrayList<ValidationMessage>(), stack.addToLiteralPath("entry[" + candidateResources.indexOf(resource) + "]"), type);
      if (r!=null && !visitedResources.containsValue(r)) {
        followResourceLinks(candidateEntries.get(r), visitedResources, candidateEntries, candidateResources, errors, stack, depth+1);
      }
    }
  }

  private Set<String> findReferences(Element start) {
    Set<String> references = new HashSet<String>();
    findReferences(start, references);
    return references;
  }

  private void findReferences(Element start, Set<String> references) {
    for (Element child : start.getChildren()) {
      if (child.getType().equals("Reference")) {
        String ref = child.getChildValue("reference");
        if (ref != null && !ref.startsWith("#"))
          references.add(ref);
      }
      if (child.getType().equals("url") || child.getType().equals("uri") || child.getType().equals("canonical")) {
        String ref = child.primitiveValue();
        if (ref != null && !ref.startsWith("#"))
          references.add(ref);
      }
      findReferences(child, references);
    }
  }

  private void validateBundleReference(List<ValidationMessage> errors, List<Element> entries, Element ref, String name, NodeStack stack, String fullUrl, String type, String id) {
    String reference = null;
    try {
      reference = ref.getNamedChildValue("reference");
    } catch (Error e) {

    }

    if (ref != null && !Utilities.noString(reference)) {
      Element target = resolveInBundle(entries, reference, fullUrl, type, id);
      rule(errors, IssueType.INVALID, ref.line(), ref.col(), stack.addToLiteralPath("reference"), target != null, "Can't find '"+reference+"' in the bundle (" + name + ")");
    }
  }

  private void validateContains(ValidatorHostContext hostContext, List<ValidationMessage> errors, String path, ElementDefinition child, ElementDefinition context, Element resource, Element element, NodeStack stack, IdStatus idstatus) throws FHIRException, FHIRException, IOException {
    String resourceName = element.getType();
    long t = System.nanoTime();
    StructureDefinition profile = this.context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/" + resourceName);
    sdTime = sdTime + (System.nanoTime() - t);
    // special case: resource wrapper is reset if we're crossing a bundle boundary, but not otherwise
    ValidatorHostContext hc = null;
    if (element.getSpecial() == SpecialElement.BUNDLE_ENTRY || element.getSpecial() == SpecialElement.BUNDLE_OUTCOME || element.getSpecial() == SpecialElement.PARAMETER ) {
      resource = element;
      hc = hostContext.forEntry(element);
    } else {
      hc = hostContext.forContained(element);
    }
    if (rule(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), profile != null, "No profile found for contained resource of type '" + resourceName + "'"))
      validateResource(hc, errors, resource, element, profile, null, idstatus, stack, false);
  }

  private void validateDocument(List<ValidationMessage> errors, List<Element> entries, Element composition, NodeStack stack, String fullUrl, String id) {
    // first entry must be a composition
    if (rule(errors, IssueType.INVALID, composition.line(), composition.col(), stack.getLiteralPath(), composition.getType().equals("Composition"),
        "The first entry in a document must be a composition")) {
      
      // the composition subject etc references must resolve in the bundle
      validateDocumentReference(errors, entries, composition, stack, fullUrl, id, false, "subject", "Composition");
      validateDocumentReference(errors, entries, composition, stack, fullUrl, id, true, "author", "Composition");
      validateDocumentReference(errors, entries, composition, stack, fullUrl, id, false, "encounter", "Composition");
      validateDocumentReference(errors, entries, composition, stack, fullUrl, id, false, "custodian", "Composition");
      validateDocumentSubReference(errors, entries, composition, stack, fullUrl, id, "Composition", "attester", false, "party");
      validateDocumentSubReference(errors, entries, composition, stack, fullUrl, id, "Composition", "event", true, "detail");
      
      validateSections(errors, entries, composition, stack, fullUrl, id);
    }
  }

  public void validateDocumentSubReference(List<ValidationMessage> errors, List<Element> entries, Element composition, NodeStack stack, String fullUrl, String id, String title, String parent, boolean repeats, String propName) {
    List<Element> list = new ArrayList<>();
    composition.getNamedChildren(parent, list);
    int i = 1;
    for (Element elem : list) {
      validateDocumentReference(errors, entries, elem, stack.push(elem, i, null, null), fullUrl, id, repeats, propName, title+"."+parent);
      i++;
    }    
  }

  public void validateDocumentReference(List<ValidationMessage> errors, List<Element> entries, Element composition, NodeStack stack, String fullUrl, String id, boolean repeats, String propName, String title) {
    if (repeats) {
      List<Element> list = new ArrayList<>();
      composition.getNamedChildren(propName, list);
      int i = 1;
      for (Element elem : list) {
        validateBundleReference(errors, entries, elem, title+"."+propName, stack.push(elem, i, null, null), fullUrl, "Composition", id);
        i++;
      }
      
    } else {
      Element elem = composition.getNamedChild(propName);
      if (elem != null) {
        validateBundleReference(errors, entries, elem, title+"."+propName, stack.push(elem, -1, null, null), fullUrl, "Composition", id);
      }
    }
  }
  
  // rule(errors, IssueType.INVALID, bundle.line(), bundle.col(), "Bundle", !"urn:guid:".equals(base), "The base 'urn:guid:' is not valid (use urn:uuid:)");
  // rule(errors, IssueType.INVALID, entry.line(), entry.col(), localStack.getLiteralPath(), !"urn:guid:".equals(ebase), "The base 'urn:guid:' is not valid");
  // rule(errors, IssueType.INVALID, entry.line(), entry.col(), localStack.getLiteralPath(), !Utilities.noString(base) || !Utilities.noString(ebase), "entry
  // does not have a base");
  // String firstBase = null;
  // firstBase = ebase == null ? base : ebase;

  private void validateElement(ValidatorHostContext hostContext, List<ValidationMessage> errors, StructureDefinition profile, ElementDefinition definition, StructureDefinition cprofile, ElementDefinition context,
      Element resource, Element element, String actualType, NodeStack stack, boolean inCodeableConcept, boolean checkDisplayInContext, String extensionUrl) throws FHIRException, FHIRException, IOException {
    // element.markValidation(profile, definition);

    if (debug) {
    	System.out.println("  "+stack.getLiteralPath());
    }
    //		time = System.nanoTime();
    // check type invariants
    checkInvariants(hostContext, errors, profile, definition, resource, element, stack, false);
    if (definition.getFixed() != null)
      checkFixedValue(errors, stack.getLiteralPath(), element, definition.getFixed(), profile.getUrl(), definition.getSliceName(), null);


    // get the list of direct defined children, including slices
    List<ElementDefinition> childDefinitions = ProfileUtilities.getChildMap(profile, definition);
    if (childDefinitions.isEmpty()) {
      if (actualType == null)
        return; // there'll be an error elsewhere in this case, and we're going to stop.
      StructureDefinition dt = null;
      if (isAbsolute(actualType)) 
        dt = this.context.fetchResource(StructureDefinition.class, actualType);
      else
        dt = this.context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/" + actualType);
      if (dt == null)
        throw new DefinitionException("Unable to resolve actual type " + actualType);

      childDefinitions = ProfileUtilities.getChildMap(dt, dt.getSnapshot().getElement().get(0));
    }

    List<ElementInfo> children = listChildren(element, stack);
    List<String> problematicPaths = assignChildren(hostContext, errors, profile, resource, stack, childDefinitions, children);

    checkCardinalities(errors, profile, element, stack, childDefinitions, children, problematicPaths);
    // 4. check order if any slices are ordered. (todo)

    // 5. inspect each child for validity
    for (ElementInfo ei : children) {
      checkChild(hostContext, errors, profile, definition, resource, element, actualType, stack, inCodeableConcept, checkDisplayInContext, ei, extensionUrl);
    }
  }

  public void checkChild(ValidatorHostContext hostContext, List<ValidationMessage> errors, StructureDefinition profile, ElementDefinition definition,
      Element resource, Element element, String actualType, NodeStack stack, boolean inCodeableConcept, boolean checkDisplayInContext, ElementInfo ei, String extensionUrl)
      throws FHIRException, IOException, DefinitionException {
    List<String> profiles = new ArrayList<String>();
    if (ei.definition != null) {
      String type = null;
      ElementDefinition typeDefn = null;
      checkMustSupport(profile, ei);

      if (ei.definition.getType().size() == 1 && !"*".equals(ei.definition.getType().get(0).getWorkingCode()) && !"Element".equals(ei.definition.getType().get(0).getWorkingCode())
          && !"BackboneElement".equals(ei.definition.getType().get(0).getWorkingCode())) {
        type = ei.definition.getType().get(0).getWorkingCode();
        // Excluding reference is a kludge to get around versioning issues
        if (ei.definition.getType().get(0).hasProfile()) {
          for (CanonicalType p : ei.definition.getType().get(0).getProfile()) {
            profiles.add(p.getValue());
          }
        }
      } else if (ei.definition.getType().size() == 1 && "*".equals(ei.definition.getType().get(0).getWorkingCode())) {
        String prefix = tail(ei.definition.getPath());
        assert prefix.endsWith("[x]");
        type = ei.name.substring(prefix.length() - 3);
        if (isPrimitiveType(type))
          type = Utilities.uncapitalize(type);
        if (ei.definition.getType().get(0).hasProfile()) {
          for (CanonicalType p : ei.definition.getType().get(0).getProfile()) {
            profiles.add(p.getValue());
          }
        }
      } else if (ei.definition.getType().size() > 1) {

        String prefix = tail(ei.definition.getPath());
        assert typesAreAllReference(ei.definition.getType()) || ei.definition.hasRepresentation(PropertyRepresentation.TYPEATTR) || prefix.endsWith("[x]") : prefix;

        if (ei.definition.hasRepresentation(PropertyRepresentation.TYPEATTR))
          type = ei.element.getType();
        else {
        prefix = prefix.substring(0, prefix.length() - 3);
        for (TypeRefComponent t : ei.definition.getType())
          if ((prefix + Utilities.capitalize(t.getWorkingCode())).equals(ei.name)) {
            type = t.getWorkingCode();
            // Excluding reference is a kludge to get around versioning issues
            if (t.hasProfile() && !type.equals("Reference"))
              profiles.add(t.getProfile().get(0).getValue());
          }
        }
        if (type == null) {
          TypeRefComponent trc = ei.definition.getType().get(0);
          if (trc.getWorkingCode().equals("Reference"))
            type = "Reference";
          else
            rule(errors, IssueType.STRUCTURE, ei.line(), ei.col(), stack.getLiteralPath(), false,
                "The type of element " + ei.name + " is not known, which is illegal. Valid types at this point are " + describeTypes(ei.definition.getType()));
        }
      } else if (ei.definition.getContentReference() != null) {
        typeDefn = resolveNameReference(profile.getSnapshot(), ei.definition.getContentReference());
      } else if (ei.definition.getType().size() == 1 && ("Element".equals(ei.definition.getType().get(0).getWorkingCode()) || "BackboneElement".equals(ei.definition.getType().get(0).getWorkingCode()))) {
        if (ei.definition.getType().get(0).hasProfile()) {
          CanonicalType pu = ei.definition.getType().get(0).getProfile().get(0);
          if (pu.hasExtension(ToolingExtensions.EXT_PROFILE_ELEMENT))
            profiles.add(pu.getValue()+"#"+pu.getExtensionString(ToolingExtensions.EXT_PROFILE_ELEMENT));
          else
            profiles.add(pu.getValue());
        }
      }

      if (type != null) {
        if (type.startsWith("@")) {
          ei.definition = findElement(profile, type.substring(1));
          type = null;
        }
      }
      NodeStack localStack = stack.push(ei.element, ei.count, ei.definition, type == null ? typeDefn : resolveType(type, ei.definition.getType()));
      String localStackLiterapPath = localStack.getLiteralPath();
      String eiPath = ei.path;
      assert(eiPath.equals(localStackLiterapPath)) : "ei.path: " + ei.path + "  -  localStack.getLiteralPath: " + localStackLiterapPath;
      boolean thisIsCodeableConcept = false;
      String thisExtension = null;
      boolean checkDisplay = true;

      checkInvariants(hostContext, errors, profile, ei.definition, resource, ei.element, localStack, true);

      ei.element.markValidation(profile, ei.definition);
      if (type != null) {
        if (isPrimitiveType(type)) {
          checkPrimitive(hostContext, errors, ei.path, type, ei.definition, ei.element, profile, stack);
        } else {
          if (ei.definition.hasFixed()) {
            checkFixedValue(errors,ei.path, ei.element, ei.definition.getFixed(), profile.getUrl(), ei.definition.getSliceName(), null);
          }
          if (ei.definition.hasPattern()) {
              checkFixedValue(errors,ei.path, ei.element, ei.definition.getPattern(), profile.getUrl(), ei.definition.getSliceName(), null, true);
          }
        }
        if (type.equals("Identifier")) {
          checkIdentifier(errors, ei.path, ei.element, ei.definition);
        } else if (type.equals("Coding")) {
          checkCoding(errors, ei.path, ei.element, profile, ei.definition, inCodeableConcept, checkDisplayInContext, stack);
        } else if (type.equals("CodeableConcept")) {
          checkDisplay = checkCodeableConcept(errors, ei.path, ei.element, profile, ei.definition, stack);
          thisIsCodeableConcept = true;
        } else if (type.equals("Reference")) {
          checkReference(hostContext, errors, ei.path, ei.element, profile, ei.definition, actualType, localStack);
        // We only check extensions if we're not in a complex extension or if the element we're dealing with is not defined as part of that complex extension
        } else if (type.equals("Extension")) {
          Element eurl = ei.element.getNamedChild("url");
          if (rule(errors, IssueType.INVALID, ei.path, eurl != null, "Extension.url is required")) {
            String url = eurl.primitiveValue();
            thisExtension = url;
            if (rule(errors, IssueType.INVALID, ei.path, !Utilities.noString(url), "Extension.url is required")) {
              if (rule(errors, IssueType.INVALID, ei.path, (extensionUrl != null) || Utilities.isAbsoluteUrl(url), "Extension.url must be an absolute URL")) {
                checkExtension(hostContext, errors, ei.path, resource, ei.element, ei.definition, profile, localStack, extensionUrl);
              }
            }
          }
        } else if (type.equals("Resource")) {
          validateContains(hostContext, errors, ei.path, ei.definition, definition, resource, ei.element, localStack, idStatusForEntry(element, ei)); // if
        // (str.matches(".*([.,/])work\\1$"))
        } else if (Utilities.isAbsoluteUrl(type)) {
          StructureDefinition defn = context.fetchTypeDefinition(type);
          if (defn != null && hasMapping("http://hl7.org/fhir/terminology-pattern", defn, defn.getSnapshot().getElementFirstRep())) {
            List<String> txtype = getMapping("http://hl7.org/fhir/terminology-pattern", defn, defn.getSnapshot().getElementFirstRep());
            if (txtype.contains("CodeableConcept")) {
              checkTerminologyCodeableConcept(errors, ei.path, ei.element, profile, ei.definition, stack, defn);
              thisIsCodeableConcept = true;
            } else if (txtype.contains("Coding")) {
              checkTerminologyCoding(errors, ei.path, ei.element, profile, ei.definition, inCodeableConcept, checkDisplayInContext, stack, defn);
            }
          }
        }
      } else {
        if (rule(errors, IssueType.STRUCTURE, ei.line(), ei.col(), stack.getLiteralPath(), ei.definition != null, "Unrecognised Content " + ei.name))
          validateElement(hostContext, errors, profile, ei.definition, null, null, resource, ei.element, type, localStack, false, true, null);
      }
      StructureDefinition p = null;
      boolean elementValidated = false;
      String tail = null;
      if (profiles.isEmpty()) {
        if (type != null) {
          p = getProfileForType(type, ei.definition.getType());

          // If dealing with a primitive type, then we need to check the current child against
          // the invariants (constraints) on the current element, because otherwise it only gets
          // checked against the primary type's invariants: LLoyd
          //if (p.getKind() == StructureDefinitionKind.PRIMITIVETYPE) {
          //  checkInvariants(hostContext, errors, ei.path, profile, ei.definition, null, null, resource, ei.element);
          //}

          rule(errors, IssueType.STRUCTURE, ei.line(), ei.col(), ei.path, p != null, "Unknown type " + type);
        }
      } else if (profiles.size()==1) {
        String url = profiles.get(0);
        if (url.contains("#")) {
          tail = url.substring(url.indexOf("#")+1);
          url = url.substring(0, url.indexOf("#"));
        }
        p = this.context.fetchResource(StructureDefinition.class, url);
        rule(errors, IssueType.STRUCTURE, ei.line(), ei.col(), ei.path, p != null, "Unknown profile " + profiles.get(0));
      } else {
        elementValidated = true;
        HashMap<String, List<ValidationMessage>> goodProfiles = new HashMap<String, List<ValidationMessage>>();
        HashMap<String, List<ValidationMessage>> badProfiles = new HashMap<String, List<ValidationMessage>>();
        for (String typeProfile : profiles) {
          String url = typeProfile;
          tail = null;
          if (url.contains("#")) {
            tail = url.substring(url.indexOf("#")+1);
            url = url.substring(0, url.indexOf("#"));
          }
          p = this.context.fetchResource(StructureDefinition.class, typeProfile);
          if (rule(errors, IssueType.STRUCTURE, ei.line(), ei.col(), ei.path, p != null, "Unknown profile " + typeProfile)) {
            List<ValidationMessage> profileErrors = new ArrayList<ValidationMessage>();
            validateElement(hostContext, profileErrors, p, getElementByTail(p, tail), profile, ei.definition, resource, ei.element, type, localStack, thisIsCodeableConcept, checkDisplay, thisExtension);
            if (hasErrors(profileErrors))
              badProfiles.put(typeProfile, profileErrors);
            else
              goodProfiles.put(typeProfile, profileErrors);
          }
        }
        if (goodProfiles.size()==1) {
          errors.addAll(goodProfiles.values().iterator().next());
        } else if (goodProfiles.size()==0) {
          rule(errors, IssueType.STRUCTURE, ei.line(), ei.col(), ei.path, false, "Unable to find matching profile among choices: " + StringUtils.join("; ", profiles));
          for (String m : badProfiles.keySet()) {
            p = this.context.fetchResource(StructureDefinition.class, m);
            for (ValidationMessage message : badProfiles.get(m)) {
              message.setMessage(message.getMessage()+" (validating against "+p.getUrl()+(p.hasVersion() ?"|"+p.getVersion(): "")+" ["+p.getName()+"])");
              errors.add(message);
            }
          }
        } else {
          warning(errors, IssueType.STRUCTURE, ei.line(), ei.col(), ei.path, false, "Found multiple matching profiles among choices: " + StringUtils.join("; ", goodProfiles.keySet()));
          for (String m : goodProfiles.keySet()) {
            p = this.context.fetchResource(StructureDefinition.class, m);
            for (ValidationMessage message : goodProfiles.get(m)) {
              message.setMessage(message.getMessage()+" (validating against "+p.getUrl()+(p.hasVersion() ?"|"+p.getVersion(): "")+" ["+p.getName()+"])");
              errors.add(message);
            }
          }
        }
      }
      if (p!=null) {
        if (!elementValidated) {
          if (ei.element.getSpecial() == SpecialElement.BUNDLE_ENTRY || ei.element.getSpecial() == SpecialElement.BUNDLE_OUTCOME || ei.element.getSpecial() == SpecialElement.PARAMETER )
            validateElement(hostContext, errors, p, getElementByTail(p, tail), profile, ei.definition, ei.element, ei.element, type, localStack, thisIsCodeableConcept, checkDisplay, thisExtension);
          else
            validateElement(hostContext, errors, p, getElementByTail(p, tail), profile, ei.definition, resource, ei.element, type, localStack, thisIsCodeableConcept, checkDisplay, thisExtension);
        }
        int index = profile.getSnapshot().getElement().indexOf(ei.definition);
        if (index < profile.getSnapshot().getElement().size() - 1) {
          String nextPath = profile.getSnapshot().getElement().get(index+1).getPath();
          if (!nextPath.equals(ei.definition.getPath()) && nextPath.startsWith(ei.definition.getPath()))
            validateElement(hostContext, errors, profile, ei.definition, null, null, resource, ei.element, type, localStack, thisIsCodeableConcept, checkDisplay, thisExtension);
        }
      }
    }
  }

  private boolean hasMapping(String url, StructureDefinition defn, ElementDefinition elem) {
    String id = null;
    for (StructureDefinitionMappingComponent m : defn.getMapping()) {
      if (url.equals(m.getUri())) {
        id = m.getIdentity();
        break;
      }
    }
    if (id != null) {
      for (ElementDefinitionMappingComponent m : elem.getMapping()) {
        if (id.equals(m.getIdentity())) {
          return true;
        }
      }
      
    }
    return false;
  }

  private List<String> getMapping(String url, StructureDefinition defn, ElementDefinition elem) {
    List<String> res = new ArrayList<>();
    String id = null;
    for (StructureDefinitionMappingComponent m : defn.getMapping()) {
      if (url.equals(m.getUri())) {
        id = m.getIdentity();
        break;
      }
    }
    if (id != null) {
      for (ElementDefinitionMappingComponent m : elem.getMapping()) {
        if (id.equals(m.getIdentity())) {
          res.add(m.getMap());
        }
      }
    }
    return res;
  }

  public void checkMustSupport(StructureDefinition profile, ElementInfo ei) {
    String usesMustSupport = profile.getUserString("usesMustSupport");
    if (usesMustSupport == null) {
      usesMustSupport = "N";
      for (ElementDefinition pe: profile.getSnapshot().getElement()) {
        if (pe.getMustSupport()) {
          usesMustSupport = "Y";
          break;
        }
      }
      profile.setUserData("usesMustSupport", usesMustSupport);
    }
    if (usesMustSupport.equals("Y")) {
      String elementSupported = ei.element.getUserString("elementSupported");
      if (elementSupported==null || ei.definition.getMustSupport())
        if (ei.definition.getMustSupport())
          ei.element.setUserData("elementSupported", "Y");
        else
          ei.element.setUserData("elementSupported", "N");
    }
  }

  public void checkCardinalities(List<ValidationMessage> errors, StructureDefinition profile, Element element, NodeStack stack,
      List<ElementDefinition> childDefinitions, List<ElementInfo> children, List<String> problematicPaths) throws DefinitionException {
    // 3. report any definitions that have a cardinality problem
    for (ElementDefinition ed : childDefinitions) {
      if (ed.getRepresentation().isEmpty()) { // ignore xml attributes
        int count = 0;
        List<ElementDefinition> slices = null;
        if (ed.hasSlicing())
          slices = ProfileUtilities.getSliceList(profile, ed);
        for (ElementInfo ei : children)
          if (ei.definition == ed)
            count++;
          else if (slices!=null) {
            for (ElementDefinition sed : slices) {
              if (ei.definition == sed) {
                count++;
                break;
              }
            }
          }
        String location = "Profile " + profile.getUrl() + ", Element '" + stack.getLiteralPath() + "." + tail(ed.getPath()) + (ed.hasSliceName()? "[" + ed.getSliceName() + (ed.hasLabel() ? " ("+ed.getLabel()+")" : "")+"]": "") + "'";
        if (ed.getMin() > 0) {
          if (problematicPaths.contains(ed.getPath()))
            hint(errors, IssueType.NOTSUPPORTED, element.line(), element.col(), stack.getLiteralPath(), count >= ed.getMin(), location + "': Unable to check minimum required (" + Integer.toString(ed.getMin()) + ") due to lack of slicing validation");
          else
            rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), count >= ed.getMin(), location + ": minimum required = " + Integer.toString(ed.getMin()) + ", but only found " + Integer.toString(count));
        }
        if (ed.hasMax() && !ed.getMax().equals("*")) {
          if (problematicPaths.contains(ed.getPath()))
            hint(errors, IssueType.NOTSUPPORTED, element.line(), element.col(), stack.getLiteralPath(), count <= Integer.parseInt(ed.getMax()), location + ": Unable to check max allowed (" + ed.getMax() + ") due to lack of slicing validation");
          else
            rule(errors, IssueType.STRUCTURE, element.line(), element.col(), stack.getLiteralPath(), count <= Integer.parseInt(ed.getMax()), location + ": max allowed = " + ed.getMax() + ", but found " + Integer.toString(count));
        }
      }
    }
  }

  public List<String> assignChildren(ValidatorHostContext hostContext, List<ValidationMessage> errors, StructureDefinition profile, Element resource,
      NodeStack stack, List<ElementDefinition> childDefinitions, List<ElementInfo> children) throws DefinitionException, IOException {
    // 2. assign children to a definition
    // for each definition, for each child, check whether it belongs in the slice
    ElementDefinition slicer = null;
    boolean unsupportedSlicing = false;
    List<String> problematicPaths = new ArrayList<String>();
    String slicingPath = null;
    int sliceOffset = 0;
    for (int i = 0; i < childDefinitions.size(); i++) {
      ElementDefinition ed = childDefinitions.get(i);
      boolean childUnsupportedSlicing = false;
      boolean process = true;
      if (ed.hasSlicing() && !ed.getSlicing().getOrdered())
        slicingPath = ed.getPath();
      else if (slicingPath!=null && ed.getPath().equals(slicingPath))
        ; // nothing
      else if (slicingPath != null && !ed.getPath().startsWith(slicingPath))
        slicingPath = null;
      // where are we with slicing
      if (ed.hasSlicing()) {
        if (slicer != null && slicer.getPath().equals(ed.getPath())) {
          String errorContext = "profile " + profile.getUrl();
          if (!resource.getChildValue("id").isEmpty())
              errorContext += "; instance " + resource.getChildValue("id");
          throw new DefinitionException("Slice encountered midway through set (path = " + slicer.getPath() + ", id = "+slicer.getId()+"); " + errorContext);
        }
        slicer = ed;
        process = false;
        sliceOffset = i;
      } else if (slicer != null && !slicer.getPath().equals(ed.getPath()))
        slicer = null;

//      if (process) {
        for (ElementInfo ei : children) {
          unsupportedSlicing = matchSlice(hostContext, errors, profile, stack, slicer, unsupportedSlicing, problematicPaths, sliceOffset, i, ed, childUnsupportedSlicing, ei);
        }
//      }
    }
    int last = -1;
    int lastSlice = -1;
    for (ElementInfo ei : children) {
      String sliceInfo = "";
      if (slicer != null)
        sliceInfo = " (slice: " + slicer.getPath()+")";
      //Lloyd: Removed this because there's no need for extension-specific logic here
/*      if (ei.path.endsWith(".extension"))
        rule(errors, IssueType.INVALID, ei.line(), ei.col(), ei.path, ei.definition != null, "Element is unknown or does not match any slice (url=\"" + ei.element.getNamedChildValue("url") + "\")" + (profile==null ? "" : " for profile " + profile.getUrl()));
      else if (!unsupportedSlicing)*/
      if (!unsupportedSlicing)
        if (ei.additionalSlice && ei.definition != null) {
          if (ei.definition.getSlicing().getRules().equals(ElementDefinition.SlicingRules.OPEN) ||
              ei.definition.getSlicing().getRules().equals(ElementDefinition.SlicingRules.OPENATEND) && true /* TODO: replace "true" with condition to check that this element is at "end" */) {
            hint(errors, IssueType.INFORMATIONAL, ei.line(), ei.col(), ei.path, false, "This element does not match any known slice" + (profile == null ? "" : " for the profile " + profile.getUrl()));
          } else if (ei.definition.getSlicing().getRules().equals(ElementDefinition.SlicingRules.CLOSED)) {
            rule(errors, IssueType.INVALID, ei.line(), ei.col(), ei.path, false, "This element does not match any known slice" + (profile == null ? "" : " for profile " + profile.getUrl() + " and slicing is CLOSED"));
          }
        } else {
          // Don't raise this if we're in an abstract profile, like Resource
          if (!profile.getAbstract())
            hint(errors, IssueType.NOTSUPPORTED, ei.line(), ei.col(), ei.path, (ei.definition != null), "Could not verify slice for profile " + profile.getUrl());
        }
      // TODO: Should get the order of elements correct when parsing elements that are XML attributes vs. elements
      boolean isXmlAttr = false;
      if (ei.definition!=null)
        for (Enumeration<PropertyRepresentation> r : ei.definition.getRepresentation()) {
          if (r.getValue() == PropertyRepresentation.XMLATTR) {
            isXmlAttr = true;
            break;
          }
        }

      if (!ToolingExtensions.readBoolExtension(profile, "http://hl7.org/fhir/StructureDefinition/structuredefinition-xml-no-order")) {
        boolean ok = (ei.definition == null) || (ei.index >= last) || isXmlAttr;
        rule(errors, IssueType.INVALID, ei.line(), ei.col(), ei.path, ok, "As specified by profile " + profile.getUrl() + ", Element '"+ei.name+"' is out of order");
      }
      if (ei.slice != null && ei.index == last && ei.slice.getSlicing().getOrdered())
        rule(errors, IssueType.INVALID, ei.line(), ei.col(), ei.path, (ei.definition == null) || (ei.sliceindex >= lastSlice) || isXmlAttr, "As specified by profile " + profile.getUrl() + ", Element '"+ei.name+"' is out of order in ordered slice");
      if (ei.definition == null || !isXmlAttr)
        last = ei.index;
      if (ei.slice != null)
        lastSlice = ei.sliceindex;
      else
        lastSlice = -1;
    }
    return problematicPaths;
  }

  public List<ElementInfo> listChildren(Element element, NodeStack stack) {
    // 1. List the children, and remember their exact path (convenience)
    List<ElementInfo> children = new ArrayList<InstanceValidator.ElementInfo>();
    ChildIterator iter = new ChildIterator(stack.getLiteralPath(), element);
    while (iter.next())
      children.add(new ElementInfo(iter.name(), iter.element(), iter.path(), iter.count()));
    return children;
  }

  public void checkInvariants(ValidatorHostContext hostContext, List<ValidationMessage> errors, StructureDefinition profile, ElementDefinition definition,
      Element resource, Element element, NodeStack stack, boolean onlyNonInherited) throws FHIRException {
	  // this was an old work around for resource/rootresource issue.
//    if (resource.getName().equals("contained")) {
//      NodeStack ancestor = stack;
//      while (ancestor != null && ancestor.element != null && (!ancestor.element.isResource() || "contained".equals(ancestor.element.getName())))
//        ancestor = ancestor.parent;
//      if (ancestor != null && ancestor.element != null)
//        checkInvariants(hostContext, errors, stack.getLiteralPath(), profile, definition, null, null, ancestor.element, element);
//    } else
      checkInvariants(hostContext, errors, stack.getLiteralPath(), profile, definition, null, null, resource, element, onlyNonInherited);
  }

  public boolean matchSlice(ValidatorHostContext hostContext, List<ValidationMessage> errors, StructureDefinition profile, NodeStack stack,
      ElementDefinition slicer, boolean unsupportedSlicing, List<String> problematicPaths, int sliceOffset, int i, ElementDefinition ed,
      boolean childUnsupportedSlicing, ElementInfo ei) throws IOException {
    boolean match = false;
    if (slicer == null || slicer == ed) {
      match = nameMatches(ei.name, tail(ed.getPath()));
    } else {
//            ei.slice = slice;
      if (nameMatches(ei.name, tail(ed.getPath())))
        try {
          match = sliceMatches(hostContext, ei.element, ei.path, slicer, ed, profile, errors, stack);
          if (match) {
            ei.slice = slicer;

            // Since a defined slice was found, this is not an additional (undefined) slice.
            ei.additionalSlice = false;
          } else if (ei.slice == null) {
            // if the specified slice is undefined, keep track of the fact this is an additional (undefined) slice, but only if a slice wasn't found previously
            ei.additionalSlice = true;
          }
        } catch (FHIRException e) {
          rule(errors, IssueType.PROCESSING, ei.line(), ei.col(), ei.path, false, e.getMessage());
          unsupportedSlicing = true;
          childUnsupportedSlicing = true;
        }
    }
    if (match) {
      boolean isOk = ei.definition == null || ei.definition == slicer || (ei.definition.getPath().endsWith("[x]") && ed.getPath().startsWith(ei.definition.getPath().replace("[x]", "")));
      if (rule(errors, IssueType.INVALID, ei.line(), ei.col(), ei.path, isOk, "Profile " + profile.getUrl() + ", Element matches more than one slice - " + (ei.definition==null || !ei.definition.hasSliceName() ? "" : ei.definition.getSliceName()) + ", " + (ed.hasSliceName() ? ed.getSliceName() : ""))) {
        ei.definition = ed;
        if (ei.slice == null) {
          ei.index = i;
        } else {
          ei.index = sliceOffset;
          ei.sliceindex = i - (sliceOffset + 1);
        }
      }
    } else if (childUnsupportedSlicing) {
      problematicPaths.add(ed.getPath());
    }
    return unsupportedSlicing;
  }

  private ElementDefinition getElementByTail(StructureDefinition p, String tail) throws DefinitionException {
    if (tail == null)
      return p.getSnapshot().getElement().get(0);
    for (ElementDefinition t : p.getSnapshot().getElement()) {
      if (tail.equals(t.getId()))
        return t;
    }
    throw new DefinitionException("Unable to find element with id '"+tail+"'");
  }

  private IdStatus idStatusForEntry(Element ep, ElementInfo ei) {
    if (isBundleEntry(ei.path)) {
      Element req = ep.getNamedChild("request");
      Element resp = ep.getNamedChild("response");
      Element fullUrl = ep.getNamedChild("fullUrl");
      Element method = null;
      Element url = null;
      if (req != null) {
        method = req.getNamedChild("method");
        url = req.getNamedChild("url");
      }
      if (resp != null) {
        return IdStatus.OPTIONAL;
      } if (method == null) {
        if (fullUrl == null)
          return IdStatus.REQUIRED;
        else if (fullUrl.primitiveValue().startsWith("urn:uuid:") || fullUrl.primitiveValue().startsWith("urn:oid:"))
          return IdStatus.OPTIONAL;
        else
          return IdStatus.REQUIRED;
      } else {
        String s = method.primitiveValue();
        if (s.equals("PUT")) {
          if (url == null)
            return IdStatus.REQUIRED;
          else
            return IdStatus.OPTIONAL; // or maybe prohibited? not clear
        } else if (s.equals("POST"))
          return IdStatus.OPTIONAL; // this should be prohibited, but see task 9102
        else // actually, we should never get to here; a bundle entry with method get/delete should not have a resource
          return IdStatus.OPTIONAL;					
      }
    } else if (isParametersEntry(ei.path) || isBundleOutcome(ei.path))
      return IdStatus.OPTIONAL; 
    else
      return IdStatus.REQUIRED; 
  }

  private void checkInvariants(ValidatorHostContext hostContext, List<ValidationMessage> errors, String path, StructureDefinition profile, ElementDefinition ed, String typename, String typeProfile, Element resource, Element element, boolean onlyNonInherited) throws FHIRException, FHIRException {
    if (noInvariantChecks)
      return;

    for (ElementDefinitionConstraintComponent inv : ed.getConstraint()) {
      if (inv.hasExpression() && (!onlyNonInherited || !inv.hasSource() || profile.getUrl().equals(inv.getSource()))) {
        @SuppressWarnings("unchecked")
        Set<String> invList = executionId.equals(element.getUserString(EXECUTION_ID)) ? (Set<String>) element.getUserData(EXECUTED_CONSTRAINT_LIST) : null;
        if (invList == null) {
          invList = new HashSet<>();
          element.setUserData(EXECUTED_CONSTRAINT_LIST, invList);
          element.setUserData(EXECUTION_ID, executionId);
        }     
        if (!invList.contains(inv.getKey())) {
          invList.add(inv.getKey());
          checkInvariant(hostContext, errors, path, profile, resource, element, inv);
        } else {
//          System.out.println("Skip "+inv.getKey()+" on "+path);
        }
      }
    }
  }

  public void checkInvariant(ValidatorHostContext hostContext, List<ValidationMessage> errors, String path, StructureDefinition profile, Element resource, Element element, ElementDefinitionConstraintComponent inv) throws FHIRException {
    ExpressionNode n = (ExpressionNode) inv.getUserData("validator.expression.cache");
    if (n == null) {
      long t = System.nanoTime();
      try {
        n = fpe.parse(fixExpr(inv.getExpression()));
      } catch (FHIRLexerException e) {
        throw new FHIRException("Problem processing expression "+inv.getExpression() +" in profile " + profile.getUrl() + " path " + path + ": " + e.getMessage());
      }
      fpeTime = fpeTime + (System.nanoTime() - t);
      inv.setUserData("validator.expression.cache", n);
    }

    String msg;
    boolean ok;
    try {
      long t = System.nanoTime();
      ok = fpe.evaluateToBoolean(hostContext, resource, hostContext.rootResource, element, n);
      fpeTime = fpeTime + (System.nanoTime() - t);
      msg = fpe.forLog();
    } catch (Exception ex) {
      ok = false;
      msg = ex.getMessage(); 
    }
    if (!ok) {
      if (!Utilities.noString(msg))
        msg = " ("+msg+")";
      if (inv.hasExtension("http://hl7.org/fhir/StructureDefinition/elementdefinition-bestpractice") &&
          ToolingExtensions.readBooleanExtension(inv, "http://hl7.org/fhir/StructureDefinition/elementdefinition-bestpractice")) {
          if (bpWarnings == BestPracticeWarningLevel.Hint) 
            hint(errors, IssueType.INVARIANT, element.line(), element.col(), path, ok, inv.getKey()+": "+inv.getHuman()+msg+" ["+n.toString()+"]");
          else if (bpWarnings == BestPracticeWarningLevel.Warning) 
            warning(errors, IssueType.INVARIANT, element.line(), element.col(), path, ok, inv.getKey()+": "+inv.getHuman()+msg+" ["+n.toString()+"]");
          else if (bpWarnings == BestPracticeWarningLevel.Error) 
            rule(errors, IssueType.INVARIANT, element.line(), element.col(), path, ok, inv.getKey()+": "+inv.getHuman()+msg+" ["+n.toString()+"]");
      } else if (inv.getSeverity() == ConstraintSeverity.ERROR) {
        rule(errors, IssueType.INVARIANT, element.line(), element.col(), path, ok, inv.getKey()+": "+inv.getHuman() + msg + " [" + n.toString() + "]");
      } else if (inv.getSeverity() == ConstraintSeverity.WARNING) {
        warning(errors, IssueType.INVARIANT, element.line(), element.line(), path, ok, inv.getKey()+": "+inv.getHuman() + msg + " [" + n.toString() + "]");
      }
    }
  }

  private void validateMessage(List<ValidationMessage> errors, List<Element> entries, Element messageHeader, NodeStack stack, String fullUrl, String id) {
    // first entry must be a messageheader
    if (rule(errors, IssueType.INVALID, messageHeader.line(), messageHeader.col(), stack.getLiteralPath(), messageHeader.getType().equals("MessageHeader"),
        "The first entry in a message must be a MessageHeader")) {
      // the composition subject and section references must resolve in the bundle
      List<Element> elements = messageHeader.getChildren("data");
      for (Element elem: elements)
        validateBundleReference(errors, entries, elem, "MessageHeader Data", stack.push(elem, -1, null, null), fullUrl, "MessageHeader", id);
    }
  }

  private void validateObservation(List<ValidationMessage> errors, Element element, NodeStack stack) {
    // all observations should have a subject, a performer, and a time

    bpCheck(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), element.getNamedChild("subject") != null, "All observations should have a subject");
	  List<Element> performers = new ArrayList<>();
	  element.getNamedChildren("performer", performers);
	  bpCheck(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), performers.size() > 0, "All observations should have a performer");
    bpCheck(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), element.getNamedChild("effectiveDateTime") != null || element.getNamedChild("effectivePeriod") != null,
        "All observations should have an effectiveDateTime or an effectivePeriod");
  }

  /*
   * The actual base entry point
   */
  /*  private void validateResource(List<ValidationMessage> errors, Element resource, Element element, StructureDefinition defn, ValidationProfileSet profiles, IdStatus idstatus, NodeStack stack) throws FHIRException, FHIRException {
    List<StructureDefinition> declProfiles = new ArrayList<StructureDefinition>();
    List<Element> meta = element.getChildrenByName("meta");
    if (!meta.isEmpty()) {
      for (Element profileName : meta.get(0).getChildrenByName("profile")) {
        StructureDefinition sd = context.fetchResource(StructureDefinition.class, profileName.getValue());
        if (sd != null)
          declProfiles.add(sd);
      }
    }

    if (!declProfiles.isEmpty()) {
      // Validate against profiles rather than the resource itself as they'll be more constrained and will cover the resource elements anyhow
      for (StructureDefinition sd : declProfiles)
        validateResource2(errors, resource, element, sd, profiles, idstatus, stack);
    } else
      validateResource2(errors, resource, element, defn, profiles, idstatus, stack);
  }*/

  private void validateResource(ValidatorHostContext hostContext, List<ValidationMessage> errors, Element resource, Element element, StructureDefinition defn, ValidationProfileSet profiles, IdStatus idstatus, NodeStack stack, boolean isEntry) throws FHIRException, FHIRException, IOException {
    assert stack != null;
    assert resource != null;

    if (isEntry || executionId == null)
      executionId = UUID.randomUUID().toString();
    boolean ok = true;

    String resourceName = element.getType(); // todo: consider namespace...?
    if (defn == null) {
      long t = System.nanoTime();
      defn = element.getProperty().getStructure();
      if (defn == null)
        defn = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/" + resourceName);
      if (profiles!=null)
        getResourceProfiles(resource, stack).addProfiles(errors, profiles, stack.getLiteralPath(), element, isEntry);
      sdTime = sdTime + (System.nanoTime() - t);
      ok = rule(errors, IssueType.INVALID, element.line(), element.col(), stack.addToLiteralPath(resourceName), defn != null, "No definition found for resource type '" + resourceName + "'");
    }

    String type = defn.getKind() == StructureDefinitionKind.LOGICAL ? defn.getId() : defn.getType();
    // special case: we have a bundle, and the profile is not for a bundle. We'll try the first entry instead 
    if (!type.equals(resourceName) && resourceName.equals("Bundle")) {
      Element first = getFirstEntry(element);
      if (first != null && first.getType().equals(type)) {
        element = first;
        resourceName = element.getType();
        idstatus = IdStatus.OPTIONAL; // why?
      }
    }
    ok = rule(errors, IssueType.INVALID, -1, -1, stack.getLiteralPath(), type.equals(resourceName), "Specified profile type was '" + type + "', but found type '" + resourceName + "'");

    if (ok) {
      if (idstatus == IdStatus.REQUIRED && (element.getNamedChild("id") == null))
        rule(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), false, "Resource requires an id, but none is present");
      else if (idstatus == IdStatus.PROHIBITED && (element.getNamedChild("id") != null))
        rule(errors, IssueType.INVALID, element.line(), element.col(), stack.getLiteralPath(), false, "Resource has an id, but none is allowed");
      start(hostContext, errors, element, element, defn, stack); // root is both definition and type
    }
  }

  private void loadProfiles(ValidationProfileSet profiles) throws DefinitionException {
    if (profiles != null) { 
      for (String profile : profiles.getCanonicalUrls()) {
        StructureDefinition p = context.fetchResource(StructureDefinition.class, profile);
        if (p == null)
          throw new DefinitionException("StructureDefinition '" + profile + "' not found by validator");
        profiles.getDefinitions().add(p);
      }
    }
  }

  private Element getFirstEntry(Element bundle) {
    List<Element> list = new ArrayList<Element>();
    bundle.getNamedChildren("entry", list);
    if (list.isEmpty())
      return null;
    Element resource = list.get(0).getNamedChild("resource");
    if (resource == null)
      return null;
    else
      return resource;
  }

  private void validateSections(List<ValidationMessage> errors, List<Element> entries, Element focus, NodeStack stack, String fullUrl, String id) {
    List<Element> sections = new ArrayList<Element>();
    focus.getNamedChildren("section", sections);
    int i = 1;
    for (Element section : sections) {
      NodeStack localStack = stack.push(section, i, null, null);

      // technically R4+, but there won't be matches from before that 
      validateDocumentReference(errors, entries, section, stack, fullUrl, id, false, "author", "Section");
      validateDocumentReference(errors, entries, section, stack, fullUrl, id, false, "focus", "Section");
      
      List<Element> sectionEntries = new ArrayList<Element>();
      section.getNamedChildren("entry", sectionEntries);
      int j = 1;
      for (Element sectionEntry : sectionEntries) {
        NodeStack localStack2 = localStack.push(sectionEntry, j, null, null);
        validateBundleReference(errors, entries, sectionEntry, "Section Entry", localStack2, fullUrl, "Composition", id);
        j++;
      }
      validateSections(errors, entries, section, localStack, fullUrl, id);
      i++;
    }
  }

  private boolean valueMatchesCriteria(Element value, ElementDefinition criteria, StructureDefinition profile) throws FHIRException {
    if (criteria.hasFixed()) {
      List<ValidationMessage> msgs = new ArrayList<ValidationMessage>();
      checkFixedValue(msgs, "{virtual}", value, criteria.getFixed(), profile.getUrl(), "value", null);
      return msgs.size() == 0;
    } else if (criteria.hasBinding() && criteria.getBinding().getStrength() == BindingStrength.REQUIRED && criteria.getBinding().hasValueSet()) {
      throw new FHIRException("Unable to resolve slice matching - slice matching by value set not done");      
    } else {
      throw new FHIRException("Unable to resolve slice matching - no fixed value or required value set");
    }
  }

  private boolean yearIsValid(String v) {
    if (v == null) {
      return false;
    }
    try {
      int i = Integer.parseInt(v.substring(0, Math.min(4, v.length())));
      return i >= 1800 && i <= thisYear() + 80;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private int thisYear() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }

  public class ChildIterator {
    private String basePath;
    private Element parent;
    private int cursor;
    private int lastCount;

    public ChildIterator(String path, Element element) {
      parent = element;
      basePath = path;
      cursor = -1;
    }

    public int count() {
      String nb = cursor == 0 ? "--" : parent.getChildren().get(cursor-1).getName();
      String na = cursor >= parent.getChildren().size() - 1 ? "--" : parent.getChildren().get(cursor+1).getName();
      if (name().equals(nb) || name().equals(na) ) {
        return lastCount;
      } else
        return -1;
    }

    public Element element() {
      return parent.getChildren().get(cursor);
    }

    public String name() {
      return element().getName();
    }

    public boolean next() {
      if (cursor == -1) {
        cursor++;
        lastCount = 0;
      } else {
        String lastName = name();
        cursor++;
        if (cursor < parent.getChildren().size() && name().equals(lastName))
          lastCount++;
        else
          lastCount = 0;
      }
      return cursor < parent.getChildren().size();
    }

    public String path() {
      int i = count();
      String sfx = "";
      String n = name();
      String fn = "";
      if (element().getProperty().isChoice()) {
        String en = element().getProperty().getName();
        en = en.substring(0, en.length()-3);
        String t = n.substring(en.length());
        if (isPrimitiveType(Utilities.uncapitalize(t)))
          t = Utilities.uncapitalize(t);
        n = en;
        fn = ".ofType("+t+")";       
      }
      if (i > -1 || (element().getSpecial() == null && element().isList())) {
        sfx = "[" + Integer.toString(lastCount) + "]";
      }
      return basePath + "." + n + sfx+fn;
    }
  }

  public class NodeStack {
    private ElementDefinition definition;
    private Element element;
    private ElementDefinition extension;
    private String literalPath; // xpath format
    private List<String> logicalPaths; // dotted format, various entry points
    private NodeStack parent;
    private ElementDefinition type;
    private String workingLang;

    public NodeStack() {
      workingLang = validationLanguage;
    }	  

    public NodeStack(Element element) {
      this.element = element;
      literalPath = element.getName();
      workingLang = validationLanguage;
    }	  

    public String addToLiteralPath(String... path) {
      StringBuilder b = new StringBuilder();
      b.append(getLiteralPath());
      for (String p : path) {
        if (p.startsWith(":")) {
          b.append("[");
          b.append(p.substring(1));
          b.append("]");
        } else {
          b.append(".");
          b.append(p);
        }	  
      }	  
      return b.toString();
    }

    private ElementDefinition getDefinition() {
      return definition;
    }

    private Element getElement() {
      return element;
    }

    protected String getLiteralPath() {
      return literalPath == null ? "" : literalPath;
    }

    private List<String> getLogicalPaths() {
      return logicalPaths == null ? new ArrayList<String>() : logicalPaths;
    }

    private ElementDefinition getType() {
      return type;
    }

    private NodeStack push(Element element, int count, ElementDefinition definition, ElementDefinition type) {
      NodeStack res = new NodeStack();
      res.parent = this;
      res.workingLang = this.workingLang;
      res.element = element;
      res.definition = definition;
      res.literalPath = getLiteralPath() + "." + element.getName();
      if (count > -1)
        res.literalPath = res.literalPath + "[" + Integer.toString(count) + "]";
      else if (element.getSpecial() == null && element.getProperty().isList())
        res.literalPath = res.literalPath + "[0]";
      else if (element.getProperty().isChoice()) {
        String n = res.literalPath.substring(res.literalPath.lastIndexOf(".")+1);
        String en = element.getProperty().getName();
        en = en.substring(0, en.length()-3);
        String t = n.substring(en.length());
        if (isPrimitiveType(Utilities.uncapitalize(t)))
          t = Utilities.uncapitalize(t);
        res.literalPath = res.literalPath.substring(0, res.literalPath.lastIndexOf("."))+"."+en+".ofType("+t+")";
      }
      res.logicalPaths = new ArrayList<String>();
      if (type != null) {
        // type will be bull if we on a stitching point of a contained resource, or if....
        res.type = type;
        String t = tail(definition.getPath());
        for (String lp : getLogicalPaths()) {
          res.logicalPaths.add(lp + "." + t);
          if (t.endsWith("[x]"))
            res.logicalPaths.add(lp + "." + t.substring(0, t.length() - 3) + type.getPath());
        }
        res.logicalPaths.add(type.getPath());
      } else if (definition != null) {
        for (String lp : getLogicalPaths())
          res.logicalPaths.add(lp + "." + element.getName());
      } else
        res.logicalPaths.addAll(getLogicalPaths());
      // CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
      // for (String lp : res.logicalPaths)
      // b.append(lp);
      // System.out.println(res.literalPath+" : "+b.toString());
      return res;
    }

    private void setType(ElementDefinition type) {
      this.type = type;
    }
  }

  public class ElementInfo {

    public int index; // order of definition in overall order. all slices get the index of the slicing definition
    public int sliceindex; // order of the definition in the slices (if slice != null)
    public int count;
    public ElementDefinition definition;
    public ElementDefinition slice;
    public boolean additionalSlice; // If true, indicates that this element is an additional slice
    private Element element;
    private String name;
    private String path;

    public ElementInfo(String name, Element element, String path, int count) {
      this.name = name;
      this.element = element;
      this.path = path;
      this.count = count;
    }

    public int col() {
      return element.col();
    }

    public int line() {
      return element.line();
    }

    @Override
    public String toString() {
      return path;
    }
  }

  public String reportTimes() {
    String s = String.format("Times: overall = %d, tx = %d, sd = %d, load = %d, fpe = %d", overall, txTime, sdTime, loadTime, fpeTime);
    overall = 0;
    txTime = 0;
    sdTime = 0;
    loadTime = 0;
    fpeTime = 0;
    return s;
  }

  public boolean isNoBindingMsgSuppressed() {
    return noBindingMsgSuppressed;
  }

  public IResourceValidator setNoBindingMsgSuppressed(boolean noBindingMsgSuppressed) {
    this.noBindingMsgSuppressed = noBindingMsgSuppressed;
    return this;
  }

  
  public boolean isNoTerminologyChecks() {
    return noTerminologyChecks;
  }

  public IResourceValidator setNoTerminologyChecks(boolean noTerminologyChecks) {
    this.noTerminologyChecks = noTerminologyChecks;
    return this;
  }

  public void checkAllInvariants(){
    for (StructureDefinition sd : context.allStructures()) {
      if (sd.getDerivation() == TypeDerivationRule.SPECIALIZATION) {
        for (ElementDefinition ed : sd.getSnapshot().getElement()) {
          for (ElementDefinitionConstraintComponent inv : ed.getConstraint()) {
            if (inv.hasExpression()) {
              try {
                ExpressionNode n = (ExpressionNode) inv.getUserData("validator.expression.cache");
                if (n == null) {
                  n = fpe.parse(fixExpr(inv.getExpression()));
                  inv.setUserData("validator.expression.cache", n);
                }
                fpe.check(null, sd.getKind() == StructureDefinitionKind.RESOURCE ?  sd.getType() : "DomainResource", ed.getPath(), n);
              } catch (Exception e) {
                System.out.println("Error processing structure ["+sd.getId()+"] path "+ed.getPath()+":"+inv.getKey()+" (\""+inv.getExpression()+"\"): "+e.getMessage());
              }
            }
          }
        }
      }
    }
  }

  private String fixExpr(String expr) {
    // this is a hack work around for past publication of wrong FHIRPath expressions
    // R4
    // waiting for 4.0.2

    // handled in 4.0.1
    if ("(component.empty() and hasMember.empty()) implies (dataAbsentReason or value)".equals(expr))
      return "(component.empty() and hasMember.empty()) implies (dataAbsentReason.exists() or value.exists())";
    if ("isModifier implies isModifierReason.exists()".equals(expr))
      return "(isModifier.exists() and isModifier) implies isModifierReason.exists()";
    if ("(%resource.kind = 'logical' or element.first().path.startsWith(%resource.type)) and (element.tail().not() or  element.tail().all(path.startsWith(%resource.differential.element.first().path.replaceMatches('\\\\..*','')&'.')))".equals(expr))
      return "(%resource.kind = 'logical' or element.first().path.startsWith(%resource.type)) and (element.tail().empty() or  element.tail().all(path.startsWith(%resource.differential.element.first().path.replaceMatches('\\\\..*','')&'.')))";
    if ("differential.element.all(id) and differential.element.id.trace('ids').isDistinct()".equals(expr))
      return "differential.element.all(id.exists()) and differential.element.id.trace('ids').isDistinct()";
    if ("snapshot.element.all(id) and snapshot.element.id.trace('ids').isDistinct()".equals(expr))
      return "snapshot.element.all(id.exists()) and snapshot.element.id.trace('ids').isDistinct()";
    
    // R3
    if ("(code or value.empty()) and (system.empty() or system = 'urn:iso:std:iso:4217')".equals(expr))
      return "(code.exists() or value.empty()) and (system.empty() or system = 'urn:iso:std:iso:4217')";
    if ("value.empty() or code!=component.code".equals(expr)) 
      return "value.empty() or (code in component.code).not()";
    if ("(code or value.empty()) and (system.empty() or system = %ucum) and (value.empty() or value > 0)".equals(expr))
      return "(code.exists() or value.empty()) and (system.empty() or system = %ucum) and (value.empty() or value > 0)";
    if ("element.all(definition and min and max)".equals(expr))
      return "element.all(definition.exists() and min.exists() and max.exists())";
    if ("telecom or endpoint".equals(expr))
      return "telecom.exists() or endpoint.exists()";
    if ("(code or value.empty()) and (system.empty() or system = %ucum) and (value.empty() or value > 0)".equals(expr))
      return "(code.exists() or value.empty()) and (system.empty() or system = %ucum) and (value.empty() or value > 0)";
    if ("searchType implies type = 'string'".equals(expr))
      return "searchType.exists() implies type = 'string'";
    if ("abatement.empty() or (abatement as boolean).not()  or clinicalStatus='resolved' or clinicalStatus='remission' or clinicalStatus='inactive'".equals(expr))
      return "abatement.empty() or (abatement is boolean).not() or (abatement as boolean).not() or (clinicalStatus = 'resolved') or (clinicalStatus = 'remission') or (clinicalStatus = 'inactive')";    
    if ("(component.empty() and related.empty()) implies (dataAbsentReason or value)".equals(expr))
      return "(component.empty() and related.empty()) implies (dataAbsentReason.exists() or value.exists())";
    
    if ("".equals(expr))
      return "";
    return expr;
  }

  public IEvaluationContext getExternalHostServices() {
    return externalHostServices;
  }

  public String getValidationLanguage() {
    return validationLanguage;
  }

  public void setValidationLanguage(String validationLanguage) {
    this.validationLanguage = validationLanguage;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }


}
