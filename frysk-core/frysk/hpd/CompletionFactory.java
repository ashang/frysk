// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.hpd;

import java.util.List;
import java.util.Iterator;
import frysk.debuginfo.DebugInfoFrame;
import frysk.proc.Task;
import jline.FileNameCompletor;
import frysk.expr.ExprSearchEngine;
import frysk.expr.ExpressionFactory;

/**
 * A collection of completers.
 */

class CompletionFactory {
    static void padSingleCandidate(List candidates) {
	if (candidates.size() == 1) {
	    candidates.set(0, ((String)candidates.get(0)) + " ");
	}
    }

    static int completeExpression(CLI cli, Input input, int cursor,
				  List candidates) {
	PTSet ptset = cli.getCommandPTSet(input);
	Iterator i = ptset.getTasks();
	if (!i.hasNext()) {
	    // Should still be able to complete $variables.
	    return -1;
	} else {
	    int newOffset = -1;
	    String incomplete = input.stringValue();
	    int start;
	    if (input.size() == 0)
		start = cursor;
	    else
		start = input.token(0).start;
	    do {
		Task task = (Task)i.next();
		DebugInfoFrame frame = cli.getTaskFrame(task);
		int tmp = ExpressionFactory.complete
		    (new ExprSearchEngine(frame),
		     incomplete, cursor - start, candidates);
		if (tmp >= 0)
		    newOffset = tmp;
	    } while (i.hasNext());
	    // System.out.println("start=" + start);
	    // System.out.println("offset=" + offset);
	    // System.out.println("candidates=" + candidates);
	    // System.out.println("newCursor=" + newCursor);
	    if (newOffset < 0)
		return -1;
	    else
		return newOffset + start;
	}
    }

    static int completeFileName(CLI cli, Input input, int cursor,
				List candidates) {
	// System.out.println("input.stringValue()=" + input.stringValue());
	// System.out.println("cursor=" + cursor);
	// System.out.println("input.size()=" + input.size());
	if (input.size() == 0) {
	    int newOffset
		= new FileNameCompletor().complete("", 0, candidates);
	    if (newOffset < 0)
		return -1;
	    else
		return newOffset + cursor;
	} else {
	    Input.Token incomplete = input.incompleteToken(cursor);
	    int newOffset = new FileNameCompletor()
		.complete(incomplete.value, incomplete.end - incomplete.start,
			  candidates);
	    return incomplete.absolute(newOffset);
	}
    }
}
