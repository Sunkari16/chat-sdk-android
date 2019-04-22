package co.chatsdk.ui.custom.inbox;

import java.util.Comparator;
import java.util.Date;

import co.chatsdk.core.dao.Thread;

/**
 * thread sorting according to date added
 */
public class InboxThreadComparator implements Comparator<Thread> {
    public static final int ORDER_TYPE_ASC = 0;
    public static final int ORDER_TYPE_DESC = 1;

    private int order = ORDER_TYPE_DESC;

    public InboxThreadComparator() {
    }

    public InboxThreadComparator(int order) {
        this.order = order;
    }

    @Override
    public int compare(Thread t1, Thread t2) {

        Date d1 = t1.getLastMessageAddedDate();
        Date d2 = t2.getLastMessageAddedDate();

        if (d1 == null) {
            d1 = t1.getCreationDate();
        }
        if (d2 == null) {
            d2 = t2.getCreationDate();
        }

        d1 = d1 != null ? d1 : new Date();
        d2 = d2 != null ? d2 : new Date();

        if (order == ORDER_TYPE_ASC) {
            return d1.compareTo(d2);
        } else {
            return d2.compareTo(d1);
        }
    }
}
