class ComplexClass {
	private:
		int x;
		int y;
	public:
		int getX(void) { return x;}
		int getY(void) {return y;}
		void setX(int a) {x = a;}
		void setY(int a) {y = a;}
};

int main()
{
	ComplexClass complex;
	complex.setX(1);
	complex.setY(2);
	char *a = 0;
	a[0] = 0;
	return 0;
}
