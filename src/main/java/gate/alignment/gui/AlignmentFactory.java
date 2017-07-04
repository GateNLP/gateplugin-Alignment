package gate.alignment.gui;

import gate.Annotation;
import gate.AnnotationSet;
import gate.compound.CompoundDocument;

/**
 * This class provides various methods that help in alignment process.
 * 
 * @author niraj
 */
public class AlignmentFactory {

  /**
   * Alignments are stored as a document feature by default under this feature name 
   */
  public static final String ALIGNMENT_FEATURE_NAME = "alignment";

  /**
   * The document that is being processed for alignment
   */
  protected CompoundDocument compoundDocument;

  /**
   * An instance of IteratingMethod that decides iterating order
   */
  protected IteratingMethod iteratingMethod;

  /**
   * ID of the source document
   */
  private String srcDocumentID;

  /**
   * ID of the target document
   */
  private String tgtDocumentID;

  /**
   * AlignmentFactory makes alignment easier
   * 
   * @param alignedDocument -&gt; document where we want to achieve
   *          alignment
   */
  public AlignmentFactory(CompoundDocument alignedDocument,
          String srcDocumentId, String tgtDocumentId, String srcInputAS,
          String tgtInputAS, String srcTokenAnnotationType,
          String tgtTokenAnnotationType, String srcUnitAnnotationType,
          String tgtUnitAnnotationType, String iteratingMethodClassName)
          throws Exception {

    this.compoundDocument = alignedDocument;
    this.srcDocumentID = srcDocumentId;
    this.tgtDocumentID = tgtDocumentId;

    iteratingMethod = (IteratingMethod)Class.forName(iteratingMethodClassName)
            .newInstance();
    iteratingMethod.init(alignedDocument, srcDocumentId, tgtDocumentId,
            srcInputAS, tgtInputAS, srcTokenAnnotationType,
            tgtTokenAnnotationType, srcUnitAnnotationType,
            tgtUnitAnnotationType);
  }

  /**
   * Gets the text for the given annotation
   */
  public String getText(Annotation annot, String documentID) {
    return iteratingMethod.getText(annot, documentID);
  }

  /**
   * Gets the underlying annotations with the given type under the given annotation
   * @param documentID - id of the document that the annot belongs to.
   */
  public AnnotationSet getUnderlyingAnnotations(Annotation annot,
          String documentID, String tokenAnnotationType) {
    return iteratingMethod.getUnderlyingAnnotations(annot, documentID, tokenAnnotationType);
  }

  /**
   * Returns the next possible pair
   */
  public Pair next() {
    return iteratingMethod.next();
  }

  /**
   * Returns the previous pair
   */
  public Pair previous() {
    return iteratingMethod.previous();
  }

  /**
   * Returns the current pair
   */
  public Pair current() {
    return iteratingMethod.current();
  }

  /**
   * Returns true if there is any next pair available 
   */
  public boolean hasNext() {
    return iteratingMethod.hasNext();
  }

  /**
   * Returns true if there is any previous pair available
   */
  public boolean hasPrevious() {
    return iteratingMethod.hasPrevious();
  }

  /**
   * ID of the source document
   */
  public String getSrcDocumentID() {
    return srcDocumentID;
  }

  /**
   * ID of the target document
   */
  public String getTgtDocumentID() {
    return tgtDocumentID;
  }
}