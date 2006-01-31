package frysk.gui.srcwin;

import java.util.Vector;

import org.gnu.gtk.HPaned;

public class MixedView extends HPaned implements View {

	private SourceView sourceWidget;
	private SourceView assemblyWidget;
	
	public MixedView(StackLevel scope, SourceWindow parent){
		super();
		
		this.sourceWidget = new SourceView(scope, parent);
		this.assemblyWidget = new SourceView(scope, parent);
		this.assemblyWidget.setMode(SourceBuffer.ASM_MODE);

		this.add1(this.sourceWidget);
		this.add2(this.assemblyWidget);
		
		this.showAll();
	}
	
	public void refresh() {
		this.sourceWidget.refresh();
		this.assemblyWidget.refresh();
	}

	public boolean findNext(String toFind, boolean caseSensitive) {
		boolean result = this.sourceWidget.findNext(toFind, caseSensitive);
		if(!result)
			result = this.assemblyWidget.findNext(toFind, caseSensitive);
		
		return result;
	}

	public boolean findPrevious(String toFind, boolean caseSensitive) {
		// TODO: How do we tell where we're searching back from?
		return false;
	}

	public boolean highlightAll(String toFind, boolean caseSensitive) {
		return this.sourceWidget.highlightAll(toFind, caseSensitive) ||
			this.assemblyWidget.highlightAll(toFind, caseSensitive);
	}

	public void scrollToFound() {
		// TODO: same problem as findPrevious
	}

	public void load(StackLevel data) {
		this.sourceWidget.load(data);
		this.assemblyWidget.load(data);
		this.assemblyWidget.setMode(SourceBuffer.ASM_MODE);
	}

	public void setSubscopeAtCurrentLine(InlineSourceView child) {
		// TODO Inlined code for mixed view? How do we do this?
	}

	public void clearSubscopeAtCurrentLine() {
		// TODO Inlined code for mixed view? How do we do this?
	}

	public void toggleChild() {
		// TODO Inlined code for mixed view? How do we do this?
	}

	public void scrollToFunction(String markName) {
		
	}

	public void scrollToLine(int line) {
		
	}

	public Vector getFunctions() {
		return null;
	}

	public StackLevel getScope() {
		return this.sourceWidget.getScope();
	}

}
