package datchat.client;

import java.awt.Toolkit;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * A document for a text component that only accepts a specified number of characters.
 *
 * @author adam
 * @date May 26, 2012
 */
public class LimitedLengthDocument extends PlainDocument {

    /** The maximum length allowed in this document. */
    private final int m_maxLength;

    /**
     * Constructor.  Creates a new LimitedLengthDocument.
     */
    public LimitedLengthDocument(){
        this(-1);
    }

    /**
     * Constructor.  Creates a new LimitedLengthDocument.
     * @param chars max the number of characters allowed.
     */
    public LimitedLengthDocument(int chars){
        super();
        m_maxLength = chars;
    }

    /**
     * {@inheritDoc}
     * Beeps if any non integer value is attempted to be inserted into this document.
     */
    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException{
        if (str != null) {
                // If a max length has been specified...
                if (m_maxLength > 0){

                    // Check if the new insert will extend beyond the max allowable length.
                    if (getLength() + str.length() > m_maxLength){
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        super.insertString(offs, str, a);
                    }
                } else {
                    // If no length was specified, insert the string.
                    super.insertString(offs, str, a);
                }
        }
    }
}