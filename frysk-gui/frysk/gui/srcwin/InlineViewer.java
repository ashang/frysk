/**
 * 
 */
package frysk.gui.srcwin;

import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gdk.Drawable;
import org.gnu.gdk.GC;
import org.gnu.gdk.Window;
import org.gnu.gtk.EventBox;
import org.gnu.gtk.Justification;
import org.gnu.gtk.Label;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextWindowType;
import org.gnu.gtk.ToolTips;
import org.gnu.pango.Alignment;
import org.gnu.pango.Layout;

import frysk.gui.srcwin.PreferenceConstants.CurrentLine;
import frysk.gui.srcwin.PreferenceConstants.ExecMarks;
import frysk.gui.srcwin.PreferenceConstants.Inline;
import frysk.gui.srcwin.PreferenceConstants.LineNumbers;
import frysk.gui.srcwin.PreferenceConstants.Margin;

/**
 * @author ajocksch
 *
 */
public class InlineViewer extends SourceViewWidget {

	protected InlineViewer nextLevel;
	protected InlineViewer prevLevel;
	
	private PCLocation scope;
	
	private boolean showEllipsis;
	
	public InlineViewer(Preferences parentPrefs){
		this(parentPrefs, false);
	}
	
	/**
	 * @param parentPrefs
	 */
	public InlineViewer(Preferences parentPrefs, boolean showEllipsis) {
		super(parentPrefs);
		this.setBorderWidth(1);
		this.showEllipsis = showEllipsis;
	}
	
	public void toggleChild(){
		/*
		 * For right now we're running under the assumptions that
		 *  a) The structure of the inline code is a linked-list, not a tree
		 *  b) We're only viewing inline code at the current PC address
		 */
		
		if(this.nextLevel == null)
			this.expanded = InlineHandler.moveDown();
		else
			this.expanded = !InlineHandler.moveUp(this);
	}
	
	public void load(PCLocation current){
		try {
			this.buf.loadFile(current.getFilename());
		} catch (Exception e){
			e.printStackTrace();
		}
		
		this.setCurrentLine(current.getLineNum());
		this.scope = current;
		
		if(showEllipsis){
			Label l = null;
			if(this.scope.getDepth() > 2)
				l = new Label(this.scope.getDepth()-1+" inlined scopes hidden...");
			else
				l = new Label(this.scope.getDepth()-1+" inlined scope hidden...");
			l.setJustification(Justification.LEFT);
			EventBox b1 = new EventBox();
			b1.add(l);
			ToolTips t = new ToolTips();
			t.setTip(b1, "Levels of inline code have been hidden. Collapse lower scopes to view these hidden levels", "");
			l = new Label("...");
			l.setJustification(Justification.LEFT);
			EventBox b2 = new EventBox();
			b2.add(l);
			t.setTip(b2, "Levels of inline code have been hidden. Collapse lower scopes to view these hidden levels", "");
			
			buf.insertText(buf.getStartIter(), "\n");
			this.addChild(b1, buf.createChildAnchor(buf.getStartIter()));
			this.addChild(b2, buf.createChildAnchor(buf.getEndIter()));
			
			b1.showAll();
			b2.showAll();
		}
	}

	public PCLocation getScope() {
		return scope;
	}

	public void setScope(PCLocation scope) {
		this.scope = scope;
	}

	public void setSubscopeAtCurrentLine(InlineViewer viewer){
		super.setSubscopeAtCurrentLine(viewer);
		
		this.expanded = true;
		
		viewer.prevLevel = this;
		this.nextLevel = viewer;
	}
	
