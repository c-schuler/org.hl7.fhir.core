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
/*
Copyright (c) 2011+, HL7, Inc
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hl7.fhir.r5.conformance.CapabilityStatementUtilities;
import org.hl7.fhir.r5.conformance.CapabilityStatementUtilities.CapabilityStatementComparisonOutput;
import org.hl7.fhir.r5.conformance.ProfileComparer;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Constants;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.FhirPublication;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.utils.KeyGenerator;
import org.hl7.fhir.r5.utils.ToolingExtensions;
import org.hl7.fhir.r5.validation.ValidationEngine.ScanOutputItem;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.cache.PackageCacheManager;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;

/**
 * A executable class that will validate one or more FHIR resources against
 * the specification
 *
 * todo: schema validation (w3c xml, json schema, shex?)
 *
 * if you want to host validation inside a process, skip this class, and look at
 * ValidationEngine
 *
 * todo: find a gome for this:

 * @author Grahame
 *
 */
public class Validator {

  public enum EngineMode {
    VALIDATION, TRANSFORM, NARRATIVE, SNAPSHOT, SCAN, CONVERT, FHIRPATH
  }

  private static String getNamedParam(String[] args, String param) {
    boolean found = false;
    for (String a : args) {
      if (found)
        return a;
      if (a.equals(param)) {
        found = true;
      }
    }
    return null;
  }

  private static String toMB(long maxMemory) {
    return Long.toString(maxMemory / (1024*1024));
  }

