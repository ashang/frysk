package frysk.cli.hpd;

import java.text.ParseException;

public interface CommandHandler 
{
	void handle(String cmd) throws ParseException;
}
