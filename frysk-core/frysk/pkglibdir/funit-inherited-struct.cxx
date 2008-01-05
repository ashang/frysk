struct Base 
{
	int a;
};

struct Derivative: public Base {
	int b;
};


int
main()
{
	Derivative der;
	der.b = 2;
	der.a = 1;

	char *a = 0;
	a[0] = 0;
	return 0;
}
