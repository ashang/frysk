package frysk.dom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;

import lib.dw.Dwarf;
import lib.dw.DwarfCommand;
//import lib.dw.DwflLine;
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
  private static final boolean DEBUG = false;
  
  private static final String GLOBAL_INCLUDE = "/usr/include";
  
  private static final String LOCAL_INCLUDE = "/usr/local/include";

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
    ArrayList arraysourcelist = getSrcFiles(proc.getExe());

    // Convert ArrayList to String Array
    String sourcelist[] = (String[]) arraysourcelist.toArray(new String [arraysourcelist.size()]);
    // Get the list of include file paths associated with this image
    ArrayList arrayincpaths = getIncludePaths(proc.getExe());
    String includepaths[] = (String[]) arrayincpaths.toArray(new String [arrayincpaths.size()]);
    for (int i = 0; i < sourcelist.length; i++)
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
            dom.addImage(proc.getMainTask().getName(), sourcelist[i], sourcelist[i]);
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
            source.setParsed(true);
          }
        hashmap.put(proc, dom);
        // if we are debugging the DOM, print it out now
        if (DEBUG)
          {
            printDOM(dom);
          }
      }
    return dom;
  }
  
  /*
   * Print out the DOM
   * 
   * @param dom is the DOMFrysk object to print out
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
   * getSrcFiles gets the source files for this image from the elf/dwarf header
   * The path from the ELF/DWARF header is used for the path for the initial
   * initial search of the source file.  If that search fails the path to the
   * executable is prepended to the beginning of that path to see if the file
   * can be found that way.
   * 
   * @param executable is a String containing the path to the executable
   * @return an ArrayList with the path(s) of the source file(s)
   */
  public static ArrayList getSrcFiles (String executable)
  {
    ArrayList sourcefiles = new ArrayList();
    if (pathFound(executable))
      {
        try
          {
            Elf elf = new Elf(executable, ElfCommand.ELF_C_READ);
            Dwarf dw = new Dwarf(elf, DwarfCommand.READ, null);
            String[] files = dw.getSourceFiles();

            // Since this call returns a lot of non-source file info, we must
            // parse it and glean the source paths from it
            for (int i = 0; i < files.length; i++)
              {
                if (DOMCompilerSuffixes.checkCPP(files[i]) || DOMCompilerSuffixes.checkC(files[i]))
                  {
                    if (pathFound(files[i]))
                      {
                        sourcefiles.add(files[i]);
                      }
                    // If we have not found the file and it has a relative path, prepend the path
                    // to the executable to the front of the paths and see what happens
                    else if (files[i].startsWith("..")) {
                      if (pathFound(executable.substring(0,executable.lastIndexOf("/")) + "/" + files[i]))
                        {
                          sourcefiles.add(executable.substring(0,executable.lastIndexOf("/")+1) + files[i]);
                        }
                    }
                  }
              }
          }
        catch (lib.elf.ElfException ee)
          {
            System.err.println("Error getting sourcefile paths: "
                               + ee.getMessage());
            return sourcefiles;
          }
      }
      return sourcefiles;
  }
  
  /*
   * get a list of the include files for this source file
   * 
   * @param executable is a String containing the path to the executable
   * @return an ArrayList containing a list of the include path(s)
   */
  public static ArrayList getIncludePaths (String executable)
  {
    ArrayList incpaths = new ArrayList();
    try
      {
        Elf elf = new Elf(executable, ElfCommand.ELF_C_READ);
        Dwarf dw = new Dwarf(elf, DwarfCommand.READ, null);
        String[] files = dw.getSourceFiles();

        // Since this call returns a lot of non-include file info, we must parse
        // it and glean the include paths from it
        for (int i = 0; i < files.length; i++)
          { 
            if ((DOMCompilerSuffixes.checkCHeader(files[i]) || 
                                                  DOMCompilerSuffixes.checkCPPHeader(files[i])) &&
                ! alreadyAdded(incpaths, files[i]))
              {
                int j = files[i].lastIndexOf("/");
                if (pathFound(files[i].substring(0, j)))
                  {
                    incpaths.add(files[i].substring(0, j));
                  }
              }
          }
        // Add the default includes used for all systems
        if (pathFound(LOCAL_INCLUDE))
          {
            incpaths.add(LOCAL_INCLUDE);
          }
        if (pathFound(GLOBAL_INCLUDE))
          {
            incpaths.add(GLOBAL_INCLUDE);
          }
        return incpaths;
      }

    catch (lib.elf.ElfException ee)
      {
        System.err.println("Error getting include paths: " + ee.getMessage());
        return null;
      }
  }
  
  /**
   * alreadyAdded checks to see if an include path is already in the list before adding it.
   * "/usr/include" and "/usr/local/include" are specail cases and are added at the end
   * automatically.
   * 
   * @param filelist is a String array containing the heretofore added include files
   * @param newfile is a String with the candidate include path to be added
   * @return true if the include is already in the list, false if not
   * 
   */
  public static boolean alreadyAdded(ArrayList filelist, String newfile )
  {
    int j = newfile.lastIndexOf("/");
    for (int i = 0; i < filelist.size(); i++)
      {
        if (newfile.substring(0,j) == GLOBAL_INCLUDE)
        if (newfile.substring(0,j).equals(GLOBAL_INCLUDE))
        if (filelist.get(i).equals(newfile.substring(0, j))) {
              return true;
            }
      }
    if (newfile.substring(0,j).equals(GLOBAL_INCLUDE) ||
            newfile.substring(0,j).equals(LOCAL_INCLUDE))
      return true;
    else
      return false;
  }
  
  /**
   * pathFound checks to be sure the source file is where the executable thinks it is
   * 
   * @param path contains a String of the path to check to see if it exists
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
