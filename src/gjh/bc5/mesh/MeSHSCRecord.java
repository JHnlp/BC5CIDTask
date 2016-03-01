package gjh.bc5.mesh;

import java.util.List;

// MeSH Supplementary Concept Record
public class MeSHSCRecord implements Comparable<MeSHSCRecord> {
	private Integer frequency; // FR, possible null
	private List<String> heading_mapped_to;// HM
	private String indexing_information;// II
	private String name_of_substance;// NM, this the heading name
	private List<String> pharmacological_action_list;// PA
	private String record_type;// RECTYPE
	private List<String> semantic_type_list;// ST
	private List<String> synonyms;// SY
	private String unique_identifier;// UI

	public MeSHSCRecord(Integer frequency, List<String> heading_mapped_to,
			String indexing_information, String name_of_substance,
			List<String> pharmacological_action_list, String record_type,
			List<String> semantic_type_list, List<String> synonyms,
			String unique_identifier) {
		super();
		this.frequency = frequency;
		this.heading_mapped_to = heading_mapped_to;
		this.indexing_information = indexing_information;
		this.name_of_substance = name_of_substance;
		this.pharmacological_action_list = pharmacological_action_list;
		this.record_type = record_type;
		this.semantic_type_list = semantic_type_list;
		this.synonyms = synonyms;
		this.unique_identifier = unique_identifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((frequency == null) ? 0 : frequency.hashCode());
		result = prime * result + ((heading_mapped_to == null) ? 0
				: heading_mapped_to.hashCode());
		result = prime * result + ((indexing_information == null) ? 0
				: indexing_information.hashCode());
		result = prime * result + ((name_of_substance == null) ? 0
				: name_of_substance.hashCode());
		result = prime * result + ((pharmacological_action_list == null) ? 0
				: pharmacological_action_list.hashCode());
		result = prime * result
				+ ((record_type == null) ? 0 : record_type.hashCode());
		result = prime * result + ((semantic_type_list == null) ? 0
				: semantic_type_list.hashCode());
		result = prime * result
				+ ((synonyms == null) ? 0 : synonyms.hashCode());
		result = prime * result + ((unique_identifier == null) ? 0
				: unique_identifier.hashCode());
		return result;
	}

	public Integer getFrequency() {
		return frequency;
	}

	public List<String> getHeadingMappedTo() {
		return heading_mapped_to;
	}

	public String getIndexingInformation() {
		return indexing_information;
	}

	public String getMeshHeadingName() {
		return name_of_substance;
	}

	public List<String> getPharmacologicalActionList() {
		return pharmacological_action_list;
	}

	public String getRecordType() {
		return record_type;
	}

	public List<String> getSemanticTypeList() {
		return semantic_type_list;
	}

	public List<String> getSynonyms() {
		return synonyms;
	}

	public String getUniqueIdentifier() {
		return unique_identifier;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MeSHSCRecord other = (MeSHSCRecord) obj;
		if (frequency == null) {
			if (other.frequency != null)
				return false;
		} else if (!frequency.equals(other.frequency))
			return false;
		if (heading_mapped_to == null) {
			if (other.heading_mapped_to != null)
				return false;
		} else if (!heading_mapped_to.equals(other.heading_mapped_to))
			return false;
		if (indexing_information == null) {
			if (other.indexing_information != null)
				return false;
		} else if (!indexing_information.equals(other.indexing_information))
			return false;
		if (name_of_substance == null) {
			if (other.name_of_substance != null)
				return false;
		} else if (!name_of_substance.equals(other.name_of_substance))
			return false;
		if (pharmacological_action_list == null) {
			if (other.pharmacological_action_list != null)
				return false;
		} else if (!pharmacological_action_list
				.equals(other.pharmacological_action_list))
			return false;
		if (record_type == null) {
			if (other.record_type != null)
				return false;
		} else if (!record_type.equals(other.record_type))
			return false;
		if (semantic_type_list == null) {
			if (other.semantic_type_list != null)
				return false;
		} else if (!semantic_type_list.equals(other.semantic_type_list))
			return false;
		if (synonyms == null) {
			if (other.synonyms != null)
				return false;
		} else if (!synonyms.equals(other.synonyms))
			return false;
		if (unique_identifier == null) {
			if (other.unique_identifier != null)
				return false;
		} else if (!unique_identifier.equals(other.unique_identifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MeSHSCRecord [frequency=" + frequency + ", heading_mapped_to="
				+ heading_mapped_to + ", indexing_information="
				+ indexing_information + ", name_of_substance="
				+ name_of_substance + ", pharmacological_action_list="
				+ pharmacological_action_list + ", record_type=" + record_type
				+ ", semantic_type_list=" + semantic_type_list + ", synonyms="
				+ synonyms + ", unique_identifier=" + unique_identifier + "]";
	}

	@Override
	public int compareTo(MeSHSCRecord o) {
		return this.getUniqueIdentifier().compareTo(o.getUniqueIdentifier());
	}

}
