package inua.eio;

public class TestULong
  extends TestLib 
{
  private final long a = 3;
  private final long b = 4;
  private final long c = -1;
  private final long d = -2;

  public void testUlong()
  {
    assertEquals(ULong.LT(a, b), true);
    assertEquals(ULong.LE(a, b), true);
    assertEquals(ULong.GT(a, b), false);
    assertEquals(ULong.GE(a, b), false);
    assertEquals(ULong.EQ(a, b), false);
    assertEquals(ULong.NE(a, b), true);

    assertEquals(ULong.LT(b, a), false);
    assertEquals(ULong.LE(b, a), false);
    assertEquals(ULong.GT(b, a), true);
    assertEquals(ULong.GE(b, a), true);
    assertEquals(ULong.EQ(b, a), false);
    assertEquals(ULong.NE(b, a), true);

    assertEquals(ULong.LT(d, c), true);
    assertEquals(ULong.LE(d, c), true);
    assertEquals(ULong.GT(d, c), false);
    assertEquals(ULong.GE(d, c), false);
    assertEquals(ULong.EQ(d, c), false);
    assertEquals(ULong.NE(d, c), true);

    assertEquals(ULong.LT(c, d), false);
    assertEquals(ULong.LE(c, d), false);
    assertEquals(ULong.GT(c, d), true);
    assertEquals(ULong.GE(c, d), true);
    assertEquals(ULong.EQ(c, d), false);
    assertEquals(ULong.NE(c, d), true);

    assertEquals(ULong.LT(a, d), true);
    assertEquals(ULong.LE(a, d), true);
    assertEquals(ULong.GT(a, d), false);
    assertEquals(ULong.GE(a, d), false);
    assertEquals(ULong.EQ(a, d), false);
    assertEquals(ULong.NE(a, d), true);

    assertEquals(ULong.LT(d, a), false);
    assertEquals(ULong.LE(d, a), false);
    assertEquals(ULong.GT(d, a), true);
    assertEquals(ULong.GE(d, a), true);
    assertEquals(ULong.EQ(d, a), false);
    assertEquals(ULong.NE(d, a), true);

    assertEquals(ULong.LT(a, a), false);
    assertEquals(ULong.LE(a, a), true);
    assertEquals(ULong.GT(a, a), false);
    assertEquals(ULong.GE(a, a), true);
    assertEquals(ULong.EQ(a, a), true);
    assertEquals(ULong.NE(a, a), false);

    assertEquals(ULong.LT(d, d), false);
    assertEquals(ULong.LE(d, d), true);
    assertEquals(ULong.GT(d, d), false);
    assertEquals(ULong.GE(d, d), true);
    assertEquals(ULong.EQ(d, d), true);
    assertEquals(ULong.NE(d, d), false);

    assertEquals(ULong.LT(0, b), true);
    assertEquals(ULong.LE(0, b), true);
    assertEquals(ULong.GT(0, b), false);
    assertEquals(ULong.GE(0, b), false);
    assertEquals(ULong.EQ(0, b), false);
    assertEquals(ULong.NE(0, b), true);

    assertEquals(ULong.LT(b, 0), false);
    assertEquals(ULong.LE(b, 0), false);
    assertEquals(ULong.GT(b, 0), true);
    assertEquals(ULong.GE(b, 0), true);
    assertEquals(ULong.EQ(b, 0), false);
    assertEquals(ULong.NE(b, 0), true);

    assertEquals(ULong.LT(0, c), true);
    assertEquals(ULong.LE(0, c), true);
    assertEquals(ULong.GT(0, c), false);
    assertEquals(ULong.GE(0, c), false);
    assertEquals(ULong.EQ(0, c), false);
    assertEquals(ULong.NE(0, c), true);

    assertEquals(ULong.LT(c, 0), false);
    assertEquals(ULong.LE(c, 0), false);
    assertEquals(ULong.GT(c, 0), true);
    assertEquals(ULong.GE(c, 0), true);
    assertEquals(ULong.EQ(c, 0), false);
    assertEquals(ULong.NE(c, 0), true);

    assertEquals(ULong.LT(0, 0), false);
    assertEquals(ULong.LE(0, 0), true);
    assertEquals(ULong.GT(0, 0), false);
    assertEquals(ULong.GE(0, 0), true);
    assertEquals(ULong.EQ(0, 0), true);
    assertEquals(ULong.NE(0, 0), false);
  }
}



    
    