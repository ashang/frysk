/**
 *	jline - Java console input library
 *	Copyright (c) 2002, 2003, 2004, 2005, Marc Prud'hommeaux <mwp1@cornell.edu>
 *	All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or
 *	without modification, are permitted provided that the following
 *	conditions are met:
 *
 *	Redistributions of source code must retain the above copyright
 *	notice, this list of conditions and the following disclaimer.
 *
 *	Redistributions in binary form must reproduce the above copyright
 *	notice, this list of conditions and the following disclaimer
 *	in the documentation and/or other materials provided with
 *	the distribution.
 *
 *	Neither the name of JLine nor the names of its contributors
 *	may be used to endorse or promote products derived from this
 *	software without specific prior written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 *	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 *	AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 *	EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *	OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *	AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *	LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *	IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 *	OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jline;


/**
 *	Synbolic constants for Console operations and virtual key bindings.
 *
 *	@see KeyEvent
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public interface ConsoleOperations
{
	String CR = System.getProperty ("line.separator");

	char BACKSPACE = '\b';
	char RESET_LINE = '\r';
	char KEYBOARD_BELL = '\07';

	char CTRL_P = 16;
	char CTRL_N = 14;
	char CTRL_B = 2;
	char CTRL_F = 6;


	/**
	 *	Logical constants for key operations.
	 */

	/**
	 *  Unknown operation.
	 */
	short UNKNOWN				= -99;

	/**
	 *  Operation that moves to the beginning of the buffer.
	 */
	short MOVE_TO_BEG			= -1;

	/**
	 *  Operation that moves to the end of the buffer.
	 */
	short MOVE_TO_END			= -3;

	/**
	 *  Operation that moved to the previous character in the buffer.
	 */
	short PREV_CHAR				= -4;

	/**
	 *  Operation that issues a newline.
	 */
	short NEWLINE				= -6;

	/**
	 *  Operation that deletes the buffer from the current character to the end.
	 */
	short KILL_LINE				= -7;

	/**
	 *  Operation that clears the screen.
	 */
	short CLEAR_SCREEN			= -8;

	/**
	 *  Operation that sets the buffer to the next history item.
	 */
	short NEXT_HISTORY			= -9;

	/**
	 *  Operation that sets the buffer to the previous history item.
	 */
	short PREV_HISTORY			= -11;

	/**
	 *  Operation that redisplays the current buffer.
	 */
	short REDISPLAY				= -13;

	/**
	 *  Operation that deletes the buffer from the cursor to the beginning.
	 */
	short KILL_LINE_PREV		= -15;

	/**
	 *  Operation that deletes the previous word in the buffer.
	 */
	short DELETE_PREV_WORD		= -16;

	/**
	 *  Operation that moves to the next character in the buffer.
	 */
	short NEXT_CHAR				= -19;

	/**
	 *  Operation that moves to the previous character in the buffer.
	 */
	short REPEAT_PREV_CHAR		= -20;

	/**
	 *  Operation that searches backwards in the command history.
	 */
	short SEARCH_PREV			= -21;

	/**
	 *  Operation that repeats the character.
	 */
	short REPEAT_NEXT_CHAR		= -24;

	/**
	 *  Operation that searches forward in the command history.
	 */
	short SEARCH_NEXT			= -25;

	/**
	 *  Operation that moved to the previous whitespace.
	 */
	short PREV_SPACE_WORD		= -27;

	/**
	 *  Operation that moved to the end of the current word.
	 */
	short TO_END_WORD			= -29;

	/**
	 *  Operation that
	 */
	short REPEAT_SEARCH_PREV	= -34;

	/**
	 *  Operation that
	 */
	short PASTE_PREV			= -36;

	/**
	 *  Operation that
	 */
	short REPLACE_MODE			= -37;

	/**
	 *  Operation that
	 */
	short SUBSTITUTE_LINE		= -38;

	/**
	 *  Operation that
	 */
	short TO_PREV_CHAR			= -39;

	/**
	 *  Operation that
	 */
	short NEXT_SPACE_WORD		= -40;

	/**
	 *  Operation that
	 */
	short DELETE_PREV_CHAR		= -41;

	/**
	 *  Operation that
	 */
	short ADD					= -42;

	/**
	 *  Operation that
	 */
	short PREV_WORD				= -43;

	/**
	 *  Operation that
	 */
	short CHANGE_META			= -44;

	/**
	 *  Operation that
	 */
	short DELETE_META			= -45;

	/**
	 *  Operation that
	 */
	short END_WORD				= -46;

	/**
	 *  Operation that
	 */
	short INSERT				= -48;

	/**
	 *  Operation that
	 */
	short REPEAT_SEARCH_NEXT	= -49;

	/**
	 *  Operation that
	 */
	short PASTE_NEXT			= -50;

	/**
	 *  Operation that
	 */
	short REPLACE_CHAR			= -51;

	/**
	 *  Operation that
	 */
	short SUBSTITUTE_CHAR		= -52;

	/**
	 *  Operation that
	 */
	short TO_NEXT_CHAR			= -53;

	/**
	 *  Operation that undoes the previous operation.
	 */
	short UNDO					= -54;

	/**
	 *  Operation that moved to the next word.
	 */
	short NEXT_WORD				= -55;

	/**
	 *  Operation that deletes the previous character.
	 */
	short DELETE_NEXT_CHAR		= -56;

	/**
	 *  Operation that toggles between uppercase and lowercase.
	 */
	short CHANGE_CASE			= -57;

	/**
	 *  Operation that performs completion operation on the current word.
	 */
	short COMPLETE				= -58;

	/**
	 *  Operation that exits the command prompt.
	 */
	short EXIT					= -59;

	/**
	 *  Operation that pastes the contents of the cliboard into the line
	 */
	short PASTE				= -60;
}

