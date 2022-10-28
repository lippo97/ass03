package pcd.ass03;

public class Update<A> {
  public int index;
  public int term;
  public A value;

  public Update() { }

  public Update(int index, int term, A value) {
    this.index = index;
    this.term = term;
    this.value = value;
  }
}
