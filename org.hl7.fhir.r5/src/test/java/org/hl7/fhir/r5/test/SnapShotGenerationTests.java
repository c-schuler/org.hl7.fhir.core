package org.hl7.fhir.r5.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.exceptions.PathEngineException;
import org.hl7.fhir.r5.conformance.ProfileUtilities;
import org.hl7.fhir.r5.conformance.ProfileUtilities.ProfileKnowledgeProvider;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.ExpressionNode.CollectionStatus;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.model.TestScript;
import org.hl7.fhir.r5.model.TestScript.AssertionResponseTypes;
import org.hl7.fhir.r5.model.TestScript.SetupActionAssertComponent;
import org.hl7.fhir.r5.model.TestScript.SetupActionOperationComponent;
import org.hl7.fhir.r5.model.TestScript.TestActionComponent;
import org.hl7.fhir.r5.model.TestScript.TestScriptFixtureComponent;
import org.hl7.fhir.r5.model.TestScript.TestScriptTestComponent;
import org.hl7.fhir.r5.test.SnapShotGenerationTests.TestFetchMode;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.model.TypeDetails;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.utils.CodingUtilities;
import org.hl7.fhir.r5.utils.EOperationOutcome;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.r5.utils.FHIRPathEngine.IEvaluationContext;
import org.hl7.fhir.r5.utils.IResourceValidator;
import org.hl7.fhir.r5.utils.NarrativeGenerator;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import junit.framework.Assert;

@RunWith(Parameterized.class)
public class SnapShotGenerationTests {

  public enum TestFetchMode {
    INPUT,
    OUTPUT, 
    INCLUDE
  }

  public static class Rule {
    private String description;
    private String expression;
    public Rule(String description, String expression) {
      super();
      this.description = description;
      this.expression = expression;
    }
    public Rule(Element rule) {
      super();
      this.description = rule.getAttribute("text");
      this.expression = rule.getAttribute("fhirpath");
    }
    public String getDescription() {
      return description;
    }
    public String getExpression() {
      return expression;
    }

  }

  public static class TestDetails {
    private String id;
    private String include;
    private String register;
    private String regex;
    private boolean gen;
    private boolean sort;
    private boolean fail;
    private List<Rule> rules = new ArrayList<>();
    private StructureDefinition source;
    private StructureDefinition included;
    private StructureDefinition expected;
    private StructureDefinition output;

