package frysk.expunit;

class MatchException extends RuntimeException {
    static final long serialVersionUID = 1;
    private final String error;
    private final Match[] matches;
    private final String output;
    MatchException(String error, Match[] matches, String output) {
	super(error);
	this.error = error;
	this.matches = matches;
	this.output = output;
    }
    public String getMessage() {
	StringBuffer msg = new StringBuffer();
	msg.append(error);
	if (matches != null) {
	    msg.append("; expecting: ");
	    for (int i = 0; i < matches.length; i++) {
		msg.append(" <<");
		msg.append(matches.toString());
		msg.append(">>");
	    }
	}
	msg.append("; buffer <<");
	msg.append(output);
	msg.append(">>");
	return msg.toString();
    }
}