// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package lib.dwfl;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DwarfDie {

    private long pointer;
    private DwarfDie[] scopes;
    private int scopeIndex;
    // XXX: Belongs in DwflDie.
    DwflModule module;

    protected boolean manageDie = false;
  
    protected DwarfDie(long pointer, DwflModule module) {
	this.pointer = pointer;
	this.module = module;
    }

    protected DwflModule getModule(){
	return module;
    }

    protected DwarfDie getCompilationUnit(){
	
	if(this.getTag().equals(DwTag.COMPILE_UNIT)){
	    return this;
	}
	
	DwarfDie[] scopes = this.getScopesDie();
	if(!scopes[scopes.length -1].getTag().equals(DwTag.COMPILE_UNIT)){
	    throw new RuntimeException("Could not retrieve CU of this die ["+this.getTag()+"]");
	}
	return scopes[scopes.length -1];
    }
    
    public long getHighPC () {
	return dwarf_highpc(pointer);
    }
    private static native long dwarf_highpc(long pointer);

    public long getLowPC () {
	return dwarf_lowpc(pointer);
    }
    private static native long dwarf_lowpc(long pointer);

    /**
     * @return entry pc or low pc if this die
     */
    public long getEntryPc(){
	return dwarf_entrypc(pointer);
    }
    private static native long dwarf_entrypc(long pointer);

    public String getName () {
	return dwarf_diename(pointer);
    }
    private static native String dwarf_diename(long pointer);
    
    public String getProducer () {
	return getCompilationUnit().getAttrString(DwAt.PRODUCER);
    }
  
    public File getDeclFile() {
	File file = null;
	try{
	    file = new File(get_decl_file(pointer));
	}catch (DwAttributeNotFoundException e) {
	    if(isDefinitionOnly()){
		file = getOriginalDie().getDeclFile();
	    }
	}
	return file;
    }
  
    public int getDeclLine() {
	int line = -1;
	
	try{
	    line = get_decl_line(pointer);
	}catch (DwAttributeNotFoundException e) {
	    if(isDefinitionOnly()){
		line = getOriginalDie().getDeclLine();
	    }
	}
	return line;
    }

    public int getDeclColumn() {
	return get_decl_column(pointer);
    }
  
    public void setScopes(DwarfDie[] scopes) {
	this.scopes = scopes;
    }

    public void setManageDie(boolean value) {
	manageDie = value;
    }
  
    /**
     * This function assumes that this die is a one corresponding to a
     * Compilation Unit Die.  It will return the scopes containing the
     * give address which fall within this Compilation Unit Die.
     * @see getScopesDie
     * @param addr PC address.
     * @return Scope DwarfDies containing addr.
     */
    public DwarfDie[] getScopes(long addr) {
	long[] vals = get_scopes(addr);
	DwarfDie[] dies = new DwarfDie[vals.length];
	DwarfDieFactory factory = DwarfDieFactory.getFactory();
	for(int i = 0; i < vals.length; i++)
	    if(vals[i] != 0)
		dies[i] = factory.makeDie(vals[i], this.module);
	    else
		dies[i] = null;

	return dies;
    }
  
    /**
     * Return the scopes containing this die.
     * @return Scope DwarfDies containing this die.
     */
    public DwarfDie[] getScopesDie() {
	long[] vals = get_scopes_die();
	DwarfDie[] dies = new DwarfDie[vals.length];
	DwarfDieFactory factory = DwarfDieFactory.getFactory();
	for(int i = 0; i < vals.length; i++) {
	    if(vals[i] != 0)
		dies[i] = factory.makeDie(vals[i], this.module);
	    else
		dies[i] = null;
	}
	return dies;
    }
  
    /**
     * @param scopes
     * @param variable
     * @return Die of variable in scopes
     */
    public DwarfDie getScopeVar (DwarfDie[] scopes, String variable) {
	long[] vals = new long[scopes.length];
	long[] die_and_scope = new long[2];
	for(int i = 0; i < scopes.length; i++)
	    vals[i] = scopes[i].pointer;

	DwarfDie die = null;
	long val = get_scopevar(die_and_scope, vals, variable);
	if (val >= 0) {
	    die = DwarfDieFactory.getFactory().makeDie(die_and_scope[0],
						       this.module);
	    die.scopes = scopes;
	    die.scopeIndex = (int)die_and_scope[1];
	}
	return die;
    }
  
    private ArrayList varNames;
    /**
     * @param scopes
     * @param variable
     * @return List of names in scopes matching variable
     */
    public List getScopeVarNames (DwarfDie[] scopes, String variable) {
	varNames = new ArrayList();    
	long[] vals = new long[scopes.length];
	for(int i = 0; i < scopes.length; i++)
	    vals[i] = scopes[i].pointer;

	get_scopevar_names(vals, variable);
	return varNames; 
    }
 
    public void addScopeVarName(String name) {
	varNames.add(name);
    }

    private ArrayList DwarfOps;
  
    public void addOps(int operator, int operand1, int operand2, int offset) {
	DwarfOp dwarfOp = new DwarfOp(operator, operand1, operand2, offset);
	DwarfOps.add(dwarfOp);
    }
  
    /**
     * @return Scopes index of this die.
     */
    public long getScopeIndex() {
	return this.scopeIndex;
    }
  
    /**
     * @param index Scopes index.
     * @return Die of scope.
     */
    public long getScope(int index) {
	return this.scopes[index].pointer;
    }
  
    /**
     * @param Return address of die.  Typically this is a static
     * address or ptr+disp.
     */
    public List getAddr()
    {
	DwarfOps = new ArrayList();
	get_addr(pointer, 0);
	return DwarfOps;
    }

    /**
     * @return The type die for the current die, following all typedefs.
     */
    public DwarfDie getUltimateType()
    {
	return getType(true);
    }
    /**
     * @return The type die for the current die.
     */
    public DwarfDie getType() {
	return getType(false);
    }
    private DwarfDie getType(boolean followTypeDef) {
	DwarfDie die = null;
	long type = get_type(pointer, followTypeDef);
	if (type != 0)
	    die = DwarfDieFactory.getFactory().makeDie(type, this.module);
	return die;
    }

    public boolean getAttrBoolean(DwAt attr) {
	return get_attr_boolean(pointer, attr.hashCode());
    }
    
    public String getAttrString(DwAt attr) {
	return get_attr_string(pointer, attr.hashCode());
    }
  
    public DwTag getTag() {
	return DwTag.valueOf(get_tag(pointer));
    }
    
  
    /**
     * @return The upper bound for this subrange die.
     */
    public int getAttrConstant(DwAt attr) {
	return get_attr_constant(pointer, attr.hashCode());
    }

    /**
     * @return The offset for this die.
     */
    public long getOffset() {
	return get_offset(pointer);
    }

    /**
     * @return The child for the current die.
     */
    public DwarfDie getChild() {
	long child = get_child(pointer);
	DwarfDie die = null;
	if (child != 0)
	    die = DwarfDieFactory.getFactory().makeDie(child, this.module);
	return die;
    }

    /**
     * @return The sibling for the current die.
     */
    public DwarfDie getSibling() {
	long sibling = get_sibling(pointer);
	DwarfDie die = null;
	if (sibling != 0)
	    die = DwarfDieFactory.getFactory().makeDie(sibling, this.module);
	return die;
    }
  
    /**
     * @param pc Program Counter
     * @return DW_AT_frame_base for current die.
     */
    public List getFrameBase(long pc) {
	this.scopes = this.getScopesDie();
      
	DwarfOps = new ArrayList();
	for (int i = this.scopeIndex; i < this.scopes.length; i++) {
	    get_framebase(pointer, this.scopes[i].pointer, pc);
	    if (DwarfOps.size() != 0)
		break;
	}
	return DwarfOps;
    }

    /**
     * @param pc - PC
     * @return DW_FORM_data for current die.  Typically this is from a
     * location list.
     */
    public List getFormData(long pc) {
	DwarfOps = new ArrayList();
	get_addr(pointer, pc);
	return DwarfOps;
    }

    public long getDataMemberLocation() {
	return get_data_member_location(pointer);
    }
  
    /**
     * @return True if this is an inlined instance of a function,
     * false otherwise
     */
    public boolean isInlinedFunction() {
	return is_inline_func();
    }
  
    public boolean hasAttribute(DwAt attr){
	return hasattr(pointer, attr.hashCode());
    }
    
    public String toString() {
	StringBuilder stringBuilder= new StringBuilder();
	DwarfDie type = getUltimateType();
	stringBuilder.append("offset 0x"+Long.toHexString(this.getOffset()) +" "+ this.getTag() + " Name: " + this.getName());
	if(type != null)
	    stringBuilder.append(" Type: " + type.toString());
	return stringBuilder.toString();
    }
  
    public StringBuilder toPrint(){
	StringBuilder stringBuilder = new StringBuilder();
	stringBuilder.append(this.getTag() + " Name: " + this.getName());
	return stringBuilder;
    }
  
    /**
     * Get die for static symbol sym in dw. 
     * @param dw
     * @param sym
     * @return die
     */
    public static DwarfDie getDecl(Dwarf dw, String sym) {
	long result = get_decl (dw.getPointer(), sym);
	DwarfDie die = null;
	if (result > 0) {
	    die = DwarfDieFactory.getFactory().makeDie(result, null);
	    die.scopes = null;
	    die.scopeIndex = 0;
	}
	return die;
    }

    /**
     * Get die for static symbol sym in CU dw.
     * @param scopes
     * @param sym
     * @return die
     */
    public static DwarfDie getDeclCU(DwarfDie[] scopes, String sym) {
	long result = get_decl_cu (scopes[0].pointer, sym);
	DwarfDie die = null;
	if (result > 0) {
	    die = DwarfDieFactory.getFactory().makeDie(result, null);
	    die.scopes = scopes;
	    die.scopeIndex = 0;
	}
	return die;
    }

    /**
     * If this die has a DW_AT_abstract_origin or DW_AT_specification
     * this function returns the die pointed to by those attributes.
     */
    public DwarfDie getOriginalDie() {
	if(this.hasAttribute(DwAt.ABSTRACT_ORIGIN) ||
		this.hasAttribute(DwAt.SPECIFICATION)){
	    long original_die = get_original_die(pointer);
	    DwarfDie die = null;
	    if (original_die != 0)
		die = DwarfDieFactory.getFactory().makeDie(original_die, this.module);
	    return die;
	}
	return null;
    }
        
    /**
     * returns true of this die represents a definition of an object, and the declaration
     * is somewhere else.
     * @return
     */
    public boolean isDefinitionOnly(){
	return (this.hasAttribute(DwAt.ABSTRACT_ORIGIN) || this.hasAttribute(DwAt.SPECIFICATION));
    }
    public boolean isDeclaration() {
	return this.hasAttribute(DwAt.DECLARATION);
    }

    public DwarfDie getDefinition() {
	// try to find the definition 
	// try using pubnames
	LinkedList pubnames = this.getModule().getPubNames();
	for (Iterator i = pubnames.iterator(); i.hasNext(); ) {
	    DwarfDie die = (DwarfDie) i.next();
	    DwarfDie originalDie = die.getOriginalDie();
	    if(originalDie != null && originalDie.getModule().getName().equals(this.getModule().getName()) &&
		    originalDie.getOffset() == this.getOffset()){
		return die;
	    }
	}
	
	return null;
    }

    public native ArrayList getEntryBreakpoints();

    public native boolean isInlineDeclaration();

    public native ArrayList getInlinedInstances();
  
    private native String get_decl_file (long var_die);
  
    private native int get_decl_line (long var_die);
  
    private native int get_decl_column (long var_die);
  
    private native long[] get_scopes (long addr);

    private native long[] get_scopes_die ();

    private native int get_scopevar(long[] die_scope, long[] scopes,
				    String variable);

    private native long get_scopevar_names (long[] scopes, String variable);
  
    private native void get_addr (long addr, long pc);
  
    private native long get_type (long addr, boolean followTypeDef);
  
    private native long get_child (long addr);
  
    private native long get_sibling (long addr);
  
    private native boolean get_attr_boolean (long addr, int attr);
  
    private native int get_attr_constant (long addr, int attr);
    
    private native String get_attr_string (long addr, int attr);
  
    private native long get_offset (long addr);

    // Package access for DwarfDieFactory
    static native int get_tag (long var_die);
  
    private native void get_framebase (long addr, long scope, long pc);

    private native long get_data_member_location (long addr);
  
    private native boolean is_inline_func ();

    private static native long get_decl (long dw, String sym);

    private static native long get_decl_cu (long dw, String sym);

    protected native void finalize();
    
    private native boolean hasattr(long pointer, int attr);

    private native long get_original_die(long pointer);

}