    public TestDetails(Element test) {
      super();
      gen = "true".equals(test.getAttribute("gen"));
      sort = "true".equals(test.getAttribute("sort"));
      fail = "true".equals(test.getAttribute("fail"));
      id = test.getAttribute("id");
      include = test.getAttribute("include");
      register = test.getAttribute("register");
      regex = test.getAttribute("regex");
      Element rule = XMLUtil.getFirstChild(test);
      while (rule != null && rule.getNodeName().equals("rule")) {
        rules.add(new Rule(rule));
        rule = XMLUtil.getNextSibling(rule);
      }
    }
    public String getId() {
      return id;
    }
    public boolean isSort() {
      return sort;
    }
    public boolean isGen() {
      return gen;
    }
    public String getInclude() {
      return include;
    }
    public boolean isFail() {
      return fail;
    }
    public StructureDefinition getIncluded() {
      return included;
    }
    public List<Rule> getRules() {
      return rules;
    }
    public StructureDefinition getSource() {
      return source;
    }
    public void setSource(StructureDefinition source) {
      this.source = source;
    }
    public StructureDefinition getExpected() {
      return expected;
    }
    public void setExpected(StructureDefinition expected) {
      this.expected = expected;
    }
    public StructureDefinition getOutput() {
      return output;
    }
    public void setOutput(StructureDefinition output) {
      this.output = output;
    }
    public void load() throws FHIRFormatError, FileNotFoundException, IOException {
      if (TestingUtilities.findTestResource("r5", "snapshot-generation", id+"-input.json"))
        source = (StructureDefinition) new JsonParser().parse(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", id+"-input.json"));
      else
        source = (StructureDefinition) new XmlParser().parse(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", id+"-input.xml"));
      if (!fail)
        expected = (StructureDefinition) new XmlParser().parse(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", id+"-expected.xml"));
      if (!Utilities.noString(include))
        included = (StructureDefinition) new XmlParser().parse(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", include+".xml"));
      if (!Utilities.noString(register)) {
        if (TestingUtilities.findTestResource("r5", "snapshot-generation", register+".xml")) {
          included = (StructureDefinition) new XmlParser().parse(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", register+".xml"));
        } else {
          included = (StructureDefinition) new JsonParser().parse(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", register+".json"));          
        }
      }
    }
  }

  public class TestPKP implements ProfileKnowledgeProvider {

    @Override
    public boolean isDatatype(String name) {
      StructureDefinition sd = TestingUtilities.context().fetchTypeDefinition(name);
      return (sd != null) && (sd.getDerivation() == TypeDerivationRule.SPECIALIZATION) && (sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE || sd.getKind() == StructureDefinitionKind.COMPLEXTYPE); 
    }

    @Override
    public boolean isResource(String typeSimple) {
      StructureDefinition sd = TestingUtilities.context().fetchTypeDefinition(typeSimple);
      return (sd != null) && (sd.getDerivation() == TypeDerivationRule.SPECIALIZATION) && (sd.getKind() == StructureDefinitionKind.RESOURCE); 
    }

    @Override
    public boolean hasLinkFor(String typeSimple) {
      return isDatatype(typeSimple);
    }

    @Override
    public String getLinkFor(String corePath, String typeSimple) {
      return Utilities.pathURL(corePath, "datatypes.html#"+typeSimple);
    }

    @Override
    public BindingResolution resolveBinding(StructureDefinition def, ElementDefinitionBindingComponent binding, String path) throws FHIRException {
      BindingResolution br = new BindingResolution();
      br.url = path+"/something.html";
      br.display = "something";
      return br;
    }

    @Override
    public BindingResolution resolveBinding(StructureDefinition def, String url, String path) throws FHIRException {
      BindingResolution br = new BindingResolution();
      br.url = path+"/something.html";
      br.display = "something";
      return br;
    }

    @Override
    public String getLinkForProfile(StructureDefinition profile, String url) {
      StructureDefinition sd = TestingUtilities.context().fetchResource(StructureDefinition.class, url);
      if (sd == null)
        return url+"|"+url;
      else
        return sd.getId()+".html|"+sd.present();
    }

    @Override
    public boolean prependLinks() {
      return false;
    }

    @Override
    public String getLinkForUrl(String corePath, String s) {
      // TODO Auto-generated method stub
      return null;
    }

  }

  private static class SnapShotGenerationTestsContext implements IEvaluationContext {
    public List<TestDetails> tests = new ArrayList<>();

    public Resource fetchFixture(String id) {
      TestFetchMode mode = TestFetchMode.INPUT;
      if (id.equals("patient"))
        return TestingUtilities.context().fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Patient");
      if (id.equals("valueset"))
        return TestingUtilities.context().fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/ValueSet");
      if (id.equals("organization"))
        return TestingUtilities.context().fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Organization");
      if (id.equals("operationoutcome"))
        return TestingUtilities.context().fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/OperationOutcome");
      if (id.equals("parameters"))
        return TestingUtilities.context().fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/Parameters");
      
      if (id.contains("-")) {
        String[] p = id.split("\\-");
        id = p[0];
        if (p[1].equals("output"))
          mode = TestFetchMode.OUTPUT;
        else if (p[1].equals("include"))
          mode = TestFetchMode.INCLUDE;
      }
      for (TestDetails td : tests) {
        if (td.getId().equals(id))
          switch (mode) {
          case INPUT: return td.getSource();
          case OUTPUT: if (td.getOutput() == null)
            throw new FHIRException("Not generated yet");
          else
            return td.getOutput();
          case INCLUDE:
            return td.getIncluded();
          default:
            throw new FHIRException("Not done yet");
          }
      }
      return null;
    }

    // FHIRPath methods
    @Override
    public Base resolveConstant(Object appContext, String name, boolean beforeContext) throws PathEngineException {
      throw new Error("Not implemented yet");
    }

    @Override
    public TypeDetails resolveConstantType(Object appContext, String name) throws PathEngineException {
      throw new Error("Not implemented yet");
    }

    @Override
    public boolean log(String argument, List<Base> focus) {
      System.out.println(argument+": "+fp.convertToString(focus));
      return true;
    }

    @Override
    public FunctionDetails resolveFunction(String functionName) {
      if ("fixture".equals(functionName))
        return new FunctionDetails("Access a fixture defined in the testing context", 0, 1);
      return null;
    }

    @Override
    public TypeDetails checkFunction(Object appContext, String functionName, List<TypeDetails> parameters) throws PathEngineException {
      if ("fixture".equals(functionName))
        return new TypeDetails(CollectionStatus.SINGLETON, TestingUtilities.context().getResourceNamesAsSet());
      return null;
    }

    @Override
    public List<Base> executeFunction(Object appContext, String functionName, List<List<Base>> parameters) {
      if ("fixture".equals(functionName)) {
        String id = fp.convertToString(parameters.get(0));
        Resource res = fetchFixture(id);
        if (res != null) {
          List<Base> list = new ArrayList<Base>();
          list.add(res);
          return list;
        }
        throw new Error("Could not resolve "+id);
      }
      throw new Error("Not implemented yet");
    }

    @Override
    public Base resolveReference(Object appContext, String url) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean conformsToProfile(Object appContext, Base item, String url) throws FHIRException {
      IResourceValidator val = TestingUtilities.context().newValidator();
      List<ValidationMessage> valerrors = new ArrayList<ValidationMessage>();
      if (item instanceof Resource) {
        val.validate(appContext, valerrors, (Resource) item, url);
        boolean ok = true;
        for (ValidationMessage v : valerrors)
          ok = ok && v.getLevel().isError();
        return ok;
      }
      throw new NotImplementedException("Not done yet (IGPublisherHostServices.SnapShotGenerationTestsContext), when item is element");
    }

    public StructureDefinition getByUrl(String url) {
      if (url == null)
        return null;
      for (TestDetails t : tests) {
        if (t.expected != null && url.equals(t.expected.getUrl()))
          return t.expected;
        if (t.included != null && url.equals(t.included.getUrl()))
          return t.included;
      }
      return null;
    }

    @Override
    public ValueSet resolveValueSet(Object appContext, String url) {
      throw new Error("Not implemented yet");
    }

  }

  private static FHIRPathEngine fp;

  @Parameters(name = "{index}: file {0}")
  public static Iterable<Object[]> data() throws ParserConfigurationException, IOException, FHIRFormatError, SAXException {

    SnapShotGenerationTestsContext context = new SnapShotGenerationTestsContext();
    Document tests = XMLUtil.parseToDom(TestingUtilities.loadTestResource("r5", "snapshot-generation", "manifest.xml"));
    Element test = XMLUtil.getFirstChild(tests.getDocumentElement());
    List<Object[]> objects = new ArrayList<Object[]>();
    while (test != null && test.getNodeName().equals("test")) {
      TestDetails t = new TestDetails(test);
      context.tests.add(t);
      t.load();
      objects.add(new Object[] {t.getId(), t, context });
      test = XMLUtil.getNextSibling(test);
    }
    return objects;

  }


  private final TestDetails test;
  private SnapShotGenerationTestsContext context;
  private List<ValidationMessage> messages;

  public SnapShotGenerationTests(String id, TestDetails test, SnapShotGenerationTestsContext context) {
    this.test = test;
    this.context = context;
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test() throws Exception {
    if (fp == null)
      fp = new FHIRPathEngine(TestingUtilities.context());
    fp.setHostServices(context);
    messages = new ArrayList<ValidationMessage>();
    
    if (test.isFail()) {
      try {
        if (test.isGen())
          testGen(true);
        else
          testSort();
        Assert.assertTrue("Should have failed", false);
      } catch (Throwable e) {
        System.out.println("Error running test: "+e.getMessage());
        if (!Utilities.noString(test.regex)) {
          Assert.assertTrue("correct error message", e.getMessage().matches(test.regex));
        } else if ("Should have failed".equals(e.getMessage())) {
          throw e;
        } else {
          Assert.assertTrue("all ok", true);
        }
        
      }
    } else if (test.isGen())
      testGen(false);
    else
      testSort();
    for (Rule r : test.getRules()) {
      StructureDefinition sdn = new StructureDefinition();
      boolean ok = fp.evaluateToBoolean(sdn, sdn, sdn, r.expression);
      Assert.assertTrue(r.description, ok);
    }
  }


  private void testSort() throws DefinitionException, FHIRException, IOException {
    StructureDefinition base = getSD(test.getSource().getBaseDefinition()); 
    test.setOutput(test.getSource().copy());
    ProfileUtilities pu = new ProfileUtilities(TestingUtilities.context(), null, null);
    pu.setIds(test.getSource(), false);
    List<String> errors = new ArrayList<String>();          
    pu.sortDifferential(base, test.getOutput(), test.getOutput().getUrl(), errors);
    if (!errors.isEmpty())
      throw new FHIRException(errors.get(0));
    IOUtils.copy(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", test.getId()+"-expected.xml"), new FileOutputStream(TestingUtilities.tempFile("snapshot", test.getId()+"-expected.xml")));
    new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(TestingUtilities.tempFile("snapshot", test.getId()+"-actual.xml")), test.getOutput());
    Assert.assertTrue("Output does not match expected", test.expected.equalsDeep(test.output));
  }

  private void testGen(boolean fail) throws Exception {
    if (!Utilities.noString(test.register)) {
      ProfileUtilities pu = new ProfileUtilities(TestingUtilities.context(), null, null);
      pu.setNewSlicingProcessing(true);
      List<String> errors = new ArrayList<String>();          
      pu.setIds(test.included, false);
      StructureDefinition base = TestingUtilities.context().fetchResource(StructureDefinition.class, test.included.getBaseDefinition());
      pu.generateSnapshot(base, test.included, test.included.getUrl(), "http://test.org/profile", test.included.getName());
      if (!TestingUtilities.context().hasResource(StructureDefinition.class, test.included.getUrl()))
        TestingUtilities.context().cacheResource(test.included);
    }
    StructureDefinition base = getSD(test.getSource().getBaseDefinition()); 
    if (!base.getUrl().equals(test.getSource().getBaseDefinition()))
      throw new Exception("URL mismatch on base: "+base.getUrl()+" wanting "+test.getSource().getBaseDefinition());
    
    StructureDefinition output = test.getSource().copy();
    ProfileUtilities pu = new ProfileUtilities(TestingUtilities.context(), messages , new TestPKP());
    pu.setNewSlicingProcessing(true);
    pu.setThrowException(true);
    //pu.setDebug(true);
    pu.setIds(test.getSource(), false);
    if (test.isSort()) {
      List<String> errors = new ArrayList<String>();
      int lastCount = output.getDifferential().getElement().size();
      pu.sortDifferential(base, output, test.getSource().getName(), errors);
      if (errors.size() > 0)
        throw new FHIRException("Sort failed: "+errors.toString());
    }
    try {
      messages.clear();
      pu.generateSnapshot(base, output, test.getSource().getUrl(), "http://test.org/profile", test.getSource().getName());
      List<ValidationMessage> ml = new ArrayList<>();
      for (ValidationMessage vm : messages) {
        if (vm.getLevel() == IssueSeverity.ERROR) {
          ml.add(vm);
        }
      }
      if (ml.size() > 0) {
        throw new FHIRException("Snapshot Generation failed: "+ml.toString());
      }
    } catch (Throwable e) {
      System.out.println("\r\nException: "+e.getMessage());
      throw e;
    }
    if (output.getDifferential().hasElement())
      new NarrativeGenerator("", "http://hl7.org/fhir", TestingUtilities.context()).setPkp(new TestPKP()).generate(output, null);
    if (!fail) {
      test.output = output;
      TestingUtilities.context().cacheResource(output);
      File dst = new File(TestingUtilities.tempFile("snapshot", test.getId()+"-expected.xml"));
      if (dst.exists())
        dst.delete();
      IOUtils.copy(TestingUtilities.loadTestResourceStream("r5", "snapshot-generation", test.getId()+"-expected.xml"), new FileOutputStream(dst));
      new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(TestingUtilities.tempFile("snapshot", test.getId()+"-actual.xml")), output);
      StructureDefinition t1 = test.expected.copy();
      t1.setText(null);
      StructureDefinition t2 = test.output.copy();
      t2.setText(null);
      Assert.assertTrue("Output does not match expected", t1.equalsDeep(t2));
    }
  }

  private StructureDefinition getSD(String url) throws DefinitionException, FHIRException, IOException {
    StructureDefinition sd = context.getByUrl(url);
    if (sd == null)
      sd = TestingUtilities.context().fetchResource(StructureDefinition.class, url);
    if (!sd.hasSnapshot()) {
      StructureDefinition base = getSD(sd.getBaseDefinition());
      ProfileUtilities pu = new ProfileUtilities(TestingUtilities.context(), messages , new TestPKP());
      pu.setNewSlicingProcessing(true);
      List<String> errors = new ArrayList<String>();          
      pu.sortDifferential(base, sd, url, errors);
      if (!errors.isEmpty())
        throw new FHIRException(errors.get(0));
      pu.setIds(sd, false);
      pu.generateSnapshot(base, sd, sd.getUrl(), "http://test.org/profile", sd.getName());
    }
    return sd;
  }
}
