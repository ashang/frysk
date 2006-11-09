package frysk.dom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import lib.dw.Dwarf;
import lib.dw.DwarfCommand;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.dw.NoDebugInfoException;
import frysk.dom.cparser.CDTParser;
import frysk.proc.Proc;
import frysk.rt.StackFrame;

//import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

public class DOMFactory
{
  private static final boolean debug = false;
  
  private static final String globalinclude = "/usr/include";
  
  private static final String localinclude = "/usr/local/include";

  private static HashMap hashmap = new HashMap();

  public static DOMFrysk createDOM (StackFrame frame, Proc proc) throws NoDebugInfoException,
      IOException
  {
    DOMFrysk dom = null;
//    DwflLine line;
    
//    line = frame.getDwflLine();
//    if (line == null)
//      return null;
//    String fullPath = line.getSourceFile();
    
    /*
     * If we have a relative path to the source, use the compilation directory instead
     */
//    if(fullPath.indexOf(".") == 0)
//      {
//        String compDir = line.getCompilationDir();
//        fullPath = compDir + "/" + fullPath;
//      }
    
    // Get the list of source files associated with this image
    String[] sourcelist = getSrcFiles(proc.getExe());
    // Get the list of include file paths associated with this image
    String[] includepaths = getIncludePaths(proc.getExe());
    int i = 0;
    while (sourcelist[i] != null)
      {

        String filename = sourcelist[i].substring(sourcelist[i].lastIndexOf("/") + 1);
        String path = sourcelist[i].substring(0, sourcelist[i].lastIndexOf("/"));
        
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
            dom.addImage(proc.getMainTask().getName(), path, path);
          }

        DOMSource source = dom.getImage(proc.getMainTask().getName()).getSource(
                                                                                filename);

        /*
         * If this source file has not previously been incorporated into the
         * dom, so do now
         */
        if (source == null)
          {
            DOMImage image = dom.getImage(proc.getMainTask().getName());
            source = new DOMSource(filename, path, includepaths);

            // Read the file lines from disk
            // XXX: Remote file access?
            BufferedReader reader = new BufferedReader(
                                                       new FileReader(
                                                                      new File(
                                                                               sourcelist[i])));
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
        // if we are debugging the DOM, print it out now
        if (debug)
          {
            printDOM(dom);
          }
        i++;
      }
    return dom;
  }
  
  /*
   * Print out the DOM
   */
  
  public static void printDOM(DOMFrysk dom) {
    Document doc = dom.getDOMFrysk();
    try {
      XMLOutputter serializer = new XMLOutputter();
      serializer.getFormat();
      serializer.output(doc, System.out);
    }
    catch (IOException e) {
      System.err.println(e);
    }
  }
  
  /*
   * get the source files for this image
   * 
   * @return a String[] with the name(s) of the source file(s)
   */
  public static String[] getSrcFiles(String executable) {

    try
      {
        Elf elf = new Elf(executable, ElfCommand.ELF_C_READ);
        Dwarf dw = new Dwarf(elf, DwarfCommand.READ, null);
        String [] files = dw.getSourceFiles();

        // Since this call returns a lot of non-source file info, we must parse
        // it and glean the source paths from it
        String[] sourcefiles = new String[files.length];
        int numberfiles = 0;
        for (int i = 0; i < files.length; i++)
          {
            if (files[i].endsWith(".c") ||
                files[i].endsWith(".cpp"))
              {
                if (pathFound(files[i])) {
                sourcefiles[numberfiles] = files[i];
                numberfiles++;
                }
              }
          }
        return sourcefiles;
      }
    catch (lib.elf.ElfException ee)
      {
        System.err.println("Error getting sourcefile paths: " + ee.getMessage());
        return null;
      }
  }
  
  /*
   * get a list of the include files for this source file
   * 
   * @return a String[] containing a list of the include path(s)
   */
  public static String[] getIncludePaths (String executable)
  {
    try
      {
        Elf elf = new Elf(executable, ElfCommand.ELF_C_READ);
        Dwarf dw = new Dwarf(elf, DwarfCommand.READ, null);
        String[] files = dw.getSourceFiles();

        // Since this call returns a lot of non-include file info, we must parse
        // it and glean the include paths from it
        String[] incfiles = new String[files.length + 2];
        int numberfiles = 0;
        for (int i = 0; i < files.length; i++)
          {
            if (files[i].endsWith(".h") && ! files[i].startsWith(globalinclude)
                && ! files[i].startsWith(localinclude))
              {
                int j = files[i].lastIndexOf("/");
                if (pathFound(files[i].substring(0, j)))
                  {
                    incfiles[numberfiles] = files[i].substring(0, j);
                    numberfiles++;
                  }
              }
          }
        // Add the default includes used for all systems
        if (pathFound(localinclude))
          {
            incfiles[numberfiles] = "/usr/local/include";
            numberfiles++;
          }
        if (pathFound(globalinclude))
          {
            incfiles[numberfiles] = "/usr/include";
          }
        return incfiles;
      }

    catch (lib.elf.ElfException ee)
      {
        System.err.println("Error getting include paths: " + ee.getMessage());
        return null;
      }
  }
  
  /**
   * pathFound checks to be sure the source file is where the executable thinks it is
   * 
   * @return true if the file is found, false if not
   * 
   */
  public static boolean pathFound(String path) {
    File f = new File(path);
    return f.exists();
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