  public static void main(String[] args) throws Exception {
    System.out.println("FHIR Validation tool " + VersionUtil.getVersionString());
    System.out.println("Detected Java version: " + System.getProperty("java.version")+" from "+System.getProperty("java.home")+" on "+System.getProperty("os.arch")+" ("+System.getProperty("sun.arch.data.model")+"bit). "+toMB(Runtime.getRuntime().maxMemory())+"MB available");
    String proxy = getNamedParam(args, "-proxy");
    if (!Utilities.noString(proxy)) {
      String[] p = proxy.split("\\:");
      System.setProperty("http.proxyHost", p[0]);
      System.setProperty("http.proxyPort", p[1]);
    }

     if (hasParam(args, "-tests")) {
      try {
			Class<?> clazz = Class.forName("org.hl7.fhir.validation.r5.tests.ValidationEngineTests");
			clazz.getMethod("execute").invoke(clazz);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (args.length == 0 || hasParam(args, "help") || hasParam(args, "?") || hasParam(args, "-?") || hasParam(args, "/?") ) {
      System.out.println("");
      System.out.println("The FHIR validation tool validates a FHIR resource or bundle.");
      System.out.println("The validation tool compares a resource against the base definitions and any");
      System.out.println("profiles declared in the resource (Resource.meta.profile) or specified on the ");
      System.out.println("command line");
      System.out.println("");
      System.out.println("The FHIR validation tool validates a FHIR resource or bundle.");
      System.out.println("Schema and schematron checking is performed, then some additional checks are performed. ");
      System.out.println("* XML & Json (FHIR versions 1.0, 1.4, 3.0, 4.0, "+Constants.VERSION_MM+")");
      System.out.println("* Turtle (FHIR versions 3.0, 4.0, "+Constants.VERSION_MM+")");
      System.out.println("");
      System.out.println("If requested, instances will also be verified against the appropriate schema");
      System.out.println("W3C XML Schema, JSON schema or ShEx, as appropriate");
      System.out.println("");
      System.out.println("Usage: org.hl7.fhir.r5.validation.ValidationEngine (parameters)");
      System.out.println("");
      System.out.println("The following parameters are supported:");
      System.out.println("[source]: a file, url, directory or pattern for resources to validate.  At");
      System.out.println("    least one source must be declared.  If there is more than one source or if");
      System.out.println("    the source is other than a single file or url and the output parameter is");
      System.out.println("    used, results will be provided as a Bundle.");
      System.out.println("    Patterns are limited to a directory followed by a filename with an embedded");
      System.out.println("    asterisk.  E.g. foo*-examples.xml or someresource.*, etc.");
      System.out.println("-version [ver]: The FHIR version to use. This can only appear once. ");
      System.out.println("    valid values 1.0 | 1.4 | 3.0 | "+VersionUtilities.CURRENT_VERSION+" or 1.0.2 | 1.4.0 | 3.0.2 | 4.0.1 | "+VersionUtilities.CURRENT_FULL_VERSION);
      System.out.println("    Default value is  "+VersionUtilities.CURRENT_VERSION);
      System.out.println("-ig [package|file|folder|url]: an IG or profile definition to load. Can be ");
      System.out.println("     the URL of an implementation guide or a package ([id]-[ver]) for");
      System.out.println("     a built implementation guide or a local folder that contains a");
      System.out.println("     set of conformance resources.");
      System.out.println("     No default value. This parameter can appear any number of times");
      System.out.println("-tx [url]: the [base] url of a FHIR terminology service");
      System.out.println("     Default value is http://tx.fhir.org. This parameter can appear once");
      System.out.println("     To run without terminology value, specific n/a as the URL");
      System.out.println("-txLog [file]: Produce a log of the terminology server operations in [file]");
      System.out.println("     Default value is not to produce a log");
      System.out.println("-profile [url]: the canonical URL to validate against (same as if it was ");
      System.out.println("     specified in Resource.meta.profile). If no profile is specified, the ");
      System.out.println("     resource is validated against the base specification. This parameter ");
      System.out.println("     can appear any number of times.");
      System.out.println("     Note: the profile (and it's dependencies) have to be made available ");
      System.out.println("     through one of the -ig parameters. Note that package dependencies will ");
      System.out.println("     automatically be resolved");
      System.out.println("-questionnaire [file|url}: the location of a questionnaire. If provided, then the validator will validate");
      System.out.println("     any QuestionnaireResponse that claims to match the Questionnaire against it");
      System.out.println("     no default value. This parameter can appear any number of times");
      System.out.println("-output [file]: a filename for the results (OperationOutcome)");
      System.out.println("     Default: results are sent to the std out.");
      System.out.println("-debug");
      System.out.println("     Produce additional information about the loading/validation process");
      System.out.println("-recurse");
      System.out.println("     Look in subfolders when -ig refers to a folder");
      System.out.println("-sct");
      System.out.println("     Specify the edition of SNOMED CT to use. Valid Choices:");
      System.out.println("       intl | us | uk | au | nl | ca | se | dk | es");
      System.out.println("     tx.fhir.org only supports a subset. To add to this list or tx.fhir.org");
      System.out.println("     ask on https://chat.fhir.org/#narrow/stream/179202-terminology");
      System.out.println("-native: use schema for validation as well");
      System.out.println("     * XML: w3c schema+schematron");
      System.out.println("     * JSON: json.schema");
      System.out.println("     * RDF: SHEX");
      System.out.println("     Default: false");
      System.out.println("-language: [lang]");
      System.out.println("     The language to use when validating coding displays - same value as for xml:lang");
      System.out.println("     Not used if the resource specifies language");
      System.out.println("     Default: no specified language");
      System.out.println("-strictExtensions: If present, treat extensions not defined within the specified FHIR version and any");
      System.out.println("     referenced implementation guides or profiles as errors.  (Default is to only raise information messages.)");
      System.out.println("-hintAboutNonMustSupport: If present, raise hints if the instance contains data elements that are not");
      System.out.println("     marked as mustSupport=true.  Useful to identify elements included that may be ignored by recipients");
      System.out.println("");
      System.out.println("The validator also supports the param -proxy=[address]:[port] for if you use a proxy");
      System.out.println("");
      System.out.println("Parameters can appear in any order");
      System.out.println("");
      System.out.println("Alternatively, you can use the validator to execute a transformation as described by a structure map.");
      System.out.println("To do this, you must provide some additional parameters:");
      System.out.println("");
      System.out.println(" -transform [map]");
      System.out.println("");
      System.out.println("* [map] the URI of the map that the transform starts with");
      System.out.println("");
      System.out.println("Any other dependency maps have to be loaded through an -ig reference ");
      System.out.println("");
      System.out.println("-transform uses the parameters -defn, -txserver, -ig (at least one with the map files), and -output");
      System.out.println("");
      System.out.println("Alternatively, you can use the validator to generate narrative for a resource.");
      System.out.println("To do this, you must provide a specific parameter:");
      System.out.println("");
      System.out.println(" -narrative");
      System.out.println("");
      System.out.println("-narrative requires the parameters -defn, -txserver, -source, and -output. ig and profile may be used");
      System.out.println("");
      System.out.println("Alternatively, you can use the validator to convert a resource or logical model.");
      System.out.println("To do this, you must provide a specific parameter:");
      System.out.println("");
      System.out.println(" -convert");
      System.out.println("");
      System.out.println("-convert requires the parameters -source and -output. ig may be used to provide a logical model");
      System.out.println("");
      System.out.println("Alternatively, you can use the validator to evaluate a FHIRPath expression on a resource or logical model.");
      System.out.println("To do this, you must provide a specific parameter:");
      System.out.println("");
      System.out.println(" -fhirpath [FHIRPath]");
      System.out.println("");
      System.out.println("* [FHIRPath] the FHIRPath expression to evaluate");
      System.out.println("");
      System.out.println("-fhirpath requires the parameters -source. ig may be used to provide a logical model");
      System.out.println("");
      System.out.println("Finally, you can use the validator to generate a snapshot for a profile.");
      System.out.println("To do this, you must provide a specific parameter:");
      System.out.println("");
      System.out.println(" -snapshot");
      System.out.println("");
      System.out.println("-snapshot requires the parameters -defn, -txserver, -source, and -output. ig may be used to provide necessary base profiles");
    } else if (hasParam(args, "-compare")) {
      System.out.print("Arguments:");
      for (String s : args)
        System.out.print(s.contains(" ") ? " \""+s+"\"" : " "+s);
      System.out.println();
      System.out.println("Directories: Current = "+System.getProperty("user.dir")+", Package Cache = "+PackageCacheManager.userDir());

      String dest =  getParam(args, "-dest");
      if (dest == null)
        System.out.println("no -dest parameter provided");
      else if (!new File(dest).isDirectory())
        System.out.println("Specified destination (-dest parameter) is not valid: \""+dest+"\")");
      else {
        // first, prepare the context
        String v = getParam(args, "-version");
        if (v == null) {
          v = "current";
          for (int i = 0; i < args.length; i++) {
            if ("-ig".equals(args[i])) {
              if (i+1 == args.length)
                throw new Error("Specified -ig without indicating ig file");
              else {
                String n = args[i+1];
                if (n.startsWith("hl7.fhir.core#")) {
                  v = VersionUtilities.getCurrentPackageVersion(n.substring(14));
                } else if (n.startsWith("hl7.fhir.r2.core#") || n.equals("hl7.fhir.r2.core")) {
                  v = "1.0";
                } else if (n.startsWith("hl7.fhir.r2b.core#") || n.equals("hl7.fhir.r2b.core")) {
                  v = "1.4";
                } else if (n.startsWith("hl7.fhir.r3.core#") || n.equals("hl7.fhir.r3.core")) {
                  v = "3.0";
                } else if (n.startsWith("hl7.fhir.r4.core#") || n.equals("hl7.fhir.r4.core")) {
                  v = "4.0";
                } else if (n.startsWith("hl7.fhir.r5.core#") || n.equals("hl7.fhir.r5.core")) {
                  v = "current";
                }
              }
            }
          }
        } else if ("1.0".equals(v)) {
          v = "1.0";
        } else if ("1.4".equals(v)) {
          v = "1.4";
        } else if ("3.0".equals(v)) {
          v = "3.0";
        } else if ("4.0".equals(v)) {
          v = "4.0";
        } else if (v.startsWith(Constants.VERSION)) {
          v = "current";
        }
        String definitions = VersionUtilities.packageForVersion(v)+"#"+v;
        System.out.println("Loading (v = "+v+", tx server http://tx.fhir.org)");
        ValidationEngine validator = new ValidationEngine(definitions, "http://tx.fhir.org", null, FhirPublication.fromCode(v));
        for (int i = 0; i < args.length; i++) {
          if ("-ig".equals(args[i])) {
            if (i+1 == args.length)
              throw new Error("Specified -ig without indicating ig file");
            else {
              String s = args[++i];
              if (!s.startsWith("hl7.fhir.core-")) {
                System.out.println("Load Package: "+s);
                validator.loadIg(s, true);
              }
            }
          }
        }
        // ok now set up the comparison
        String left = getParam(args, "-left");
        String right = getParam(args, "-right");
        Resource resLeft =  validator.getContext().fetchResource(Resource.class, left);
        Resource resRight = validator.getContext().fetchResource(Resource.class, right);
        if (resLeft == null) {
          System.out.println("Unable to locate left resource " +left);
        }
        if (resRight == null) {
          System.out.println("Unable to locate right resource " +right);
        }
        if (resLeft != null && resRight != null) {
          if (resLeft instanceof StructureDefinition && resRight instanceof StructureDefinition) {
            System.out.println("Comparing StructureDefinitions "+left+" to "+right);
            ProfileComparer pc = new ProfileComparer(validator.getContext(), dest);
            StructureDefinition sdL = (StructureDefinition) resLeft;
            StructureDefinition sdR = (StructureDefinition) resRight;
            pc.compareProfiles(sdL, sdR);
            System.out.println("Generating output to "+dest+"...");
            File htmlFile = new File(pc.generate());
            Desktop.getDesktop().browse(htmlFile.toURI());
            System.out.println("Done");
          } else if (resLeft instanceof CapabilityStatement && resRight instanceof CapabilityStatement) {
            String nameLeft = chooseName(args, "leftName", (MetadataResource) resLeft);
            String nameRight = chooseName(args, "rightName", (MetadataResource) resRight);
            System.out.println("Comparing CapabilityStatements "+left+" to "+right);
            CapabilityStatementUtilities pc = new CapabilityStatementUtilities(validator.getContext(), dest, new KeyGenerator("http://fhir.org/temp/"+UUID.randomUUID().toString().toLowerCase()));
            CapabilityStatement capL = (CapabilityStatement) resLeft;
            CapabilityStatement capR = (CapabilityStatement) resRight;
            CapabilityStatementComparisonOutput output = pc.isCompatible(nameLeft, nameRight, capL, capR);
            
            String destTxt = Utilities.path(dest, "output.txt");
            System.out.println("Generating output to "+destTxt+"...");
            StringBuilder b = new StringBuilder();
            for (ValidationMessage msg : output.getMessages()) {
              b.append(msg.summary());
              b.append("\r\n");
            }
            TextFile.stringToFile(b.toString(), destTxt);
            new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(dest, "CapabilityStatement-union.xml")), output.getSuperset());
            new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(dest, "CapabilityStatement-intersection.xml")), output.getSubset());
            new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(dest, "OperationOutcome-issues.xml")), output.getOutcome());
            new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(dest, "CapabilityStatement-union.json")), output.getSuperset());
            new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(dest, "CapabilityStatement-intersection.json")), output.getSubset());
            new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(Utilities.path(dest, "OperationOutcome-issues.json")), output.getOutcome());
            
            String destHtml = Utilities.path(dest, "index.html");
            File htmlFile = new File(destHtml);
            Desktop.getDesktop().browse(htmlFile.toURI());
            System.out.println("Done");
          } else 
            System.out.println("Unable to compare left resource " +left+" ("+resLeft.fhirType()+") with right resource "+right+" ("+resRight.fhirType()+")");
        }
      }
    } else {
      System.out.print("Arguments:");
      for (String s : args)
        System.out.print(s.contains(" ") ? " \""+s+"\"" : " "+s);
      System.out.println();
      System.out.println("Directories: Current = "+System.getProperty("user.dir")+", Package Cache = "+PackageCacheManager.userDir());

      String map = null;
      List<String> igs = new ArrayList<String>();
      List<String> questionnaires = new ArrayList<String>();
      String txServer = "http://tx.fhir.org";
      boolean doNative = false;
      boolean anyExtensionsAllowed = true;
      boolean hintAboutNonMustSupport = false;
      boolean recursive = false;
      List<String> profiles = new ArrayList<String>();
      EngineMode mode = EngineMode.VALIDATION;
      String output = null;
      List<String> sources= new ArrayList<String>();
      Map<String, String> locations = new HashMap<String, String>();
      String sv = "current";
      String txLog = null;
      String mapLog = null;
      String lang = null;
      String fhirpath = null;
      String snomedCT = "900000000000207008";
      boolean doDebug = false;

      // load the parameters - so order doesn't matter
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-version"))  {
          sv = args[++i];
          sv = VersionUtilities.getCurrentPackageVersion(sv);
        } else if (args[i].equals("-output")) {
          if (i+1 == args.length)
            throw new Error("Specified -output without indicating output file");
          else
            output = args[++i];
        } else if (args[i].equals("-profile")) {
          String p = null;
          if (i+1 == args.length)
            throw new Error("Specified -profile without indicating profile source");
          else {
            p = args[++i];
            profiles.add(p);
          }
          if (p != null && i+1 < args.length && args[i+1].equals("@")) {
            i++;
            if (i+1 == args.length)
              throw new Error("Specified -profile with @ without indicating profile location");
            else
              locations.put(p, args[++i]);
          }
        } else if (args[i].equals("-questionnaire")) {
          if (i+1 == args.length)
            throw new Error("Specified -questionnaire without indicating questionnaire file");
          else
            questionnaires.add(args[++i]);
        } else if (args[i].equals("-native")) {
          doNative = true;          
        } else if (args[i].equals("-debug")) {
          doDebug = true;
        } else if (args[i].equals("-sct")) {
          String s = args[++i];
          if ("intl".equalsIgnoreCase(s))
            snomedCT = "900000000000207008";
          else if ("us".equalsIgnoreCase(s))
            snomedCT = "731000124108";
          else if ("uk".equalsIgnoreCase(s))
            snomedCT = "999000041000000102";
          else if ("au".equalsIgnoreCase(s))
            snomedCT = "32506021000036107";
          else if ("ca".equalsIgnoreCase(s))
            snomedCT = "20611000087101";
          else if ("nl".equalsIgnoreCase(s))
            snomedCT = "11000146104";
          else if ("se".equalsIgnoreCase(s))
            snomedCT = "45991000052106";
          else if ("es".equalsIgnoreCase(s))
            snomedCT = "449081005";
          else if ("dk".equalsIgnoreCase(s))
            snomedCT = "554471000005108";
          else 
            throw new Error("Snomed edition '"+s+"' not known");            
        } else if (args[i].equals("-recurse")) {
          recursive = true;
        } else if (args[i].equals("-strictExtensions")) {
          anyExtensionsAllowed = false;
        } else if (args[i].equals("-hintAboutNonMustSupport")) {
          hintAboutNonMustSupport = true;
        } else if (args[i].equals("-transform")) {
          map = args[++i];
          mode = EngineMode.TRANSFORM;
        } else if (args[i].equals("-narrative")) {
          mode = EngineMode.NARRATIVE;
        } else if (args[i].equals("-snapshot")) {
          mode = EngineMode.SNAPSHOT;
        } else if (args[i].equals("-scan")) {
          mode = EngineMode.SCAN;
        } else if (args[i].equals("-tx")) {
          if (i+1 == args.length)
            throw new Error("Specified -tx without indicating terminology server");
          else
            txServer = "n/a".equals(args[++i]) ? null : args[i];
        } else if (args[i].equals("-txLog")) {
          if (i+1 == args.length)
            throw new Error("Specified -txLog without indicating file");
          else
            txLog = args[++i];
        } else if (args[i].equals("-log")) {
          if (i+1 == args.length)
            throw new Error("Specified -log without indicating file");
          else
            mapLog = args[++i];
        } else if (args[i].equals("-language")) {
          if (i+1 == args.length)
            throw new Error("Specified -language without indicating language");
          else
            lang = args[++i];
        } else if (args[i].equals("-ig") || args[i].equals("-defn")) {
          if (i+1 == args.length)
            throw new Error("Specified "+args[i]+" without indicating ig file");
          else {
            String s = args[++i];
            if (s.equals("hl7.fhir.core")) {
              sv = "current";
            } else if (s.startsWith("hl7.fhir.core#")) {
              sv = VersionUtilities.getCurrentPackageVersion(s.substring(14));
            } else if (s.startsWith("hl7.fhir.r2.core#") || s.equals("hl7.fhir.r2.core")) {
              sv = "1.0";
            } else if (s.startsWith("hl7.fhir.r2b.core#") || s.equals("hl7.fhir.r2b.core")) {
              sv = "1.4";
            } else if (s.startsWith("hl7.fhir.r3.core#") || s.equals("hl7.fhir.r3.core")) {
              sv = "3.0";
            } else if (s.startsWith("hl7.fhir.r4.core#") || s.equals("hl7.fhir.r4.core")) {
              sv = "4.0";
            } else if (s.startsWith("hl7.fhir.r5.core#") || s.equals("hl7.fhir.r5.core")) {
              sv = "current";
            }
            else
              igs.add(s);
          }
        } else if (args[i].equals("-map")) {
          if (map == null) {
            if (i+1 == args.length)
              throw new Error("Specified -map without indicating map file");
            else
              map = args[++i];
          } else {
            throw new Exception("Can only nominate a single -map parameter");
          }
        } else if (args[i].startsWith("-x")) {
          i++;
        } else if (args[i].equals("-convert")) {
          mode = EngineMode.CONVERT;
        } else if (args[i].equals("-fhirpath")) {
            mode = EngineMode.FHIRPATH;
            if (fhirpath == null)
              if (i+1 == args.length)
                throw new Error("Specified -fhirpath without indicating a FHIRPath expression");
              else
                fhirpath = args[++i];
            else
              throw new Exception("Can only nominate a single -fhirpath parameter");
        } else {
          sources.add(args[i]);
        }
      }
      if  (sources.isEmpty())
        throw new Exception("Must provide at least one source file");

      // Comment this out because definitions filename doesn't necessarily contain version (and many not even be 14 characters long).  Version gets spit out a couple of lines later after we've loaded the context
      String definitions = VersionUtilities.packageForVersion(sv)+"#"+VersionUtilities.getCurrentVersion(sv);
      System.out.println("  .. FHIR Version "+sv+", definitions from "+definitions);
      System.out.println("  .. connect to tx server @ "+txServer);
      ValidationEngine validator = new ValidationEngine(definitions, txServer, txLog, FhirPublication.fromCode(sv));
      validator.setDebug(doDebug);
      System.out.println("    (v"+validator.getContext().getVersion()+")");
      if (sv != null)
        validator.setVersion(sv);
      for (String src : igs) {
        System.out.println("+  .. load IG from "+src);
        validator.loadIg(src, recursive);
      }
      validator.setQuestionnaires(questionnaires);
      validator.setNative(doNative);
      validator.setHintAboutNonMustSupport(hintAboutNonMustSupport);
      validator.setAnyExtensionsAllowed(anyExtensionsAllowed);
      validator.setLanguage(lang);
      validator.setSnomedExtension(snomedCT);

      IParser x;
      if (output != null && output.endsWith(".json"))
        x = new JsonParser();
      else
        x = new XmlParser();
      x.setOutputStyle(OutputStyle.PRETTY);

      if (mode == EngineMode.TRANSFORM) {
        if  (sources.size() > 1)
          throw new Exception("Can only have one source when doing a transform (found "+sources+")");
        if  (txServer == null)
          throw new Exception("Must provide a terminology server when doing a transform");
        if  (map == null)
          throw new Exception("Must provide a map when doing a transform");
        try {
          validator.setMapLog(mapLog);
          org.hl7.fhir.r5.elementmodel.Element r = validator.transform(sources.get(0), map);
          System.out.println(" ...success");
          if (output != null) {
            FileOutputStream s = new FileOutputStream(output);
            if (output != null && output.endsWith(".json"))
              new org.hl7.fhir.r5.elementmodel.JsonParser(validator.getContext()).compose(r, s, OutputStyle.PRETTY, null);
            else
              new org.hl7.fhir.r5.elementmodel.XmlParser(validator.getContext()).compose(r, s, OutputStyle.PRETTY, null);
            s.close();
          }
        } catch (Exception e) {
          System.out.println(" ...Failure: "+e.getMessage());
          e.printStackTrace();
        }
      } else if (mode == EngineMode.NARRATIVE) {
        DomainResource r = validator.generate(sources.get(0), sv);
        System.out.println(" ...generated narrative successfully");
        if (output != null) {
          validator.handleOutput(r, output, sv);
        }
      } else if (mode == EngineMode.SNAPSHOT) {        
        StructureDefinition r = validator.snapshot(sources.get(0), sv);
        System.out.println(" ...generated snapshot successfully");
        if (output != null) {
          validator.handleOutput(r, output, sv);
        }
      } else if (mode == EngineMode.CONVERT) {
        validator.convert(sources.get(0), output);
        System.out.println(" ...convert");
      } else if (mode == EngineMode.FHIRPATH) {
        System.out.println(" ...evaluating "+fhirpath);
        System.out.println(validator.evaluateFhirPath(sources.get(0), fhirpath));
      } else {
        if  (definitions == null)
          throw new Exception("Must provide a defn when doing validation");
        for (String s : profiles) {
          if (!validator.getContext().hasResource(StructureDefinition.class, s) && !validator.getContext().hasResource(ImplementationGuide.class, s)) {
            System.out.println("Fetch Profile from "+s);
            validator.loadProfile(locations.getOrDefault(s, s));
          }
        }
        if (mode == EngineMode.SCAN) {
          if (Utilities.noString(output))
            throw new Exception("Output parameter required when scanning");
          if (!(new File(output).isDirectory()))
            throw new Exception("Output '"+output+"' must be a directory when scanning");
          System.out.println("  .. scan "+sources+" against loaded IGs");
          Set<String> urls = new HashSet<>();
          for (ImplementationGuide ig : validator.getContext().allImplementationGuides()) {
            if (ig.getUrl().contains("/ImplementationGuide") && !ig.getUrl().equals("http://hl7.org/fhir/ImplementationGuide/fhir"))
              urls.add(ig.getUrl());
          }
          List<ScanOutputItem> res = validator.validateScan(sources, urls);
          validator.genScanOutput(output, res);         
          System.out.println("Done. output in "+Utilities.path(output, "scan.html"));
        } else { 
          if (profiles.size() > 0)
            System.out.println("  .. validate "+sources+" against "+profiles.toString());
          else
            System.out.println("  .. validate "+sources);
          validator.prepare(); // generate any missing snapshots
          Resource r = validator.validate(sources, profiles);
          int ec = 0;
          if (output == null) {
            if (r instanceof Bundle)
              for (BundleEntryComponent e : ((Bundle)r).getEntry())
                ec  = displayOO((OperationOutcome)e.getResource()) + ec;
            else
              ec = displayOO((OperationOutcome)r);
          } else {
            FileOutputStream s = new FileOutputStream(output);
            x.compose(s, r);
            s.close();
          }
          System.exit(ec > 0 ? 1 : 0);
        }
      }
    }
  }

  private static String chooseName(String[] args, String name, MetadataResource mr) {
    String s = getParam(args, "-"+name);
    if (Utilities.noString(s))
      s = mr.present();
    return s;
  }

  private static String getGitBuild() {
    return "??";
  }

  private static int displayOO(OperationOutcome oo) {
    int error = 0;
    int warn = 0;
    int info = 0;
    String file = ToolingExtensions.readStringExtension(oo, ToolingExtensions.EXT_OO_FILE);

    for (OperationOutcomeIssueComponent issue : oo.getIssue()) {
      if (issue.getSeverity()==OperationOutcome.IssueSeverity.FATAL || issue.getSeverity()==OperationOutcome.IssueSeverity.ERROR)
        error++;
      else if (issue.getSeverity()==OperationOutcome.IssueSeverity.WARNING)
        warn++;
      else
        info++;
    }

    System.out.println((error==0?"Success...":"*FAILURE* ")+ "validating "+file+": "+" error:"+Integer.toString(error)+" warn:"+Integer.toString(warn)+" info:"+Integer.toString(info));
    for (OperationOutcomeIssueComponent issue : oo.getIssue()) {
      System.out.println(getIssueSummary(issue));
    }
    System.out.println();
    return error;
  }

  private static String getIssueSummary(OperationOutcomeIssueComponent issue) {
    String loc = null;
    if (issue.hasExpression()) {
      int line = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_LINE, -1);
      int col = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_COL, -1);
      loc = issue.getExpression().get(0).asStringValue() + (line >= 0 && col >= 0 ? " (line "+Integer.toString(line)+", col"+Integer.toString(col)+")" : ""); 
    } else if (issue.hasLocation()) {
      loc = issue.getLocation().get(0).asStringValue();
    } else {
      int line = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_LINE, -1);
      int col = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_COL, -1);
      loc = (line >= 0 && col >= 0 ? "line "+Integer.toString(line)+", col"+Integer.toString(col) : "??"); 
    }
    return "  " + issue.getSeverity().getDisplay() + " @ " + loc + " : " + issue.getDetails().getText();
  }


  private static boolean hasParam(String[] args, String param) {
    for (String a : args)
      if (a.equals(param))
        return true;
    return false;
  }

  private static String getParam(String[] args, String param) {
    for (int i = 0; i < args.length - 1; i++)
      if (args[i].equals(param))
        return args[i+1];
    return null;
  }


	private static boolean hasTransformParam(String[] args) {
		for (String s : args) {
			if (s.equals("-transform"))
				return true;
		}
		return false;
	}
}
