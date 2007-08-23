// RolloverSpinnerListModel.java
// A custom spinner model that rolls over the end of a list back to the
// beginning (or vice versa).
//
// swiped, with minor modifications, from "Java Swing, 2nd Edition"
// Marc Loy, et.al., O'Reilly & Associates.

import javax.swing.*;
import java.util.List;

public class RolloverSpinnerListModel extends SpinnerListModel {

    public RolloverSpinnerListModel(Object[] items) { super(items); }

    public RolloverSpinnerListModel(List items) { super(items); }

    public Object getNextValue() {
	Object nv = super.getNextValue();
	if (nv != null) {
	    return nv;
	}
	return getList().get(0);
    }

    public Object getPreviousValue() {
	Object pv = super.getPreviousValue();
	if (pv != null) {
	    return pv;
	}
	List l = getList();
	return l.get(l.size() - 1);
    }
}