	protected void drawMargin(){
		if(!showEllipsis){
			super.drawMargin();
			return;
		}
		
		Window drawingArea = this.getWindow(TextWindowType.LEFT);
		
		// draw the background for the margin
		GC context = new GC((Drawable) drawingArea);
		int r = this.lnfPrefs.getInt(Margin.R, 54741);
		int g = this.lnfPrefs.getInt(Margin.G, 56283);
		int b = this.lnfPrefs.getInt(Margin.B, 65535);
		context.setRGBForeground(new Color(r, g, b));
		drawingArea.drawRectangle(context, true, 0, 0, drawingArea.getWidth(), drawingArea.getHeight());
		
		// get preference settings
		boolean showLines = this.lnfPrefs.getBoolean(LineNumbers.SHOW, true);
		boolean showMarks = this.lnfPrefs.getBoolean(ExecMarks.SHOW, true);
		
		// get the y coordinates for the top and bottom of the window
		int minY = drawingArea.getClipRegion().getClipbox().getY();
		int maxY = minY+drawingArea.getClipRegion().getClipbox().getHeight();
		
		// find out what the actual starting coordinates of the first line on screen is
		TextIter firstIter = this.getIterAtLocation(this.windowToBufferCoords(TextWindowType.LEFT, 0, minY));
		VerticalLineRange firstRange = this.getLineYRange(firstIter);
		int actualFirstStart = this.bufferToWindowCoords(TextWindowType.LEFT, 0, firstRange.getY()).getY();
		
		// get the line numbers we'll be drawing
		int firstLine = firstIter.getLineNumber();
		int lastLine = this.getIterAtLocation(this.windowToBufferCoords(TextWindowType.LEFT, 0, maxY)).getLineNumber();

		// Get Color to draw the text in
		r = this.lnfPrefs.getInt(LineNumbers.R, 0);
		g = this.lnfPrefs.getInt(LineNumbers.G, 0);
		b = this.lnfPrefs.getInt(LineNumbers.B, 0);
		context.setRGBForeground(new Color(r,g,b));
		
		// get inline color
		int inlineR = this.lnfPrefs.getInt(Inline.R, 65535);
		int inlineG = this.lnfPrefs.getInt(Inline.G, 65535);
		int inlineB = this.lnfPrefs.getInt(Inline.B, 0);
		
		// gets current line color
		int lineR = this.lnfPrefs.getInt(CurrentLine.R, 30000);
		int lineG = this.lnfPrefs.getInt(CurrentLine.G, 65535);
		int lineB = this.lnfPrefs.getInt(CurrentLine.B, 30000);
		
		// gets executable mark color
		int markR = this.lnfPrefs.getInt(ExecMarks.R, 0);
		int markG = this.lnfPrefs.getInt(ExecMarks.G, 0);
		int markB = this.lnfPrefs.getInt(ExecMarks.B, 0);
		
		int currentHeight = 0;		
		int actualIndex = 0;
		int totalInlinedLines = 0;
		
		int drawingHeight = 0;
		int gapHeight = 0;
		
		// If the refresh is starting after the current line, we have to add that offset in to
		// make sure the gap in line numbers is maintained
		if(expanded && firstLine+1 > this.buf.getCurrentLine())
			gapHeight = this.getLineYRange(this.getBuffer().getLineIter(this.buf.getCurrentLine()+1)).getHeight();
		
		boolean firstTime = true;
		
		for(int i = firstLine; i <= lastLine && i < this.buf.getLineCount(false); i++){
		
			if(i+1 > this.buf.getCurrentLine())
				drawingHeight = currentHeight + gapHeight;
			else
				drawingHeight = currentHeight;
			
			// get the current line height, etc.
			int lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
			int iconStart = lineHeight/2;
			
			if(firstTime){
				firstTime = false;
				currentHeight += this.getLineYRange(this.getBuffer().getLineIter(actualIndex++)).getHeight();
				i--;
				continue;
			}
			
			if(totalInlinedLines == 1){
				// draw background for the expanded lines
//				context.setRGBForeground(new Color(inlineR, inlineG, inlineB));
//				drawingArea.drawRectangle(context, true, 0, actualFirstStart+currentHeight, 
//						this.marginWriteOffset+20, lineHeight);
//				context.setRGBForeground(new Color(r,g,b));
				
				totalInlinedLines = 0;
			
				gapHeight = this.getLineYRange(this.getBuffer().getLineIter(actualIndex++)).getHeight();
 
				i--;
				continue;
			}
			
			// For the current line, draw background using the currentLine color
			if(i == this.buf.getCurrentLine() - 1){
				context.setRGBForeground(new Color(lineR, lineG, lineB));
				drawingArea.drawRectangle(context, true, 0, actualFirstStart+drawingHeight, 
					this.marginWriteOffset+20, lineHeight);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			
			// If it is executable, draw a mark
			if(showMarks && this.buf.isLineExecutable(i)){
				context.setRGBForeground(new Color(markR,markG,markB));
				drawingArea.drawLine(context, this.marginWriteOffset+5, actualFirstStart+drawingHeight+iconStart, 
						this.marginWriteOffset+12, actualFirstStart+drawingHeight+iconStart);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			if(i == this.buf.getCurrentLine() - 1){
//				context.setRGBForeground(new Color(inlineR, inlineG, inlineB));
//				drawingArea.drawRectangle(context, true, 0, actualFirstStart+currentHeight, 
//						this.marginWriteOffset+20, lineHeight);
//				context.setRGBForeground(new Color(r,g,b));
				
				context.setRGBForeground(new Color(markR,markG,markB));
				context.setRGBBackground(new Color(inlineR, inlineG, inlineB));
				Layout lo = new Layout(this.getContext());
				lo.setAlignment(Alignment.RIGHT);
				lo.setText("i");
				drawingArea.drawLayout(context, this.marginWriteOffset+5, actualFirstStart+drawingHeight, lo);
				context.setRGBForeground(new Color(r,g,b));
				
				if(this.expanded)
					totalInlinedLines = 1;
				else
					totalInlinedLines = 0;
			}
			
			// Draw line numbers
			if(showLines){
				Layout lo = new Layout(this.getContext());
				lo.setAlignment(Alignment.RIGHT);
				lo.setWidth(this.marginWriteOffset);
				lo.setText(""+(i+1));
				
				drawingArea.drawLayout(context, this.marginWriteOffset, actualFirstStart+drawingHeight, lo);
			}
			
			// draw breakpoints
			if(this.buf.isLineBroken(i)){
				int iconHeight = lineHeight - 8;
				
				context.setRGBForeground(new Color(65535,0,0));
				drawingArea.drawRectangle(context, true, this.marginWriteOffset+5, actualFirstStart+currentHeight+4, iconHeight, iconHeight);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			// update height for next line
			currentHeight += this.getLineYRange(this.getBuffer().getLineIter(actualIndex++)).getHeight();
		}
		
	}
}
