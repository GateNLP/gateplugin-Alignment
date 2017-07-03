package gate.alignment.gui;

import gate.alignment.AlignmentActionInitializationException;

/**
 * Implementers of this interface are the resources wishing to publish
 * data in the Alignment editor. The registered instance of
 * DataPublisherAction appears as one of the tabs in the Alignment
 * Editor. This is useful for displaying various statistics, such as the
 * collected entries, model parameters etc.
 * 
 * @author niraj
 */
public interface DataPublisherAction {

  /**
   * This method should be used for initializing any resources required
   * by the execute() method. This method is called whenever it loaded
   * for the first time.
   * 
   * @param args
   * @throws AlignmentActionInitializationException
   */
  public void init(String[] args) throws AlignmentActionInitializationException;

  /**
   * This method should free up the memory by releasing any resources
   * occupied this method. It is called just before the alignment editor
   * is closed.
   */
  public void cleanup();

  /**
   * data model used for obtaining data to be published.
   */
  public void setDataModel(DefaultDataModel ddm);

  /**
   * Number of columns.
   */
  public int getColumnCount();

  /**
   * Number of rows
   */
  public int getRowCount();

  /**
   * Gets the value at the specified row and column
   */
  public String getValueAt(int rowIndex, int columnIndex);

  /**
   * Column title
   */
  public String getColumnName(int column);

  /**
   * Title for the table (this one appears in the tab heading)
   */
  public String getTableTitle();
}
