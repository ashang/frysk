package frysk.cli.hpd;

/**
 * A node of a tree representation of a process/thread set in hpdf notation.
 * On creation sets a parent, but when you add it to a real parent, the refence get repointed to that parent,
 * just in case, might change depending on implementation.
 */
public class PTNode
{
	public final static int TYPE_RANGE = 0;
	public final static int TYPE_REG = 1;

	PTNode parent;
	PTNode leftChild;
	PTNode rightChild;
	int id;
	int type;

	public PTNode(PTNode parent, int type)
	{
		this.parent = parent;
		this.id = -1;
		this.type = type;
	}

	public PTNode(PTNode parent, int id, int type)
	{
		this.parent = parent;
		this.id = id;
		this.type = type;
	}

	public PTNode(int type)
	{
		this.parent = null;
		this.id = -1;
		this.type = type;
	}

	public PTNode(int id, int type)
	{
		this.parent = null;
		this.id = id;
		this.type = type;
	}

	public boolean isLeaf()
	{
		boolean result = false;

		if (rightChild == null && leftChild == null)
			result = true;

		return result;
	}

	public void setParent(PTNode parent)
	{
		this.parent = parent;
	}

	public void setLeft(PTNode leftChild)
	{
		if (leftChild != null)
			leftChild.setParent(this);

		this.leftChild = leftChild;
	}

	public void setRight(PTNode rightChild)
	{
		if (rightChild != null)
			rightChild.setParent(this);

		this.rightChild = rightChild;
	}

	public PTNode getParent()
	{
		return parent;
	}

	public PTNode getLeft()
	{
		return leftChild;
	}

	public PTNode getRight()
	{
		return rightChild;
	}

	public int getID()
	{
		return id;
	}

	public int getType()
	{
		return type;
	}
}
