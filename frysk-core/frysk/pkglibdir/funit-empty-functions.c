static void crash (){
  char* a = 0;
  a[0] = 0;
}

int main (int argc, char **argv){
  crash();
  return 0;
}
