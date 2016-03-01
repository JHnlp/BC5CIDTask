package gjh.bc5.mesh;

import java.util.Collections;
import java.util.List;

public class MeSHDiscriptorRecord implements Comparable<MeSHDiscriptorRecord> {
	private String mesh_heading; // MH, this the heading name
	private String record_type;// RECTYPE
	private String descriptor_class;// DC
	private List<String> allowable_qualifier_list;// AQ
	private String annotation; // AN
	private String mesh_scope_note;// MS
	private List<String> semantic_type_list;// ST
	private String unique_identifier;// UI
	private List<String> pharmacological_action_list;// PA
	private List<String> tree_number_list;// MN
	private List<String> entry_names;// ENTRY or PRINT ENTRY

	public MeSHDiscriptorRecord(String mesh_heading, String record_type,
			String descriptor_class, List<String> allowable_qualifier_list,
			String annotation, String mesh_scope_note,
			List<String> semantic_type_list, String unique_identifier,
			List<String> pharmacological_action_list,
			List<String> tree_number_list, List<String> entry_names) {
		super();
		this.mesh_heading = mesh_heading;
		this.record_type = record_type;
		this.descriptor_class = descriptor_class;
		this.allowable_qualifier_list = allowable_qualifier_list;
		this.annotation = annotation;
		this.mesh_scope_note = mesh_scope_note;
		this.semantic_type_list = semantic_type_list;
		this.unique_identifier = unique_identifier;
		this.pharmacological_action_list = pharmacological_action_list;
		this.tree_number_list = tree_number_list;
		this.entry_names = entry_names;
	}

	public String getMeshHeadingName() {
		return mesh_heading;
	}

	public String getRecordType() {
		return record_type;
	}

	public String getDescriptorClass() {
		return descriptor_class;
	}

	public List<String> getAllowableQualifierList() {
		return Collections.unmodifiableList(allowable_qualifier_list);
	}

	public String getAnnotation() {
		return annotation;
	}

	public String getMeshScopeNote() {
		return mesh_scope_note;
	}

	public List<String> getSemanticTypeList() {
		return Collections.unmodifiableList(semantic_type_list);
	}

	public String getUniqueIdentifier() {
		return unique_identifier;
	}

	public List<String> getPharmacologicalActionList() {
		return Collections.unmodifiableList(pharmacological_action_list);
	}

	public List<String> getTreeNumberList() {
		return Collections.unmodifiableList(tree_number_list);
	}

	@Override
	public int compareTo(MeSHDiscriptorRecord o) {
		return this.getUniqueIdentifier().compareTo(o.getUniqueIdentifier());
	}

	@Override
	public String toString() {
		return "MeSHDiscriptorRecord [mesh_heading=" + mesh_heading
				+ ", record_type=" + record_type + ", descriptor_class="
				+ descriptor_class + ", allowable_qualifier_list="
				+ allowable_qualifier_list + ", annotation=" + annotation
				+ ", mesh_scope_note=" + mesh_scope_note
				+ ", semantic_type_list=" + semantic_type_list
				+ ", unique_identifier=" + unique_identifier
				+ ", pharmacological_action_list=" + pharmacological_action_list
				+ ", tree_number_list=" + tree_number_list + ", entry_names="
				+ entry_names + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((allowable_qualifier_list == null) ? 0
				: allowable_qualifier_list.hashCode());
		result = prime * result
				+ ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((descriptor_class == null) ? 0
				: descriptor_class.hashCode());
		result = prime * result
				+ ((entry_names == null) ? 0 : entry_names.hashCode());
		result = prime * result
				+ ((mesh_heading == null) ? 0 : mesh_heading.hashCode());
		result = prime * result
				+ ((mesh_scope_note == null) ? 0 : mesh_scope_note.hashCode());
		result = prime * result + ((pharmacological_action_list == null) ? 0
				: pharmacological_action_list.hashCode());
		result = prime * result
				+ ((record_type == null) ? 0 : record_type.hashCode());
		result = prime * result + ((semantic_type_list == null) ? 0
				: semantic_type_list.hashCode());
		result = prime * result + ((tree_number_list == null) ? 0
				: tree_number_list.hashCode());
		result = prime * result + ((unique_identifier == null) ? 0
				: unique_identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MeSHDiscriptorRecord other = (MeSHDiscriptorRecord) obj;
		if (allowable_qualifier_list == null) {
			if (other.allowable_qualifier_list != null)
				return false;
		} else if (!allowable_qualifier_list
				.equals(other.allowable_qualifier_list))
			return false;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (descriptor_class == null) {
			if (other.descriptor_class != null)
				return false;
		} else if (!descriptor_class.equals(other.descriptor_class))
			return false;
		if (entry_names == null) {
			if (other.entry_names != null)
				return false;
		} else if (!entry_names.equals(other.entry_names))
			return false;
		if (mesh_heading == null) {
			if (other.mesh_heading != null)
				return false;
		} else if (!mesh_heading.equals(other.mesh_heading))
			return false;
		if (mesh_scope_note == null) {
			if (other.mesh_scope_note != null)
				return false;
		} else if (!mesh_scope_note.equals(other.mesh_scope_note))
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
		if (tree_number_list == null) {
			if (other.tree_number_list != null)
				return false;
		} else if (!tree_number_list.equals(other.tree_number_list))
			return false;
		if (unique_identifier == null) {
			if (other.unique_identifier != null)
				return false;
		} else if (!unique_identifier.equals(other.unique_identifier))
			return false;
		return true;
	}

}
