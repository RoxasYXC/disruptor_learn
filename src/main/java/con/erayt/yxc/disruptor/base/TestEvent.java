package con.erayt.yxc.disruptor.base;

public class TestEvent{
	private String name;
	private long id;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "TestEvent [name=" + name + ", id=" + id + "]";
	}
}
