#include <stdio.h>
#include <stdlib.h>
#include <alloca.h>
#include <strings.h>

void
show_bitvec (unsigned long * bitvec, int nr_longs)
{
  int i;
  for (i = 0; i < nr_longs; i++) fprintf (stderr, "%08x ", bitvec[i]);
  fprintf (stderr, "\n");
}

#define set_bit(bv, b) (bv[(b)/(8*sizeof(long))] |= (1<<((b)%(8*sizeof(long)))))
#define test_bit(bv, b) (bv[(b)/(8*sizeof(long))] & (1<<((b)%(8*sizeof(long)))))
#define clear_bit(bv, b) (bv[(b)/(8*sizeof(long))] &= ~(1<<((b)%(8*sizeof(long)))))

main (int ac, char * av[])
{
  unsigned long * bitvec;
  int nr_bits		= atoi (av[1]);
  int bit_to_set	= atoi (av[2]);
  int bit_to_clear	= atoi (av[3]);
  int bit_to_test	= atoi (av[4]);
  int nr_bits_per_long	= 8 * sizeof(long);
  int nr_longs		= (nr_bits + (nr_bits_per_long -1))/nr_bits_per_long;

  fprintf (stderr, "nr_longs = %d\n", nr_longs);

  bitvec = alloca (nr_longs * sizeof(long));
  bzero (bitvec, nr_longs * sizeof(long));

  show_bitvec (bitvec, nr_longs);

  if (bit_to_set < nr_bits) set_bit (bitvec, bit_to_set);
  show_bitvec (bitvec, nr_longs);

  if (bit_to_clear < nr_bits) clear_bit (bitvec, bit_to_clear);
  show_bitvec (bitvec, nr_longs);

  if (bit_to_test < nr_bits)
    fprintf (stderr, "bit %d is %s\n", bit_to_test,
	     (test_bit (bitvec, bit_to_test) ? "on" : "off"));
}
