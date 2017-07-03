package gate.compound;

import java.util.List;
import java.util.Map;
import java.util.Set;

import gate.Document;
import gate.TextualDocument;
import gate.alignment.Alignment;

/**
 * Compound document is a collection of individual documents. When it is
 * created, it doesn't contain any composite document but one can create it by
 * using the combine PR.
 * 
 * @author niraj
 * 
 */
public interface CompoundDocument extends TextualDocument {

	/**
	 * Returns the currently selected document
	 */
	public Document getCurrentDocument();

	/**
	 * Given a document ID, it should return its respective document
	 */
	public Document getDocument(String docid);

	/**
	 * Given a language of the document, it should set the respective document
	 * to be the current document
	 */
	public void setCurrentDocument(String documentID);

	/**
	 * Given a language of the document, it should set the respective document
	 * to be the current document
	 */
	public void removeDocument(String documentID);

	/**
	 * Add a new document
	 */
	public void addDocument(String documentID, Document document);

	/**
	 * The method should return a map of documents where the key is the language
	 * and values is a respective document
	 */
	public Map<String,Document> getDocuments();

	/**
	 * The method should return all document IDs
	 */
	public List<String> getDocumentIDs();

	/**
	 * Should be called to add a new member
	 */
	public void addCompoundDocumentListener(CompoundDocumentListener listener);

	/**
	 * Should be called to remove a document member
	 */
	public void removeCompoundDocumentListener(CompoundDocumentListener listener);
	
	/**
	 * The alignment object contains information about the alignment of text.
	 * If the alignment with given name doesn't exist, this method creates an
	 * empty alignment object and returns it.
	 */
	public Alignment getAlignmentInformation(String alignmentFeatureName);
	
	/**
	 * This method returns all available alignment feature names
	 */
	public Set<String> getAllAlignmentFeatureNames();
	
	/**
	 * Alignment object with the provided feature name is deleted
	 */
	public void removeAlignmentInformation(String alignmentFeatureName);
}