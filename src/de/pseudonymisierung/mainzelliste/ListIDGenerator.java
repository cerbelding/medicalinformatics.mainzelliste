package de.pseudonymisierung.mainzelliste;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class ListIDGenerator implements IDGenerator<ListID> {

    private String idType;
    private Properties props;
    private IDGeneratorMemory mem;
	private List<String> eagerGenRelatedIdTypes;

    @Override
    public void init(IDGeneratorMemory mem, String idType, String[] eagerGenRelatedIdTypes, Properties props) {
        this.mem = mem;
        if(mem.get("counter") == null) {
            mem.set("counter", "0");
        }

        this.idType = idType;
        this.props = props;
		this.eagerGenRelatedIdTypes = Arrays.asList(eagerGenRelatedIdTypes);
    }

    public Properties getProps() {
        return this.props;
    }

    public Set<String> availableIdTypes() {
        Set<String> idTypes = new HashSet<String>();
        for(String idType: this.props.getProperty("idgenerators").split(",")) {
            idTypes.add(idType.trim());
        }
        return idTypes;
    }

    public boolean holdsIdType(String idType) {
        return this.availableIdTypes().contains(idType);
    }

    @Override
    public ListID getNext() {
        int counter = Integer.parseInt(this.mem.get("counter"));
        ListID lid = new ListID(this.getIdType(), "" + counter);
        counter++;
        this.mem.set("counter", "" + counter);
        return lid;
    }

    @Override
    public ListID buildId(String id) {
        return new ListID(this.getIdType(), id);

    }

    @Override
    public boolean verify(String idString) {
        return true;
    }

    @Override
    public String correct(String idString) {
        return "";
    }

    @Override
    public String getIdType() {
        return this.idType;
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    @Override
    public Optional<IDGeneratorMemory> getMemory() {
        return Optional.empty();
    }

	@Override
	public boolean isPersistent() { return true; }

	@Override
	public boolean isEagerGenerationOn(String idType) {
		return eagerGenRelatedIdTypes.contains("*") || eagerGenRelatedIdTypes.contains(idType);
	}
    
}
