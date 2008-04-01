//This file is part of the program FRYSK.

//Copyright 2007, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.debuginfo;

import java.util.List;

import javax.naming.NameNotFoundException;

import lib.dwfl.DwarfDie;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.value.Value;
import frysk.scopes.Variable;

public class TestAddress 
	extends TestLib
{   
    public void testAddrStaticInt() throws NameNotFoundException
    {
	testAddress ("static_int", "static_int_address");
    }
    
    public void testAddrVolatileInt() throws NameNotFoundException
    {
	testAddress ("volatile_int", "volatile_int_address");
    }
    
    public void testAddrGlobalChar () throws NameNotFoundException
    {
	testAddress ("global_char", "global_char_address");
    }

    /**
     * Function that checks if a variable's location is evaluated correctly.
     * 
     * @param variable - Variable name whose location is to be evaluated
     * @param address - Variable name that contains the actual address of the variable
     * @throws NameNotFoundException
     */
    private void testAddress (String variable, String address) throws NameNotFoundException
    {
	DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace
	                       (getStoppedTask());
	ObjectDeclarationSearchEngine declarationSearchEngine = new ObjectDeclarationSearchEngine(frame);
	
	/* Evaluate the location of the variable.
	 */ 
	Variable var = (Variable) declarationSearchEngine.getVariable(variable);
	DwarfDie varDie = var.getVariableDie();
	List ops = varDie.getFormData(frame.getAdjustedAddress());
	LocationExpression locExpr = new LocationExpression(varDie);
	PieceLocation loc = new PieceLocation(locExpr.decode(frame, ops,var.getType
		                              (frame.getTask().getISA()).getSize()));

	 /* Get the value of the address.
	 */
	Variable addr = (Variable) declarationSearchEngine.getVariable(address);
	DwarfDie addrDie = addr.getVariableDie();
	List opsAddr = addrDie.getFormData(frame.getAdjustedAddress());
	LocationExpression locExprAddr = new LocationExpression(
		addrDie);
	PieceLocation p = new PieceLocation(locExprAddr.decode(frame, opsAddr, addr.getType
		                            (frame.getTask().getISA()).getSize()));
	Value addrVal = new Value(addr.getType(frame.getTask().getISA()), p);
	
	assertEquals ("Address", addrVal.asLong(), loc.getAddress());
    }
    
    private Task getStoppedTask ()
    {
	return this.getStoppedTask("funit-addresses");
    }
    
    private Task getStoppedTask (String process)
    {
	// Starts program and runs it to crash.
	DaemonBlockedAtSignal daemon = new DaemonBlockedAtSignal 
	                               (new String[] { getExecPath(process) });
	return daemon.getMainTask();
    }  
}