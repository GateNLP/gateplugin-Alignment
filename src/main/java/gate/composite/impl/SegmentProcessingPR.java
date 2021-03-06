package gate.composite.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.LanguageAnalyser;
import gate.ProcessingResource;
import gate.composite.CombiningMethod;
import gate.composite.CombiningMethodException;
import gate.composite.CompositeDocument;
import gate.compound.CompoundDocument;
import gate.compound.impl.CompoundDocumentImpl;
import gate.corpora.DocumentImpl;
import gate.creole.AbstractController;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.GateRuntimeException;
import gate.util.OffsetComparator;
import gate.util.Out;

/**
 * As the name suggests, the PR is useful processing segments of the text. Given
 * a analyser, annotation type and a document, this PR creates a composite
 * documents for every annotation with type as specified by the &lt;annotation
 * type&gt;. Since the composite documents are linked with their original
 * documents, when the PR processing the composite document, the composite
 * document takes care of transferring relevant annotations back to the original
 * document. This is a good way of processing just a segment of a document.
 * 
 * @author niraj
 */
public class SegmentProcessingPR 
        extends AbstractLanguageAnalyser 
        implements ProcessingResource, ControllerAwarePR {

  private static final long serialVersionUID = 8528040629940314055L;

  /**
   * Controller that should be used to process segments.
   */
  private LanguageAnalyser analyser;

  /**
   * annotation type that the segment is annotated with.
   */
  private String segmentAnnotationType;

  /**
   * Only the annotation that has the specified feature should be considered for
   * annotating.
   */
  private String segmentAnnotationFeatureName;

  /**
   * Only the annotation that has the feature specified as
   * segmentAnnotationFeatureName with the value of
   * segmentAnnotaitonFeatureValue then only the annotation is considered for
   * annotating.
   */
  private String segmentAnnotationFeatureValue;

  /**
   * Annotation set that contains the segment annotation and the annotations to
   * be copied to the composite document.
   */
  private String inputASName;

  private boolean debug = false;

  /**
   * Should be called to execute this PR on a document.
   * 
   * @throws gate.creole.ExecutionException if error
   */
  @Override
  public void execute() throws ExecutionException {
    // if no document provided
    if(document == null) { throw new ExecutionException("Document is null!"); }
    // annotation set to use
    AnnotationSet set =
        inputASName == null || inputASName.trim().length() == 0 ? document
            .getAnnotations() : document.getAnnotations(inputASName);
    AnnotationSet segmentSet = set.get(segmentAnnotationType);
    if(set.isEmpty()) {
      Out.prln("Could not find annotations of type: " + segmentAnnotationType
          + " in the document: " + document.getName());
      return;
    }
    
    // a combining method that creates a composite document with the
    // annotation as identified by the annotation id
    CombiningMethod combiningMethodInst = new CombineFromAnnotID();
    
    CompoundDocument compoundDoc = new CompoundDocumentImpl();
    // initializing an empty compound document
    try {
      compoundDoc.init();
    } catch(ResourceInstantiationException e) {
      throw new ExecutionException(e);
    }
    
    String originalDocument = document.getName();
    if(document instanceof CompoundDocument) {
      if(debug) {
        System.out
            .println("Document is a compound document and using the memeber \""
                + document.getName() + "\" for processing");
      }
      compoundDoc.addDocument(document.getName(),
          ((CompoundDocument)document).getCurrentDocument());
    } else {
      if(debug) {
        System.out.println("Document is a normal GATE document with name \""
            + document.getName() + "\"");
      }
      // add the current document as a member of the compound document
      compoundDoc.addDocument(document.getName(), document);
    }
    Corpus tempCorpus = null;
    Corpus oldCorpus = analyser.getCorpus();
    Document oldDoc = analyser.getDocument();

    try {
      Map<String, Object> map = new HashMap<>();
      map.put(CombineFromAnnotID.INPUT_AS_NAME_FEATURE_NAME, inputASName);
      map.put(CombineFromAnnotID.DOCUMENT_ID_FEATURE_NAME, document.getName());
      FeatureMap hideMap = Factory.newFeatureMap();
      Gate.setHiddenAttribute(hideMap, true);
      tempCorpus =
          (Corpus)Factory.createResource("gate.corpora.CorpusImpl",
              Factory.newFeatureMap(), hideMap, "compoundDocCorpus");
      tempCorpus.add(compoundDoc);
      analyser.setDocument(compoundDoc);
      analyser.setCorpus(tempCorpus);
      List<Annotation> segmentList = new ArrayList<>(segmentSet);
      Collections.sort(segmentList, new OffsetComparator());
      for(Annotation annotation : segmentList) {
        if(debug) {
          System.out.println("Processing annotation" + annotation.getType()
              + "=>" + annotation.getId());
        }
        // only consider the annotation if it has a specific feature and a value
        if(segmentAnnotationFeatureName != null
            && segmentAnnotationFeatureName.length() != 0
            && segmentAnnotationFeatureValue != null
            && segmentAnnotationFeatureValue.length() != 0) {
          Object value =
              annotation.getFeatures().get(segmentAnnotationFeatureName);
          if(value == null || !value.equals(segmentAnnotationFeatureValue)) {
            continue;
          }
        }
        String nameForCompositeDoc = "Composite" + Gate.genSym();
        map.put(CombineFromAnnotID.ANNOTATION_ID_FEATURE_NAME,
            annotation.getId());
        CompositeDocument compositeDoc = null;
        try {
          if(debug) {
            System.out.println("Creating temp composite document:"
                + nameForCompositeDoc);
          }
          compositeDoc = combiningMethodInst.combine(compoundDoc, map);
          compositeDoc.setName(nameForCompositeDoc);
          
          // try and make sure any annotations created in the segment will have
          // IDs that are valid in the original document
          if(document instanceof DocumentImpl) {
            ((CompositeDocumentImpl)compositeDoc)
                .setNextAnnotationId(((DocumentImpl)document)
                    .peakAtNextAnnotationId());
          }
          
          compoundDoc.addDocument(nameForCompositeDoc, compositeDoc);
          // change focus to composite document
          compoundDoc.setCurrentDocument(nameForCompositeDoc);
          // now run the application on the composite document
          // The analyser could be a PR or a controller. If it is a controller
          // it uses the heuristic that if the document of the controller is 
          // not null, the controller knows it is being run as a subpipeline 
          // in which case the controller started/finished callbacks are not
          // invoked. Instead we have to invoke them in our own callback code.
          // If The application is a PR, we have to invoke the callbacks of 
          // the PR in our own callback code.
          analyser.execute();
        } catch(CombiningMethodException e) {
          throw new ExecutionException(e);
        } finally {
                    
          // finally get rid of the composite document
          compoundDoc.removeDocument(nameForCompositeDoc);
          if(compositeDoc != null) {
            gate.Factory.deleteResource(compositeDoc);
          }
        }
      }
    } catch(ResourceInstantiationException e) {
      throw new ExecutionException(e);
    } finally {
      // make sure you are resetting the reference
      analyser.setCorpus(oldCorpus);
      analyser.setDocument(oldDoc);
      
      Factory.deleteResource(compoundDoc);

      compoundDoc.removeDocument(originalDocument);
      if(tempCorpus != null) {
        // clear the corpus before deleting it
        tempCorpus.clear();
        gate.Factory.deleteResource(tempCorpus);
      }
    }
  }

  /**
   * Gets the set analyser. The analyser is used for processing the segmented
   * document.
   */
  public LanguageAnalyser getAnalyser() {
    return analyser;
  }

  /**
   * Sets the analyser. The analyser is used for processing the segmented
   * document.
   * 
   * @param analyser
   */
  public void setAnalyser(LanguageAnalyser analyser) {
    this.analyser = analyser;
  }

  /**
   * Sets the analyser. The analyser is used for processing the segmented
   * document.
   */
  @Deprecated
  public void setController(CorpusController controller) {
    if(!(controller instanceof LanguageAnalyser)) { throw new GateRuntimeException(
        "controller must be of type LanguageAnalyser"); }
    setAnalyser((LanguageAnalyser)controller);
  }

  /**
   * Annotation type that has been used for segmenting the document. The PR uses
   * annotations of this type to create new composite documents and process them
   * individually.
   */
  public String getSegmentAnnotationType() {
    return segmentAnnotationType;
  }

  /**
   * Annotation type that has been used for segmenting the document. The PR uses
   * annotations of this type to create new composite documents and process them
   * individually.
   */
  public void setSegmentAnnotationType(String segmentAnnotationType) {
    this.segmentAnnotationType = segmentAnnotationType;
  }

  /**
   * Annotation set to use for obtaining segment annotations and the annotations
   * to copy into the composite document.
   */
  public String getInputASName() {
    return inputASName;
  }

  /**
   * Annotation set to use for obtaining segment annotations and the annotations
   * to copy into the composite document.
   */
  public void setInputASName(String inputAS) {
    this.inputASName = inputAS;
  }

  public String getSegmentAnnotationFeatureName() {
    return segmentAnnotationFeatureName;
  }

  public void setSegmentAnnotationFeatureName(
      String segmentAnnotationFeatureName) {
    this.segmentAnnotationFeatureName = segmentAnnotationFeatureName;
  }

  public String getSegmentAnnotationFeatureValue() {
    return segmentAnnotationFeatureValue;
  }

  public void setSegmentAnnotationFeatureValue(
      String segmentAnnotationFeatureValue) {
    this.segmentAnnotationFeatureValue = segmentAnnotationFeatureValue;
  }

  @Override
  public void controllerExecutionStarted(Controller c) throws ExecutionException {
    if(analyser != null) {
      if(analyser instanceof AbstractController) {
        ((AbstractController)analyser).invokeControllerExecutionStarted();
      } else if (analyser instanceof ControllerAwarePR) {
        ((ControllerAwarePR)analyser).controllerExecutionStarted(c);
      }
    }
  }

  @Override
  public void controllerExecutionFinished(Controller c) throws ExecutionException {
    if(analyser != null) {
      if(analyser instanceof AbstractController) {
        ((AbstractController)analyser).invokeControllerExecutionFinished();
      } else if (analyser instanceof ControllerAwarePR) {
        ((ControllerAwarePR)analyser).controllerExecutionFinished(c);
      }
    }
  }

  @Override
  public void controllerExecutionAborted(Controller c, Throwable t) throws ExecutionException {
     if(analyser != null) {
       if(analyser instanceof AbstractController) {
        ((AbstractController)analyser).invokeControllerExecutionAborted(t);
      } else if (analyser instanceof ControllerAwarePR) {
        ((ControllerAwarePR)analyser).controllerExecutionAborted(c, t);
      }
    }

  }

} // class SegmentProcessingPR
