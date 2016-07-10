package datchat.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractListModel;

/**
 * A List model that supports being sorted, (or not) as well as applying a DataFilter to show or hide data.
 *
 * @author adam
 * @param <E>
 */
public class SortableListModel<E extends Comparable> extends AbstractListModel<E> {

    /** The items in this model. */
    private final List<E> m_items;

    /** The items in this model which have passed the filter. */
    private final List<E> m_filteredItems;

    /** True if this model should maintain sorting, false otherwise. */
    private final boolean m_sort;
    
    /** A comparator to use for sorting, null if unset. */
    private Comparator<E> m_comparator;

    /** Creates a new instance of SortedListModel with sorting active. */
    public SortableListModel() {
        this(true);
    }
    
    /**
     * Creates a new SortableListModel with sorting active and a supplied comparator.
     * @param comparator the Comparator to use.  If null, the Element type's default comparison methods will be used.
     */
    public SortableListModel(Comparator<E> comparator) {
        this(true);
        m_comparator = comparator;
    }

    /**
     * Creates a new instance of SortableListModel with sorting either active or not if param 'sort' is true or false.
     * @param sort true if this model should maintain sorting.
     */
    public SortableListModel(boolean sort) {
        m_sort = sort;
        m_items = new ArrayList<>();
        m_filteredItems = new ArrayList<>();
    }

    @Override
    public int getSize() {
        return m_items.size();
    }

    @Override
    public E getElementAt(int i) {
        return m_items.get(i);
    }

    /**
     * Returns a copy of the items in this model as a List.  THIS METHOD ALWAYS RETURNS ALL DATA, NEVER FILTERED DATA.
     * @return a copy of the items in this model.
     */
    public List<E> getAllElements() {
        return new ArrayList<>(m_items);
    }

    public void addAllElements(Collection<E> items) {
        // If no filter is set, just add them all, sort them and update the UI.
        m_items.addAll(items);
        if (m_sort) {
            if (m_comparator != null) {
                Collections.sort(m_items, m_comparator);
            } else {
                Collections.sort(m_items);
            }
        }
        fireContentsChanged(this, 0, m_items.size());
    }

    /**
     * Adds an element to this model and fires necessary events to update the UI.
     * @param e the Element to add.
     */
    public void addElement(E e) {
        // If no filter is set, just update the 'real' data and fire the udpate event.
        m_items.add(e);
        if (m_sort) {
            if (m_comparator != null) {
                Collections.sort(m_items, m_comparator);
            } else {
                Collections.sort(m_items);
            }
        }
        fireContentsChanged(this, m_items.indexOf(e), getSize());
    }

    /** Clears all data from this SortedListModel and fires necessary events. */
    public void clear() {
        int size = getSize();
        m_items.clear();
        m_filteredItems.clear();
        fireIntervalRemoved(this, 0, size);
    }

    public void removeElement(E e) {
        // If no filter is set, just work over the 'real' data.
        if (m_items.contains(e)) {
            int index = m_items.indexOf(e);
            m_items.remove(e);
            fireIntervalRemoved(this, index, index);
        }
    }

    /**
     * Returns true if this model already contains the supplied element.
     * @param element the Element to check.
     * @return true if this model already contains the supplied element, false otherwise.  Always returns falls if E the
     * element is null.
     */
    public boolean contains(E element) {
        if (element == null) {
            return false;
        }
        return m_items.contains(element);
    }
}