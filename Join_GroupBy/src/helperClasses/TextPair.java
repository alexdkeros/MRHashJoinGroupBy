package helperClasses;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class TextPair implements WritableComparable<TextPair> {

	/*
	 * attributes
	 */
	private Text first;
	private Text second;

	/*
	 * constructors
	 */
	public TextPair() {
		set(new Text(), new Text());
	}

	public TextPair(String first, String second) {
		set(new Text(first), new Text(second));
	}

	public TextPair(Text first, Text second) {
		set(first, second);
	}

	public void set(Text first, Text second) {
		this.first = first;
		this.second = second;
	}
	
	public void set(String first, String second){
		set(new Text(first), new Text(second));
	}

	/*
	 * getters
	 */
	public Text getFirst() {
		return first;
	}

	public Text getSecond() {
		return second;
	}

	/*
	 * @see org.apache.hadoop.io.Writable#write(java.io.DataOutput)
	 */
	@Override
	public void write(DataOutput out) throws IOException {
		first.write(out);
		second.write(out);
	}

	/*
	 * @see org.apache.hadoop.io.Writable#readFields(java.io.DataInput)
	 */
	@Override
	public void readFields(DataInput in) throws IOException {
		first.readFields(in);
		second.readFields(in);
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return first.hashCode() * 163 + second.hashCode();
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof TextPair) {
			TextPair tp = (TextPair) o;
			return first.equals(tp.first) && second.equals(tp.second);
		}
		return false;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return first + "\t" + second;
	}

	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TextPair tp) {
		int cmp = first.compareTo(tp.first);
		if (cmp != 0) {
			return cmp;
		}
		return second.compareTo(tp.second);
	}
}
