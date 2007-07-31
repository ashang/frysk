#ifndef UTRACER_ERRMSGS_H
#define UTRACER_ERRMSGS_H

const char * utrace_emsg[] = {
  "No errors.",				  // UTRACER_EBASE         
  "Invalid engine.",			  // UTRACER_EENGINE,    
  "Invalid utracing structure.",	  // UTRACER_ETRACING,   
  "Invalid utraced structure.",		  // UTRACER_ETRACED,    
  "Register out of range.",		  // UTRACER_EREG,       
  "Syscall number out of range.",	  // UTRACER_ESYSRANGE,  
  "Process not quiesced.",		  // UTRACER_ESTATE,     
  "Error reading user pages.",		  // UTRACER_EPAGES,     
  "Null mm_struct, task probably died.",  // UTRACER_EMM,        
  "Invalid regset."			  // UTRACER_EREGSET,    
};					  
  

#endif  /* UTRACER_ERRMSGS_H */
