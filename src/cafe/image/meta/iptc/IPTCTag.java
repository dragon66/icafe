package cafe.image.meta.iptc;

public interface IPTCTag {
	public int getTag();
	public String getName();
	public boolean allowMultiple();
	public String getDataAsString(byte[] data);
}