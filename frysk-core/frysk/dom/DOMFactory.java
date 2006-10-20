

package frysk.dom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import lib.dw.DwflLine;
import lib.dw.NoDebugInfoException;
import frysk.dom.cparser.CDTParser;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskException;

public class DOMFactory
{

  private static HashMap hashmap = new HashMap();

  public static DOMFrysk createDOM (Task task) throws NoDebugInfoException,
      IOException
  {
    DOMFrysk dom;
    DwflLine line;
    try
      {
        line = task.getDwflLineXXX();
      }
    catch (TaskException e)
      {
        throw new NoDebugInfoException("Couldn't access task " + task.getName());
      }
    if (line == null)
      throw new NoDebugInfoException(
                                     "Could not find debug information for task "
                                         + task.getName());
    String fullPath = line.getSourceFile();
    
    /*
     * If we have a relative path to the source, use the compilation directory instead
     */
    if(fullPath.indexOf(".") == 0)
      {
        String compDir = line.getCompilationDir();
        fullPath = compDir + "/" + fullPath;
      }
    
    String filename = fullPath.substring(fullPath.lastIndexOf("/") + 1);
    String path = fullPath.substring(0, fullPath.lastIndexOf("/"));

    Proc proc = task.getProc();

    if (hashmap.containsKey(proc))
      {
        // retrieve the previously created dom
        dom = (DOMFrysk) hashmap.get(proc);
      }
    else
      {
        // create a new dom and associate it with the given task
        // XXX create a fake name for now, must create unique names later
        dom = new DOMFrysk("TaskTask");
        dom.addImage(task.getProc().getMainTask().getName(), path, path);
      }

    DOMSource source = dom.getImage(task.getProc().getMainTask().getName()).getSource(filename);

    /*
     * If this source file has not previously been incorporated into the dom, so
     * do now
     */
    if (source == null)
      {
        DOMImage image = dom.getImage(task.getProc().getMainTask().getName());
        source = new DOMSource(filename, path);

        // Read the file lines from disk
        // XXX: Remote file access?
        BufferedReader reader = new BufferedReader(
                                                   new FileReader(
                                                                  new File(
                                                                           fullPath)));
        int offset = 0;
        int lineNum = 0;

        while (reader.ready())
          {
            String text = reader.readLine();
            // XXX: detect executable lines?
            DOMLine l = new DOMLine(lineNum++, text + "\n", offset, false,
                                    false, Long.parseLong("deadbeef", 16));
            source.addLine(l);

            offset += text.length() + 1;
          }
        image.addSource(source);

        // Parse the file and populate the DOM
        StaticParser parser = new CDTParser();
        parser.parse(dom, source, image);
      }
    hashmap.put(proc, dom);
    return dom;
  }
  
  /*
   * remove the spaces from the DOM ID 'cause JDOM don't like spaces there
   * 
   * @param name of the task
   */
  
/*  private static String removeSpace(String name) {
    // if there are no spaces, just return the name
    if (name.lastIndexOf(" ") == -1)
      return name;
    
    char[] newname = new char[name.length()];
    char[] namechararray = name.toCharArray();
    int i = 0;
    for(int j=0; j<name.length(); j++) {
      if (Character.isSpaceChar(namechararray[j]))
        continue;
      newname[i] = namechararray[j];
      i++;
    }
    String str = new String(newname);
    if (str.length() <= 8)
      return str;
    return str.substring(0,8);
  }  */

}
